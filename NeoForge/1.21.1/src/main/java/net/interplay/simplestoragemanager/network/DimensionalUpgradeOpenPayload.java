package net.interplay.simplestoragemanager.network;

import net.minecraft.client.Minecraft;
import io.netty.buffer.ByteBuf;
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

import java.util.List;

@EventBusSubscriber
public record DimensionalUpgradeOpenPayload(List<String> dimensions, List<String> selected, boolean offhand)
        implements CustomPacketPayload {
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

    public static void handle(DimensionalUpgradeOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                Minecraft.getInstance().setScreen(
                        new DimensionalUpgradeSelectionScreen(payload.dimensions(), payload.selected(), payload.offhand()));
            }
        });
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, DimensionalUpgradeOpenPayload::handle);
    }
}
