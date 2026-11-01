package net.interplay.simplestoragemanager.event;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.access.MultiblockDiscovery;
import net.interplay.simplestoragemanager.data.TerminalConnection;
import net.interplay.simplestoragemanager.data.TerminalDataComponents;
import net.interplay.simplestoragemanager.config.SimplestoragemanagerConfig;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.logic.TerminalOwnershipRules;
import net.interplay.simplestoragemanager.network.DimensionalUpgradeOpenPayload;
import net.interplay.simplestoragemanager.world.TerminalSavedData;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Handles terminal use, inventory connection toggles and block-break tracking. */
@EventBusSubscriber(modid = SimplestoragemanagerMod.MODID)
public final class TerminalInteractionEvents {
    private TerminalInteractionEvents() { }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE.get())) {
            if (event.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, event.getPos(), event.getFace()) != null) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) {
                openDimensionSelector(player, event.getHand());
            }
            return;
        }
        if (!stack.is(SimplestoragemanagerModItems.STORAGE_MANAGER.get())) return;
        Level level = event.getLevel();
        Direction side = event.getFace();
        if (!event.getEntity().isShiftKeyDown()) {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, event.getPos(), side) == null) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (!level.isClientSide && event.getEntity() instanceof ServerPlayer player) openTerminal(player, event.getHand());
            }
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, event.getPos(), side) == null) {
            player.sendSystemMessage(Component.translatable("message.simplestoragemanager.no_inventory"));
            return;
        }

        UUID id = ensureTerminalId(player, event.getHand());
        TerminalSavedData data = TerminalSavedData.get(player.serverLevel());
        UUID owner = data.owner(id);
        if (!TerminalOwnershipRules.canConnectInventory(owner, player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.simplestoragemanager.terminal_not_owner_connection"));
            return;
        }
        List<TerminalConnection> connections = new ArrayList<>(data.connections(id));
        int existing = -1;
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).containsBlock(level.dimension(), event.getPos())) {
                existing = i;
                break;
            }
        }
        if (existing >= 0) {
            connections.remove(existing);
            data.putConnections(id, connections);
            player.sendSystemMessage(Component.translatable("message.simplestoragemanager.disconnected",
                    event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()));
            refreshOpenMenu(player);
            return;
        }

        MultiblockDiscovery.Result discovery = MultiblockDiscovery.discover(player.serverLevel(), event.getPos(), side);
        if (discovery.connection() == null) {
            player.sendSystemMessage(Component.translatable("message.simplestoragemanager.no_inventory"));
            return;
        }
        if (discovery.exceedsBounds()) {
            player.sendSystemMessage(Component.translatable("message.simplestoragemanager.multiblock_too_large"));
            return;
        }
        if (SimplestoragemanagerConfig.PROTECT_CONNECTED_INVENTORIES.get()
                && data.isConnectedToOtherTerminal(player.serverLevel(), id, discovery.connection())) {
            player.sendSystemMessage(Component.translatable("message.simplestoragemanager.inventory_already_connected"));
            return;
        }
        if (!data.claimOwner(id, player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.simplestoragemanager.terminal_not_owner_connection"));
            return;
        }
        connections.add(discovery.connection());
        data.putConnections(id, connections);
        player.sendSystemMessage(Component.translatable("message.simplestoragemanager.connected",
                event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()));
        refreshOpenMenu(player);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) {
                openDimensionSelector(player, event.getHand());
            }
            return;
        }
        if (!event.getItemStack().is(SimplestoragemanagerModItems.STORAGE_MANAGER.get())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) openTerminal(player, event.getHand());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            TerminalSavedData.get(level).markBroken(level.dimension(), event.getPos());
        }
    }

    public static void openTerminal(ServerPlayer player, InteractionHand hand) {
        ensureTerminalId(player, hand);
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("gui.simplestoragemanager.storage_manager_gui.label_terminal");
            }

            @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
                    net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player ignored) {
                return new StorageManagerGUIMenu(id, inventory, hand);
            }
        }, buffer -> buffer.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1));
    }

    private static void openDimensionSelector(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE.get())) return;
        List<String> dimensions = player.registryAccess().registryOrThrow(Registries.DIMENSION).keySet()
                .stream().map(Object::toString).sorted().toList();
        Set<String> known = Set.copyOf(dimensions);
        List<String> selected = stack.getOrDefault(TerminalDataComponents.DIMENSION_SELECTION.get(), List.<String>of())
                .stream().filter(known::contains).distinct().limit(5).toList();
        PacketDistributor.sendToPlayer(player,
                new DimensionalUpgradeOpenPayload(dimensions, selected, hand == InteractionHand.OFF_HAND));
    }

    public static UUID ensureTerminalId(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        UUID id = stack.get(TerminalDataComponents.TERMINAL_ID.get());
        if (id == null) {
            id = UUID.randomUUID();
            stack.set(TerminalDataComponents.TERMINAL_ID.get(), id);
            player.getInventory().setChanged();
        }
        return id;
    }

    public static void refreshOpenMenu(ServerPlayer player) {
        if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
            menu.refreshServerView();
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }
}
