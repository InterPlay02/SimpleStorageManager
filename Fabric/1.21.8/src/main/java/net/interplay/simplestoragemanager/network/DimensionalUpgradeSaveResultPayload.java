package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

/** S2C: tells the selection screen whether the server accepted the saved dimensions. */
public record DimensionalUpgradeSaveResultPayload(boolean success) implements CustomPacketPayload {
    public static final Type<DimensionalUpgradeSaveResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "dimensional_upgrade_save_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionalUpgradeSaveResultPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, DimensionalUpgradeSaveResultPayload::success,
                    DimensionalUpgradeSaveResultPayload::new);

    @Override
    public Type<DimensionalUpgradeSaveResultPayload> type() {
        return TYPE;
    }
}
