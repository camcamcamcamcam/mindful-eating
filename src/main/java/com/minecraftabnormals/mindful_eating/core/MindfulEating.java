package com.minecraftabnormals.mindful_eating.core;

import com.minecraftabnormals.abnormals_core.common.world.storage.tracking.DataProcessors;
import com.minecraftabnormals.abnormals_core.common.world.storage.tracking.TrackedData;
import com.minecraftabnormals.abnormals_core.common.world.storage.tracking.TrackedDataManager;
import net.minecraft.item.Food;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

@Mod(MindfulEating.MODID)
@Mod.EventBusSubscriber(modid = MindfulEating.MODID)
public class MindfulEating
{
    public static final String MODID = "mindful_eating";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static HashMap<String, Integer> ORIGINAL_ITEMS = new HashMap<>();

    public static HashMap<String, Food> ORIGINAL_FOODS = new HashMap<>();

    public static final TrackedData<ResourceLocation> LAST_FOOD = TrackedData.Builder.create(DataProcessors.RESOURCE_LOCATION, () -> new ResourceLocation("stick")).enableSaving().build();

    public static final TrackedData<Integer> SHEEN_COOLDOWN = TrackedData.Builder.create(DataProcessors.INT, () -> 0).enableSaving().build();

    public static final TrackedData<Boolean> HURT_OR_HEAL = TrackedData.Builder.create(DataProcessors.BOOLEAN, () -> false).enableSaving().build();

    public MindfulEating() {

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(this);

        TrackedDataManager.INSTANCE.registerData(new ResourceLocation(MindfulEating.MODID, "last_food"), LAST_FOOD);
        TrackedDataManager.INSTANCE.registerData(new ResourceLocation(MindfulEating.MODID, "correct_food"), SHEEN_COOLDOWN);
        TrackedDataManager.INSTANCE.registerData(new ResourceLocation(MindfulEating.MODID, "hurt_or_heal"), HURT_OR_HEAL);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MEConfig.COMMON_SPEC);
    }
}
