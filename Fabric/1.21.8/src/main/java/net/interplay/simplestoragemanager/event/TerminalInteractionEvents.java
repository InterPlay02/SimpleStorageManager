package net.interplay.simplestoragemanager.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.access.AccessRule;
import net.interplay.simplestoragemanager.access.InventoryAdapter;
import net.interplay.simplestoragemanager.access.MultiblockDiscovery;
import net.interplay.simplestoragemanager.config.SimplestoragemanagerConfig;
import net.interplay.simplestoragemanager.data.TerminalConnection;
import net.interplay.simplestoragemanager.data.TerminalDataComponents;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.logic.TerminalOwnershipRules;
import net.interplay.simplestoragemanager.network.DimensionalUpgradeOpenPayload;
import net.interplay.simplestoragemanager.world.TerminalSavedData;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Terminal use, connection toggles and block-break tracking. */
public final class TerminalInteractionEvents {
    private TerminalInteractionEvents() { }

    public static void register() {
        UseBlockCallback.EVENT.register(TerminalInteractionEvents::rightClickBlock);
        UseItemCallback.EVENT.register(TerminalInteractionEvents::rightClickItem);
        PlayerBlockBreakEvents.BEFORE.register(TerminalInteractionEvents::beforeBreak);
    }

    private static InteractionResult rightClickBlock(net.minecraft.world.entity.player.Player player, Level level,
                                                     InteractionHand hand,
                                                     net.minecraft.world.phys.BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        Direction side = hit.getDirection();
        if (stack.is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE)) {
            if (InventoryAdapter.findStorage(level instanceof ServerLevel server ? server : null, hit.getBlockPos(), side) != null) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;
            openDimensionSelector((ServerPlayer) player, hand);
            return InteractionResult.SUCCESS;
        }
        if (!stack.is(SimplestoragemanagerModItems.STORAGE_MANAGER)) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) {
            if (hasStorage(level, hit.getBlockPos(), side)) return InteractionResult.PASS;
            if (!level.isClientSide) openTerminal((ServerPlayer) player, hand);
            return InteractionResult.SUCCESS;
        }

        // Shift-click always consumes the interaction, even when the connection fails.
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (!hasStorage(level, hit.getBlockPos(), side)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.no_inventory"));
            return InteractionResult.SUCCESS;
        }

        UUID id = ensureTerminalId(serverPlayer, hand);
        TerminalSavedData data = TerminalSavedData.get(serverPlayer.level());
        UUID owner = data.owner(id);
        if (!TerminalOwnershipRules.canConnectInventory(owner, serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.terminal_not_owner_connection"));
            return InteractionResult.SUCCESS;
        }

        List<TerminalConnection> connections = new ArrayList<>(data.connections(id));
        int existing = -1;
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).containsBlock(level.dimension(), hit.getBlockPos())) {
                existing = i;
                break;
            }
        }
        if (existing >= 0) {
            TerminalConnection removed = connections.remove(existing);
            TerminalSavedData.get(serverPlayer.level()).clearCustomName(removed.connectionId());
            data.putConnections(id, connections);
            serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.disconnected",
                    hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ()));
            refreshOpenMenu(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        MultiblockDiscovery.Result discovery = MultiblockDiscovery.discover(serverPlayer.level(), hit.getBlockPos(), side);
        if (discovery.connection() == null) {
            serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.no_inventory"));
            return InteractionResult.SUCCESS;
        }
        if (discovery.exceedsBounds()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.multiblock_too_large"));
            return InteractionResult.SUCCESS;
        }
        if (SimplestoragemanagerConfig.protectConnectedInventories()
                && data.isConnectedToOtherTerminal(serverPlayer.level(), id, discovery.connection())) {
            serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.inventory_already_connected"));
            return InteractionResult.SUCCESS;
        }
        if (!data.claimOwner(id, serverPlayer.getUUID())) {
            serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.terminal_not_owner_connection"));
            return InteractionResult.SUCCESS;
        }

        TerminalConnection newConnection = discovery.connection();
        InventoryAdapter.Resolution initial = InventoryAdapter.resolve(
                serverPlayer.level(), newConnection, serverPlayer, AccessRule.ALWAYS);
        if (initial.adapter() != null) {
            newConnection = newConnection.withImplementationKind(initial.adapter().implementationKind());
        }
        connections.add(newConnection);
        TerminalSavedData.get(serverPlayer.level()).clearCustomName(newConnection.connectionId());
        data.putConnections(id, connections);
        serverPlayer.sendSystemMessage(Component.translatable("message.simplestoragemanager.connected",
                hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ()));
        refreshOpenMenu(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    private static boolean hasStorage(Level level, net.minecraft.core.BlockPos pos, Direction side) {
        return InventoryAdapter.findStorage(level instanceof ServerLevel server ? server : null, pos, side) != null;
    }

    private static InteractionResult rightClickItem(net.minecraft.world.entity.player.Player player,
                                                                     Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE)) {
            if (!level.isClientSide) openDimensionSelector((ServerPlayer) player, hand);
            return InteractionResult.SUCCESS;
        }
        if (!stack.is(SimplestoragemanagerModItems.STORAGE_MANAGER)) return InteractionResult.PASS;
        if (!level.isClientSide) openTerminal((ServerPlayer) player, hand);
        return InteractionResult.SUCCESS;
    }

    private static boolean beforeBreak(Level level, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                                       net.minecraft.world.level.block.entity.BlockEntity entity) {
        if (!level.isClientSide && level instanceof ServerLevel server) {
            TerminalSavedData.get(server).markBroken(server.dimension(), pos);
        }
        return true;
    }

    public static void openTerminal(ServerPlayer player, InteractionHand hand) {
        ensureTerminalId(player, hand);
        // The hand is synced as an extra screen-opening byte: 0 = main hand, 1 = off hand.
        player.openMenu(new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<Byte>() {
            @Override public Component getDisplayName() {
                return Component.translatable("gui.simplestoragemanager.storage_manager_gui.label_terminal");
            }

            @Override public Byte getScreenOpeningData(ServerPlayer player) {
                return (byte) (hand == InteractionHand.MAIN_HAND ? 0 : 1);
            }

            @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
                    net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player ignored) {
                return new StorageManagerGUIMenu(id, inventory, hand);
            }
        });
    }

    private static void openDimensionSelector(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE)) return;
        List<String> dimensions = player.registryAccess().lookupOrThrow(Registries.DIMENSION).keySet()
                .stream().map(Object::toString).sorted().toList();
        Set<String> known = Set.copyOf(dimensions);
        List<String> selected = stack.getOrDefault(TerminalDataComponents.DIMENSION_SELECTION, List.<String>of())
                .stream().filter(known::contains).distinct().limit(5).toList();
        ServerPlayNetworking.send(player,
                new DimensionalUpgradeOpenPayload(dimensions, selected, hand == InteractionHand.OFF_HAND));
    }

    public static UUID ensureTerminalId(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        UUID id = stack.get(TerminalDataComponents.TERMINAL_ID);
        if (id == null) {
            id = UUID.randomUUID();
            stack.set(TerminalDataComponents.TERMINAL_ID, id);
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
