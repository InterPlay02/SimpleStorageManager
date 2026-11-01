package net.interplay.simplestoragemanager.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.List;
import java.util.Set;

/** A saved connection anchor and the blocks that form its inventory structure. */
public record TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side,
                                 List<BlockPos> members, Set<BlockPos> brokenMembers, int lastSlotCount,
                                 Map<BlockPos, ResourceLocation> memberBlockIds, long brokenAtMillis) {
    public static final long BROKEN_CONNECTION_TIMEOUT_MILLIS = 60L * 60L * 1000L;

    public TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side) {
        this(dimension, pos, side, List.of(pos.immutable()), Set.of(), 9, Map.of(), 0L);
    }

    public TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side,
                              List<BlockPos> members, Set<BlockPos> brokenMembers, int lastSlotCount) {
        this(dimension, pos, side, members, brokenMembers, lastSlotCount, Map.of(), 0L);
    }

    public TerminalConnection(ResourceKey<Level> dimension, BlockPos pos, Direction side,
                              List<BlockPos> members, Set<BlockPos> brokenMembers, int lastSlotCount,
                              Map<BlockPos, ResourceLocation> memberBlockIds) {
        this(dimension, pos, side, members, brokenMembers, lastSlotCount, memberBlockIds,
                brokenMembers.isEmpty() ? 0L : System.currentTimeMillis());
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
    }

    public boolean sameBlock(TerminalConnection other) {
        return other != null && dimension.equals(other.dimension) && pos.equals(other.pos);
    }

    public boolean containsBlock(ResourceKey<Level> targetDimension, BlockPos targetPos) {
        return dimension.equals(targetDimension) && members.contains(targetPos);
    }

    public boolean broken() {
        return !brokenMembers.isEmpty();
    }

    public TerminalConnection withMembers(List<BlockPos> newMembers) {
        return new TerminalConnection(dimension, pos, side, newMembers, brokenMembers, lastSlotCount,
                memberBlockIds, brokenAtMillis);
    }

    public TerminalConnection withMemberBlockIds(Map<BlockPos, ResourceLocation> blockIds) {
        return new TerminalConnection(dimension, pos, side, members, brokenMembers, lastSlotCount, blockIds,
                brokenAtMillis);
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
                updatedBlockIds, updatedBroken.isEmpty() ? 0L : brokenAtMillis);
    }

    public TerminalConnection withBrokenMember(BlockPos member) {
        java.util.HashSet<BlockPos> updated = new java.util.HashSet<>(brokenMembers);
        updated.add(member.immutable());
        long since = brokenAtMillis == 0L ? System.currentTimeMillis() : brokenAtMillis;
        return new TerminalConnection(dimension, pos, side, members, updated, lastSlotCount, memberBlockIds, since);
    }

    public TerminalConnection withUnconfirmedMissingMember(BlockPos member) {
        java.util.HashSet<BlockPos> updated = new java.util.HashSet<>(brokenMembers);
        updated.add(member.immutable());
        return new TerminalConnection(dimension, pos, side, members, updated, lastSlotCount, memberBlockIds,
                brokenAtMillis);
    }

    public TerminalConnection withoutBrokenMember(BlockPos member) {
        java.util.HashSet<BlockPos> updated = new java.util.HashSet<>(brokenMembers);
        updated.remove(member);
        return new TerminalConnection(dimension, pos, side, members, updated, lastSlotCount, memberBlockIds,
                updated.isEmpty() ? 0L : brokenAtMillis);
    }

    public TerminalConnection withSlotCount(int count) {
        return new TerminalConnection(dimension, pos, side, members, brokenMembers, count, memberBlockIds,
                brokenAtMillis);
    }

    public boolean brokenConnectionExpired(long nowMillis) {
        return broken() && brokenAtMillis > 0
                && nowMillis - brokenAtMillis >= BROKEN_CONNECTION_TIMEOUT_MILLIS;
    }
}
