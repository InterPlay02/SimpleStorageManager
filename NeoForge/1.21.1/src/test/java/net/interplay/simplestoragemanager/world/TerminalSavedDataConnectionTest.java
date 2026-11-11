package net.interplay.simplestoragemanager.world;

import net.interplay.simplestoragemanager.data.TerminalConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TerminalSavedDataConnectionTest {
    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));

    @Test
    void connectionIdsAndOrderSurviveSaveReloadAndDisconnect() {
        UUID terminalId = UUID.randomUUID();
        BlockPos firstPosition = new BlockPos(1, 2, 3);
        TerminalConnection first = new TerminalConnection(OVERWORLD, firstPosition, null)
                .withMemberBlockIds(Map.of(firstPosition, ResourceLocation.withDefaultNamespace("chest")))
                .withImplementationKind(TerminalConnection.ImplementationKind.VANILLA_CONTAINER);
        TerminalConnection middle = new TerminalConnection(OVERWORLD, new BlockPos(4, 5, 6), null);
        TerminalConnection last = new TerminalConnection(OVERWORLD, new BlockPos(7, 8, 9), null);
        TerminalSavedData original = new TerminalSavedData();
        original.putConnections(terminalId, List.of(first, middle, last));

        TerminalSavedData loaded = TerminalSavedData.load(original.save(new CompoundTag(), null), null);
        List<TerminalConnection> beforeDisconnect = loaded.connections(terminalId);
        assertEquals(List.of(first.connectionId(), middle.connectionId(), last.connectionId()),
                beforeDisconnect.stream().map(TerminalConnection::connectionId).toList());
        assertEquals(List.of(0, 1, 2), beforeDisconnect.stream().map(TerminalConnection::order).toList());
        assertEquals(ResourceLocation.withDefaultNamespace("chest"), beforeDisconnect.get(0).blockId());
        assertEquals(TerminalConnection.ImplementationKind.VANILLA_CONTAINER,
                beforeDisconnect.get(0).implementationKind());

        loaded.putConnections(terminalId, List.of(beforeDisconnect.get(0), beforeDisconnect.get(2)));
        TerminalSavedData afterDisconnect = TerminalSavedData.load(loaded.save(new CompoundTag(), null), null);
        List<TerminalConnection> remaining = afterDisconnect.connections(terminalId);
        assertEquals(List.of(first.connectionId(), last.connectionId()),
                remaining.stream().map(TerminalConnection::connectionId).toList());
        assertEquals(List.of(0, 1), remaining.stream().map(TerminalConnection::order).toList());
    }

    @Test
    void legacyConnectionsReceiveStableUniqueIdsWithoutLosingTheirLocations() {
        UUID terminalId = UUID.randomUUID();
        CompoundTag root = new CompoundTag();
        ListTag terminals = new ListTag();
        CompoundTag terminal = new CompoundTag();
        terminal.putUUID("Id", terminalId);
        ListTag connections = new ListTag();
        connections.add(legacyConnection(10, 0));
        connections.add(legacyConnection(20, 1));
        terminal.put("Connections", connections);
        terminals.add(terminal);
        root.put("Terminals", terminals);

        List<TerminalConnection> firstLoad = TerminalSavedData.load(root, null).connections(terminalId);
        List<TerminalConnection> secondLoad = TerminalSavedData.load(root, null).connections(terminalId);
        assertEquals(2, firstLoad.size());
        assertEquals(List.of(10L, 20L), firstLoad.stream().map(connection -> connection.pos().asLong()).toList());
        assertEquals(firstLoad.stream().map(TerminalConnection::connectionId).toList(),
                secondLoad.stream().map(TerminalConnection::connectionId).toList());
        assertNotEquals(firstLoad.get(0).connectionId(), firstLoad.get(1).connectionId());
        assertEquals(List.of(0, 1), firstLoad.stream().map(TerminalConnection::order).toList());
    }

    private static CompoundTag legacyConnection(long position, int side) {
        CompoundTag connection = new CompoundTag();
        connection.putString("Dimension", "minecraft:overworld");
        connection.putLong("Anchor", position);
        connection.putInt("Side", side);
        ListTag members = new ListTag();
        members.add(LongTag.valueOf(position));
        connection.put("Members", members);
        connection.put("Broken", new ListTag());
        connection.put("BlockIds", new ListTag());
        connection.putInt("SlotCount", 9);
        return connection;
    }

    @Test
    void customNamesPersistPerConnectionAndClearWhenTheConnectionIsRemoved() {
        TerminalSavedData data = new TerminalSavedData();
        UUID terminalId = UUID.randomUUID();
        UUID otherConnectionId = UUID.randomUUID();
        data.putCustomName(terminalId, "Obsidian Vault");
        data.putCustomName(otherConnectionId, "Backup Base");

        TerminalSavedData loaded = TerminalSavedData.load(data.save(new CompoundTag(), null), null);
        assertEquals("Obsidian Vault", loaded.customName(terminalId));
        assertEquals("Backup Base", loaded.customName(otherConnectionId));

        loaded.clearCustomName(terminalId);
        assertNull(loaded.customName(terminalId));
        assertEquals("Backup Base", loaded.customName(otherConnectionId));

        TerminalSavedData reloaded = TerminalSavedData.load(loaded.save(new CompoundTag(), null), null);
        assertNull(reloaded.customName(terminalId));
        assertEquals("Backup Base", reloaded.customName(otherConnectionId));
    }
}
