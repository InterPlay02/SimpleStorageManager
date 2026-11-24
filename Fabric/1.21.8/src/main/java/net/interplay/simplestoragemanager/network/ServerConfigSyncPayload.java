package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** Sends the server-authoritative scroll speed to a joining client. */
public record ServerConfigSyncPayload(int scrollSpeed) implements CustomPacketPayload {
    public static final Type<ServerConfigSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "server_config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerConfigSyncPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ServerConfigSyncPayload::scrollSpeed,
                    ServerConfigSyncPayload::new);

    @Override
    public Type<ServerConfigSyncPayload> type() {
        return TYPE;
    }
}
