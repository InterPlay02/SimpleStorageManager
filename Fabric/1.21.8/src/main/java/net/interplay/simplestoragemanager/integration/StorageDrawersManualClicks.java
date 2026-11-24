package net.interplay.simplestoragemanager.integration;

import com.jaquadro.minecraft.storagedrawers.api.capabilities.IItemHandler;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities;
import com.jaquadro.minecraft.storagedrawers.capabilities.DrawerItemHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.interplay.simplestoragemanager.access.InventoryAccess;
import net.interplay.simplestoragemanager.access.InventoryAdapter;
import net.interplay.simplestoragemanager.logic.ManualInsertionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Adaptação Fabric dos cliques manuais em gavetas (capacidade e inserção por gaveta física). */
public final class StorageDrawersManualClicks {
    public static final int UNSUPPORTED = -1;
    private static final int FIRST_HANDLER_SLOT = 1;

    private StorageDrawersManualClicks() {
    }

    /**
     * Fabric troca {@code PlatformDrawerItemHandler} por {@code DrawerItemHandler} e usa
     * {@code ChameleonCapability.getCapability(level, pos)}. O resultado continua sendo um
     * {@link InventoryAccess}, que é o tipo esperado pelo menu.
     */
    public static InventoryAccess stabilize(Level level, BlockPos position, Direction side,
                                            InventoryAccess fallback) {
        if (!FabricLoader.getInstance().isModLoaded("storagedrawers") || level == null || level.isClientSide) {
            return fallback;
        }
        IItemHandler handler = Capabilities.ITEM_HANDLER.getCapability(level, position);
        return handler instanceof DrawerItemHandler
                ? new StorageDrawersStableItemHandler(level, position, side) : fallback;
    }

    public static boolean isStableHandler(InventoryAccess handler) {
        return handler instanceof StorageDrawersStableItemHandler;
    }

    public static boolean supports(Level level, InventoryAdapter adapter, int slot) {
        return resolve(level, adapter, slot) != null;
    }

    public static int simulatedCapacity(Level level, InventoryAdapter adapter, int slot, ItemStack carried) {
        Target target = resolve(level, adapter, slot);
        if (target == null) return UNSUPPORTED;
        DrawerSlot drawerSlot = target.drawerSlot();
        if (drawerSlot == null || !canStore(drawerSlot.drawer(), carried)) return 0;
        return capacity(drawerSlot.drawer(), carried);
    }

    public static int insert(Level level, InventoryAdapter adapter, int slot,
                             ItemStack carried, int requested) {
        Target target = resolve(level, adapter, slot);
        if (target == null) return UNSUPPORTED;
        DrawerSlot drawerSlot = target.drawerSlot();
        if (drawerSlot == null || carried.isEmpty() || requested <= 0
                || !canStore(drawerSlot.drawer(), carried)) return 0;

        IDrawer drawer = drawerSlot.drawer();
        int before = drawer.getStoredItemCount();
        int available = capacity(drawer, carried);
        int offered = Math.min(Math.min(requested, carried.getCount()), available);
        if (offered <= 0) return 0;

        if (drawer.isEmpty()) {
            drawer = drawer.setStoredItem(carried);
            if (drawer.isEmpty()
                    || !ItemStack.isSameItemSameComponents(drawer.getStoredItemPrototype(), carried)) return 0;
        }

        drawer.adjustStoredItemCount(offered);
        int inserted = Math.max(0, Math.min(offered, drawer.getStoredItemCount() - before));
        carried.shrink(inserted);
        return inserted;
    }

    private static boolean canStore(IDrawer drawer, ItemStack stack) {
        if (stack.isEmpty() || !drawer.isEnabled() || drawer.isMissing()) return false;
        IDrawerAttributes attributes = drawer.getAttributes();
        return !attributes.isSuspended() && !attributes.isUnlimitedVending()
                && drawer.canItemBeStored(stack);
    }

    private static int capacity(IDrawer drawer, ItemStack stack) {
        int stored = drawer.isEmpty() ? 0 : drawer.getStoredItemCount();
        int maxCapacity = drawer.isEmpty()
                ? drawer.getMaxCapacity(stack)
                : drawer.getMaxCapacity();
        return ManualInsertionRules.remainingCapacity(maxCapacity, stored);
    }

    private static Target resolve(Level level, InventoryAdapter adapter, int slot) {
        if (level == null || level.isClientSide || adapter == null || adapter.handler() == null
                || slot < 0 || slot >= adapter.getSlots()) return null;

        InventoryAccess exposed = adapter.handler();
        BlockPos position = adapter.connection().pos();
        if (!(exposed instanceof StorageDrawersStableItemHandler stableHandler)
                || !stableHandler.matches(level, position, adapter.connection().side())
                || !level.isLoaded(position)
                || adapter.connection().members().stream().anyMatch(member -> !level.isLoaded(member))) return null;
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof BlockEntityDrawers drawers)) return null;

        IItemHandler current = Capabilities.ITEM_HANDLER.getCapability(level, position);
        IDrawerGroup group = BlockEntityDrawers.getGroup(drawers);
        if (!(current instanceof DrawerItemHandler) || group == null || !group.isGroupValid()
                || current.getSlots() != group.getDrawerCount() + FIRST_HANDLER_SLOT) return null;

        DrawerSlot drawerSlot = resolveDrawerSlot(group, slot);
        return new Target(drawerSlot);
    }

    private static DrawerSlot resolveDrawerSlot(IDrawerGroup group, int slot) {
        if (slot < 0 || slot >= group.getDrawerCount()) return null;
        IDrawer drawer = group.getDrawer(slot);
        return drawer == null ? null : new DrawerSlot(drawer);
    }

    private record Target(DrawerSlot drawerSlot) {
    }

    private record DrawerSlot(IDrawer drawer) {
    }
}
