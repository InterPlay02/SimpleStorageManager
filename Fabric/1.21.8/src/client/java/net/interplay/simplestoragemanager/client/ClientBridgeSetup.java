package net.interplay.simplestoragemanager.client;

import net.minecraft.client.Minecraft;

import net.interplay.simplestoragemanager.ClientBridge;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModScreens;

/** Liga o recebimento de MenuStateUpdateMessage à tela de terminal atualmente aberta. */
public final class ClientBridgeSetup {
    private ClientBridgeSetup() {
    }

    public static void register() {
        // O bridge no source set main delega aqui; o cliente aplica o estado na tela visível.
        ClientBridge.menuStateHandler = payload -> {
            if (Minecraft.getInstance().screen instanceof SimplestoragemanagerModScreens.ScreenAccessor accessor) {
                accessor.updateMenuState(payload.elementType(), payload.name(), payload.elementState());
            }
        };
    }
}
