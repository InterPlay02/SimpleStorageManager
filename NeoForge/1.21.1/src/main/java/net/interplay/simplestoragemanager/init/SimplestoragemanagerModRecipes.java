package net.interplay.simplestoragemanager.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.recipe.StorageManagerUpgradeRecipe;

public final class SimplestoragemanagerModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> REGISTRY =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, SimplestoragemanagerMod.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageManagerUpgradeRecipe>> STORAGE_MANAGER_UPGRADE =
            REGISTRY.register("storage_manager_upgrade", () -> StorageManagerUpgradeRecipe.StorageManagerUpgradeRecipeSerializer.INSTANCE);

    private SimplestoragemanagerModRecipes() {
    }
}
