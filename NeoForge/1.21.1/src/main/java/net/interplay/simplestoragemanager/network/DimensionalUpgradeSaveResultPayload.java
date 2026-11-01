package net.interplay.simplestoragemanager.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.client.gui.DimensionalUpgradeSelectionScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber
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

    public static void handle(DimensionalUpgradeSaveResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.CLIENTBOUND
                    && Minecraft.getInstance().screen instanceof DimensionalUpgradeSelectionScreen screen) {
                screen.handleSaveResult(payload.success());
            }
        });
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, DimensionalUpgradeSaveResultPayload::handle);
    }
}
