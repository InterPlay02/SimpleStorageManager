package net.interplay.simplestoragemanager.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber
public record TerminalNoticeDismissPayload(String dimension, BlockPos pos) implements CustomPacketPayload {
    public static final Type<TerminalNoticeDismissPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_notice_dismiss"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalNoticeDismissPayload> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, TerminalNoticeDismissPayload::dimension,
            BlockPos.STREAM_CODEC, TerminalNoticeDismissPayload::pos,
            TerminalNoticeDismissPayload::new);

    @Override public Type<TerminalNoticeDismissPayload> type() { return TYPE; }

    public static void handle(TerminalNoticeDismissPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof StorageManagerGUIMenu menu) {
            context.enqueueWork(() -> menu.dismissNotice(player, payload));
        }
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalNoticeDismissPayload::handle);
    }
}
