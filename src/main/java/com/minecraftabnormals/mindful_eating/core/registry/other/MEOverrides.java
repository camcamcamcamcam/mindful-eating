package com.minecraftabnormals.mindful_eating.core.registry.other;

import com.google.gson.JsonObject;
import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import net.minecraft.client.Minecraft;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class MEOverrides {

    public static void generateDefaultConfig() {
        JsonObject object = new JsonObject();

        JsonObject hunger = new JsonObject();
        JsonObject saturation = new JsonObject();
        JsonObject speedy = new JsonObject();
        JsonObject stackability = new JsonObject();
        JsonObject gorgable = new JsonObject();

        for (Item item : new Item[]{Items.COOKED_MUTTON, Items.COOKED_RABBIT, Items.COOKED_SALMON, Items.COOKED_COD}) {
            saturation.addProperty(item.getRegistryName().toString(), Math.round(10.0F * item.getFoodProperties().getSaturationModifier() - item.getFoodProperties().getNutrition()) / 10.0F);
        }

        for (Item item : new Item[]{Items.MELON_SLICE, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.COOKED_MUTTON, Items.COOKED_RABBIT,
                Items.COOKED_SALMON, Items.COOKED_COD, Items.BEETROOT, Items.BEETROOT_SOUP}) {
            speedy.addProperty(item.getRegistryName().toString(), true);
        }

        for (Item item: new Item[]{Items.BEETROOT_SOUP,
                Items.MUSHROOM_STEW, Items.SUSPICIOUS_STEW, Items.RABBIT_STEW}) {
            stackability.addProperty(item.getRegistryName().toString(), 16);
        }
        stackability.addProperty(Items.CAKE.getRegistryName().toString(), 64);

        object.add("hunger", hunger);
        object.add("saturation", saturation);
        object.add("speedy", speedy);
        object.add("stackability", stackability);
        object.add("gorgable", gorgable);

        File file = new File(Minecraft.getInstance().gameDirectory,
                Paths.get("..", "src", "main", "resources", "data", MindfulEating.MODID, "food_changes.json").toString()).getAbsoluteFile();

        String data = object.toString();

        try(FileWriter writer = new FileWriter(file)) {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void changeHunger(Item item, int hunger) {
        ObfuscationReflectionHelper.setPrivateValue(FoodProperties.class, item.getFoodProperties(), hunger, "f_38723_");
        // System.out.println("Changed hunger of " + item + " to " + hunger);
    }

    public static void changeSaturation(Item item, float saturation) {
        ObfuscationReflectionHelper.setPrivateValue(FoodProperties.class, item.getFoodProperties(), saturation, "f_38724_");
        // System.out.println("Changed saturation of " + item + " to " + saturation);
    }

    public static void changeFastEating(Item item, boolean fast) {
        ObfuscationReflectionHelper.setPrivateValue(FoodProperties.class, item.getFoodProperties(), fast, "f_38727_");
        // System.out.println("Changed fastEating of " + item + " to " + fast);
    }

    public static void changeCanEatWhenFull(Item item, boolean gorgable) {
        ObfuscationReflectionHelper.setPrivateValue(FoodProperties.class, item.getFoodProperties(), gorgable, "f_38726_");
        // System.out.println("Changed canEatWhenFull of " + item + " to " + gorgable);
    }

    public static void changeStackability(Item item, int size) {
        ObfuscationReflectionHelper.setPrivateValue(Item.class, item, size, "f_41370_");
        // System.out.println("Changed stackability of " + item + " to " + size);
    }

}
