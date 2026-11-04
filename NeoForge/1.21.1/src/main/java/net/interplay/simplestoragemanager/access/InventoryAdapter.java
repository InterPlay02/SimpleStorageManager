package net.interplay.simplestoragemanager.access;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.fml.ModList;
import net.interplay.simplestoragemanager.data.TerminalConnection;
import net.interplay.simplestoragemanager.integration.StorageDrawersManualClicks;
import net.interplay.simplestoragemanager.integration.SophisticatedStorageManualClicks;
import net.interplay.simplestoragemanager.world.TerminalSavedData;
import net.interplay.simplestoragemanager.data.TerminalConnection.ImplementationKind;

/** A live server-side view over one inventory structure. */
public final class InventoryAdapter {
    private final TerminalConnection connection;
    private final IItemHandler handler;
    private final Component displayName;
    private final boolean storage;
    private final ImplementationKind implementationKind;

    private InventoryAdapter(TerminalConnection connection, IItemHandler handler, Component displayName,
                             boolean storage, ImplementationKind implementationKind) {
        this.connection = connection;
        this.handler = handler;
        this.displayName = displayName;
        this.storage = storage;
        this.implementationKind = implementationKind;
    }
    public static Resolution resolve(ServerLevel level, TerminalConnection connection, Player player, AccessRule accessRule) {
        if (level == null || !level.dimension().equals(connection.dimension())) {
            return new Resolution(ConnectionStatus.DIMENSION_MISSING, null);
        }
        if (!accessRule.allows(player, connection)) {
            return new Resolution(ConnectionStatus.BLOCK_REMOVED, null);
        }
        if (connection.broken()) {
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

        IItemHandler anchorHandler = null;
        for (BlockPos member : connection.members()) {
            IItemHandler memberHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, member, connection.side());
            if (memberHandler == null) {
                return new Resolution(ConnectionStatus.BLOCK_REMOVED, null);
            }
            if (anchorHandler == null) {
                anchorHandler = memberHandler;
            }
        }

        IItemHandler exposed = anchorHandler;
        if (ModList.get().isLoaded("storagedrawers")) {
            exposed = StorageDrawersManualClicks.stabilize(
                    level, connection.pos(), connection.side(), exposed);
        }
        BlockEntity anchorEntity = level.getBlockEntity(connection.pos());
        boolean storage = anchorEntity instanceof BaseContainerBlockEntity && !(anchorEntity instanceof AbstractFurnaceBlockEntity);
        BlockState state = level.getBlockState(connection.pos());
        ItemStack blockStack = new ItemStack(state.getBlock());
        Component name = anchorEntity instanceof Nameable nameable
                ? nameable.getDisplayName()
                : (blockStack.isEmpty() ? state.getBlock().getName() : blockStack.getHoverName());
        return new Resolution(ConnectionStatus.AVAILABLE, new InventoryAdapter(
                connection, exposed, name, storage, implementationKind(
                        level, connection, exposed)));
    }

    private static ImplementationKind implementationKind(ServerLevel level, TerminalConnection connection,
                                                           IItemHandler handler) {
        if (ModList.get().isLoaded("storagedrawers")
                && StorageDrawersManualClicks.isStableHandler(handler)) {
            return ImplementationKind.STORAGE_DRAWERS;
        }
        InventoryAdapter probe = new InventoryAdapter(connection, handler, Component.empty(), false,
                ImplementationKind.UNKNOWN);
        if (ModList.get().isLoaded("sophisticatedbackpacks")
                || ModList.get().isLoaded("sophisticatedstorage")) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (SophisticatedStorageManualClicks.supports(level, probe, slot)) {
                    return ImplementationKind.SOPHISTICATED;
                }
            }
        }
        if (handler instanceof InvWrapper || handler instanceof SidedInvWrapper) {
            return ImplementationKind.VANILLA_CONTAINER;
        }
        return ImplementationKind.MODDED_HANDLER;
    }

    public TerminalConnection connection() { return connection; }
    public IItemHandler handler() { return handler; }
    public Component displayName() { return displayName; }
    public boolean isStorage() { return storage; }
    public ImplementationKind implementationKind() { return implementationKind; }
    public int getSlots() { return handler.getSlots(); }

    public static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public boolean canInsert(int slot, ItemStack stack) {
        return slot >= 0 && slot < handler.getSlots() && handler.isItemValid(slot, stack);
    }

    public record Resolution(ConnectionStatus status, InventoryAdapter adapter) { }

}
