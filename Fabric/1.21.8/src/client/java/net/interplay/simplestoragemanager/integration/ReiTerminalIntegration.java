package net.interplay.simplestoragemanager.integration;

import me.shedaniel.math.Point;
import me.shedaniel.math.impl.PointHelper;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.client.displays.ClientsidedCraftingDisplay;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.recipe.StorageManagerUpgradeRecipe;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Integração REI (Roughly Enough Items) do terminal, descoberta pelo entrypoint
 * {@code rei_client} do fabric.mod.json — sem anotação {@code @REIPluginClient}.
 *
 * <p>Registra o terminal como workstation de crafting, um transfer handler para a grade 3x3
 * do menu, um focused-stack provider para todos os itens da tela e os atalhos de receita/usos.
 */
public final class ReiTerminalIntegration implements REIClientPlugin {

    /** Mesmo id do {@code BuiltinPlugin.CRAFTING} do REI, sem depender do artefato de plugins. */
    private static final CategoryIdentifier<Display> CRAFTING =
            CategoryIdentifier.of("minecraft", "plugins/crafting");

    private static boolean shortcutsRegistered;
    private static Screen boundScreen;

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.addWorkstations(CRAFTING, EntryStacks.of(SimplestoragemanagerModItems.STORAGE_MANAGER));
    }

    // Vanilla 1.21.8 marca Ingredient.items() como deprecated, mas é a única forma de ler os itens.
    @SuppressWarnings("deprecation")
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        // The upgrade recipe uses a custom serializer, so REI's vanilla filling rules skip it.
        // Expose it as a crafting display so the "uses" lookup (U) finds the terminal item.
        // REI alimenta os fillers tanto com a receita crua quanto com RecipeHolder (1.21.2+),
        // dependendo do caminho de reload. Registramos os dois para garantir que o display
        // com os INSUMOS seja criado — sem ele, o U (usos) não encontra a receita.
        registry.beginFiller(StorageManagerUpgradeRecipe.class)
                .fill(recipe -> createDisplay(recipe));
        registry.beginFiller(RecipeHolder.class).fill(holder -> {
            if (holder.value() instanceof StorageManagerUpgradeRecipe recipe) {
                return createDisplay(recipe);
            }
            return null;
        });
    }

    @SuppressWarnings("deprecation")
    private static Display createDisplay(StorageManagerUpgradeRecipe recipe) {
        List<EntryIngredient> inputs = new ArrayList<>();
        for (Optional<Ingredient> ingredient : recipe.pattern.ingredients()) {
            List<EntryStack<?>> stacks = new ArrayList<>();
            ingredient.ifPresent(value -> value.items()
                    .forEach(holder -> stacks.add(EntryStacks.of(new ItemStack(holder.value())))));
            inputs.add(stacks.isEmpty() ? EntryIngredient.empty() : EntryIngredient.of(stacks));
        }
        List<EntryIngredient> outputs = List.of(
                EntryIngredient.of(EntryStacks.of(recipe.getResultItem(RegistryAccess.EMPTY))));
        return new ClientsidedCraftingDisplay.Shaped(inputs, outputs, Optional.empty(),
                recipe.pattern.width(), recipe.pattern.height());
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerFocusedStack(new TerminalFocusedStackProvider());
        registerShortcuts();
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        // Os 9 slots da grade são o destino do botão "+" do REI; o inventário do jogador é a fonte.
        registry.register(SimpleTransferHandler.create(
                StorageManagerGUIMenu.class,
                CRAFTING,
                new SimpleTransferHandler.IntRange(
                        StorageManagerGUIMenu.CRAFTING_GRID_START,
                        StorageManagerGUIMenu.CRAFTING_GRID_START + StorageManagerGUIMenu.CRAFTING_GRID_COUNT)));
    }

    /**
     * Atalhos R/U dentro do terminal: o REI resolve o item focado de forma genérica, que não
     * enxerga os slots desenhados manualmente pelo terminal. Resolvemos o item com a tela e
     * abrimos a busca de receitas/usos diretamente pela API pública do REI.
     */
    private static void registerShortcuts() {
        if (shortcutsRegistered) {
            return;
        }
        shortcutsRegistered = true;
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof StorageManagerGUIScreen terminal) || boundScreen == screen) {
                return;
            }
            boundScreen = screen;
            ScreenEvents.remove(screen).register(removed -> {
                if (removed == boundScreen) {
                    boundScreen = null;
                }
            });
            ScreenKeyboardEvents.allowKeyPress(terminal).register((current, keyCode, scanCode, modifiers) -> {
                ConfigObject config = ConfigObject.getInstance();
                boolean usage = config.getUsageKeybind().matchesKey(keyCode, scanCode);
                boolean recipe = !usage && config.getRecipeKeybind().matchesKey(keyCode, scanCode);
                if (!usage && !recipe) {
                    return true;
                }

                Point mouse = PointHelper.ofMouse();
                ItemStack stack = terminal.getHoveredItem(mouse.x, mouse.y);
                if (stack.isEmpty()) {
                    return true;
                }

                ViewSearchBuilder builder = ViewSearchBuilder.builder();
                if (usage) {
                    builder.addUsagesFor(lookupStack(stack));
                } else {
                    builder.addRecipesFor(lookupStack(stack));
                }
                builder.open();
                return false;
            });
        });
    }

    /**
     * A API REI 20.0.811 devolve {@code CompoundEventResult<EntryStack<?>>}. O tipo é usado
     * totalmente qualificado para não criar um import direto de architectury (dependência
     * transitiva do REI, disponível apenas quando o REI está instalado).
     */
    private static dev.architectury.event.CompoundEventResult<EntryStack<?>> focused(EntryStack<?> stack) {
        return dev.architectury.event.CompoundEventResult.interruptTrue(stack);
    }

    private static dev.architectury.event.CompoundEventResult<EntryStack<?>> pass() {
        return dev.architectury.event.CompoundEventResult.pass();
    }

    /** Busca por identidade do item: NBT/components do stack podem impedir o match de usos. */
    private static EntryStack<?> lookupStack(ItemStack stack) {
        return EntryStacks.of(new ItemStack(stack.getItem()));
    }

    /** Responde pelo item sob o mouse em qualquer área da GUI do terminal. */
    private static final class TerminalFocusedStackProvider implements FocusedStackProvider {

        @Override
        public dev.architectury.event.CompoundEventResult<EntryStack<?>> provide(Screen screen, Point mouse) {
            if (!(screen instanceof StorageManagerGUIScreen terminal)) {
                return pass();
            }
            ItemStack stack = terminal.getHoveredItem(mouse.x, mouse.y);
            return stack.isEmpty() ? pass() : focused(lookupStack(stack));
        }

        @Override
        public double getPriority() {
            // O provider padrão do REI usa hoveredSlot e roda em prioridade -10.
            return 100.0;
        }
    }
}
