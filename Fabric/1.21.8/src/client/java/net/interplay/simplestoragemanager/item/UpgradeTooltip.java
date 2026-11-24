package net.interplay.simplestoragemanager.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.List;

/** Installs the client-side tooltip split/key behaviour used by upgrade items. */
public final class UpgradeTooltip {
    private static final int DESCRIPTION_WIDTH = 180;

    private UpgradeTooltip() { }

    public static void register() {
        TooltipSupport.shiftDown = () -> Minecraft.getInstance().options.keyShift.isDown() || Screen.hasShiftDown();
        TooltipSupport.shiftKey = () -> Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
        TooltipSupport.description = UpgradeTooltip::splitDescription;
    }

    private static List<Component> splitDescription(String translationKey) {
        Component description = Component.translatable(translationKey);
        List<FormattedText> lines = Minecraft.getInstance().font.getSplitter()
                .splitLines(description, DESCRIPTION_WIDTH, Style.EMPTY);
        return lines.stream()
                .map(line -> (Component) Component.literal(line.getString()).withStyle(ChatFormatting.GRAY))
                .toList();
    }
}
