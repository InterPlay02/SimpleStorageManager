package net.interplay.simplestoragemanager.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModMenus;
import net.interplay.simplestoragemanager.recipe.StorageManagerUpgradeRecipe;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.List;
import java.util.Optional;

/**
 * JEI (Just Enough Items) integration for the Simple Storage Manager terminal.
 *
 * <p>This class is the JEI layer only. It is discovered by JEI through the {@link JeiPlugin}
 * annotation and registers:
 *
 * <ul>
 *   <li>A {@linkplain mezz.jei.api.registration.IVanillaCategoryExtensionRegistration#getCraftingCategory
 *       crafting category extension} for the custom upgrade recipe. That recipe is {@code isSpecial()}
 *       because it preserves the terminal's components, and JEI's default extension skips special
 *       recipes. The terminal itself is deliberately NOT registered as a crafting catalyst: that
 *       would make the "uses" (U) shortcut show every crafting recipe instead of the upgrade.</li>
 *   <li>A {@linkplain IRecipeTransferRegistration#addRecipeTransferHandler recipe transfer handler}
 *       for the terminal menu, exposing the 9-slot crafting grid as JEI recipe slots and the
 *       player's inventory (main + hotbar) as ingredient sources. This makes the JEI ingredient
 *       list track the player's items while the terminal GUI is open, so the recipe keybinding
 *       recognizes items that are in the player's inventory, and makes the "[+] move items to
 *       crafting grid" button target the terminal's crafting grid.</li>
 *   <li>A {@linkplain IGuiHandlerRegistration#addGuiContainerHandler GUI container handler}
 *       that tells JEI which item stack is under the mouse when hovering over slots in the
 *       terminal screen. This makes JEI's keyboard shortcuts (R = recipes, U = uses, and the
 *       favorite keybinding) work when the cursor is over an item in the player's inventory
 *       or crafting grid within the terminal GUI.</li>
 * </ul>
 *
 * <p>The server remains the authority for the actual item transfer. The real move is performed by
 * the terminal menu's existing click handling, exactly like the vanilla crafting table. The mod
 * works without JEI installed; this class is only loaded when JEI is present.
 */
@JeiPlugin
public final class JeiTerminalIntegration implements IModPlugin {

    public static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    /**
     * A receita de upgrade é {@code isSpecial()} de propósito: o resultado copia o terminal de
     * entrada para preservar seus componentes (configuração, filtros, cadeado, etc.). A extensão
     * padrão de crafting do JEI ignora receitas {@code isSpecial()}, então registramos uma
     * extensão específica para que o JEI a exiba na categoria de crafting.
     */
    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(
            StorageManagerUpgradeRecipe.class,
            new UpgradeRecipeExtension()
        );
    }

    /**
     * Não registramos o Storage Manager como catalyst/estadora de crafting: se fizéssemos isso,
     * o atalho "usos" (U) sobre o item mostraria todas as receitas de crafting do jogo em vez da
     * receita de upgrade onde o terminal é ingrediente.
     */

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        MenuType<StorageManagerGUIMenu> menuType = SimplestoragemanagerModMenus.STORAGE_MANAGER_GUI.get();
        registration.addRecipeTransferHandler(
            StorageManagerGUIMenu.class,
            menuType,
            RecipeTypes.CRAFTING,
            // Recipe slots: the 9 terminal crafting-grid slots (destination for JEI's "[+] move items
            // to crafting grid" button).
            StorageManagerGUIMenu.CRAFTING_GRID_START,
            StorageManagerGUIMenu.CRAFTING_GRID_COUNT,
            // Inventory slots: the player's main inventory + hotbar (sources for JEI's "[+]" button
            // and tracked by JEI's ingredient list so the recipe keybinding sees the player's items).
            StorageManagerGUIMenu.PLAYER_INVENTORY_START,
            StorageManagerGUIMenu.PLAYER_INVENTORY_COUNT
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(
            StorageManagerGUIScreen.class,
            new TerminalGuiHandler()
        );
    }

    /** Extensão que faz o JEI exibir a receita de upgrade (special) como uma receita moldada. */
    private static final class UpgradeRecipeExtension
            implements ICraftingCategoryExtension<StorageManagerUpgradeRecipe> {

        @Override
        public void setRecipe(RecipeHolder<StorageManagerUpgradeRecipe> recipeHolder,
                              IRecipeLayoutBuilder builder,
                              ICraftingGridHelper craftingGridHelper,
                              IFocusGroup focuses) {
            StorageManagerUpgradeRecipe recipe = recipeHolder.value();
            craftingGridHelper.createAndSetOutputs(
                builder, List.of(recipe.getResultItem(RegistryAccess.EMPTY)));
            craftingGridHelper.createAndSetIngredients(
                builder, recipe.getIngredients(), recipe.pattern.width(), recipe.pattern.height());
        }

        @Override
        public int getWidth(RecipeHolder<StorageManagerUpgradeRecipe> recipeHolder) {
            return recipeHolder.value().pattern.width();
        }

        @Override
        public int getHeight(RecipeHolder<StorageManagerUpgradeRecipe> recipeHolder) {
            return recipeHolder.value().pattern.height();
        }

        @Override
        public boolean isHandled(RecipeHolder<StorageManagerUpgradeRecipe> recipeHolder) {
            return true;
        }
    }

    /**
     * GUI container handler that exposes item stacks under the mouse in the terminal screen
     * to JEI, so keyboard shortcuts (R = recipes, U = uses, favorite) work when hovering
     * over items in the terminal's player inventory and crafting grid slots.
     *
     * <p>The hit-test goes through {@link StorageManagerGUIScreen#getHoveredItem}, which resolves
     * the terminal crafting grid, the player's personal inventory, the crafting result and the
     * remote inventories (snapshots) under the cursor. That way the shortcut keys also work for
     * items that live inside connected inventories.</p>
     */
    private static final class TerminalGuiHandler implements IGuiContainerHandler<StorageManagerGUIScreen> {

        @Override
        public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
            IClickableIngredientFactory builder,
            StorageManagerGUIScreen screen,
            double mouseX,
            double mouseY
        ) {
            ItemStack stack = screen.getHoveredItem(mouseX, mouseY);
            if (stack.isEmpty()) {
                return Optional.empty();
            }
            Rect2i area = screen.getHoveredSlotRect(mouseX, mouseY);
            if (area == null) {
                return Optional.empty();
            }
            return builder.createBuilder(stack).buildWithArea(area);
        }
    }
}
