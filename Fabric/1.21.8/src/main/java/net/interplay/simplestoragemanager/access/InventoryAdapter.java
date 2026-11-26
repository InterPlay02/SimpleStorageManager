package net.interplay.simplestoragemanager.access;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.interplay.simplestoragemanager.data.TerminalConnection;
import net.interplay.simplestoragemanager.data.TerminalConnection.ImplementationKind;
import net.interplay.simplestoragemanager.integration.StorageDrawersManualClicks;
import net.interplay.simplestoragemanager.world.TerminalSavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Resolves a terminal connection to a slot-oriented {@link InventoryAccess}.
 * Storage lookups never load chunks; availability is checked before access.
 */
public final class InventoryAdapter {
    private final TerminalConnection connection;
    private final InventoryAccess access;
    private final Component displayName;
    private final ImplementationKind implementationKind;

    private InventoryAdapter(TerminalConnection connection, InventoryAccess access, Component displayName,
                             ImplementationKind implementationKind) {
        this.connection = connection;
        this.access = access;
        this.displayName = displayName;
        this.implementationKind = implementationKind;
    }

    /** Finds a Fabric Transfer API storage for a block and side without loading chunks. */
    public static Storage<ItemVariant> findStorage(ServerLevel level, BlockPos pos, Direction side) {
        if (level == null || !level.isLoaded(pos)) return null;
        return ItemStorage.SIDED.find(level, pos, side);
    }

    public static Resolution resolve(ServerLevel level, TerminalConnection connection, Player player,
                                     AccessRule accessRule) {
        if (level == null || !level.dimension().equals(connection.dimension())) {
            return new Resolution(ConnectionStatus.DIMENSION_MISSING, null);
        }
        if (!accessRule.allows(player, connection) || connection.broken()) {
            return new Resolution(ConnectionStatus.BLOCK_REMOVED, null);
        }

        for (BlockPos member : connection.members()) {
            if (!isChunkLoaded(level, member)) {
                return new Resolution(ConnectionStatus.CHUNK_UNLOADED, null);
            }
            if (!TerminalSavedData.matchesCurrentBlock(level, connection, member)) {
                return new Resolution(ConnectionStatus.BLOCK_REMOVED, null);
            }
        }

        Storage<ItemVariant> anchorStorage = null;
        for (BlockPos member : connection.members()) {
            Storage<ItemVariant> memberStorage = findStorage(level, member, connection.side());
            if (memberStorage == null) return new Resolution(ConnectionStatus.BLOCK_REMOVED, null);
            if (anchorStorage == null) anchorStorage = memberStorage;
        }

        BlockEntity anchorEntity = level.getBlockEntity(connection.pos());
        BlockState state = level.getBlockState(connection.pos());
        ItemStack blockStack = new ItemStack(state.getBlock());
        Component name = anchorEntity instanceof Nameable nameable
                ? nameable.getDisplayName()
                : (blockStack.isEmpty() ? state.getBlock().getName() : blockStack.getHoverName());

        InventoryAccess access = InventoryAccessView.of(anchorStorage);
        // Adaptação Fabric: estabiliza o handler de gavetas antes de expor o inventário ao menu.
        access = StorageDrawersManualClicks.stabilize(level, connection.pos(), connection.side(), access);
        return new Resolution(ConnectionStatus.AVAILABLE, new InventoryAdapter(
                connection, access, name,
                implementationKind(level, connection, anchorStorage, anchorEntity, access)));
    }

    private static ImplementationKind implementationKind(ServerLevel level, TerminalConnection connection,
                                                         Storage<ItemVariant> storage, BlockEntity entity,
                                                         InventoryAccess access) {
        String className = entity == null ? "" : entity.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        // O handler estável é a prova confiável de que a conexão é uma gaveta.
        // O check do mod vem primeiro para não carregar classes do mod ausente.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("storagedrawers")
                && StorageDrawersManualClicks.isStableHandler(access)) {
            return ImplementationKind.STORAGE_DRAWERS;
        }
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("storagedrawers")
                && className.contains("drawer")) {
            return ImplementationKind.STORAGE_DRAWERS;
        }
        if (storage instanceof net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
                || entity instanceof Container) {
            return ImplementationKind.VANILLA_CONTAINER;
        }
        return ImplementationKind.MODDED_HANDLER;
    }

    public TerminalConnection connection() { return connection; }
    public InventoryAccess handler() { return access; }
    public Component displayName() { return displayName; }
    public ImplementationKind implementationKind() { return implementationKind; }
    public int getSlots() { return access.getSlots(); }

    /** Chunk presence check that must never force a chunk load. */
    public static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public record Resolution(ConnectionStatus status, InventoryAdapter adapter) { }

    /** Adapts a Transfer API storage to the slot-oriented operations used by the terminal logic. */
    static final class InventoryAccessView implements InventoryAccess {
        private final Storage<ItemVariant> storage;
        private final SlottedStorage<ItemVariant> slotted;
        private final List<StorageView<ItemVariant>> views;

        private InventoryAccessView(Storage<ItemVariant> storage, SlottedStorage<ItemVariant> slotted,
                                    List<StorageView<ItemVariant>> views) {
            this.storage = storage;
            this.slotted = slotted;
            this.views = views;
        }

        static InventoryAccess of(Storage<ItemVariant> storage) {
            if (storage instanceof SlottedStorage<ItemVariant> slotted) {
                return new InventoryAccessView(storage, slotted, null);
            }
            // Storages without stable slots are snapshotted so slot order stays fixed per view.
            List<StorageView<ItemVariant>> views = new ArrayList<>();
            Iterator<StorageView<ItemVariant>> iterator = storage.iterator();
            while (iterator.hasNext()) {
                StorageView<ItemVariant> view = iterator.next();
                if (!view.isResourceBlank() && view.getAmount() > 0) views.add(view);
            }
            return new InventoryAccessView(storage, null, views);
        }

        @Override
        public int getSlots() {
            return slotted != null ? slotted.getSlotCount() : views.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            StorageView<ItemVariant> view = view(slot);
            return view == null || view.isResourceBlank() ? ItemStack.EMPTY
                    : view.getResource().toStack(safeCount(view.getAmount()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return stack;
            ItemVariant variant = ItemVariant.of(stack);
            Transaction transaction = Transaction.openOuter();
            long inserted = slotted != null && slot >= 0 && slot < slotted.getSlotCount()
                    ? slotted.getSlot(slot).insert(variant, stack.getCount(), transaction)
                    : storage.insert(variant, stack.getCount(), transaction);
            if (simulate) transaction.abort();
            else transaction.commit();
            int accepted = safeCount(inserted);
            return accepted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) return ItemStack.EMPTY;
            StorageView<ItemVariant> view = view(slot);
            if (view == null || view.isResourceBlank()) return ItemStack.EMPTY;
            ItemVariant variant = view.getResource();
            Transaction transaction = Transaction.openOuter();
            long extracted = slotted != null && slotted.getSlot(slot) instanceof SingleSlotStorage<ItemVariant> single
                    ? single.extract(variant, amount, transaction)
                    : storage.extract(variant, amount, transaction);
            if (simulate) transaction.abort();
            else transaction.commit();
            return extracted <= 0 ? ItemStack.EMPTY : variant.toStack(safeCount(extracted));
        }

        @Override
        public int getSlotLimit(int slot) {
            StorageView<ItemVariant> view = view(slot);
            if (view == null) return 0;
            long capacity = view.getCapacity();
            return capacity <= 0 ? 64 : (int) Math.min(Integer.MAX_VALUE, capacity);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty() || !storage.supportsInsertion()) return false;
            ItemVariant variant = ItemVariant.of(stack);
            Transaction transaction = Transaction.openOuter();
            long inserted = slotted != null && slot >= 0 && slot < slotted.getSlotCount()
                    ? slotted.getSlot(slot).insert(variant, 1, transaction)
                    : storage.insert(variant, 1, transaction);
            transaction.abort();
            return inserted > 0;
        }

        private StorageView<ItemVariant> view(int slot) {
            if (slot < 0) return null;
            if (slotted != null) {
                return slot < slotted.getSlotCount() ? slotted.getSlot(slot) : null;
            }
            return slot < views.size() ? views.get(slot) : null;
        }

        private static int safeCount(long value) {
            return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
        }
    }
}
