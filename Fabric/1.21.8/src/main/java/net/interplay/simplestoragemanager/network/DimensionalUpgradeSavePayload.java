package net.interplay.simplestoragemanager.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

import java.util.List;

/** C2S: carries the chosen dimensions back to the server for validation and storage. */
public record DimensionalUpgradeSavePayload(List<String> selected, boolean offhand) implements CustomPacketPayload {
    // Same string-list codec shape as the open payload; the compact constructor freezes the list.
    private static final StreamCodec<ByteBuf, List<String>> STRING_LIST_CODEC =
            ByteBufCodecs.<ByteBuf, String>list().apply(ByteBufCodecs.STRING_UTF8);
    public static final Type<DimensionalUpgradeSavePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "dimensional_upgrade_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionalUpgradeSavePayload> STREAM_CODEC =
            StreamCodec.composite(
                    STRING_LIST_CODEC, DimensionalUpgradeSavePayload::selected,
                    ByteBufCodecs.BOOL, DimensionalUpgradeSavePayload::offhand,
                    DimensionalUpgradeSavePayload::new);

    public DimensionalUpgradeSavePayload {
        selected = List.copyOf(selected);
    }

    @Override
    public Type<DimensionalUpgradeSavePayload> type() {
        return TYPE;
    }
}
