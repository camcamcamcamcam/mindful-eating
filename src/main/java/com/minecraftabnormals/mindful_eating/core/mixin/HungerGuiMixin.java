package com.minecraftabnormals.mindful_eating.core.mixin;

import com.minecraftabnormals.mindful_eating.client.HungerOverlay;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeIngameGui.class)
public class HungerGuiMixin {
    @Inject(method = "renderFood", at = @At(value = "HEAD"))
    public void renderFood(int width, int height, PoseStack poseStack, CallbackInfo ci) {
        RenderSystem.setShaderTexture(0, HungerOverlay.GUI_EMPTY_ICONS_LOCATION);
    }
}
