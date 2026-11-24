package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** C2S: refresh request carrying the current search, scroll position and size choice. */
public record TerminalViewRequestPayload(String search, int scrollOffset, boolean big) implements CustomPacketPayload {
    public static final Type<TerminalViewRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_view_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalViewRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TerminalViewRequestPayload::search,
            ByteBufCodecs.VAR_INT, TerminalViewRequestPayload::scrollOffset,
            ByteBufCodecs.BOOL, TerminalViewRequestPayload::big,
            TerminalViewRequestPayload::new);

    @Override public Type<TerminalViewRequestPayload> type() { return TYPE; }
}
