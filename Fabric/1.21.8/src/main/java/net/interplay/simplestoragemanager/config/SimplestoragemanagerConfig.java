package net.interplay.simplestoragemanager.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small JSON config replacing the NeoForge TOML spec. Values keep the same
 * names and semantics; scrollSpeed is synced to clients from the server.
 */
public final class SimplestoragemanagerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("simplestoragemanager-server.json");

    private static boolean protectConnectedInventories = false;
    private static int scrollSpeed = 27;
    private static int syncedScrollSpeed = 27;

    private SimplestoragemanagerConfig() { }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(PATH, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            protectConnectedInventories = root.has("protectConnectedInventories")
                    && root.get("protectConnectedInventories").getAsBoolean();
            scrollSpeed = clampScrollSpeed(root.has("scrollSpeed")
                    ? root.get("scrollSpeed").getAsInt() : 27);
            syncedScrollSpeed = scrollSpeed;
        } catch (IOException | RuntimeException e) {
            SimplestoragemanagerMod.LOGGER.warn("Could not read SSM config, using defaults: {}", e.getMessage());
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("protectConnectedInventories", protectConnectedInventories);
        root.addProperty("scrollSpeed", scrollSpeed);
        try {
            Files.writeString(PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SimplestoragemanagerMod.LOGGER.warn("Could not save SSM config: {}", e.getMessage());
        }
    }

    private static int clampScrollSpeed(int value) {
        return Math.max(1, Math.min(500, value));
    }

    /** Server-side protection of connected inventories. */
    public static boolean protectConnectedInventories() {
        return protectConnectedInventories;
    }

    /** Configured scroll speed on the logical server. */
    public static int scrollSpeed() {
        return scrollSpeed;
    }

    /** Last value received from the server, used by the client GUI. */
    public static int syncedScrollSpeed() {
        return syncedScrollSpeed;
    }

    public static void setSyncedScrollSpeed(int value) {
        syncedScrollSpeed = clampScrollSpeed(value);
    }
}
