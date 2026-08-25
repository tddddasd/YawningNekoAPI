package org.tdddd.yawning_neko_api.events;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.yawning_neko_api.data.EntityAttackEffectManager;
import org.tdddd.yawning_neko_api.data.EntityUpgradeManager;

import java.util.List;
import java.util.Random;

public class EntityAttackEffectHandler {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public void onEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            EntityUpgradeManager.setupEntityUpgradeLevel(mob);
        }
    }

    @SubscribeEvent
    public void onEntityAttack(LivingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof Mob attacker) {
            LivingEntity target = event.getEntity();
            int currentLevel = EntityUpgradeManager.getEntityUpgradeLevel(attacker);
            List<EntityAttackEffectManager.AttackEffect> effects =
                    EntityAttackEffectManager.getAttackEffects(attacker, currentLevel);
            boolean upgraded = attemptPreAttackLevelUp(attacker, effects);

            if (upgraded) {
                int newLevel = EntityUpgradeManager.getEntityUpgradeLevel(attacker);
                effects = EntityAttackEffectManager.getAttackEffects(attacker, newLevel);
            }

            for (EntityAttackEffectManager.AttackEffect effect : effects) {
                float roll = RANDOM.nextFloat();
                if (roll <= effect.getProbability()) {
                    applyEffect(target, effect);
                }
            }
        }
    }

    private void applyEffect(LivingEntity target, EntityAttackEffectManager.AttackEffect effectConfig) {
        MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                new net.minecraft.resources.ResourceLocation(effectConfig.getEffectId())
        );

        if (mobEffect != null) {
            int duration = effectConfig.getDuration();
            if (duration == -1) {
                duration = Integer.MAX_VALUE;
            }

            MobEffectInstance effectInstance = new MobEffectInstance(
                    mobEffect,
                    duration,
                    effectConfig.getAmplifier(),
                    effectConfig.isAmbient(),
                    effectConfig.hasVisibleParticles(),
                    effectConfig.shouldShowIcon()
            );

            target.addEffect(effectInstance);
        }
    }

    private boolean attemptPreAttackLevelUp(Mob attacker, List<EntityAttackEffectManager.AttackEffect> effects) {
        boolean upgraded = false;
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType()).toString();
        EntityUpgradeManager.UpgradeConfig config = EntityUpgradeManager.getUpgradeConfig(entityId);

        if (config == null) {
            return false;
        }

        for (EntityAttackEffectManager.AttackEffect effect : effects) {
            String effectId = effect.getEffectId();

            if (config.levelUpChances.containsKey(effectId)) {
                float upgradeChance = config.levelUpChances.get(effectId);
                float roll = RANDOM.nextFloat();

                if (roll <= upgradeChance) {
                    int currentEffectLevel = EntityUpgradeManager.getEffectSpecificLevel(attacker, effectId);
                    int maxEffectLevel = EntityAttackEffectManager.getMaxEffectLevel(attacker, effectId);

                    if (currentEffectLevel < maxEffectLevel) {
                        EntityUpgradeManager.increaseEffectSpecificLevel(attacker, effectId, maxEffectLevel);
                        upgraded = true;
                    }
                }
            }
        }

        return upgraded;
    }
}