package net.interplay.simplestoragemanager.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.access.ConnectionStatus;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server snapshot for terminal layout, remote slots, origin previews and operation reconciliation. */
@EventBusSubscriber
public record TerminalViewPayload(boolean big, int scrollOffset, int maxScroll,
                                  List<Section> sections, List<VisibleSlot> visibleSlots,
                                  List<Notice> notices, boolean locked,
                                  List<OriginTarget> originTargets, long operationSequence,
                                  List<PlayerSlot> playerSlots,
                                  boolean canMutateInventories) implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STACK_CODEC = StreamCodec.of(
            (buffer, stack) -> {
                int count = stack.isEmpty() ? 0 : stack.getCount();
                ByteBufCodecs.VAR_INT.encode(buffer, count);
                if (count > 0) {
                    buffer.writeNbt((CompoundTag) stack.copyWithCount(1).save(buffer.registryAccess()));
                }
            },
            buffer -> {
                int count = ByteBufCodecs.VAR_INT.decode(buffer);
                if (count <= 0) return ItemStack.EMPTY;
                CompoundTag tag = buffer.readNbt();
                if (tag == null) return ItemStack.EMPTY;
                ItemStack stack = ItemStack.parseOptional(buffer.registryAccess(), tag);
                if (!stack.isEmpty()) stack.setCount(count);
                return stack;
            });
    private static final StreamCodec<RegistryFriendlyByteBuf, Section> SECTION_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, Section::label,
            ByteBufCodecs.stringUtf8(128), Section::coordinate,
            ByteBufCodecs.VAR_INT, Section::status,
            ByteBufCodecs.VAR_INT, Section::slotCount,
            Section::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, VisibleSlot> VISIBLE_SLOT_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VisibleSlot::section,
            ByteBufCodecs.VAR_INT, VisibleSlot::slot,
            STACK_CODEC, VisibleSlot::stack,
            ByteBufCodecs.VAR_INT, VisibleSlot::slotLimit,
            VisibleSlot::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, Notice> NOTICE_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32767), Notice::dimension,
            BlockPos.STREAM_CODEC, Notice::pos,
            ByteBufCodecs.VAR_INT, Notice::sectionIndex,
            ByteBufCodecs.VAR_INT, Notice::slotCount,
            Notice::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, OriginTarget> ORIGIN_TARGET_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OriginTarget::section,
                    ByteBufCodecs.VAR_INT, OriginTarget::slot,
                    STACK_CODEC, OriginTarget::item,
                    ByteBufCodecs.VAR_INT, OriginTarget::count,
                    ByteBufCodecs.VAR_INT, OriginTarget::slotLimit,
                    OriginTarget::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerSlot> PLAYER_SLOT_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PlayerSlot::slot,
                    STACK_CODEC, PlayerSlot::stack,
                    PlayerSlot::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Section>> SECTIONS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Section>list().apply(SECTION_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<VisibleSlot>> VISIBLE_SLOTS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, VisibleSlot>list().apply(VISIBLE_SLOT_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Notice>> NOTICES_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Notice>list().apply(NOTICE_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<OriginTarget>> ORIGIN_TARGETS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, OriginTarget>list().apply(ORIGIN_TARGET_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<PlayerSlot>> PLAYER_SLOTS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, PlayerSlot>list().apply(PLAYER_SLOT_CODEC);
    public static final Type<TerminalViewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalViewPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.BOOL.encode(buffer, payload.big());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.scrollOffset());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.maxScroll());
                SECTIONS_CODEC.encode(buffer, payload.sections());
                VISIBLE_SLOTS_CODEC.encode(buffer, payload.visibleSlots());
                NOTICES_CODEC.encode(buffer, payload.notices());
                ByteBufCodecs.BOOL.encode(buffer, payload.locked());
                ORIGIN_TARGETS_CODEC.encode(buffer, payload.originTargets());
                ByteBufCodecs.VAR_LONG.encode(buffer, payload.operationSequence());
                PLAYER_SLOTS_CODEC.encode(buffer, payload.playerSlots());
                ByteBufCodecs.BOOL.encode(buffer, payload.canMutateInventories());
            },
            buffer -> new TerminalViewPayload(
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    SECTIONS_CODEC.decode(buffer),
                    VISIBLE_SLOTS_CODEC.decode(buffer),
                    NOTICES_CODEC.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ORIGIN_TARGETS_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_LONG.decode(buffer),
                    PLAYER_SLOTS_CODEC.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer)));

    public TerminalViewPayload(boolean big, int scrollOffset, int maxScroll, List<Section> sections,
                               List<VisibleSlot> visibleSlots, List<Notice> notices) {
        this(big, scrollOffset, maxScroll, sections, visibleSlots, notices, false,
                List.of(), 0, List.of(), false);
    }

    public TerminalViewPayload(boolean big, int scrollOffset, int maxScroll, List<Section> sections,
                               List<VisibleSlot> visibleSlots, List<Notice> notices, boolean locked) {
        this(big, scrollOffset, maxScroll, sections, visibleSlots, notices, locked,
                List.of(), 0, List.of(), false);
    }

    public TerminalViewPayload(boolean big, int scrollOffset, int maxScroll, List<Section> sections,
                               List<VisibleSlot> visibleSlots, List<Notice> notices, boolean locked,
                               List<OriginTarget> originTargets) {
        this(big, scrollOffset, maxScroll, sections, visibleSlots, notices, locked,
                originTargets, 0, List.of(), false);
    }

    public TerminalViewPayload(boolean big, int scrollOffset, int maxScroll, List<Section> sections,
                               List<VisibleSlot> visibleSlots, List<Notice> notices, boolean locked,
                               List<OriginTarget> originTargets, long operationSequence,
                               List<PlayerSlot> playerSlots) {
        this(big, scrollOffset, maxScroll, sections, visibleSlots, notices, locked,
                originTargets, operationSequence, playerSlots, false);
    }

    public TerminalViewPayload {
        sections = List.copyOf(sections);
        visibleSlots = visibleSlots.stream()
                .filter(slot -> slot != null && slot.stack() != null)
                .map(slot -> new VisibleSlot(slot.section(), slot.slot(), slot.stack().copy(), slot.slotLimit()))
                .toList();
        notices = List.copyOf(notices);
        originTargets = List.copyOf(originTargets);
        playerSlots = playerSlots.stream()
                .filter(slot -> slot != null && slot.stack() != null)
                .map(slot -> new PlayerSlot(slot.slot(), slot.stack().copy()))
                .toList();
    }

    @Override
    public Type<TerminalViewPayload> type() { return TYPE; }

    public static void handle(TerminalViewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                if (context.player().containerMenu instanceof StorageManagerGUIMenu menu) {
                    menu.applyView(payload);
                }
                if (net.minecraft.client.Minecraft.getInstance().screen instanceof StorageManagerGUIScreen screen) {
                    screen.applyView(payload);
                }
            }
        });
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalViewPayload::handle);
    }

    public record Section(Component label, String coordinate, int status, int slotCount) {
        public ConnectionStatus connectionStatus() {
            ConnectionStatus[] values = ConnectionStatus.values();
            return status >= 0 && status < values.length ? values[status] : ConnectionStatus.BLOCK_REMOVED;
        }
    }

    public record VisibleSlot(int section, int slot, ItemStack stack, int slotLimit) {
        public VisibleSlot(int section, int slot, ItemStack stack) {
            this(section, slot, stack, stack.getMaxStackSize());
        }

        public VisibleSlot {
            slotLimit = Math.max(0, slotLimit);
        }
    }
    public record Notice(String dimension, BlockPos pos, int sectionIndex, int slotCount) { }
    public record OriginTarget(int section, int slot, ItemStack item, int count, int slotLimit) {
        public OriginTarget {
            item = item == null ? ItemStack.EMPTY : item.copyWithCount(1);
            count = Math.max(0, count);
            slotLimit = Math.max(0, slotLimit);
        }

        @Override
        public ItemStack item() {
            return item.copy();
        }
    }
    public record PlayerSlot(int slot, ItemStack stack) {
        public PlayerSlot {
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
