package net.interplay.simplestoragemanager.item.inventory;

import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;

import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;

import javax.annotation.Nonnull;

/** Stores the terminal's three upgrade slots in its ItemStack container component. */
@EventBusSubscriber
public class StorageManagerInventoryCapability extends ComponentItemHandler {
	@SubscribeEvent
	public static void onItemDropped(ItemTossEvent event) {
		if (event.getEntity().getItem().getItem() == SimplestoragemanagerModItems.STORAGE_MANAGER.get()) {
			Player player = event.getPlayer();
			if (player.containerMenu instanceof StorageManagerGUIMenu)
				player.closeContainer();
		}
	}

	public StorageManagerInventoryCapability(MutableDataComponentHolder parent) {
		super(parent, DataComponents.CONTAINER, 3);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return stack.getItem() != SimplestoragemanagerModItems.STORAGE_MANAGER.get();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return super.getStackInSlot(slot).copy();
	}
}
