package net.interplay.simplestoragemanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

@EventBusSubscriber
public record TerminalConnectionStatusPayload(int section, int status, Component label,
                                             List<TerminalViewPayload.VisibleSlot> visibleSlots,
                                             boolean refreshView)
        implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, List<TerminalViewPayload.VisibleSlot>> VISIBLE_SLOTS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, TerminalViewPayload.VisibleSlot>list()
                    .apply(TerminalViewPayload.VISIBLE_SLOT_CODEC);
    public static final Type<TerminalConnectionStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_connection_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalConnectionStatusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalConnectionStatusPayload::section,
                    ByteBufCodecs.VAR_INT, TerminalConnectionStatusPayload::status,
                    ComponentSerialization.STREAM_CODEC, TerminalConnectionStatusPayload::label,
                    VISIBLE_SLOTS_CODEC, TerminalConnectionStatusPayload::visibleSlots,
                    ByteBufCodecs.BOOL, TerminalConnectionStatusPayload::refreshView,
                    TerminalConnectionStatusPayload::new);

    public TerminalConnectionStatusPayload {
        visibleSlots = List.copyOf(visibleSlots);
    }

    @Override
    public Type<TerminalConnectionStatusPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalConnectionStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() != PacketFlow.CLIENTBOUND) return;
            if (context.player().containerMenu instanceof StorageManagerGUIMenu menu) {
                menu.applyConnectionStatus(payload.section(), payload.status(), payload.label());
            }
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof StorageManagerGUIScreen screen) {
                screen.applyConnectionStatus(payload);
            }
        });
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalConnectionStatusPayload::handle);
    }
}
