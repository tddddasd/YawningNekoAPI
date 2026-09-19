package org.tdddd.yawning_neko_api.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.data.EntityImmunityEffectManager;

@EventBusSubscriber(modid = Yawning_neko_api.MODID)
public class EffectImmunityHandler {

    /**
     * 原 1.20.1 用 {@code event.setResult(Event.Result.DENY)}；
     * 26.1.2 的 MobEffectEvent.Applicable 改用自己的三态枚举
     * {@code MobEffectEvent.Applicable.Result.DO_NOT_APPLY}。
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null) return;

        // 26.1.2：MobEffectInstance#getEffect() 返回 Holder<MobEffect>，
        // 且 ForgeRegistries.MOB_EFFECTS -> BuiltInRegistries.MOB_EFFECT
        Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect().value());
        if (effectId == null) return;

        if (EntityImmunityEffectManager.isImmune(entity, effectId.toString())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
