package net.interplay.simplestoragemanager.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

public class StorageManagerItem extends Item {
	public StorageManagerItem() {
		super(new Item.Properties().stacksTo(1));
	}

	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged && !oldStack.equals(newStack);
	}
}
