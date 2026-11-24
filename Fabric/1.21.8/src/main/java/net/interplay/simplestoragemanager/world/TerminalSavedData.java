package net.interplay.simplestoragemanager.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.interplay.simplestoragemanager.data.TerminalConnection;
import net.interplay.simplestoragemanager.logic.InventoryFilterLogic;
import net.interplay.simplestoragemanager.logic.InventoryOrganizerLogic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.interplay.simplestoragemanager.access.InventoryAdapter;

/** Persistent terminal identities, connections, broken members and replacement notices. */
public final class TerminalSavedData extends SavedData {
    private static final String DATA_NAME = "simple_storage_manager_terminals";

    // 1.21.8 replaced SavedData.Factory with SavedDataType + a Codec for the inner NBT.
    // The context supplies the real registry access so filter ItemStacks still parse/save.
    public static final SavedDataType<TerminalSavedData> TYPE = new SavedDataType<>(
            DATA_NAME,
            context -> new TerminalSavedData(),
            context -> codec(context.levelOrThrow().registryAccess()),
            DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final Map<UUID, List<TerminalConnection>> connections = new HashMap<>();
    private final Map<UUID, List<Notice>> notices = new HashMap<>();
    private final Map<UUID, UUID> owners = new HashMap<>();
    private final Set<UUID> lockedTerminals = new HashSet<>();
    private final Map<UUID, String> customNames = new HashMap<>();
    private final Map<UUID, InventoryFilterLogic.FilterConfig> filterConfigs = new HashMap<>();
    private final Map<UUID, InventoryOrganizerLogic.OrganizeConfig> organizeConfigs = new HashMap<>();

    public static TerminalSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        return (overworld == null ? level : overworld).getDataStorage().computeIfAbsent(TYPE);
    }

    /** Adapts the old NBT load/save pair to the 1.21.8 codec-based saved data API. */
    public static Codec<TerminalSavedData> codec(HolderLookup.Provider provider) {
        return CompoundTag.CODEC.comapFlatMap(
                tag -> DataResult.success(load(tag, provider)),
                data -> data.save(new CompoundTag(), provider));
    }

    // NBT 1.21.8 dropped CompoundTag.getUUID/hasUUID/putUUID; keep the legacy four-int layout.
    private static boolean hasUuid(CompoundTag tag, String key) {
        return tag.getIntArray(key).map(array -> array.length == 4).orElse(false);
    }

    private static UUID getUuid(CompoundTag tag, String key) {
        int[] array = tag.getIntArray(key).orElse(null);
        return array != null && array.length == 4 ? UUIDUtil.uuidFromIntArray(array) : null;
    }

    private static void putUuid(CompoundTag tag, String key, UUID uuid) {
        tag.putIntArray(key, UUIDUtil.uuidToIntArray(uuid));
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
        pruneFilterConfigs(connectionIds(ordered));
        setDirty();
    }

    private static java.util.Set<UUID> connectionIds(List<TerminalConnection> connections) {
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        for (TerminalConnection connection : connections) {
            ids.add(connection.connectionId());
        }
        return ids;
    }

    private void pruneFilterConfigs(java.util.Set<UUID> activeConnectionIds) {
        // Filter and organize configs are keyed by connection UUID globally, so prune by active ids
        // across all terminals to avoid keeping configs for connections that no longer exist.
        boolean changed = false;
        for (var connectionId : new java.util.ArrayList<>(filterConfigs.keySet())) {
            if (!activeConnectionIds.contains(connectionId)) {
                filterConfigs.remove(connectionId);
                changed = true;
            }
        }
        for (var connectionId : new java.util.ArrayList<>(organizeConfigs.keySet())) {
            if (!activeConnectionIds.contains(connectionId)) {
                organizeConfigs.remove(connectionId);
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
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
                            || InventoryAdapter.findStorage(level, member, existing.side()) == null) {
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

    /** Presentation-only custom title for one terminal connection; null when the inventory keeps its own name. */
    public synchronized String customName(UUID connectionId) {
        return customNames.get(connectionId);
    }

    public synchronized void putCustomName(UUID connectionId, String name) {
        customNames.put(connectionId, name);
        setDirty();
    }

    public synchronized void clearCustomName(UUID connectionId) {
        if (customNames.remove(connectionId) != null) {
            setDirty();
        }
    }

    public synchronized InventoryFilterLogic.FilterConfig getFilterConfig(UUID connectionId) {
        return filterConfigs.get(connectionId);
    }

    public synchronized void putFilterConfig(UUID connectionId, InventoryFilterLogic.FilterConfig config) {
        filterConfigs.put(connectionId, config);
        setDirty();
    }

    public synchronized InventoryOrganizerLogic.OrganizeConfig getOrganizeConfig(UUID connectionId) {
        return organizeConfigs.getOrDefault(connectionId,
                InventoryOrganizerLogic.defaultConfig(InventoryOrganizerLogic.OrganizeOption.NAME));
    }

    public synchronized void putOrganizeConfig(UUID connectionId,
                                               InventoryOrganizerLogic.OrganizeConfig config) {
        organizeConfigs.put(connectionId, config);
        setDirty();
    }

    public static TerminalSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TerminalSavedData data = new TerminalSavedData();
        // 1.21.8: getList/getCompound now return Optional or have *OrEmpty variants.
        ListTag terminalList = tag.getListOrEmpty("Terminals");
        for (int i = 0; i < terminalList.size(); i++) {
            CompoundTag terminalTag = terminalList.getCompoundOrEmpty(i);
            if (!hasUuid(terminalTag, "Id")) {
                continue;
            }
            UUID id = getUuid(terminalTag, "Id");
            if (hasUuid(terminalTag, "Owner")) {
                data.owners.put(id, getUuid(terminalTag, "Owner"));
            }
            if (terminalTag.getBoolean("Locked").orElse(false)) {
                data.lockedTerminals.add(id);
            }
            List<TerminalConnection> terminalConnections = new ArrayList<>();
            ListTag connectionList = terminalTag.getListOrEmpty("Connections");
            Set<UUID> connectionIds = new HashSet<>();
            boolean migratedConnections = false;
            for (int j = 0; j < connectionList.size(); j++) {
                CompoundTag connectionTag = connectionList.getCompoundOrEmpty(j);
                ResourceKey<Level> dimension = dimension(connectionTag.getString("Dimension").orElse(""));
                BlockPos anchor = BlockPos.of(connectionTag.getLong("Anchor").orElse(0L));
                int sideValue = connectionTag.getInt("Side").orElse(0);
                Direction side = sideValue < 0 ? null : Direction.from3DDataValue(sideValue);
                List<BlockPos> members = new ArrayList<>();
                ListTag memberList = connectionTag.getListOrEmpty("Members");
                for (int k = 0; k < memberList.size(); k++) {
                    members.add(BlockPos.of(memberList.get(k).asLong().orElse(0L)));
                }
                java.util.Set<BlockPos> broken = new java.util.HashSet<>();
                ListTag brokenList = connectionTag.getListOrEmpty("Broken");
                for (int k = 0; k < brokenList.size(); k++) {
                    broken.add(BlockPos.of(brokenList.get(k).asLong().orElse(0L)));
                }
                Map<BlockPos, ResourceLocation> memberBlockIds = new HashMap<>();
                ListTag blockIdList = connectionTag.getListOrEmpty("BlockIds");
                for (int k = 0; k < blockIdList.size(); k++) {
                    CompoundTag blockIdTag = blockIdList.getCompoundOrEmpty(k);
                    memberBlockIds.put(BlockPos.of(blockIdTag.getLong("Pos").orElse(0L)),
                            ResourceLocation.parse(blockIdTag.getString("Id").orElse("")));
                }
                int slotCount = connectionTag.getInt("SlotCount").orElse(9);
                long brokenAtMillis = connectionTag.contains("BrokenAt")
                        ? connectionTag.getLong("BrokenAt").orElse(0L)
                        : (broken.isEmpty() ? 0L : System.currentTimeMillis());
                UUID connectionId = hasUuid(connectionTag, "ConnectionId")
                        ? getUuid(connectionTag, "ConnectionId")
                        : legacyConnectionId(id, dimension, anchor, side, j);
                migratedConnections |= !hasUuid(connectionTag, "ConnectionId")
                        || !connectionTag.contains("Order") || !connectionTag.contains("ImplementationKind")
                        || connectionTag.contains("Order") && connectionTag.getInt("Order").orElse(j) != j
                        || !connectionTag.contains("BlockId") && memberBlockIds.containsKey(anchor);
                int collision = 0;
                while (!connectionIds.add(connectionId)) {
                    connectionId = legacyConnectionId(id, dimension, anchor, side, j + ++collision);
                    migratedConnections = true;
                }
                ResourceLocation blockId = connectionTag.contains("BlockId")
                        ? ResourceLocation.parse(connectionTag.getString("BlockId").orElse(""))
                        : memberBlockIds.get(anchor);
                TerminalConnection.ImplementationKind implementationKind =
                        implementationKind(connectionTag.getString("ImplementationKind").orElse(""));
                terminalConnections.add(new TerminalConnection(dimension, anchor, side, members, broken, slotCount,
                        memberBlockIds, brokenAtMillis, connectionId,
                        connectionTag.getInt("Order").orElse(j),
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
            ListTag noticeList = terminalTag.getListOrEmpty("Notices");
            for (int j = 0; j < noticeList.size(); j++) {
                CompoundTag noticeTag = noticeList.getCompoundOrEmpty(j);
                terminalNotices.add(new Notice(dimension(noticeTag.getString("Dimension").orElse("")),
                        BlockPos.of(noticeTag.getLong("Pos").orElse(0L)),
                        noticeTag.getInt("SectionIndex").orElse(0),
                        noticeTag.getInt("SlotCount").orElse(9)));
            }
            if (!terminalNotices.isEmpty()) {
                data.notices.put(id, terminalNotices);
            }
        }
        ListTag customNameList = tag.getListOrEmpty("CustomNames");
        for (int i = 0; i < customNameList.size(); i++) {
            CompoundTag customNameTag = customNameList.getCompoundOrEmpty(i);
            UUID customNameConnectionId = hasUuid(customNameTag, "ConnectionId")
                    ? getUuid(customNameTag, "ConnectionId") : UUID.randomUUID();
            String customName = customNameTag.getString("Name").orElse("");
            if (!customName.isEmpty()) {
                data.customNames.put(customNameConnectionId, customName);
            }
        }
        ListTag filterConfigList = tag.getListOrEmpty("FilterConfigs");
        for (int i = 0; i < filterConfigList.size(); i++) {
            CompoundTag filterConfigTag = filterConfigList.getCompoundOrEmpty(i);
            UUID connectionId = hasUuid(filterConfigTag, "ConnectionId")
                    ? getUuid(filterConfigTag, "ConnectionId") : null;
            if (connectionId != null && filterConfigTag.contains("Mode")) {
                data.filterConfigs.put(connectionId,
                        InventoryFilterLogic.fromTag(provider, filterConfigTag));
            }
        }
        ListTag organizeConfigList = tag.getListOrEmpty("OrganizeConfigs");
        for (int i = 0; i < organizeConfigList.size(); i++) {
            CompoundTag organizeConfigTag = organizeConfigList.getCompoundOrEmpty(i);
            UUID connectionId = hasUuid(organizeConfigTag, "ConnectionId")
                    ? getUuid(organizeConfigTag, "ConnectionId") : null;
            if (connectionId != null && organizeConfigTag.contains("Option")) {
                int option = organizeConfigTag.getInt("Option").orElse(0);
                boolean ascending = organizeConfigTag.getBoolean("Ascending").orElse(false);
                boolean organized = organizeConfigTag.getBoolean("Organized").orElse(false);
                InventoryOrganizerLogic.OrganizeOption opt;
                try {
                    opt = InventoryOrganizerLogic.OrganizeOption.values()[option];
                } catch (Exception ignored) {
                    opt = InventoryOrganizerLogic.OrganizeOption.NAME;
                }
                data.organizeConfigs.put(connectionId,
                        new InventoryOrganizerLogic.OrganizeConfig(opt, ascending, organized));
            }
        }
        return data;
    }

    // SavedData no longer declares save(CompoundTag, Provider); the codec in TYPE calls this method.
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag terminalList = new ListTag();
        Set<UUID> terminalIds = new HashSet<>(connections.keySet());
        terminalIds.addAll(notices.keySet());
        terminalIds.addAll(owners.keySet());
        terminalIds.addAll(lockedTerminals);
        for (UUID id : terminalIds) {
            CompoundTag terminalTag = new CompoundTag();
            putUuid(terminalTag, "Id", id);
            UUID owner = owners.get(id);
            if (owner != null) putUuid(terminalTag, "Owner", owner);
            if (lockedTerminals.contains(id)) terminalTag.putBoolean("Locked", true);
            ListTag connectionList = new ListTag();
            for (TerminalConnection connection : connections.getOrDefault(id, List.of())) {
                CompoundTag connectionTag = new CompoundTag();
                connectionTag.putString("Dimension", connection.dimension().location().toString());
                connectionTag.putLong("Anchor", connection.pos().asLong());
                connectionTag.putInt("Side", connection.side() == null ? -1 : connection.side().get3DDataValue());
                putUuid(connectionTag, "ConnectionId", connection.connectionId());
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
        ListTag customNameList = new ListTag();
        for (Map.Entry<UUID, String> entry : customNames.entrySet()) {
            CompoundTag customNameTag = new CompoundTag();
            putUuid(customNameTag, "ConnectionId", entry.getKey());
            customNameTag.putString("Name", entry.getValue() == null ? "" : entry.getValue());
            customNameList.add(customNameTag);
        }
        tag.put("CustomNames", customNameList);

        ListTag filterConfigList = new ListTag();
        for (Map.Entry<UUID, InventoryFilterLogic.FilterConfig> entry : filterConfigs.entrySet()) {
            CompoundTag filterConfigTag = new CompoundTag();
            putUuid(filterConfigTag, "ConnectionId", entry.getKey());
            filterConfigTag.merge(InventoryFilterLogic.toTag(provider, entry.getValue()));
            filterConfigList.add(filterConfigTag);
        }
        tag.put("FilterConfigs", filterConfigList);

        ListTag organizeConfigList = new ListTag();
        for (Map.Entry<UUID, InventoryOrganizerLogic.OrganizeConfig> entry : organizeConfigs.entrySet()) {
            CompoundTag organizeConfigTag = new CompoundTag();
            putUuid(organizeConfigTag, "ConnectionId", entry.getKey());
            organizeConfigTag.putInt("Option", entry.getValue().option().ordinal());
            organizeConfigTag.putBoolean("Ascending", entry.getValue().ascending());
            organizeConfigTag.putBoolean("Organized", entry.getValue().organized());
            organizeConfigList.add(organizeConfigTag);
        }
        tag.put("OrganizeConfigs", organizeConfigList);

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
