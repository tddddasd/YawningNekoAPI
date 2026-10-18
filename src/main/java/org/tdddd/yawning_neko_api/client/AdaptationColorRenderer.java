package org.tdddd.yawning_neko_api.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.data.AdaptationColorConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

/**
 * Tints a living entity for {@link AdaptationColorConfig#DURATION_TICKS} ticks after an adaptation
 * event. The tint is applied as a shader colour around the entity render, which covers both the
 * vanilla and the GeckoLib renderers without touching either of them.
 */
@Mod.EventBusSubscriber(modid = Yawning_neko_api.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdaptationColorRenderer {

    private static long renderCalls = 0L;

    private AdaptationColorRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        // Sampled probe: proves the hook fires at all, without flooding the log.
        if ((++renderCalls % 200L) == 0L) {
        }
        int eventType = DamageAdaptation.getActiveAdaptationEvent(entity);
        if (eventType < 0 || !AdaptationColorConfig.applies(entity)) {
            return;
        }
        int argb = AdaptationColorConfig.colorFor(eventType);
        if (argb == -1) {
            return;
        }
        float alpha = ((argb >> 24) & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F, alpha);
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
