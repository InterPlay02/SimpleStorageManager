package net.interplay.simplestoragemanager.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.data.TerminalConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Session-only FIFO records for item stacks extracted from connected inventories. */
@EventBusSubscriber(modid = SimplestoragemanagerMod.MODID)
public final class TerminalOriginMemory {
    private static final Map<Key, List<OriginRecord>> RECORDS = new HashMap<>();
    private static long nextId;

    private TerminalOriginMemory() {
    }

    public static synchronized void record(ServerPlayer player, UUID terminalId,
                                           TerminalConnection connection, int slot, ItemStack stack) {
        if (terminalId == null || connection == null || stack.isEmpty()) return;
        Key key = new Key(player.getUUID(), terminalId);
        RECORDS.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(new OriginRecord(++nextId, connection, slot, stack.copyWithCount(1), stack.getCount()));
    }

    public static synchronized List<OriginRecord> records(ServerPlayer player, UUID terminalId) {
        return List.copyOf(RECORDS.getOrDefault(new Key(player.getUUID(), terminalId), List.of()));
    }

    public static synchronized void consume(ServerPlayer player, UUID terminalId, long recordId, int count) {
        Key key = new Key(player.getUUID(), terminalId);
        List<OriginRecord> records = RECORDS.get(key);
        if (records == null) return;
        for (int i = 0; i < records.size(); i++) {
            OriginRecord record = records.get(i);
            if (record.id() != recordId) continue;
            int remaining = record.remaining() - Math.max(0, count);
            if (remaining <= 0) records.remove(i);
            else records.set(i, record.withRemaining(remaining));
            break;
        }
        if (records.isEmpty()) RECORDS.remove(key);
    }

    public static synchronized void discard(ServerPlayer player, UUID terminalId, long recordId) {
        Key key = new Key(player.getUUID(), terminalId);
        List<OriginRecord> records = RECORDS.get(key);
        if (records == null) return;
        records.removeIf(record -> record.id() == recordId);
        if (records.isEmpty()) RECORDS.remove(key);
    }

    public static synchronized void clearTerminal(ServerPlayer player, UUID terminalId) {
        RECORDS.remove(new Key(player.getUUID(), terminalId));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        synchronized (TerminalOriginMemory.class) {
            RECORDS.keySet().removeIf(key -> key.playerId().equals(event.getEntity().getUUID()));
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        synchronized (TerminalOriginMemory.class) {
            RECORDS.clear();
            nextId = 0;
        }
    }

    public record OriginRecord(long id, TerminalConnection connection, int slot, ItemStack item, int remaining) {
        public OriginRecord {
            item = item.copyWithCount(1);
            remaining = Math.max(0, remaining);
        }

        @Override
        public ItemStack item() {
            return item.copy();
        }

        private OriginRecord withRemaining(int count) {
            return new OriginRecord(id, connection, slot, item, count);
        }
    }

    private record Key(UUID playerId, UUID terminalId) {
    }
}
