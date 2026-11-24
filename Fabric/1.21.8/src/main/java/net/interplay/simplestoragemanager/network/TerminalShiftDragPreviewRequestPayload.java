package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

import java.util.List;

/** C2S: asks the server to plan a shift-drag for the given menu slots. */
public record TerminalShiftDragPreviewRequestPayload(long requestId, List<Integer> menuSlots)
        implements CustomPacketPayload {
    // Explicit varint wrapper so the list layout stays identical to the original.
    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> INTEGER_CODEC = StreamCodec.of(
            (buffer, value) -> ByteBufCodecs.VAR_INT.encode(buffer, value),
            ByteBufCodecs.VAR_INT::decode);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Integer>> SLOTS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Integer>list().apply(INTEGER_CODEC);
    public static final Type<TerminalShiftDragPreviewRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_shift_drag_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalShiftDragPreviewRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, TerminalShiftDragPreviewRequestPayload::requestId,
                    SLOTS_CODEC, TerminalShiftDragPreviewRequestPayload::menuSlots,
                    TerminalShiftDragPreviewRequestPayload::new);

    public TerminalShiftDragPreviewRequestPayload {
        menuSlots = List.copyOf(menuSlots);
    }

    @Override
    public Type<TerminalShiftDragPreviewRequestPayload> type() {
        return TYPE;
    }
}
