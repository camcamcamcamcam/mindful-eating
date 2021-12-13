package com.minecraftabnormals.mindful_eating.core.registry.other;

import com.minecraftabnormals.abnormals_core.common.world.storage.tracking.IDataManager;
import com.minecraftabnormals.mindful_eating.compat.FarmersDelightCompat;
import com.minecraftabnormals.mindful_eating.core.MEConfig;
import com.minecraftabnormals.mindful_eating.core.MindfulEating;
import com.minecraftabnormals.mindful_eating.core.ExhaustionSource;
import net.minecraft.block.Block;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SoupItem;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
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
        if (event.getItem().isFood() && event.getEntityLiving() instanceof PlayerEntity) {
            ResourceLocation currentFood = event.getItem().getItem().getRegistryName();
            IDataManager playerManager = ((IDataManager) event.getEntityLiving());
            playerManager.setValue(MindfulEating.LAST_FOOD, currentFood);

            if (ModList.get().isLoaded("farmersdelight") && FarmersDelightCompat.ENABLE_STACKABLE_SOUP_ITEMS)
                return;

            if (event.getItem().getItem() instanceof SoupItem) {
                event.getItem().shrink(1);
                if (event.getItem().isEmpty()) {
                    event.setResultStack(new ItemStack(Items.BOWL));
                } else {
                    if (!((PlayerEntity)event.getEntityLiving()).abilities.isCreativeMode) {
                        ItemStack itemstack = new ItemStack(Items.BOWL);
                        PlayerEntity playerentity = (PlayerEntity) event.getEntityLiving();
                        if (!playerentity.inventory.addItemStackToInventory(itemstack)) {
                            playerentity.dropItem(itemstack, false);
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
        if (block instanceof CakeBlock) {
            ResourceLocation currentFood = block.asItem().getRegistryName();
            IDataManager playerManager = ((IDataManager) event.getEntityLiving());
            playerManager.setValue(MindfulEating.LAST_FOOD, currentFood);
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
        PlayerEntity player = event.getPlayer();
        float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.MINE);
        player.addExhaustion(0.005F * ratio);
    }

    // when the player deals damage
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        PlayerEntity player = event.getPlayer();
        float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.ATTACK);
        player.addExhaustion(0.1F * ratio);
    }

    // when the player takes damage
    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.HURT);
            player.addExhaustion(event.getSource().getHungerDamage() * ratio);
        }
    }

    // when the player naturally regenerates or heals from potion effects
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.HEAL);
            player.addExhaustion(6.0F * event.getAmount() * ratio);
        }
    }

    // when the player jumps
    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            float ratio = exhaustionReductionLongSheen(player, ExhaustionSource.JUMP);
            if (player.isSprinting()) {
                player.addExhaustion(0.2F * ratio);
            } else {
                player.addExhaustion(0.05F * ratio);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START)
            return;

        PlayerEntity player = event.player;
        IDataManager playerManager = ((IDataManager) player);

        if (player.getActivePotionEffects().size() != 0) {
                for (EffectInstance effect : player.getActivePotionEffects()) {
                    if (effect.getPotion() == Effects.HUNGER) {
                        player.addExhaustion(0.0025F * (float) (player.getActivePotionEffect(Effects.HUNGER).getAmplifier() + 1) * exhaustionReductionShortSheen(player, ExhaustionSource.EFFECT));
                        break;
                    }
                }
            }

        float reduction = 0;

        double disX = player.getPosX() - player.lastTickPosX;
        double disY = player.getPosY() - player.lastTickPosY;
        double disZ = player.getPosZ() - player.lastTickPosZ;

        if (player.world.isRemote ^ playerManager.getValue(MindfulEating.HURT_OR_HEAL)) {
            playerManager.setValue(MindfulEating.SHEEN_COOLDOWN, max(0, playerManager.getValue(MindfulEating.SHEEN_COOLDOWN) - 1));
        }

        if (player.getMotion().length() == 0.0 || disX == 0.0 && disZ == 0.0) {
            return;
        }

        int distance = Math.round(MathHelper.sqrt(disX * disX + disZ * disZ) * 100.0F);

        if (player.isSwimming() || player.areEyesInFluid(FluidTags.WATER)) {
            reduction = 0.0001F * exhaustionReductionShortSheen(player, ExhaustionSource.SWIM) * Math.round(MathHelper.sqrt(disX * disX + disY * disY + disZ * disZ) * 100.0F);
        } else if (player.isInWater()) {
            reduction = 0.0001F * exhaustionReductionShortSheen(player, ExhaustionSource.SWIM) * distance;
        } else if (player.isOnGround() && player.isSprinting()) {
            reduction = 0.001F * exhaustionReductionShortSheen(player, ExhaustionSource.SPRINT) * distance;
        }

        player.getFoodStats().addExhaustion(reduction);
    }


    public static float exhaustionReductionShortSheen(PlayerEntity player, ExhaustionSource source) {
        return exhaustionReductionLongSheen(player, source, 7);
    }

    public static float exhaustionReductionLongSheen(PlayerEntity player, ExhaustionSource source) {
        return exhaustionReductionLongSheen(player, source, 15); // used to be 20
        // TODO healing makes the sheen keep going for ages for some reason
    }

    public static float exhaustionReductionLongSheen(PlayerEntity player, ExhaustionSource source, int cooldown) {
        IDataManager playerManager = ((IDataManager) player);

        playerManager.setValue(MindfulEating.HURT_OR_HEAL, source == ExhaustionSource.HURT || source == ExhaustionSource.HEAL);

        if (!MEConfig.COMMON.proportionalDiet.get()) {
            Set<IDietGroup> groups = DietApi.getInstance().getGroups(player, new ItemStack(ForgeRegistries.ITEMS.getValue(playerManager.getValue(MindfulEating.LAST_FOOD))));

            for (IDietGroup group : groups) {
                for (String configGroup : MEConfig.COMMON.foodGroupExhaustion[source.ordinal()].get().split("/"))
                if (group.getName().equals(configGroup)){
                    playerManager.setValue(MindfulEating.SHEEN_COOLDOWN, cooldown);
                    return -MEConfig.COMMON.exhaustionReduction.get().floatValue();
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
