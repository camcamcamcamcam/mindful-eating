package com.minecraftabnormals.mindful_eating.client;

import com.minecraftabnormals.mindful_eating.compat.AppleskinCompat;
import com.minecraftabnormals.mindful_eating.compat.FarmersDelightCompat;
import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teamabnormals.blueprint.common.world.storage.tracking.IDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.diet.api.DietApi;
import top.theillusivec4.diet.api.IDietGroup;

import java.util.Random;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MindfulEating.MODID, value = Dist.CLIENT)
public class HungerOverlay {

    public static final ResourceLocation GUI_HUNGER_ICONS_LOCATION = new ResourceLocation(MindfulEating.MODID, "textures/gui/hunger_icons.png");
    public static final ResourceLocation GUI_NOURISHMENT_ICONS_LOCATION = new ResourceLocation(MindfulEating.MODID, "textures/gui/nourished_icons.png");
    public static final ResourceLocation GUI_SATURATION_ICONS_LOCATION = new ResourceLocation(MindfulEating.MODID, "textures/gui/saturation_icons.png");

    private static final Minecraft minecraft = Minecraft.getInstance();

    private static final Random random = new Random();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hungerIconOverride(RenderGameOverlayEvent.PostLayer event) {
        if (event.getOverlay() == ForgeIngameGui.FOOD_LEVEL_ELEMENT) {
            if (ModList.get().isLoaded("farmersdelight")) {
                FarmersDelightCompat.resetNourishedHungerOverlay();
            }
        }
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new HungerOverlay());
        OverlayRegistry.enableOverlay(ForgeIngameGui.FOOD_LEVEL_ELEMENT, false);

        OverlayRegistry.registerOverlayAbove(ForgeIngameGui.FOOD_LEVEL_ELEMENT, "Mindful Eating Hunger", ((gui, poseStack, partialTicks, width, height) -> {
            boolean isMounted = minecraft.player != null && minecraft.player.getVehicle() instanceof LivingEntity;
            if (!isMounted && !minecraft.options.hideGui && gui.shouldDrawSurvivalElements()) {
                renderHungerIcons(gui, poseStack);
            }
        }));
    }

    public static void renderHungerIcons(ForgeIngameGui gui, PoseStack poseStack) {
        Player player = minecraft.player;
        IDataManager playerManager = ((IDataManager) player);
        Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(ForgeRegistries.ITEMS.getValue(playerManager.getValue(MindfulEating.LAST_FOOD))));
        if (groups.isEmpty()) return;

        FoodData foodData = player.getFoodData();

        int top = minecraft.getWindow().getGuiScaledHeight() - gui.right_height;
        int left = minecraft.getWindow().getGuiScaledWidth() / 2 + 91;

        drawHungerIcons(player, foodData, top, left, poseStack, playerManager , groups.toArray(new IDietGroup[0]));
    }

    public static void drawHungerIcons(Player player, FoodData stats, int top, int left, PoseStack poseStack, IDataManager playerManager, IDietGroup[] groups) {
        RenderSystem.setShaderTexture(0, GUI_HUNGER_ICONS_LOCATION);
        RenderSystem.enableBlend();
        minecraft.getProfiler().push("food");

        int level = stats.getFoodLevel();
        int ticks = minecraft.gui.getGuiTicks();
        float modifiedSaturation = Math.min(stats.getSaturationLevel(), 20);

        for (int i = 0; i < 10; ++i) {
            int idx = i * 2 + 1;
            int x = left - i * 8 - 9;
            int y = top;
            int icon = 0;

            FoodGroups foodGroup = FoodGroups.byDietGroup(groups[i % groups.length]);
            int group = foodGroup != null ? foodGroup.getTextureOffset() : 0;
            byte background = 0;

            if (player.hasEffect(MobEffects.HUNGER))
            {
                icon += 36;
                background = 13;
            }

            if (ModList.get().isLoaded("farmersdelight")
                    && player.hasEffect(ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("farmersdelight", "nourishment")))
                    && FarmersDelightCompat.NOURISHED_HUNGER_OVERLAY) {
                FarmersDelightCompat.setNourishedHungerOverlay(false);
                RenderSystem.setShaderTexture(0, GUI_NOURISHMENT_ICONS_LOCATION);
                icon -= player.hasEffect(MobEffects.HUNGER) ? 45 : 27;
                background = 0;
            }

            if (player.getFoodData().getSaturationLevel() <= 0.0F && ticks % (level * 3 + 1) == 0)
            {
                y = top + (random.nextInt(3) - 1);
            }

            minecraft.gui.blit(poseStack, x, y, background * 9, group, 9, 9, 126, 45);
            if (idx < level) {
                minecraft.gui.blit(poseStack, x, y, icon + 36, group, 9, 9, 126, 45);
            }
            else if (idx == level) {
                minecraft.gui.blit(poseStack, x, y, icon + 45, group, 9, 9, 126, 45);
            }

            RenderSystem.setShaderTexture(0, GUI_SATURATION_ICONS_LOCATION);

            if (ModList.get().isLoaded("appleskin") && AppleskinCompat.SHOW_SATURATION_OVERLAY) {
                float effectiveSaturationOfBar = (modifiedSaturation / 2.0F) - i;

                int v = group;
                int u;

                if (effectiveSaturationOfBar >= 1)
                    u = 4 * 9;
                else if (effectiveSaturationOfBar > .75)
                    u = 3 * 9;
                else if (effectiveSaturationOfBar > .5)
                    u = 2 * 9;
                else if (effectiveSaturationOfBar > .25)
                    u = 9;
                else
                    u = 0;

                minecraft.gui.blit(poseStack, x, y, u, v, 9, 9, 126, 45);
            }

            if (idx <= level) {
                int tick = ticks % 20;
                if (playerManager.getValue(MindfulEating.SHEEN_COOLDOWN) > 0 && ((tick < idx + level / 4 && tick > idx - level / 4)
                        || (tick == 49 && i == 0))) {
                    minecraft.gui.blit(poseStack, x, y, 45, group, 9, 9, 126, 45);
                }
            }
            RenderSystem.setShaderTexture(0, GUI_HUNGER_ICONS_LOCATION);
        }
        minecraft.getProfiler().pop();
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
    }
}
