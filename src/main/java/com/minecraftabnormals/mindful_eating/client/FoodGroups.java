package com.minecraftabnormals.mindful_eating.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import top.theillusivec4.diet.api.IDietGroup;

import javax.annotation.Nullable;
import java.util.Map;

public enum FoodGroups {
    PROTEINS("proteins", 0),
    FRUITS("fruits", 1),
    VEGETABLES("vegetables", 2),
    GRAINS("grains", 3),
    SUGARS("sugars", 4);

    private static final Map<String, FoodGroups> BY_DIET_GROUP = new Object2ObjectOpenHashMap<>();

    private final String dietGroup;
    private final int textureOffset;

    FoodGroups(String dietGroup, int textureOffset) {
        this.dietGroup = dietGroup;
        this.textureOffset = textureOffset * 9;
    }

    public int getTextureOffset() {
        return this.textureOffset;
    }

    @Nullable
    public static FoodGroups byDietGroup(IDietGroup group) {
        return BY_DIET_GROUP.get(group.getName());
    }

    static {
        for (FoodGroups group : values()) {
            BY_DIET_GROUP.put(group.dietGroup, group);
        }
    }
}
