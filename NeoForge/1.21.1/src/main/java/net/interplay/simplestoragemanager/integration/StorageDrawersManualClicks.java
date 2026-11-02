package net.interplay.simplestoragemanager.integration;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.capabilities.PlatformDrawerItemHandler;
import net.interplay.simplestoragemanager.access.InventoryAdapter;
import net.interplay.simplestoragemanager.logic.ManualInsertionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class StorageDrawersManualClicks {
    public static final int UNSUPPORTED = -1;
    private static final int VIRTUAL_SLOT = 0;
    private static final int FIRST_PHYSICAL_SLOT = 1;
    private static final int SINGLE_ITEM_COUNT = 1;

    private StorageDrawersManualClicks() {
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
            drawer = drawer.setStoredItem(carried.copyWithCount(SINGLE_ITEM_COUNT));
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

        IItemHandler exposed = adapter.handler();
        if (!(exposed instanceof PlatformDrawerItemHandler)
                || adapter.connection().members().stream().noneMatch(level::isLoaded)) return null;

        BlockPos position = adapter.connection().pos();
        if (!level.isLoaded(position)) return null;
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof BlockEntityDrawers drawers)) return null;

        IItemHandler current = level.getCapability(
                Capabilities.ItemHandler.BLOCK, position, adapter.connection().side());
        IDrawerGroup group = BlockEntityDrawers.getGroup(drawers);
        if (!(current instanceof PlatformDrawerItemHandler) || group == null || !group.isGroupValid()
                || current.getSlots() != exposed.getSlots()
                || current.getSlots() != group.getDrawerCount() + FIRST_PHYSICAL_SLOT
                || slot == VIRTUAL_SLOT && slot >= current.getSlots()) return null;

        DrawerSlot drawerSlot = slot == VIRTUAL_SLOT ? null : resolveDrawerSlot(group, slot);
        return new Target(drawerSlot);
    }

    private static DrawerSlot resolveDrawerSlot(IDrawerGroup group, int slot) {
        int orderedIndex = slot - FIRST_PHYSICAL_SLOT;
        int[] order = group.getAccessibleDrawerSlots();
        if (orderedIndex < 0 || orderedIndex >= order.length) return null;
        IDrawer drawer = group.getDrawer(order[orderedIndex]);
        return drawer == null ? null : new DrawerSlot(drawer);
    }

    private record Target(DrawerSlot drawerSlot) {
    }

    private record DrawerSlot(IDrawer drawer) {
    }
}
