package org.tdddd.yawning_neko_api.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.data.EntityImmunityEffectManager;

@Mod.EventBusSubscriber(modid = Yawning_neko_api.MODID)
public class EffectImmunityHandler {

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null) return;

        String effectId = ForgeRegistries.MOB_EFFECTS.getKey(effectInstance.getEffect()).toString();

        if (EntityImmunityEffectManager.isImmune(entity, effectId)) {
            event.setResult(Event.Result.DENY);
        }
    }
}