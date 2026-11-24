package net.interplay.simplestoragemanager.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

import java.util.List;

/** S2C: hands the available dimension list to the selection screen. */
public record DimensionalUpgradeOpenPayload(List<String> dimensions, List<String> selected, boolean offhand)
        implements CustomPacketPayload {
    // Plain UTF-8 string lists; both lists are defensively copied in the compact constructor.
    private static final StreamCodec<ByteBuf, List<String>> STRING_LIST_CODEC =
            ByteBufCodecs.<ByteBuf, String>list().apply(ByteBufCodecs.STRING_UTF8);
    public static final Type<DimensionalUpgradeOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "dimensional_upgrade_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionalUpgradeOpenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    STRING_LIST_CODEC, DimensionalUpgradeOpenPayload::dimensions,
                    STRING_LIST_CODEC, DimensionalUpgradeOpenPayload::selected,
                    ByteBufCodecs.BOOL, DimensionalUpgradeOpenPayload::offhand,
                    DimensionalUpgradeOpenPayload::new);

    public DimensionalUpgradeOpenPayload {
        dimensions = List.copyOf(dimensions);
        selected = List.copyOf(selected);
    }

    @Override
    public Type<DimensionalUpgradeOpenPayload> type() {
        return TYPE;
    }
}
