package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** C2S: flips the lock state of the currently connected inventory. */
public record TerminalLockTogglePayload() implements CustomPacketPayload {
    public static final Type<TerminalLockTogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_lock_toggle"));
    // Empty payload encoded as a single unit value.
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalLockTogglePayload> STREAM_CODEC =
            StreamCodec.unit(new TerminalLockTogglePayload());

    @Override
    public Type<TerminalLockTogglePayload> type() {
        return TYPE;
    }
}
