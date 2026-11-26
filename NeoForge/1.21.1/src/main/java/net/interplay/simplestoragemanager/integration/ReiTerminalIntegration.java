package net.interplay.simplestoragemanager.integration;

import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapedDisplay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.recipe.StorageManagerUpgradeRecipe;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * REI (Roughly Enough Items) integration for the Simple Storage Manager terminal.
 *
 * <p>This is the REI layer only, discovered through the {@link REIPluginClient}
 * annotation. It mirrors what REI does for the vanilla crafting table:
 *
 * <ul>
 *   <li>Um display para a receita de upgrade na categoria de crafting, registrado com
 *       {@code registerRecipeFiller}: a receita é {@code isSpecial()} e o gerador padrão do REI
 *       não a enxerga. O item do terminal não é workstation, para que o U mostre a receita onde
 *       ele é ingrediente em vez de todas as receitas de crafting.</li>
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

    /**
     * A receita de upgrade é {@code isSpecial()} para preservar os componentes do terminal e o
     * gerador padrão de displays do REI cobre apenas ShapedRecipe/ShapelessRecipe. Registramos um
     * filler para que o REI a exiba na categoria de crafting e o U sobre o terminal mostre essa
     * receita em vez de todas as receitas do jogo.
     */
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(
            StorageManagerUpgradeRecipe.class,
            RecipeType.CRAFTING,
            holder -> {
                StorageManagerUpgradeRecipe recipe = holder.value();
                return new DefaultCustomShapedDisplay(
                    holder,
                    inputs(recipe),
                    List.of(EntryIngredient.of(EntryStacks.of(
                        recipe.getResultItem(RegistryAccess.EMPTY)))),
                    recipe.pattern.width(),
                    recipe.pattern.height()
                );
            }
        );
    }

    /**
     * Não registramos o item do terminal como workstation de crafting: isso faria o U mostrar
     * todas as receitas da categoria (o equivalente à setinha do NEI 1.7.10) em vez da receita
     * onde o terminal é ingrediente.
     */

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

    /** Converte os ingredients da receita de upgrade nas entradas esperadas pelo REI. */
    private static List<EntryIngredient> inputs(StorageManagerUpgradeRecipe recipe) {
        List<EntryIngredient> inputs = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            List<EntryStack<?>> stacks = new ArrayList<>();
            for (ItemStack stack : ingredient.getItems()) {
                if (!stack.isEmpty()) stacks.add(EntryStacks.of(stack));
            }
            inputs.add(stacks.isEmpty() ? EntryIngredient.empty() : EntryIngredient.of(stacks));
        }
        return inputs;
    }

    /**
     * Resolves the stack under the mouse from the terminal's crafting grid, the player's
     * personal inventory (main inventory + hotbar) and the remote inventories (snapshots),
     * so REI's shortcut keys (recipes / uses / favorite) work on hover without requiring a
     * previous click.
     */
    private static final class TerminalFocusedStackProvider implements FocusedStackProvider {

        @Override
        public CompoundEventResult<EntryStack<?>> provide(Screen screen, Point mouse) {
            if (!(screen instanceof StorageManagerGUIScreen terminal)) {
                return CompoundEventResult.pass();
            }
            ItemStack stack = terminal.getHoveredItem(mouse.x, mouse.y);
            if (stack.isEmpty()) {
                return CompoundEventResult.pass();
            }
            return CompoundEventResult.<EntryStack<?>>interruptTrue(EntryStacks.of(stack));
        }

        @Override
        public double getPriority() {
            // REI's default provider uses hoveredSlot and runs at priority -10. The terminal
            // draws its slots at custom coordinates, so this provider must run first.
            return 100.0;
        }
    }
}
