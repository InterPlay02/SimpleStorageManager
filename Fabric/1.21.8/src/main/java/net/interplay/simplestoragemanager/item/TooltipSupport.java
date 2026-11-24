package net.interplay.simplestoragemanager.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Client behaviour is installed by the client entrypoint, keeping Minecraft
 * client classes out of the common source set.
 */
public final class TooltipSupport {
    public static BooleanSupplier shiftDown = () -> false;
    public static Supplier<Component> shiftKey = () -> Component.literal("Shift");
    public static Function<String, List<Component>> description =
            key -> List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY));

    private TooltipSupport() { }
}
