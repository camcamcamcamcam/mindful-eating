package com.minecraftabnormals.mindful_eating.core.registry.other;

import com.minecraftabnormals.mindful_eating.compat.FarmersDelightCompat;
import com.minecraftabnormals.mindful_eating.core.ExhaustionSource;
import com.minecraftabnormals.mindful_eating.core.MEConfig;
import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import com.teamabnormals.blueprint.common.world.storage.tracking.IDataManager;
import com.teamabnormals.blueprint.core.util.TagUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.diet.api.DietApi;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.DietEvent;
import top.theillusivec4.diet.api.IDietGroup;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.max;

@Mod.EventBusSubscriber(modid = MindfulEating.MODID)
public class MEEvents {

    @SubscribeEvent
    public static void disableDietBuffs(DietEvent.ApplyEffect event) {
        event.setCanceled(!MEConfig.COMMON.nativeDietBuffs.get());
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (event.getItem().isEdible() && event.getEntityLiving() instanceof Player player) {
            ResourceLocation currentFood = event.getItem().getItem().getRegistryName();
            IDataManager playerManager = ((IDataManager) event.getEntityLiving());
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(event.getItem().getItem()));

            if (!groups.isEmpty()) {
                playerManager.setValue(MindfulEating.LAST_FOOD, currentFood);
            }

            if (ModList.get().isLoaded("farmersdelight") && FarmersDelightCompat.ENABLE_STACKABLE_SOUP_ITEMS && !(event.getItem().getItem() instanceof SuspiciousStewItem))
                return;

            if (event.getItem().getItem() instanceof BowlFoodItem || event.getItem().getItem() instanceof SuspiciousStewItem) {
                event.getItem().shrink(1);
                if (event.getItem().isEmpty()) {
                    event.setResultStack(new ItemStack(Items.BOWL));
                } else {
                    if (!player.getAbilities().instabuild) {
                        ItemStack itemstack = new ItemStack(Items.BOWL);
                        if (!player.getInventory().add(itemstack)) {
                            player.drop(itemstack, false);
                        }
                    }

                    event.setResultStack(event.getItem());
                }
            }
        }
    }

    // when the player eats cake
    @SubscribeEvent
    public static void onCakeEaten(PlayerInteractEvent.RightClickBlock event) {
        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
        Player player = event.getPlayer();
        ItemStack heldItem = event.getItemStack();

        if (block instanceof CakeBlock) {
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(block));
            if (player.getFoodData().needsFood() && !groups.isEmpty() && !heldItem.is(TagUtil.itemTag("forge", "tools/knives"))) {
                ResourceLocation currentFood = block.asItem().getRegistryName();
                IDataManager playerManager = ((IDataManager) player);
                playerManager.setValue(MindfulEating.LAST_FOOD, currentFood);
            }
        }

        if (ModList.get().isLoaded("farmersdelight")) {
            FarmersDelightCompat.pieEatenCheck(block, player, heldItem);
        }
    }

    // while the player is harvesting a block
    @SubscribeEvent
    public static void onPlayerMining(PlayerEvent.BreakSpeed event) {
        exhaustionReductionShortSheen(event.getPlayer(), ExhaustionSource.MINE);
    }

    // when the player harvests a block
    @SubscribeEvent
    public static void onBlockHarvested(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.MINE);
        player.causeFoodExhaustion(0.005F * ratio);
    }

    // when the player deals damage
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getPlayer();
        float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.ATTACK);
        player.causeFoodExhaustion(0.1F * ratio);
    }

    // when the player takes damage
    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent event) {
        if (event.getEntityLiving() instanceof Player) {
            Player player = (Player) event.getEntityLiving();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.HURT);
            player.causeFoodExhaustion(event.getSource().getFoodExhaustion() * ratio);
        }
    }

    // when the player naturally regenerates or heals from potion effects
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (event.getEntityLiving() instanceof Player) {
            Player player = (Player) event.getEntityLiving();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.HEAL);
            player.causeFoodExhaustion(6.0F * event.getAmount() * ratio);
        }
    }

    // when the player jumps
    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntityLiving() instanceof Player) {
            Player player = (Player) event.getEntityLiving();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.JUMP);
            if (player.isSprinting()) {
                player.causeFoodExhaustion(0.2F * ratio);
            } else {
                player.causeFoodExhaustion(0.05F * ratio);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START)
            return;

        Player player = event.player;
        IDataManager playerManager = ((IDataManager) player);

        if (player.getActiveEffects().size() != 0) {
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (effect.getEffect() == MobEffects.HUNGER) {
                    player.causeFoodExhaustion(0.0025F * (float) (player.getEffect(MobEffects.HUNGER).getAmplifier() + 1) * exhaustionReductionShortSheen(player, ExhaustionSource.EFFECT));
                    break;
                }
            }
        }

        float reduction = 0;

        double disX = player.getX() - player.xOld;
        double disY = player.getY() - player.yOld;
        double disZ = player.getZ() - player.zOld;

        if (player.level.isClientSide ^ playerManager.getValue(MindfulEating.HURT_OR_HEAL)) {
            playerManager.setValue(MindfulEating.SHEEN_COOLDOWN, max(0, playerManager.getValue(MindfulEating.SHEEN_COOLDOWN) - 1));
        }

        if (player.getDeltaMovement().length() == 0.0 || disX == 0.0 && disZ == 0.0) {
            return;
        }

        int distance = Math.round(Mth.sqrt((float) disX * (float) disX + (float) disZ * (float) disZ) * 100.0F);

        if (player.isSwimming() || player.isEyeInFluid(FluidTags.WATER)) {
            reduction = 0.0001F * exhaustionReductionShortSheen(player, ExhaustionSource.SWIM) * Math.round(Mth.sqrt((float) disX * (float) disX + (float) disZ * (float) disZ) * 100.0F);
        } else if (player.isInWater()) {
            reduction = 0.0001F * exhaustionReductionShortSheen(player, ExhaustionSource.SWIM) * distance;
        } else if (player.isOnGround() && player.isSprinting()) {
            reduction = 0.001F * exhaustionReductionShortSheen(player, ExhaustionSource.SPRINT) * distance;
        }

        player.getFoodData().addExhaustion(reduction);
    }


    public static float exhaustionReductionShortSheen(Player player, ExhaustionSource source) {
        return exhaustionReductionLongSheen(player, source, 7);
    }

    public static float exhaustionReductionLongSheen(Player player, ExhaustionSource source) {
        return exhaustionReductionLongSheen(player, source, 15); // used to be 20
        //TODO healing makes the sheen keep going for ages for some reason, and natural generation is not counted at all
    }

    public static float exhaustionReductionLongSheen(Player player, ExhaustionSource source, int cooldown) {
        IDataManager playerManager = ((IDataManager) player);

        playerManager.setValue(MindfulEating.HURT_OR_HEAL, source == ExhaustionSource.HURT || source == ExhaustionSource.HEAL);

        if (!MEConfig.COMMON.proportionalDiet.get()) {
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(ForgeRegistries.ITEMS.getValue(playerManager.getValue(MindfulEating.LAST_FOOD))));

            for (IDietGroup group : groups) {
                for (String configGroup : MEConfig.COMMON.foodGroupExhaustion[source.ordinal()].get().split("/")) {
                    if (group.getName().equals(configGroup)) {
                        playerManager.setValue(MindfulEating.SHEEN_COOLDOWN, cooldown);
                        return -MEConfig.COMMON.exhaustionReduction.get().floatValue();
                    }
                }
            }

            return 0.0F;

        } else {
            AtomicReference<Float> percentage = new AtomicReference<>(0.0F);
            DietCapability.get(player).ifPresent(tracker -> {
                for (String configGroup : MEConfig.COMMON.foodGroupExhaustion[source.ordinal()].get().split("/"))
                    percentage.set(tracker.getValue(configGroup));
            });
            if (percentage.get() > 0.0F)
                playerManager.setValue(MindfulEating.SHEEN_COOLDOWN, cooldown);
            return max(-percentage.get(), 1.0F);
        }
    }

}
