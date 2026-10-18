package org.tdddd.yawning_neko_api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.tdddd.yawning_neko_api.data.AdaptationColorConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

/**
 * Suppresses the vanilla hurt flash while the adaptation tint is showing.
 *
 * <p>The red flash is driven by the client-side {@code LivingEntity#hurtTime}, so this needs no mixin
 * and cannot break on a mapping change: each client tick it zeros hurtTime for the short window in
 * which an adaptation event is active on a tinted entity.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class AdaptationHurtFlashSuppressor {

    private AdaptationHurtFlashSuppressor() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.hurtTime > 0
                    && AdaptationColorConfig.applies(living)
                    && DamageAdaptation.getActiveAdaptationEvent(living) >= 0) {
                living.hurtTime = 0;
                living.hurtDuration = 0;
            }
        }
    }
}
