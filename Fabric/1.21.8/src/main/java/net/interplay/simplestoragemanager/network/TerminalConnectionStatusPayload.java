package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

import java.util.List;

/** S2C: connection state for one section plus an optional view refresh. */
public record TerminalConnectionStatusPayload(int section, int status, Component label,
                                              List<TerminalViewPayload.VisibleSlot> visibleSlots,
                                              boolean refreshView)
        implements CustomPacketPayload {
    // Reuses the visible-slot codec declared by TerminalViewPayload so both packets stay identical.
    private static final StreamCodec<RegistryFriendlyByteBuf, List<TerminalViewPayload.VisibleSlot>> VISIBLE_SLOTS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, TerminalViewPayload.VisibleSlot>list()
                    .apply(TerminalViewPayload.VISIBLE_SLOT_CODEC);
    public static final Type<TerminalConnectionStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_connection_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalConnectionStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalConnectionStatusPayload::section,
                    ByteBufCodecs.VAR_INT, TerminalConnectionStatusPayload::status,
                    ComponentSerialization.STREAM_CODEC, TerminalConnectionStatusPayload::label,
                    VISIBLE_SLOTS_CODEC, TerminalConnectionStatusPayload::visibleSlots,
                    ByteBufCodecs.BOOL, TerminalConnectionStatusPayload::refreshView,
                    TerminalConnectionStatusPayload::new);

    public TerminalConnectionStatusPayload {
        visibleSlots = List.copyOf(visibleSlots);
    }

    @Override
    public Type<TerminalConnectionStatusPayload> type() {
        return TYPE;
    }
}
