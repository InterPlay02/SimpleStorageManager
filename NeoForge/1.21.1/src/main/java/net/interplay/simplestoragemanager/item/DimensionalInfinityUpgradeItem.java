package net.interplay.simplestoragemanager.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

public class DimensionalInfinityUpgradeItem extends Item {
	public DimensionalInfinityUpgradeItem() {
		super(new Item.Properties());
	}

	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged && !oldStack.equals(newStack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
								TooltipFlag tooltipFlag) {
		boolean showDescription = Minecraft.getInstance().options.keyShift.isDown() || Screen.hasShiftDown();
		if (showDescription) {
			UpgradeTooltip.appendDescription(tooltipComponents,
					"item.simplestoragemanager.dimensional_infinity_upgrade.description");
		} else {
			tooltipComponents.add(Component.translatable("item.simplestoragemanager.upgrade.tooltip",
					Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY));
		}
	}
}