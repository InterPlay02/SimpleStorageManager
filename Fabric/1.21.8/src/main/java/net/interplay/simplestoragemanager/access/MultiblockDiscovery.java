package net.interplay.simplestoragemanager.access;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.interplay.simplestoragemanager.data.TerminalConnection;
import net.interplay.simplestoragemanager.logic.MultiblockBounds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Discovers the inventory structure represented by a clicked block without loading chunks. */
public final class MultiblockDiscovery {
    private MultiblockDiscovery() {
    }

    public static Result discover(ServerLevel level, BlockPos anchor, Direction side) {
        return discover(level, anchor, side, null);
    }

    private static Result discover(ServerLevel level, BlockPos anchor, Direction side, Set<BlockPos> candidates) {
        if (!level.isLoaded(anchor) || InventoryAdapter.findStorage(level, anchor, side) == null) {
            return new Result(null, false);
        }
        BlockState state = level.getBlockState(anchor);
        if (state.getBlock() instanceof ChestBlock) {
            return discoverChest(level, anchor, side, state, candidates);
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(anchor.immutable());
        visited.add(anchor.immutable());
        boolean overflow = false;
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (candidates != null && !candidates.contains(next)) {
                    continue;
                }
                if (!isSameMultiblockInventory(level, current, next, side)) {
                    continue;
                }
                if (!withinFiveByFiveByFive(visited, next)) {
                    overflow = true;
                    continue;
                }
                if (visited.add(next.immutable())) {
                    queue.addLast(next.immutable());
                }
            }
        }
        List<BlockPos> members = new ArrayList<>(visited);
        return new Result(connection(level, anchor, side, members), overflow);
    }

    /**
     * Rebuilds the members of a saved connection using the current inventory identity rules.
     * Candidate restriction prevents an existing connection from silently absorbing new blocks.
     */
    public static List<TerminalConnection> rediscover(ServerLevel level, TerminalConnection saved) {
        Set<BlockPos> remaining = new LinkedHashSet<>(saved.members());
        List<TerminalConnection> rebuilt = new ArrayList<>();
        while (!remaining.isEmpty()) {
            BlockPos anchor = remaining.iterator().next();
            Result result = discover(level, anchor, saved.side(), remaining);
            if (result.connection() == null || result.exceedsBounds()) {
                return List.of(saved);
            }
            TerminalConnection connection = result.connection().withSlotCount(saved.lastSlotCount());
            rebuilt.add(connection);
            remaining.removeAll(connection.members());
        }
        return rebuilt;
    }

    private static Result discoverChest(ServerLevel level, BlockPos anchor, Direction side, BlockState state,
                                        Set<BlockPos> candidates) {
        List<BlockPos> members = new ArrayList<>();
        members.add(anchor.immutable());
        Direction connectedDirection = ChestBlock.getConnectedDirection(state);
        if (connectedDirection != null) {
            BlockPos connected = anchor.relative(connectedDirection);
            if ((candidates == null || candidates.contains(connected))
                    && isSameMultiblockInventory(level, anchor, connected, side)) {
                members.add(connected.immutable());
            }
        }
        return new Result(connection(level, anchor, side, members), false);
    }

    private static TerminalConnection connection(ServerLevel level, BlockPos anchor, Direction side,
                                                  List<BlockPos> members) {
        Map<BlockPos, ResourceLocation> blockIds = members.stream().collect(java.util.stream.Collectors.toMap(
                BlockPos::immutable,
                member -> BuiltInRegistries.BLOCK.getKey(level.getBlockState(member).getBlock())));
        return new TerminalConnection(level.dimension(), anchor, side, members, Set.of(), 9)
                .withMemberBlockIds(blockIds);
    }

    public static boolean isSameMultiblockInventory(ServerLevel level, BlockPos first, BlockPos second,
                                                    Direction side) {
        if (!level.isLoaded(first) || !level.isLoaded(second)) {
            return false;
        }
        Direction direction = directionBetween(first, second);
        if (direction == null) {
            return false;
        }

        BlockState firstState = level.getBlockState(first);
        BlockState secondState = level.getBlockState(second);
        boolean firstIsChest = firstState.getBlock() instanceof ChestBlock;
        boolean secondIsChest = secondState.getBlock() instanceof ChestBlock;
        if (firstIsChest || secondIsChest) {
            return firstState.getBlock() == secondState.getBlock()
                    && firstIsChest
                    && ChestBlock.getConnectedDirection(firstState) == direction
                    && ChestBlock.getConnectedDirection(secondState) == direction.getOpposite()
                    && InventoryAdapter.findStorage(level, first, side) != null
                    && InventoryAdapter.findStorage(level, second, side) != null;
        }

        Storage<ItemVariant> firstStorage = InventoryAdapter.findStorage(level, first, side);
        Storage<ItemVariant> secondStorage = InventoryAdapter.findStorage(level, second, side);
        if (firstStorage == null || secondStorage == null) return false;
        // Fabric storages are wrappers and may not be reference-stable, so group by block entity identity.
        // Cada gaveta tem sua própria entidade de bloco: o caminho de drawers permanece individual.
        net.minecraft.world.level.block.entity.BlockEntity firstEntity = level.getBlockEntity(first);
        net.minecraft.world.level.block.entity.BlockEntity secondEntity = level.getBlockEntity(second);
        if (firstEntity != null && secondEntity != null) return firstEntity == secondEntity;
        return firstStorage == secondStorage;
    }

    private static Direction directionBetween(BlockPos first, BlockPos second) {
        for (Direction direction : Direction.values()) {
            if (first.relative(direction).equals(second)) {
                return direction;
            }
        }
        return null;
    }

    private static boolean withinFiveByFiveByFive(Set<BlockPos> current, BlockPos next) {
        int minX = next.getX(), minY = next.getY(), minZ = next.getZ();
        int maxX = next.getX(), maxY = next.getY(), maxZ = next.getZ();
        for (BlockPos position : current) {
            minX = Math.min(minX, position.getX());
            minY = Math.min(minY, position.getY());
            minZ = Math.min(minZ, position.getZ());
            maxX = Math.max(maxX, position.getX());
            maxY = Math.max(maxY, position.getY());
            maxZ = Math.max(maxZ, position.getZ());
        }
        minX = Math.min(minX, next.getX());
        minY = Math.min(minY, next.getY());
        minZ = Math.min(minZ, next.getZ());
        maxX = Math.max(maxX, next.getX());
        maxY = Math.max(maxY, next.getY());
        maxZ = Math.max(maxZ, next.getZ());
        return MultiblockBounds.fits(minX, minY, minZ, maxX, maxY, maxZ,
                next.getX(), next.getY(), next.getZ());
    }

    public record Result(TerminalConnection connection, boolean exceedsBounds) {
    }
}
