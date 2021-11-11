package com.minecraftabnormals.mindful_eating.core.mixin;

import net.minecraft.util.FoodStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodStats.class)
public abstract class TestMixin {

    @Inject(at = @At("HEAD"), method = "addExhaustion")
    public void addExhaustion(float exhaustion, CallbackInfo ci) {
        // if (exhaustion != 0.0F) System.out.println(exhaustion);
    }
}
