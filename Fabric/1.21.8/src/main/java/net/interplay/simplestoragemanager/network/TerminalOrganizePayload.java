package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** Client request to auto-organize one connected inventory section. */
public record TerminalOrganizePayload(int section, int option) implements CustomPacketPayload {
    public static final Type<TerminalOrganizePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_organize"));

    // Manual codec keeps the exact varint layout used before the port.
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalOrganizePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.section());
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.option());
                    },
                    buffer -> new TerminalOrganizePayload(
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer)));

    @Override
    public Type<TerminalOrganizePayload> type() {
        return TYPE;
    }
}
