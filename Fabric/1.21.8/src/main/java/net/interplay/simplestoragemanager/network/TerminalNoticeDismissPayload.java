package net.interplay.simplestoragemanager.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** C2S: dismisses a stored "container moved" notice identified by dimension and position. */
public record TerminalNoticeDismissPayload(String dimension, BlockPos pos) implements CustomPacketPayload {
    public static final Type<TerminalNoticeDismissPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_notice_dismiss"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalNoticeDismissPayload> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, TerminalNoticeDismissPayload::dimension,
            BlockPos.STREAM_CODEC, TerminalNoticeDismissPayload::pos,
            TerminalNoticeDismissPayload::new);

    @Override public Type<TerminalNoticeDismissPayload> type() { return TYPE; }
}
