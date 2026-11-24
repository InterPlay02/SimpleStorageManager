package net.interplay.simplestoragemanager.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.interplay.simplestoragemanager.item.inventory.StorageManagerInventoryCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric has no item-toss callback; close the terminal when its item is dropped. */
@Mixin(LivingEntity.class)
public abstract class ItemTossMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"))
    private void onItemDropped(ItemStack stack, boolean throwRandomly, boolean retainOwnership,
                               CallbackInfoReturnable<ItemEntity> info) {
        if ((Object) this instanceof Player player) {
            StorageManagerInventoryCapability.onTerminalDropped(stack, player);
        }
    }
}
