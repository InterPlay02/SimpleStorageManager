package net.interplay.simplestoragemanager.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.interplay.simplestoragemanager.data.TerminalDataComponents;

public class StorageManagerItem extends Item {
	public StorageManagerItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public Component getName(ItemStack stack) {
		Boolean advanced = stack.get(TerminalDataComponents.TERMINAL_ADVANCED.get());
		return Component.translatable(advanced != null && advanced
				? "item.simplestoragemanager.storage_manager_advanced"
				: "item.simplestoragemanager.storage_manager");
	}

	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged && !oldStack.equals(newStack);
	}
}
