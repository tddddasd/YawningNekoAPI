package org.tdddd.yawning_neko_api.client;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.tdddd.yawning_neko_api.data.AdaptationColorConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

/**
 * Suppresses the vanilla hurt flash while the adaptation tint is showing. 26.1 dropped
 * ClientTickEvent, so this rides EntityTickEvent.Post and only acts on the client side.
 */
@OnlyIn(Dist.CLIENT)
public final class AdaptationHurtFlashSuppressor {

    private AdaptationHurtFlashSuppressor() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!living.level().isClientSide()) {
            return;
        }
        if (living.hurtTime > 0
                && AdaptationColorConfig.applies(living)
                && DamageAdaptation.getActiveAdaptationEvent(living) >= 0) {
            living.hurtTime = 0;
            living.hurtDuration = 0;
        }
    }
}