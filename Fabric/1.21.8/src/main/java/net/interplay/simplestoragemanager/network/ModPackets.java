package net.interplay.simplestoragemanager.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.data.TerminalDataComponents;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModMenus;
import net.interplay.simplestoragemanager.logic.UpgradeAccessRules;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.HashSet;
import java.util.Set;

/** Registers every payload codec and the serverbound (C2S) receivers. */
public final class ModPackets {
    private ModPackets() {
    }

    /** Codec registration must happen on both sides before any receiver is registered. */
    public static void registerTypes() {
        // C2S: client requests. Receivers live in registerServerHandlers().
        PayloadTypeRegistry.playC2S().register(DimensionalUpgradeSavePayload.TYPE, DimensionalUpgradeSavePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MenuStateUpdateMessage.TYPE, MenuStateUpdateMessage.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalClearCraftingPayload.TYPE, TerminalClearCraftingPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalFilterSavePayload.TYPE, TerminalFilterSavePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalLockTogglePayload.TYPE, TerminalLockTogglePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalNoticeDismissPayload.TYPE, TerminalNoticeDismissPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalOrganizePayload.TYPE, TerminalOrganizePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalRenamePayload.TYPE, TerminalRenamePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalShiftDragPreviewRequestPayload.TYPE, TerminalShiftDragPreviewRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalStorageClickPayload.TYPE, TerminalStorageClickPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalViewRequestPayload.TYPE, TerminalViewRequestPayload.STREAM_CODEC);

        // S2C: server snapshots/results. Receivers live in ClientPayloadHandlers.
        PayloadTypeRegistry.playS2C().register(DimensionalUpgradeOpenPayload.TYPE, DimensionalUpgradeOpenPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DimensionalUpgradeSaveResultPayload.TYPE, DimensionalUpgradeSaveResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MenuStateUpdateMessage.TYPE, MenuStateUpdateMessage.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TerminalConnectionStatusPayload.TYPE, TerminalConnectionStatusPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TerminalShiftDragPreviewPayload.TYPE, TerminalShiftDragPreviewPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TerminalViewPayload.TYPE, TerminalViewPayload.STREAM_CODEC);
    }

    /** Fabric invokes these callbacks directly on the server thread. */
    public static void registerServerHandlers() {
        // The save request is item-based, not menu-based; validate the upgrade and the selection.
        ServerPlayNetworking.registerGlobalReceiver(DimensionalUpgradeSavePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = player.getItemInHand(hand);
            boolean valid = stack.is(SimplestoragemanagerModItems.DIMENSIONAL_INFINITY_UPGRADE);
            Set<String> registered = new HashSet<>();
            player.registryAccess().lookupOrThrow(Registries.DIMENSION).keySet()
                    .forEach(key -> registered.add(key.toString()));
            valid &= UpgradeAccessRules.isValidSelection(payload.selected(), registered);
            if (valid) {
                stack.set(TerminalDataComponents.DIMENSION_SELECTION, payload.selected());
                player.getInventory().setChanged();
            }
            ServerPlayNetworking.send(player, new DimensionalUpgradeSaveResultPayload(valid));
        });

        // Shared menu-state packet: keep the original 256/8192 length limits on the server too.
        ServerPlayNetworking.registerGlobalReceiver(MenuStateUpdateMessage.TYPE, (payload, context) -> {
            if (payload.name().length() > 256 || payload.elementState() instanceof String string && string.length() > 8192)
                return;
            if (context.player().containerMenu instanceof SimplestoragemanagerModMenus.MenuAccessor menu) {
                menu.getMenuState().put(payload.elementType() + ":" + payload.name(), payload.elementState());
            }
        });

        // All remaining requests require the terminal menu to be open on the server.
        ServerPlayNetworking.registerGlobalReceiver(TerminalClearCraftingPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.clearCraftingGrid(player);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TerminalFilterSavePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.handleFilterSave(player, payload);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TerminalLockTogglePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.toggleInventoryLock(player);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TerminalNoticeDismissPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.dismissNotice(player, payload);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TerminalOrganizePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.handleOrganize(player, payload.section(), payload.option());
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TerminalRenamePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.handleRename(player, payload);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TerminalShiftDragPreviewRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.handleShiftDragPreview(player, payload);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TerminalStorageClickPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.handleStorageClick(player, payload);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TerminalViewRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.handleViewRequest(payload);
            }
        });
    }
}
