package net.interplay.simplestoragemanager.integration;

import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapedDisplay;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.interplay.simplestoragemanager.recipe.StorageManagerUpgradeRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registra o display da receita de upgrade no lado do servidor, para que o REI o sincronize
 * com os clientes. O recipe usa serializer customizado e {@code isSpecial() == true}, então o
 * gerador de displays padrão do REI (apenas ShapedRecipe/ShapelessRecipe) não o enxerga.
 */
public final class ReiUpgradeRecipePlugin implements REICommonPlugin {

    // Vanilla 1.21.8 marca Ingredient.items() como deprecated, mas é a única forma de ler os itens.
    @SuppressWarnings("deprecation")
    @Override
    public void registerDisplays(ServerDisplayRegistry registry) {
        registry.beginRecipeFiller(StorageManagerUpgradeRecipe.class)
                .filterType(RecipeType.CRAFTING)
                .fill(holder -> {
                    StorageManagerUpgradeRecipe recipe = holder.value();
                    List<EntryIngredient> inputs = new ArrayList<>();
                    for (Optional<Ingredient> ingredient : recipe.pattern.ingredients()) {
                        List<EntryStack<?>> stacks = new ArrayList<>();
                        ingredient.ifPresent(value -> value.items()
                                .forEach(holderItem -> stacks.add(EntryStacks.of(new ItemStack(holderItem.value())))));
                        inputs.add(stacks.isEmpty() ? EntryIngredient.empty() : EntryIngredient.of(stacks));
                    }
                    List<EntryIngredient> outputs = List.of(
                            EntryIngredient.of(EntryStacks.of(recipe.getResultItem(RegistryAccess.EMPTY))));
                    return new DefaultCustomShapedDisplay(inputs, outputs, Optional.of(holder.id().location()),
                            recipe.pattern.width(), recipe.pattern.height());
                });
    }
}
