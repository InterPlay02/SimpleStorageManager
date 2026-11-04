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

/** Carries click and drag targets to the server for authoritative inventory mutation. */
@EventBusSubscriber
public record TerminalStorageClickPayload(int section, int slot, int button, boolean shift,
                                          List<SlotTarget> dragTargets, List<Integer> menuDragSlots,
                                          long operationSequence, long shiftDragPreviewRequestId)
        implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, SlotTarget> SLOT_TARGET_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlotTarget::section,
            ByteBufCodecs.VAR_INT, SlotTarget::slot,
            SlotTarget::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<SlotTarget>> DRAG_TARGETS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, SlotTarget>list().apply(SLOT_TARGET_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT_CODEC = StreamCodec.of(
            (buffer, value) -> ByteBufCodecs.VAR_INT.encode(buffer, value),
            buffer -> ByteBufCodecs.VAR_INT.decode(buffer));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Integer>> MENU_DRAG_SLOTS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Integer>list().apply(INT_CODEC);
    public static final Type<TerminalStorageClickPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_storage_click"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStorageClickPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.section());
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.slot());
                        ByteBufCodecs.VAR_INT.encode(buffer, payload.button());
                        ByteBufCodecs.BOOL.encode(buffer, payload.shift());
                        DRAG_TARGETS_CODEC.encode(buffer, payload.dragTargets());
                        MENU_DRAG_SLOTS_CODEC.encode(buffer, payload.menuDragSlots());
                        ByteBufCodecs.VAR_LONG.encode(buffer, payload.operationSequence());
                        ByteBufCodecs.VAR_LONG.encode(buffer, payload.shiftDragPreviewRequestId());
                    },
                    buffer -> new TerminalStorageClickPayload(
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.BOOL.decode(buffer),
                            DRAG_TARGETS_CODEC.decode(buffer),
                            MENU_DRAG_SLOTS_CODEC.decode(buffer),
                            ByteBufCodecs.VAR_LONG.decode(buffer),
                            ByteBufCodecs.VAR_LONG.decode(buffer)));

    public TerminalStorageClickPayload(int section, int slot, int button, boolean shift) {
        this(section, slot, button, shift, List.of(), List.of(), 0, 0);
    }

    public TerminalStorageClickPayload(int section, int slot, int button, boolean shift, List<SlotTarget> dragTargets) {
        this(section, slot, button, shift, dragTargets, List.of(), 0, 0);
    }

    public TerminalStorageClickPayload(int section, int slot, int button, boolean shift,
                                       List<SlotTarget> dragTargets, List<Integer> menuDragSlots) {
        this(section, slot, button, shift, dragTargets, menuDragSlots, 0, 0);
    }

    public TerminalStorageClickPayload(int section, int slot, int button, boolean shift,
                                       List<SlotTarget> dragTargets, List<Integer> menuDragSlots,
                                       long operationSequence) {
        this(section, slot, button, shift, dragTargets, menuDragSlots, operationSequence, 0);
    }

    public TerminalStorageClickPayload {
        dragTargets = List.copyOf(dragTargets);
        menuDragSlots = List.copyOf(menuDragSlots);
    }

    @Override public Type<TerminalStorageClickPayload> type() { return TYPE; }

    public static void handle(TerminalStorageClickPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof StorageManagerGUIMenu menu) {
            context.enqueueWork(() -> menu.handleStorageClick(player, payload));
        }
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalStorageClickPayload::handle);
    }

    public record SlotTarget(int section, int slot) { }
}
