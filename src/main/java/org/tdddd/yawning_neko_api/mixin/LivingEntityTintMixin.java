package org.tdddd.yawning_neko_api.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.yawning_neko_api.data.AdaptationColorConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

/**
 * The adaptation colour display.
 *
 * <p>Wraps the entity render in a shader colour instead of redirecting the model draw call: GeckoLib
 * mobs are drawn by GeoEntityRenderer, which extends LivingEntityRenderer but never calls
 * EntityModel#renderToBuffer, so a model-draw redirect silently skips them. This hook fires for both
 * pipelines, and both of their render types read the ColorModulator uniform.</p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityTintMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), require = 0)
    private void yawning$tintPre(LivingEntity entity, float entityYaw, float partialTick,
                                 com.mojang.blaze3d.vertex.PoseStack poseStack,
                                 net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                 int packedLight, CallbackInfo ci) {
        int eventType = DamageAdaptation.getActiveAdaptationEvent(entity);
        if (eventType < 0 || !AdaptationColorConfig.applies(entity)) {
            return;
        }
        int argb = AdaptationColorConfig.colorFor(eventType);
        if (argb == -1) {
            return;
        }
        RenderSystem.setShaderColor(((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F,
                ((argb >>> 24) & 0xFF) / 255.0F);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"), require = 0)
    private void yawning$tintPost(LivingEntity entity, float entityYaw, float partialTick,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                  int packedLight, CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

}
