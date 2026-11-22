package net.interplay.simplestoragemanager.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModMenus;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.Optional;

/**
 * JEI (Just Enough Items) integration for the Simple Storage Manager terminal.
 *
 * <p>This class is the JEI layer only. It is discovered by JEI through the {@link JeiPlugin}
 * annotation and registers:
 *
 * <ul>
 *   <li>The terminal block as a {@linkplain IRecipeCatalystRegistration#addRecipeCatalysts recipe catalyst}
 *       for vanilla crafting recipes, so JEI associates the terminal with crafting.</li>
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

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(
            RecipeTypes.CRAFTING,
            SimplestoragemanagerModItems.STORAGE_MANAGER.get()
        );
    }

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

    /**
     * GUI container handler that exposes item stacks under the mouse in the terminal screen
     * to JEI, so keyboard shortcuts (R = recipes, U = uses, favorite) work when hovering
     * over items in the terminal's player inventory and crafting grid slots.
     */
    private static final class TerminalGuiHandler implements IGuiContainerHandler<StorageManagerGUIScreen> {

        @Override
        public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
            IClickableIngredientFactory builder,
            StorageManagerGUIScreen screen,
            double mouseX,
            double mouseY
        ) {
            double x = mouseX - screen.getGuiLeft();
            double y = mouseY - screen.getGuiTop();
            Slot slot = screen.getSlotAtRelative(x, y);
            if (slot != null) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    int areaX = screen.getGuiLeft() + slot.x;
                    int areaY = screen.getGuiTop() + slot.y;
                    return builder.createBuilder(stack)
                        .buildWithArea(new Rect2i(areaX, areaY, 16, 16));
                }
            }
            return Optional.empty();
        }
    }
}
