package com.minecraftabnormals.mindful_eating.client;

import com.minecraftabnormals.abnormals_core.common.world.storage.tracking.IDataManager;
import com.minecraftabnormals.mindful_eating.compat.AppleskinCompat;
import com.minecraftabnormals.mindful_eating.compat.FarmersDelightCompat;
import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effects;
import net.minecraft.util.FoodStats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.diet.api.DietApi;
import top.theillusivec4.diet.api.IDietGroup;

import java.util.Random;
import java.util.Set;

@Mod.EventBusSubscriber(modid = MindfulEating.MODID, value = Dist.CLIENT)
public class HungerOverlay {

    public static final ResourceLocation GUI_HUNGER_ICONS_LOCATION = new ResourceLocation(MindfulEating.MODID, "textures/gui/hunger_icons.png");
    public static final ResourceLocation GUI_NOURISHMENT_ICONS_LOCATION = new ResourceLocation(MindfulEating.MODID, "textures/gui/nourished_icons.png");
    public static final ResourceLocation GUI_SATURATION_ICONS_LOCATION = new ResourceLocation(MindfulEating.MODID, "textures/gui/saturation_icons.png");
    public static final ResourceLocation GUI_EMPTY_ICONS_LOCATION = new ResourceLocation(MindfulEating.MODID, "textures/gui/empty_icons.png");


    private static final Minecraft MC = Minecraft.getInstance();

    private static final Random RANDOM = new Random();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hungerIconOverride(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.FOOD) {
            MC.textureManager.bind(GUI_EMPTY_ICONS_LOCATION);
            if (ModList.get().isLoaded("farmersdelight")) {
                FarmersDelightCompat.resetNourishedHungerOverlay();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hungerIconOverride(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.FOOD) {
            ClientPlayerEntity player = MC.player;
            IDataManager playerManager = ((IDataManager) player);
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(ForgeRegistries.ITEMS.getValue(playerManager.getValue(MindfulEating.LAST_FOOD))));
            if (groups.isEmpty()) return;

            renderHungerIcons(event.getWindow(), event.getMatrixStack(), event.getPartialTicks(), player, groups.toArray(new IDietGroup[0]));
        }
    }

    private static void renderHungerIcons(MainWindow window, MatrixStack matrixStack, float partialTicks, ClientPlayerEntity player, IDietGroup[] groups) {
        MC.textureManager.bind(GUI_HUNGER_ICONS_LOCATION);

        IDataManager playerManager = ((IDataManager) player);

        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        MC.getProfiler().push("food");

        RenderSystem.enableBlend();

        int left = width / 2 + 91;
        int top = height - ForgeIngameGui.right_height + 10;
        ForgeIngameGui.right_height += 10;

        FoodStats stats = player.getFoodData();
        int level = stats.getFoodLevel();
        float modifiedSaturation = Math.min(stats.getSaturationLevel(), 20);

        for (int i = 0; i < 10; ++i)
        {
            int idx = i * 2 + 1;
            int x = left - i * 8 - 9;
            int y = top;
            int icon = 0;

            FoodGroups foodGroup = FoodGroups.byDietGroup(groups[i % groups.length]);
            int group = foodGroup != null ? foodGroup.getTextureOffset() : 0;
            byte background = 0;

            if (player.hasEffect(Effects.HUNGER))
            {
                icon += 36;
                background = 13;
            }

            if (ModList.get().isLoaded("farmersdelight")
                    && player.hasEffect(ForgeRegistries.POTIONS.getValue(new ResourceLocation("farmersdelight:nourished")))
                    && FarmersDelightCompat.NOURISHED_HUNGER_OVERLAY) {
                FarmersDelightCompat.setNourishedHungerOverlay(false);
                MC.textureManager.bind(GUI_NOURISHMENT_ICONS_LOCATION);
                icon -= player.hasEffect(Effects.HUNGER) ? 45 : 27;
                background = 0;
            }

            if (player.getFoodData().getSaturationLevel() <= 0.0F && partialTicks % (level * 3 + 1) == 0)
            {
                y = top + (RANDOM.nextInt(3) - 1);
            }

            blit(matrixStack, x, y, background * 9, group, 9, 9);

            if (idx < level) {
                blit(matrixStack, x, y, icon + 36, group, 9, 9);
            }
            else if (idx == level)
                blit(matrixStack, x, y, icon + 45, group, 9, 9);

            MC.textureManager.bind(GUI_SATURATION_ICONS_LOCATION);

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

                blit(matrixStack, x, y, u, v, 9, 9);
            }

            if (idx <= level) {
                int tick = MC.gui.getGuiTicks() % 20;
                if (playerManager.getValue(MindfulEating.SHEEN_COOLDOWN) > 0 && ((tick < idx + level / 4 && tick > idx - level / 4)
                        || (tick == 49 && i == 0))) {
                    blit(matrixStack, x, y, 45, group, 9, 9);
                }
            }

            MC.textureManager.bind(GUI_HUNGER_ICONS_LOCATION);

        }

        RenderSystem.disableBlend();
        MC.getProfiler().pop();

        MC.textureManager.bind(AbstractGui.GUI_ICONS_LOCATION);
    }

    public static void blit(MatrixStack matrixStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        AbstractGui.blit(matrixStack, x, y, -90, uOffset, vOffset, uWidth, vHeight, 45, 126);
    }
}
