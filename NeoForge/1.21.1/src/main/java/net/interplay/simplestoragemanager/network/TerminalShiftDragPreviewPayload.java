package net.interplay.simplestoragemanager.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber
public record TerminalShiftDragPreviewPayload(long requestId, boolean complete,
                                              List<SourceResult> sources,
                                              List<RemoteResult> destinations)
        implements CustomPacketPayload {
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
    private static final StreamCodec<RegistryFriendlyByteBuf, SourceResult> SOURCE_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SourceResult::menuSlot,
            STACK_CODEC, SourceResult::remaining,
            SourceResult::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, RemoteResult> REMOTE_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RemoteResult::section,
            ByteBufCodecs.VAR_INT, RemoteResult::slot,
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, UUID::getMostSignificantBits,
                    ByteBufCodecs.VAR_LONG, UUID::getLeastSignificantBits,
                    UUID::new), RemoteResult::connectionId,
            ByteBufCodecs.VAR_INT, RemoteResult::storageSlot,
            STACK_CODEC, RemoteResult::stack,
            RemoteResult::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<SourceResult>> SOURCES_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, SourceResult>list().apply(SOURCE_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<RemoteResult>> REMOTES_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, RemoteResult>list().apply(REMOTE_CODEC);
    public static final Type<TerminalShiftDragPreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_shift_drag_preview_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalShiftDragPreviewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, TerminalShiftDragPreviewPayload::requestId,
                    ByteBufCodecs.BOOL, TerminalShiftDragPreviewPayload::complete,
                    SOURCES_CODEC, TerminalShiftDragPreviewPayload::sources,
                    REMOTES_CODEC, TerminalShiftDragPreviewPayload::destinations,
                    TerminalShiftDragPreviewPayload::new);

    public TerminalShiftDragPreviewPayload {
        sources = List.copyOf(sources);
        destinations = List.copyOf(destinations);
    }

    @Override
    public Type<TerminalShiftDragPreviewPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalShiftDragPreviewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.CLIENTBOUND
                    && net.minecraft.client.Minecraft.getInstance().screen instanceof StorageManagerGUIScreen screen) {
                screen.applyShiftDragPreview(payload);
            }
        });
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        SimplestoragemanagerMod.addNetworkMessage(TYPE, STREAM_CODEC, TerminalShiftDragPreviewPayload::handle);
    }

    public record SourceResult(int menuSlot, ItemStack remaining) {
        public SourceResult {
            remaining = remaining == null ? ItemStack.EMPTY : remaining.copy();
        }

        @Override
        public ItemStack remaining() {
            return remaining.copy();
        }
    }

    public record RemoteResult(int section, int slot, UUID connectionId,
                               int storageSlot, ItemStack stack) {
        private static final UUID NO_CONNECTION = new UUID(0L, 0L);

        public RemoteResult(int section, int slot, ItemStack stack) {
            this(section, slot, NO_CONNECTION, slot, stack);
        }

        public RemoteResult {
            connectionId = connectionId == null ? NO_CONNECTION : connectionId;
            storageSlot = Math.max(0, storageSlot);
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
