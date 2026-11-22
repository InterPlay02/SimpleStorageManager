package net.interplay.simplestoragemanager.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

import net.interplay.simplestoragemanager.data.TerminalDataComponents;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;

/**
 * Upgrade recipe: converts a basic Storage Manager (center slot) into an Advanced Storage Manager.
 * All components from the basic terminal are preserved on the result. Only a basic (non-advanced)
 * terminal is accepted.
 */
public class StorageManagerUpgradeRecipe implements CraftingRecipe {
    private static final int TERMINAL_SLOT = 4;

    public final ShapedRecipePattern pattern;
    private final String group;
    private final CraftingBookCategory category;
    private final boolean showNotification;

    public StorageManagerUpgradeRecipe(String group, CraftingBookCategory category,
                                       ShapedRecipePattern pattern, boolean showNotification) {
        this.group = group;
        this.category = category;
        this.pattern = pattern;
        this.showNotification = showNotification;
    }

    public StorageManagerUpgradeRecipe(String group, CraftingBookCategory category,
                                       ShapedRecipePattern pattern) {
        this(group, category, pattern, true);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return StorageManagerUpgradeRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        ItemStack result = new ItemStack(SimplestoragemanagerModItems.STORAGE_MANAGER.get());
        result.set(TerminalDataComponents.TERMINAL_ADVANCED.get(), true);
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.pattern.ingredients();
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!this.pattern.matches(input)) return false;
        return isBasicTerminal(input.getItem(TERMINAL_SLOT));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        ItemStack terminal = input.getItem(TERMINAL_SLOT);
        if (!isBasicTerminal(terminal)) return ItemStack.EMPTY;
        ItemStack result = terminal.copy();
        result.set(TerminalDataComponents.TERMINAL_ADVANCED.get(), true);
        return result;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    private static boolean isBasicTerminal(ItemStack stack) {
        return stack.is(SimplestoragemanagerModItems.STORAGE_MANAGER.get())
                && !Boolean.TRUE.equals(stack.get(TerminalDataComponents.TERMINAL_ADVANCED.get()));
    }

    public static final class StorageManagerUpgradeRecipeSerializer
            implements RecipeSerializer<StorageManagerUpgradeRecipe> {
        public static final StorageManagerUpgradeRecipeSerializer INSTANCE =
                new StorageManagerUpgradeRecipeSerializer();

        private StorageManagerUpgradeRecipeSerializer() {}

        private static final MapCodec<StorageManagerUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(recipe -> recipe.category),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(recipe -> recipe.showNotification)
        ).apply(instance, StorageManagerUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, StorageManagerUpgradeRecipe> STREAM_CODEC = StreamCodec.of(
                StorageManagerUpgradeRecipeSerializer::toNetwork,
                StorageManagerUpgradeRecipeSerializer::fromNetwork
        );

        @Override
        public MapCodec<StorageManagerUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StorageManagerUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static StorageManagerUpgradeRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
            boolean showNotification = buffer.readBoolean();
            return new StorageManagerUpgradeRecipe(group, category, pattern, showNotification);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, StorageManagerUpgradeRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            buffer.writeBoolean(recipe.showNotification);
        }
    }
}
