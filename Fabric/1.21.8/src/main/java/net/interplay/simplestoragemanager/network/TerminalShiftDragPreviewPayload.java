package net.interplay.simplestoragemanager.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;

import java.util.List;
import java.util.UUID;

/** S2C: preview (possibly streamed in parts) of where a shift-drag would place items. */
public record TerminalShiftDragPreviewPayload(long requestId, boolean complete,
                                               List<SourceResult> sources,
                                               List<RemoteResult> destinations,
                                               List<PlayerResult> playerDestinations)
        implements CustomPacketPayload {
    // Custom stack codec: the count is serialized separately from the NBT so stacks above 99 survive.
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STACK_CODEC = StreamCodec.of(
            (buffer, stack) -> {
                int count = stack.isEmpty() ? 0 : stack.getCount();
                ByteBufCodecs.VAR_INT.encode(buffer, count);
                if (count > 0) {
                    buffer.writeNbt((CompoundTag) ItemStack.CODEC.encodeStart(
                            buffer.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                            stack.copyWithCount(1)).result().orElseThrow());
                }
            },
            buffer -> {
                int count = ByteBufCodecs.VAR_INT.decode(buffer);
                if (count <= 0) return ItemStack.EMPTY;
                CompoundTag tag = buffer.readNbt();
                if (tag == null) return ItemStack.EMPTY;
                ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(
                        buffer.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
                        .result().orElse(ItemStack.EMPTY);
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
            // UUIDs travel as two varlongs (most / least significant bits).
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, UUID::getMostSignificantBits,
                    ByteBufCodecs.VAR_LONG, UUID::getLeastSignificantBits,
                    UUID::new), RemoteResult::connectionId,
            ByteBufCodecs.VAR_INT, RemoteResult::storageSlot,
            STACK_CODEC, RemoteResult::stack,
            RemoteResult::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerResult> PLAYER_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerResult::menuSlot,
            STACK_CODEC, PlayerResult::stack,
            PlayerResult::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<SourceResult>> SOURCES_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, SourceResult>list().apply(SOURCE_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<RemoteResult>> REMOTES_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, RemoteResult>list().apply(REMOTE_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<PlayerResult>> PLAYERS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, PlayerResult>list().apply(PLAYER_CODEC);
    public static final Type<TerminalShiftDragPreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "terminal_shift_drag_preview_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalShiftDragPreviewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, TerminalShiftDragPreviewPayload::requestId,
                    ByteBufCodecs.BOOL, TerminalShiftDragPreviewPayload::complete,
                    SOURCES_CODEC, TerminalShiftDragPreviewPayload::sources,
                    REMOTES_CODEC, TerminalShiftDragPreviewPayload::destinations,
                    PLAYERS_CODEC, TerminalShiftDragPreviewPayload::playerDestinations,
                    TerminalShiftDragPreviewPayload::new);

    public TerminalShiftDragPreviewPayload {
        sources = List.copyOf(sources);
        destinations = List.copyOf(destinations);
        playerDestinations = List.copyOf(playerDestinations);
    }

    @Override
    public Type<TerminalShiftDragPreviewPayload> type() {
        return TYPE;
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

    public record PlayerResult(int menuSlot, ItemStack stack) {
        public PlayerResult {
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
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
