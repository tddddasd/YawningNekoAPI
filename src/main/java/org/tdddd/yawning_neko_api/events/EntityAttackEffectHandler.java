package org.tdddd.yawning_neko_api.events;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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
    public void onEntityAttack(LivingDamageEvent.Post event) {
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
        
        Identifier effectKey = Identifier.tryParse(effectConfig.getEffectId());
        if (effectKey == null) return;
        Holder<MobEffect> mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectKey).orElse(null);

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
        
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString();
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
