package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** C2S: renames one connected inventory section. */
public record TerminalRenamePayload(int section, String name) implements CustomPacketPayload {
    public static final Type<TerminalRenamePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_rename"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalRenamePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalRenamePayload::section,
                    ByteBufCodecs.STRING_UTF8, TerminalRenamePayload::name,
                    TerminalRenamePayload::new);

    @Override
    public Type<TerminalRenamePayload> type() {
        return TYPE;
    }
}
