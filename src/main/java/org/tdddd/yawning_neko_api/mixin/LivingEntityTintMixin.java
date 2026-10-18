package org.tdddd.yawning_neko_api.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.yawning_neko_api.data.AdaptationColorConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

/**
 * Vanilla-model tint, copied from epca's working mixin: the model draw call is redirected and its RGB
 * arguments are multiplied. A redirect is matched against the invocation itself, so the tint survives
 * argument shifts and other mixins rewriting the same call.
 *
 * <p>The colour comes from {@link AdaptationColorConfig} (adaptation_color_models/*.json).</p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityTintMixin {

    @Unique
    private static final ThreadLocal<LivingEntity> YAWNING_CURRENT_ENTITY = new ThreadLocal<>();

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void yawning$captureEntity(LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       CallbackInfo ci) {
        YAWNING_CURRENT_ENTITY.set(entity);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void yawning$releaseEntity(LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       CallbackInfo ci) {
        YAWNING_CURRENT_ENTITY.remove();
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void yawning$renderTintedModel(EntityModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer,
                                           int packedLight, int packedOverlay,
                                           float red, float green, float blue, float alpha) {
        LivingEntity entity = YAWNING_CURRENT_ENTITY.get();
        if (entity != null) {
            int eventType = DamageAdaptation.getActiveAdaptationEvent(entity);
            if (eventType >= 0 && AdaptationColorConfig.applies(entity)) {
                int argb = AdaptationColorConfig.colorFor(eventType);
                if (argb != -1) {
                    red *= ((argb >> 16) & 0xFF) / 255.0F;
                    green *= ((argb >> 8) & 0xFF) / 255.0F;
                    blue *= (argb & 0xFF) / 255.0F;
                    alpha *= ((argb >>> 24) & 0xFF) / 255.0F;
                }
            }
        }
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
