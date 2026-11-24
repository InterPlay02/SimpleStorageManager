package net.interplay.simplestoragemanager;

import net.interplay.simplestoragemanager.network.MenuStateUpdateMessage;
import net.interplay.simplestoragemanager.network.ServerConfigSyncPayload;

import java.util.function.Consumer;

/**
 * Bridges common code to client-only handlers without importing client classes.
 * The client initializer installs the real consumers on a Minecraft client.
 */
public final class ClientBridge {
    public static Consumer<MenuStateUpdateMessage> menuStateHandler = message -> { };
    public static Consumer<MenuStateUpdateMessage> menuStateSender = message -> { };
    public static Consumer<ServerConfigSyncPayload> configHandler = message -> { };

    private ClientBridge() { }
}
