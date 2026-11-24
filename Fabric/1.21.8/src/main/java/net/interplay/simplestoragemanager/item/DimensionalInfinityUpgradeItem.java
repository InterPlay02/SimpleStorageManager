package net.interplay.simplestoragemanager.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

public class DimensionalInfinityUpgradeItem extends Item {
	public DimensionalInfinityUpgradeItem(Item.Properties properties) {
		super(properties);
	}

	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged && !oldStack.equals(newStack);
	}

	// Vanilla 1.21.8 marca appendHoverText(...) como deprecated, mas ainda é a API de tooltip.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
	                                    Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
		boolean showDescription = TooltipSupport.shiftDown.getAsBoolean();
		if (showDescription) {
			TooltipSupport.description.apply("item.simplestoragemanager.dimensional_infinity_upgrade.description").forEach(tooltip::accept);
	} else {
			tooltip.accept(Component.translatable("item.simplestoragemanager.upgrade.tooltip",
					TooltipSupport.shiftKey.get()).withStyle(ChatFormatting.GRAY));
		}
	}
}