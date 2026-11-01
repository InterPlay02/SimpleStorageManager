package net.interplay.simplestoragemanager.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.List;

final class UpgradeTooltip {
    private static final int DESCRIPTION_WIDTH = 180;

    private UpgradeTooltip() {
    }

    static void appendDescription(List<Component> tooltip, String translationKey) {
        Component description = Component.translatable(translationKey);
        List<FormattedText> lines = Minecraft.getInstance().font.getSplitter()
                .splitLines(description, DESCRIPTION_WIDTH, Style.EMPTY);
        for (FormattedText line : lines) {
            tooltip.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
        }
    }
}
