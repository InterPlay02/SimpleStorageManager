package net.interplay.simplestoragemanager.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A saved connection anchor and the blocks that form its inventory structure. */
public record TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side,
                                 List<BlockPos> members, Set<BlockPos> brokenMembers, int lastSlotCount,
                                 Map<BlockPos, ResourceLocation> memberBlockIds, long brokenAtMillis,
                                 UUID connectionId, int order, ResourceLocation blockId,
                                 ImplementationKind implementationKind) {
    public static final long BROKEN_CONNECTION_TIMEOUT_MILLIS = 60L * 60L * 1000L;

    public TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side) {
        this(dimension, pos, side, List.of(pos.immutable()), Set.of(), 9, Map.of(), 0L,
                UUID.randomUUID(), 0, null, ImplementationKind.UNKNOWN);
    }

    public TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side,
                              List<BlockPos> members, Set<BlockPos> brokenMembers, int lastSlotCount) {
        this(dimension, pos, side, members, brokenMembers, lastSlotCount, Map.of(), 0L,
                UUID.randomUUID(), 0, null, ImplementationKind.UNKNOWN);
    }

    public TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side,
                              List<BlockPos> members, Set<BlockPos> brokenMembers, int lastSlotCount,
                              Map<BlockPos, ResourceLocation> memberBlockIds) {
        this(dimension, pos, side, members, brokenMembers, lastSlotCount, memberBlockIds,
                brokenMembers.isEmpty() ? 0L : System.currentTimeMillis(),
                UUID.randomUUID(), 0, memberBlockIds.get(pos), ImplementationKind.UNKNOWN);
    }

    public TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side,
                              List<BlockPos> members, Set<BlockPos> brokenMembers, int lastSlotCount,
                              Map<BlockPos, ResourceLocation> memberBlockIds, long brokenAtMillis) {
        this(dimension, pos, side, members, brokenMembers, lastSlotCount, memberBlockIds, brokenAtMillis,
                UUID.randomUUID(), 0, memberBlockIds.get(pos), ImplementationKind.UNKNOWN);
    }

    public TerminalConnection {
        pos = pos.immutable();
        members = members.stream().map(BlockPos::immutable).distinct().toList();
        if (members.isEmpty()) {
            members = List.of(pos);
        }
        brokenMembers = brokenMembers.stream().map(BlockPos::immutable).collect(java.util.stream.Collectors.toUnmodifiableSet());
        lastSlotCount = Math.max(9, lastSlotCount);
        brokenAtMillis = brokenMembers.isEmpty() ? 0L : Math.max(0L, brokenAtMillis);
        memberBlockIds = memberBlockIds.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().immutable(), Map.Entry::getValue));
        connectionId = connectionId == null ? UUID.randomUUID() : connectionId;
        order = Math.max(0, order);
        blockId = blockId == null ? memberBlockIds.get(pos) : blockId;
        implementationKind = implementationKind == null ? ImplementationKind.UNKNOWN : implementationKind;
    }

    public boolean containsBlock(ResourceKey<Level> targetDimension, BlockPos targetPos) {
        return dimension.equals(targetDimension) && members.contains(targetPos);
    }

    public boolean broken() {
        return !brokenMembers.isEmpty();
    }

    public TerminalConnection withMemberBlockIds(Map<BlockPos, ResourceLocation> blockIds) {
        return new TerminalConnection(dimension, pos, side, members, brokenMembers, lastSlotCount, blockIds,
                brokenAtMillis, connectionId, order, blockIds.get(pos), implementationKind);
    }

    public TerminalConnection withIdentity(UUID id, int listOrder) {
        return new TerminalConnection(dimension, pos, side, members, brokenMembers, lastSlotCount,
                memberBlockIds, brokenAtMillis, id, listOrder, blockId, implementationKind);
    }

    public TerminalConnection withImplementationKind(ImplementationKind kind) {
        return new TerminalConnection(dimension, pos, side, members, brokenMembers, lastSlotCount,
                memberBlockIds, brokenAtMillis, connectionId, order, blockId, kind);
    }

    public TerminalConnection withPersistentIdentity(TerminalConnection previous) {
        return new TerminalConnection(dimension, pos, side, members, brokenMembers, lastSlotCount,
                memberBlockIds, brokenAtMillis, previous.connectionId, previous.order,
                previous.blockId, previous.implementationKind);
    }

    public TerminalConnection withoutMember(BlockPos member) {
        List<BlockPos> updatedMembers = members.stream().filter(existing -> !existing.equals(member)).toList();
        if (updatedMembers.isEmpty()) return null;
        Set<BlockPos> updatedBroken = brokenMembers.stream().filter(existing -> !existing.equals(member))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<BlockPos, ResourceLocation> updatedBlockIds = new java.util.HashMap<>(memberBlockIds);
        updatedBlockIds.remove(member);
        BlockPos updatedAnchor = pos.equals(member) ? updatedMembers.get(0) : pos;
        return new TerminalConnection(dimension, updatedAnchor, side, updatedMembers, updatedBroken, lastSlotCount,
                updatedBlockIds, updatedBroken.isEmpty() ? 0L : brokenAtMillis, connectionId, order,
                updatedBlockIds.get(updatedAnchor), implementationKind);
    }

    public TerminalConnection withBrokenMember(BlockPos member) {
        java.util.HashSet<BlockPos> updated = new java.util.HashSet<>(brokenMembers);
        updated.add(member.immutable());
        long since = brokenAtMillis == 0L ? System.currentTimeMillis() : brokenAtMillis;
        return new TerminalConnection(dimension, pos, side, members, updated, lastSlotCount, memberBlockIds, since,
                connectionId, order, blockId, implementationKind);
    }

    public TerminalConnection withUnconfirmedMissingMember(BlockPos member) {
        java.util.HashSet<BlockPos> updated = new java.util.HashSet<>(brokenMembers);
        updated.add(member.immutable());
        return new TerminalConnection(dimension, pos, side, members, updated, lastSlotCount, memberBlockIds,
                brokenAtMillis, connectionId, order, blockId, implementationKind);
    }

    public TerminalConnection withSlotCount(int count) {
        return new TerminalConnection(dimension, pos, side, members, brokenMembers, count, memberBlockIds,
                brokenAtMillis, connectionId, order, blockId, implementationKind);
    }

    public boolean brokenConnectionExpired(long nowMillis) {
        return broken() && brokenAtMillis > 0
                && nowMillis - brokenAtMillis >= BROKEN_CONNECTION_TIMEOUT_MILLIS;
    }

    public enum ImplementationKind {
        UNKNOWN,
        VANILLA_CONTAINER,
        MODDED_HANDLER,
        SOPHISTICATED,
        STORAGE_DRAWERS
    }
}
