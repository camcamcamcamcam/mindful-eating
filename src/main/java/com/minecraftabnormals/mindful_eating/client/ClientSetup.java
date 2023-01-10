package com.minecraftabnormals.mindful_eating.client;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetup {
    public static void init(FMLClientSetupEvent event) {
        HungerOverlay.init();
    }
}
