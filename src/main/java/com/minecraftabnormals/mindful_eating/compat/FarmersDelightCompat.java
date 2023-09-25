package com.minecraftabnormals.mindful_eating.compat;

import com.minecraftabnormals.abnormals_core.common.world.storage.tracking.IDataManager;
import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import top.theillusivec4.diet.api.DietApi;
import top.theillusivec4.diet.api.IDietGroup;
import vectorwing.farmersdelight.blocks.PieBlock;
import vectorwing.farmersdelight.data.ItemTags;
import vectorwing.farmersdelight.setup.Configuration;
import vectorwing.farmersdelight.utils.tags.ModTags;

import java.util.Set;

public class FarmersDelightCompat {

    public static boolean ENABLE_STACKABLE_SOUP_ITEMS = Configuration.ENABLE_STACKABLE_SOUP_ITEMS.get();
    public static boolean NOURISHED_HUNGER_OVERLAY = Configuration.NOURISHED_HUNGER_OVERLAY.get();

    public static boolean TEMPORARY_NOURISHED_HUNGER_OVERLAY = NOURISHED_HUNGER_OVERLAY;

    public static void setNourishedHungerOverlay(boolean flag) {
        TEMPORARY_NOURISHED_HUNGER_OVERLAY = NOURISHED_HUNGER_OVERLAY;
        Configuration.NOURISHED_HUNGER_OVERLAY.set(flag);
    }

    public static void resetNourishedHungerOverlay() {
        NOURISHED_HUNGER_OVERLAY = TEMPORARY_NOURISHED_HUNGER_OVERLAY;
    }

    public static void pieEatenCheck(Block block, PlayerEntity player, ItemStack heldItem) {
        if (block instanceof PieBlock) {
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(block));
            if (player.getFoodData().needsFood() && !groups.isEmpty() && !heldItem.getItem().is(ModTags.KNIVES)) {
                ResourceLocation currentFood = block.asItem().getRegistryName();
                IDataManager playerManager = ((IDataManager) player);
                playerManager.setValue(MindfulEating.LAST_FOOD, currentFood);
            }
        }
    }
}
