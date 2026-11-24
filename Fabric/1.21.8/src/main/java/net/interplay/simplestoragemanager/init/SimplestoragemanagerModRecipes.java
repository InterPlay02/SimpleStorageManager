package net.interplay.simplestoragemanager.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.recipe.StorageManagerUpgradeRecipe;

/** Registers the special recipe that upgrades a terminal in the crafting grid. */
public final class SimplestoragemanagerModRecipes {
    public static final RecipeSerializer<StorageManagerUpgradeRecipe> STORAGE_MANAGER_UPGRADE =
            StorageManagerUpgradeRecipe.StorageManagerUpgradeRecipeSerializer.INSTANCE;

    private SimplestoragemanagerModRecipes() { }

    public static void register() {
        // BuiltInRegistries holds the live registry; Registries only holds resource keys.
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(SimplestoragemanagerMod.MODID, "storage_manager_upgrade"),
                STORAGE_MANAGER_UPGRADE);
    }
}
