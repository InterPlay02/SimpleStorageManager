package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
public record TerminalViewRequestPayload(String search, int scrollOffset, boolean big) implements CustomPacketPayload {
    public static final Type<TerminalViewRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_view_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalViewRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TerminalViewRequestPayload::search,
            ByteBufCodecs.VAR_INT, TerminalViewRequestPayload::scrollOffset,
            ByteBufCodecs.BOOL, TerminalViewRequestPayload::big,
            TerminalViewRequestPayload::new);

    @Override public Type<TerminalViewRequestPayload> type() { return TYPE; }

    public static void handle(TerminalViewRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof StorageManagerGUIMenu menu) {
            context.enqueueWork(() -> menu.handleViewRequest(payload));
        }
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalViewRequestPayload::handle);
    }
}
