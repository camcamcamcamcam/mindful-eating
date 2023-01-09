package com.minecraftabnormals.mindful_eating.compat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.HUDOverlayEvent;

public class AppleskinCompat {

    public static boolean SHOW_SATURATION_OVERLAY = ModConfig.SHOW_SATURATION_OVERLAY.get();

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void disableSaturation(HUDOverlayEvent.Saturation event) {
        event.setCanceled(true);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void disableHungerRestored(HUDOverlayEvent.HungerRestored event) {
        event.setCanceled(true);
    }

}
