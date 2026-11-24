package net.interplay.simplestoragemanager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.interplay.simplestoragemanager.config.SimplestoragemanagerConfig;
import net.interplay.simplestoragemanager.data.TerminalDataComponents;
import net.interplay.simplestoragemanager.event.TerminalInteractionEvents;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModMenus;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModRecipes;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModTabs;
import net.interplay.simplestoragemanager.network.ModPackets;
import net.interplay.simplestoragemanager.network.ServerConfigSyncPayload;
import net.interplay.simplestoragemanager.world.TerminalOriginMemory;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Common bootstrap: registration, networking, config and server callbacks. */
public final class SimplestoragemanagerMod {
    public static final String MODID = "simplestoragemanager";
    public static final Logger LOGGER = LogManager.getLogger("SimpleStorageManager");

    private SimplestoragemanagerMod() { }

    public static void init() {
        SimplestoragemanagerModItems.register();
        SimplestoragemanagerModRecipes.register();
        TerminalDataComponents.register();
        SimplestoragemanagerModTabs.register();
        SimplestoragemanagerModMenus.register();
        SimplestoragemanagerConfig.load();

        ModPackets.registerTypes();
        ModPackets.registerServerHandlers();
        PayloadTypeRegistry.playS2C().register(ServerConfigSyncPayload.TYPE, ServerConfigSyncPayload.STREAM_CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayNetworking.send(handler.getPlayer(),
                    new ServerConfigSyncPayload(SimplestoragemanagerConfig.scrollSpeed()));
        });

        // Connection availability is revalidated without ever loading chunks.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
                    menu.tickConnectionAvailability();
                }
            }
        });

        TerminalInteractionEvents.register();
        TerminalOriginMemory.register();
    }
}
