package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** C2S: requests a full clear of the terminal's crafting grid. */
public record TerminalClearCraftingPayload() implements CustomPacketPayload {
    public static final Type<TerminalClearCraftingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_clear_crafting"));
    // Empty payload: a single unit value is enough on the wire.
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalClearCraftingPayload> STREAM_CODEC =
            StreamCodec.unit(new TerminalClearCraftingPayload());

    @Override
    public Type<TerminalClearCraftingPayload> type() {
        return TYPE;
    }
}
