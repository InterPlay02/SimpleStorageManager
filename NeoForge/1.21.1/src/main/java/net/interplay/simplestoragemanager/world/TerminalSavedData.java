package net.interplay.simplestoragemanager.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.interplay.simplestoragemanager.data.TerminalConnection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Persistent terminal identities, connections, broken members and replacement notices. */
public final class TerminalSavedData extends SavedData {
    private static final String DATA_NAME = "simple_storage_manager_terminals";
    public static final Factory<TerminalSavedData> FACTORY =
            new Factory<>(TerminalSavedData::new, TerminalSavedData::load, null);

    private final Map<UUID, List<TerminalConnection>> connections = new HashMap<>();
    private final Map<UUID, List<Notice>> notices = new HashMap<>();
    private final Map<UUID, UUID> owners = new HashMap<>();
    private final Set<UUID> lockedTerminals = new HashSet<>();

    public static TerminalSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        return (overworld == null ? level : overworld).getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public synchronized List<TerminalConnection> connections(UUID terminalId) {
        return List.copyOf(connections.getOrDefault(terminalId, List.of()));
    }

    public synchronized void putConnections(UUID terminalId, List<TerminalConnection> values) {
        List<TerminalConnection> ordered = new ArrayList<>(values.size());
        Set<UUID> identifiers = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            TerminalConnection connection = values.get(i);
            UUID identifier = connection.connectionId();
            while (!identifiers.add(identifier)) {
                identifier = UUID.randomUUID();
            }
            ordered.add(connection.withIdentity(identifier, i));
        }
        connections.put(terminalId, ordered);
        setDirty();
    }

    public synchronized UUID owner(UUID terminalId) {
        return owners.get(terminalId);
    }

    public synchronized boolean isLocked(UUID terminalId) {
        return lockedTerminals.contains(terminalId);
    }

    public synchronized void setLocked(UUID terminalId, boolean locked) {
        boolean changed = locked ? lockedTerminals.add(terminalId) : lockedTerminals.remove(terminalId);
        if (changed) setDirty();
    }

    public synchronized boolean claimOwner(UUID terminalId, UUID playerId) {
        UUID owner = owners.get(terminalId);
        if (owner != null) return owner.equals(playerId);
        owners.put(terminalId, playerId);
        setDirty();
        return true;
    }

    public synchronized void removeConnectedBlock(ResourceKey<Level> dimension, BlockPos pos) {
        boolean changed = false;
        for (Map.Entry<UUID, List<TerminalConnection>> entry : connections.entrySet()) {
            List<TerminalConnection> updated = new ArrayList<>();
            for (TerminalConnection connection : entry.getValue()) {
                if (!connection.containsBlock(dimension, pos)) {
                    updated.add(connection);
                    continue;
                }
                changed = true;
                TerminalConnection remaining = connection.withoutMember(pos);
                if (remaining != null) updated.add(remaining);
            }
            List<TerminalConnection> ordered = new ArrayList<>(updated.size());
            for (int i = 0; i < updated.size(); i++) {
                ordered.add(updated.get(i).withIdentity(updated.get(i).connectionId(), i));
            }
            entry.setValue(ordered);
        }
        if (changed) setDirty();
    }

    public synchronized boolean isConnectedToOtherTerminal(ServerLevel level, UUID terminalId,
                                                            TerminalConnection candidate) {
        Set<BlockPos> candidateMembers = Set.copyOf(candidate.members());
        for (Map.Entry<UUID, List<TerminalConnection>> entry : connections.entrySet()) {
            if (entry.getKey().equals(terminalId)) continue;
            for (TerminalConnection existing : entry.getValue()) {
                if (!existing.dimension().equals(candidate.dimension())) continue;
                for (BlockPos member : existing.members()) {
                    if (!candidateMembers.contains(member) || existing.brokenMembers().contains(member)
                            || !level.isLoaded(member) || !matchesCurrentBlock(level, existing, member)
                            || level.getCapability(Capabilities.ItemHandler.BLOCK, member, existing.side()) == null) {
                        continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean matchesCurrentBlock(ServerLevel level, TerminalConnection connection, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        var expected = connection.memberBlockIds().get(pos);
        if (expected == null) return true;
        BlockState current = level.getBlockState(pos);
        return expected.equals(BuiltInRegistries.BLOCK.getKey(current.getBlock()));
    }

    public synchronized void markBroken(ResourceKey<Level> dimension, BlockPos pos) {
        boolean changed = false;
        for (Map.Entry<UUID, List<TerminalConnection>> entry : connections.entrySet()) {
            List<TerminalConnection> updated = new ArrayList<>();
            for (TerminalConnection connection : entry.getValue()) {
                if (connection.containsBlock(dimension, pos)
                        && (!connection.brokenMembers().contains(pos) || connection.brokenAtMillis() == 0L)) {
                    connection = connection.withBrokenMember(pos);
                    changed = true;
                }
                updated.add(connection);
            }
            entry.setValue(updated);
        }
        if (changed) {
            setDirty();
        }
    }

    public synchronized void addNotice(UUID terminalId, ResourceKey<Level> dimension, BlockPos pos) {
        addNotice(terminalId, dimension, pos, 0, 9);
    }

    public synchronized void addNotice(UUID terminalId, ResourceKey<Level> dimension, BlockPos pos,
                                        int sectionIndex, int slotCount) {
        List<Notice> terminalNotices = notices.computeIfAbsent(terminalId, ignored -> new ArrayList<>());
        Notice notice = new Notice(dimension, pos.immutable(), sectionIndex, slotCount);
        if (terminalNotices.stream().noneMatch(existing -> existing.sameLocation(notice))) {
            terminalNotices.add(notice);
            setDirty();
        }
    }

    public synchronized List<Notice> notices(UUID terminalId) {
        return List.copyOf(notices.getOrDefault(terminalId, List.of()));
    }

    public synchronized void dismissNotice(UUID terminalId, ResourceKey<Level> dimension, BlockPos pos) {
        List<Notice> terminalNotices = notices.get(terminalId);
        if (terminalNotices != null && terminalNotices.removeIf(notice -> notice.dimension().equals(dimension)
                && notice.pos().equals(pos))) {
            setDirty();
        }
    }

    public static TerminalSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TerminalSavedData data = new TerminalSavedData();
        ListTag terminalList = tag.getList("Terminals", Tag.TAG_COMPOUND);
        for (int i = 0; i < terminalList.size(); i++) {
            CompoundTag terminalTag = terminalList.getCompound(i);
            if (!terminalTag.hasUUID("Id")) {
                continue;
            }
            UUID id = terminalTag.getUUID("Id");
            if (terminalTag.hasUUID("Owner")) {
                data.owners.put(id, terminalTag.getUUID("Owner"));
            }
            if (terminalTag.getBoolean("Locked")) {
                data.lockedTerminals.add(id);
            }
            List<TerminalConnection> terminalConnections = new ArrayList<>();
            ListTag connectionList = terminalTag.getList("Connections", Tag.TAG_COMPOUND);
            Set<UUID> connectionIds = new HashSet<>();
            boolean migratedConnections = false;
            for (int j = 0; j < connectionList.size(); j++) {
                CompoundTag connectionTag = connectionList.getCompound(j);
                ResourceKey<Level> dimension = dimension(connectionTag.getString("Dimension"));
                BlockPos anchor = BlockPos.of(connectionTag.getLong("Anchor"));
                Direction side = connectionTag.getInt("Side") < 0 ? null : Direction.from3DDataValue(connectionTag.getInt("Side"));
                List<BlockPos> members = new ArrayList<>();
                ListTag memberList = connectionTag.getList("Members", Tag.TAG_LONG);
                for (int k = 0; k < memberList.size(); k++) {
                    members.add(BlockPos.of(((net.minecraft.nbt.LongTag) memberList.get(k)).getAsLong()));
                }
                java.util.Set<BlockPos> broken = new java.util.HashSet<>();
                ListTag brokenList = connectionTag.getList("Broken", Tag.TAG_LONG);
                for (int k = 0; k < brokenList.size(); k++) {
                    broken.add(BlockPos.of(((net.minecraft.nbt.LongTag) brokenList.get(k)).getAsLong()));
                }
                Map<BlockPos, ResourceLocation> memberBlockIds = new HashMap<>();
                ListTag blockIdList = connectionTag.getList("BlockIds", Tag.TAG_COMPOUND);
                for (int k = 0; k < blockIdList.size(); k++) {
                    CompoundTag blockIdTag = blockIdList.getCompound(k);
                    memberBlockIds.put(BlockPos.of(blockIdTag.getLong("Pos")),
                            ResourceLocation.parse(blockIdTag.getString("Id")));
                }
                int slotCount = connectionTag.contains("SlotCount") ? connectionTag.getInt("SlotCount") : 9;
                long brokenAtMillis = connectionTag.contains("BrokenAt")
                        ? connectionTag.getLong("BrokenAt")
                        : (broken.isEmpty() ? 0L : System.currentTimeMillis());
                UUID connectionId = connectionTag.hasUUID("ConnectionId")
                        ? connectionTag.getUUID("ConnectionId")
                        : legacyConnectionId(id, dimension, anchor, side, j);
                migratedConnections |= !connectionTag.hasUUID("ConnectionId")
                        || !connectionTag.contains("Order") || !connectionTag.contains("ImplementationKind")
                        || connectionTag.contains("Order") && connectionTag.getInt("Order") != j
                        || !connectionTag.contains("BlockId") && memberBlockIds.containsKey(anchor);
                int collision = 0;
                while (!connectionIds.add(connectionId)) {
                    connectionId = legacyConnectionId(id, dimension, anchor, side, j + ++collision);
                    migratedConnections = true;
                }
                ResourceLocation blockId = connectionTag.contains("BlockId")
                        ? ResourceLocation.parse(connectionTag.getString("BlockId"))
                        : memberBlockIds.get(anchor);
                TerminalConnection.ImplementationKind implementationKind =
                        implementationKind(connectionTag.getString("ImplementationKind"));
                terminalConnections.add(new TerminalConnection(dimension, anchor, side, members, broken, slotCount,
                        memberBlockIds, brokenAtMillis, connectionId,
                        connectionTag.contains("Order") ? connectionTag.getInt("Order") : j,
                        blockId, implementationKind));
            }
            terminalConnections.sort(java.util.Comparator.comparingInt(TerminalConnection::order));
            for (int order = 0; order < terminalConnections.size(); order++) {
                terminalConnections.set(order, terminalConnections.get(order).withIdentity(
                        terminalConnections.get(order).connectionId(), order));
            }
            data.connections.put(id, terminalConnections);
            if (migratedConnections) data.setDirty();

            List<Notice> terminalNotices = new ArrayList<>();
            ListTag noticeList = terminalTag.getList("Notices", Tag.TAG_COMPOUND);
            for (int j = 0; j < noticeList.size(); j++) {
                CompoundTag noticeTag = noticeList.getCompound(j);
                terminalNotices.add(new Notice(dimension(noticeTag.getString("Dimension")),
                        BlockPos.of(noticeTag.getLong("Pos")),
                        noticeTag.contains("SectionIndex") ? noticeTag.getInt("SectionIndex") : 0,
                        noticeTag.contains("SlotCount") ? noticeTag.getInt("SlotCount") : 9));
            }
            if (!terminalNotices.isEmpty()) {
                data.notices.put(id, terminalNotices);
            }
        }
        return data;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag terminalList = new ListTag();
        Set<UUID> terminalIds = new HashSet<>(connections.keySet());
        terminalIds.addAll(notices.keySet());
        terminalIds.addAll(owners.keySet());
        terminalIds.addAll(lockedTerminals);
        for (UUID id : terminalIds) {
            CompoundTag terminalTag = new CompoundTag();
            terminalTag.putUUID("Id", id);
            UUID owner = owners.get(id);
            if (owner != null) terminalTag.putUUID("Owner", owner);
            if (lockedTerminals.contains(id)) terminalTag.putBoolean("Locked", true);
            ListTag connectionList = new ListTag();
            for (TerminalConnection connection : connections.getOrDefault(id, List.of())) {
                CompoundTag connectionTag = new CompoundTag();
                connectionTag.putString("Dimension", connection.dimension().location().toString());
                connectionTag.putLong("Anchor", connection.pos().asLong());
                connectionTag.putInt("Side", connection.side() == null ? -1 : connection.side().get3DDataValue());
                connectionTag.putUUID("ConnectionId", connection.connectionId());
                connectionTag.putInt("Order", connection.order());
                if (connection.blockId() != null) {
                    connectionTag.putString("BlockId", connection.blockId().toString());
                }
                connectionTag.putString("ImplementationKind", connection.implementationKind().name());
                ListTag memberList = new ListTag();
                connection.members().forEach(member -> memberList.add(net.minecraft.nbt.LongTag.valueOf(member.asLong())));
                connectionTag.put("Members", memberList);
                ListTag brokenList = new ListTag();
                connection.brokenMembers().forEach(member -> brokenList.add(net.minecraft.nbt.LongTag.valueOf(member.asLong())));
                connectionTag.put("Broken", brokenList);
                if (connection.brokenAtMillis() > 0) {
                    connectionTag.putLong("BrokenAt", connection.brokenAtMillis());
                }
                ListTag blockIdList = new ListTag();
                connection.memberBlockIds().forEach((member, blockId) -> {
                    CompoundTag blockIdTag = new CompoundTag();
                    blockIdTag.putLong("Pos", member.asLong());
                    blockIdTag.putString("Id", blockId.toString());
                    blockIdList.add(blockIdTag);
                });
                connectionTag.put("BlockIds", blockIdList);
                connectionTag.putInt("SlotCount", connection.lastSlotCount());
                connectionList.add(connectionTag);
            }
            terminalTag.put("Connections", connectionList);
            ListTag noticeList = new ListTag();
            for (Notice notice : notices.getOrDefault(id, List.of())) {
                CompoundTag noticeTag = new CompoundTag();
                noticeTag.putString("Dimension", notice.dimension().location().toString());
                noticeTag.putLong("Pos", notice.pos().asLong());
                noticeTag.putInt("SectionIndex", notice.sectionIndex());
                noticeTag.putInt("SlotCount", notice.slotCount());
                noticeList.add(noticeTag);
            }
            terminalTag.put("Notices", noticeList);
            terminalList.add(terminalTag);
        }
        tag.put("Terminals", terminalList);
        return tag;
    }

    private static ResourceKey<Level> dimension(String value) {
        ResourceLocation location = ResourceLocation.parse(value.isEmpty() ? "minecraft:overworld" : value);
        return ResourceKey.create(Registries.DIMENSION, location);
    }

    private static UUID legacyConnectionId(UUID terminalId, ResourceKey<Level> dimension,
                                           BlockPos anchor, Direction side, int index) {
        String identity = terminalId + "|" + dimension.location() + "|" + anchor.asLong() + "|"
                + (side == null ? -1 : side.get3DDataValue()) + "|" + index;
        return UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static TerminalConnection.ImplementationKind implementationKind(String value) {
        try {
            return TerminalConnection.ImplementationKind.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return TerminalConnection.ImplementationKind.UNKNOWN;
        }
    }

    public record Notice(ResourceKey<Level> dimension, BlockPos pos, int sectionIndex, int slotCount) {
        public Notice {
            pos = pos.immutable();
            sectionIndex = Math.max(0, sectionIndex);
            slotCount = Math.max(9, slotCount);
        }

        public boolean sameLocation(Notice other) {
            return other != null && dimension.equals(other.dimension) && pos.equals(other.pos);
        }
    }
}
