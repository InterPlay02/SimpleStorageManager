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

import java.util.List;

@EventBusSubscriber
public record TerminalShiftDragPreviewRequestPayload(long requestId, List<Integer> menuSlots)
        implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> INTEGER_CODEC = StreamCodec.of(
            (buffer, value) -> ByteBufCodecs.VAR_INT.encode(buffer, value),
            ByteBufCodecs.VAR_INT::decode);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Integer>> SLOTS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Integer>list().apply(INTEGER_CODEC);
    public static final Type<TerminalShiftDragPreviewRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_shift_drag_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalShiftDragPreviewRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, TerminalShiftDragPreviewRequestPayload::requestId,
                    SLOTS_CODEC, TerminalShiftDragPreviewRequestPayload::menuSlots,
                    TerminalShiftDragPreviewRequestPayload::new);

    public TerminalShiftDragPreviewRequestPayload {
        menuSlots = List.copyOf(menuSlots);
    }

    @Override
    public Type<TerminalShiftDragPreviewRequestPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalShiftDragPreviewRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof StorageManagerGUIMenu menu) {
            context.enqueueWork(() -> menu.handleShiftDragPreview(player, payload));
        }
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalShiftDragPreviewRequestPayload::handle);
    }
}
