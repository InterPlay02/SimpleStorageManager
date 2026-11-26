package net.interplay.simplestoragemanager.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.interplay.simplestoragemanager.ClientBridge;
import net.interplay.simplestoragemanager.network.ClientPayloadHandlers;
import net.interplay.simplestoragemanager.config.SimplestoragemanagerConfig;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModScreens;
import net.interplay.simplestoragemanager.item.UpgradeTooltip;
import net.interplay.simplestoragemanager.network.ServerConfigSyncPayload;

/** Client entrypoint: screens, S2C receivers and client-only hooks. */
public final class SimpleStorageManagerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimplestoragemanagerModScreens.register();
        ClientPayloadHandlers.register();
        ClientBridgeSetup.register();
        UpgradeTooltip.register();

        ClientBridge.menuStateSender = ClientPlayNetworking::send;
        ClientBridge.configHandler = payload ->
                SimplestoragemanagerConfig.setSyncedScrollSpeed(payload.scrollSpeed());
        ClientPlayNetworking.registerGlobalReceiver(ServerConfigSyncPayload.TYPE, (payload, context) ->
                ClientBridge.configHandler.accept(payload));
    }
}
