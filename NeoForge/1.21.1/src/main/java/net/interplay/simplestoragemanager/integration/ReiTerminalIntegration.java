package net.interplay.simplestoragemanager.integration;

import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.client.gui.TerminalScreenLayout;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

/**
 * REI (Roughly Enough Items) integration for the Simple Storage Manager terminal.
 *
 * <p>This is the REI layer only, discovered through the {@link REIPluginClient}
 * annotation. It mirrors what REI does for the vanilla crafting table:
 *
 * <ul>
 *   <li>The terminal item is registered as a workstation for vanilla crafting recipes,
 *       so REI associates the terminal with crafting (the equivalent of a catalyst).</li>
 *   <li>A {@link SimpleTransferHandler} is registered for {@link StorageManagerGUIMenu}.
 *       The nine terminal crafting-grid slots are the input slots (destination of the
 *       "[+] move items" button) and, exactly like REI's vanilla crafting handler, the
 *       player's own inventory is used as the ingredient source. That is how REI is able
 *       to find items inside the player's inventory while the terminal GUI is open.</li>
 *   <li>A {@link FocusedStackProvider} resolves the item under the mouse in the terminal
 *       screen. The terminal renders its slots at custom positions, so REI's default
 *       provider (based on {@code AbstractContainerScreen.hoveredSlot}) cannot see them.
 *       This makes the recipe/uses shortcut keys work when hovering over items in the
 *       player inventory or in the crafting grid.</li>
 * </ul>
 *
 * <p>Only compile-time REI classes are used here. The mod works normally without REI
 * installed because nothing references this class unless REI scans for its plugins.
 */
@REIPluginClient
public final class ReiTerminalIntegration implements REIClientPlugin {

    /**
     * Same identifier as REI's {@code BuiltinPlugin.CRAFTING}. Declared locally so that the
     * default-plugin artifact is not needed on the compile classpath.
     */
    private static final CategoryIdentifier<Display> CRAFTING =
            CategoryIdentifier.of("minecraft", "plugins/crafting");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.addWorkstations(CRAFTING,
                EntryStacks.of(SimplestoragemanagerModItems.STORAGE_MANAGER.get()));
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerFocusedStack(new TerminalFocusedStackProvider());
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        // Vanilla CraftingMenu uses IntRange(1, 10) (slots 1..9 = 3x3 grid, slot 0 = result).
        // The terminal keeps the same layout in its own slots, just at different indices.
        // The 3-argument create() overload uses player.getInventory() as the ingredient
        // source, which is exactly how REI tracks the player's items for the vanilla table.
        registry.register(SimpleTransferHandler.create(
                StorageManagerGUIMenu.class,
                CRAFTING,
                new SimpleTransferHandler.IntRange(
                        StorageManagerGUIMenu.CRAFTING_GRID_START,
                        StorageManagerGUIMenu.CRAFTING_GRID_START + StorageManagerGUIMenu.CRAFTING_GRID_COUNT)));
    }

    private static TerminalScreenLayout.Variant variantOf(StorageManagerGUIScreen screen) {
        return screen.getYSize() >= TerminalScreenLayout.BIG.imageHeight()
                ? TerminalScreenLayout.BIG
                : TerminalScreenLayout.SMALL;
    }

    /**
     * Resolves the stack under the mouse from the terminal's crafting grid and the player's
     * inventory (main inventory + hotbar), so REI's shortcut keys (recipes / uses / favorite)
     * work on hover without requiring a previous click.
     */
    private static final class TerminalFocusedStackProvider implements FocusedStackProvider {

        @Override
        public CompoundEventResult<EntryStack<?>> provide(Screen screen, Point mouse) {
            if (!(screen instanceof StorageManagerGUIScreen terminal)) {
                return CompoundEventResult.pass();
            }

            double x = mouse.x - terminal.getGuiLeft();
            double y = mouse.y - terminal.getGuiTop();

            Slot slot = terminal.getSlotAtRelative(x, y);
            if (slot != null) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    return CompoundEventResult.<EntryStack<?>>interruptTrue(EntryStacks.of(stack));
                }
            }

            // The result slot is not part of getSlotAtRelative(); handle it here as well.
            TerminalScreenLayout.Point result = variantOf(terminal).result();
            if (x >= result.x() && x < result.x() + TerminalScreenLayout.SLOT_SIZE
                    && y >= result.y() && y < result.y() + TerminalScreenLayout.SLOT_SIZE) {
                ItemStack stack = terminal.getMenu().slots.get(terminal.getMenu().craftingResultIndex).getItem();
                if (!stack.isEmpty()) {
                    return CompoundEventResult.<EntryStack<?>>interruptTrue(EntryStacks.of(stack));
                }
            }

            return CompoundEventResult.pass();
        }

        @Override
        public double getPriority() {
            // REI's default provider uses hoveredSlot and runs at priority -10. The terminal
            // draws its slots at custom coordinates, so this provider must run first.
            return 100.0;
        }
    }
}
