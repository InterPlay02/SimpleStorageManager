package net.interplay.simplestoragemanager.integration;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModMenus;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

/**
 * EMI (Every Mod Is Included) integration for the Simple Storage Manager terminal.
 *
 * <p>This class is the EMI layer only. It registers the terminal screen with EMI through the
 * official API and exposes a {@link StandardRecipeHandler} for the terminal's
 * {@link StorageManagerGUIMenu}, mirroring how the vanilla crafting-table handler is set up:
 *
 * <ul>
 *   <li>The nine crafting-grid slots are the crafting destination.</li>
 *   <li>The terminal's crafting grid plus the player's personal inventory (including hotbar)
 *       are the ingredient sources.</li>
 *   <li>The recipe feasibility and the actual fill are delegated to EMI's official
 *       {@link StandardRecipeHandler} machinery (input sources, crafting slots, output slot).</li>
 * </ul>
 *
 * <p>The server remains the authority for the actual transfer; EMI performs the fill through
 * its own recipe-filling pipeline, exactly like it does for the vanilla crafting table.
 */
@EmiEntrypoint
public final class EmiTerminalIntegration implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addScreenBoundsProvider(StorageManagerGUIScreen.class, EmiTerminalIntegration::getScreenBounds);
        registry.addGenericScreenBoundsProvider(screen -> {
            if (screen instanceof StorageManagerGUIScreen sms) {
                return getScreenBounds(sms);
            }
            return Bounds.EMPTY;
        });
        registry.<StorageManagerGUIMenu>addRecipeHandler(
                SimplestoragemanagerModMenus.STORAGE_MANAGER_GUI.get(), new TerminalEmiRecipeHandler());
        registry.addStackProvider(StorageManagerGUIScreen.class, new TerminalStackProvider());
    }

    private static Bounds getScreenBounds(StorageManagerGUIScreen screen) {
        return new Bounds(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize());
    }

    /**
     * EMI recipe handler for the terminal's crafting grid.
     *
     * <p>Like the vanilla crafting-table handler, the input sources are the nine crafting-grid
     * slots followed by the player's personal inventory (main inventory + hotbar), and the
     * crafting slots are the nine crafting-grid slots. The output slot is the terminal's crafting
     * result slot. EMI uses these to detect ingredients, decide whether a recipe can be crafted,
     * and perform the transfer.
     */
    private static final class TerminalEmiRecipeHandler implements StandardRecipeHandler<StorageManagerGUIMenu> {

        @Override
        public List<Slot> getInputSources(StorageManagerGUIMenu handler) {
            List<Slot> sources = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                sources.add(handler.getSlot(handler.craftingGridStart + i));
            }
            for (int p = 0; p < 36; p++) {
                sources.add(handler.getSlot(handler.mainInventoryStart + p));
            }
            return List.copyOf(sources);
        }

        @Override
        public List<Slot> getCraftingSlots(StorageManagerGUIMenu handler) {
            List<Slot> slots = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                slots.add(handler.getSlot(handler.craftingGridStart + i));
            }
            return List.copyOf(slots);
        }

        @Override
        public Slot getOutputSlot(StorageManagerGUIMenu handler) {
            return handler.slots.get(handler.craftingResultIndex);
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
        }
    }

    /**
     * EMI stack provider for the terminal screen. Resolves the item stack under the mouse cursor
     * from the crafting grid and the player's personal inventory (main + hotbar), so that EMI
     * shortcut keys (R = view recipes, U = view uses) work when the terminal GUI is open.
     *
     * <p>Without this provider, {@link dev.emi.emi.screen.EmiScreenManager#getHoveredStack}
     * falls back to {@code HandledScreenAccessor#getFocusedSlot()}, which is null unless the
     * player has previously clicked a slot — so hovering alone yields an empty stack and EMI
     * does nothing on key press.
     */
    private static final class TerminalStackProvider implements EmiStackProvider<StorageManagerGUIScreen> {

        @Override
        public EmiStackInteraction getStackAt(StorageManagerGUIScreen screen, int mouseX, int mouseY) {
            double x = mouseX - screen.getGuiLeft();
            double y = mouseY - screen.getGuiTop();
            Slot slot = screen.getSlotAtRelative(x, y);
            if (slot != null) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    // clickable=false: teclas de atalho (R/U) podem resolver o stack sob o mouse,
                    // mas cliques não são interceptados pelo EMI — deixam o Minecraft tratar normalmente.
                    return new EmiStackInteraction(EmiStack.of(stack), null, false);
                }
            }
            return EmiStackInteraction.EMPTY;
        }
    }
}
