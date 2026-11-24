package net.interplay.simplestoragemanager.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.interplay.simplestoragemanager.ClientBridge;
import net.interplay.simplestoragemanager.client.gui.DimensionalUpgradeSelectionScreen;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModMenus;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

/** Registers the clientbound (S2C) receivers. All screen access stays in the client source set. */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    /** Fabric invokes these callbacks on the client/render thread. */
    public static void register() {
        // Opening the dimension picker is a pure screen operation.
        ClientPlayNetworking.registerGlobalReceiver(DimensionalUpgradeOpenPayload.TYPE, (payload, context) ->
                context.client().setScreen(new DimensionalUpgradeSelectionScreen(
                        payload.dimensions(), payload.selected(), payload.offhand())));

        ClientPlayNetworking.registerGlobalReceiver(DimensionalUpgradeSaveResultPayload.TYPE, (payload, context) -> {
            if (context.client().screen instanceof DimensionalUpgradeSelectionScreen screen) {
                screen.handleSaveResult(payload.success());
            }
        });

        // Bidirectional packet: client side keeps the original 256/8192 limits, routes the
        // payload to the bridge (which forwards it to the open screen) and updates the menu state.
        ClientPlayNetworking.registerGlobalReceiver(MenuStateUpdateMessage.TYPE, (payload, context) -> {
            if (payload.name().length() > 256 || payload.elementState() instanceof String string && string.length() > 8192)
                return;
            ClientBridge.menuStateHandler.accept(payload);
            if (context.player().containerMenu instanceof SimplestoragemanagerModMenus.MenuAccessor menu) {
                menu.getMenuState().put(payload.elementType() + ":" + payload.name(), payload.elementState());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(TerminalConnectionStatusPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.applyConnectionStatus(payload.section(), payload.status(), payload.label());
            }
            if (context.client().screen instanceof StorageManagerGUIScreen screen) {
                screen.applyConnectionStatus(payload);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(TerminalShiftDragPreviewPayload.TYPE, (payload, context) -> {
            if (context.client().screen instanceof StorageManagerGUIScreen screen) {
                screen.applyShiftDragPreview(payload);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(TerminalViewPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.applyView(payload);
            }
            if (context.client().screen instanceof StorageManagerGUIScreen screen) {
                screen.applyView(payload);
            }
        });
    }
}
