package com.minecraftabnormals.mindful_eating.compat;

import net.minecraftforge.client.gui.IIngameOverlay;
import vectorwing.farmersdelight.client.gui.NourishmentHungerOverlay;
import vectorwing.farmersdelight.common.Configuration;

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

}
