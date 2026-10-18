package org.tdddd.yawning_neko_api.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.yawning_neko_api.data.AdaptationColorConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * GeckoLib tint at the vertex level.
 *
 * <p>renderRecursively (a concrete GeoEntityRenderer method that epca's renderers do not override)
 * calls renderCubesOfBone with the model RGBA; GeckoLib's render types take that vertex colour, which
 * is why RenderSystem#setShaderColor had no visible effect. The redirect multiplies those RGB values
 * with the colour from AdaptationColorConfig, mirroring the vanilla EntityModel#renderToBuffer path.</p>
 */
@Pseudo
@OnlyIn(Dist.CLIENT)
@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(targets = "software.bernie.geckolib.renderer.GeoEntityRenderer")
public abstract class GeoEntityTintMixin {

    @Unique
    private static final ThreadLocal<Entity> YAWNING_GEO_ENTITY = new ThreadLocal<>();

    @Inject(method = "preRender", at = @At("HEAD"), remap = false, require = 0)
    private void yawning$captureGeoEntity(PoseStack poseStack, Entity entity, BakedGeoModel model,
                                          MultiBufferSource bufferSource, VertexConsumer buffer,
                                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                          float red, float green, float blue, float alpha, CallbackInfo ci) {
        YAWNING_GEO_ENTITY.set(entity);
    }

    @Inject(method = "doPostRenderCleanup", at = @At("RETURN"), remap = false, require = 0)
    private void yawning$releaseGeoEntity(CallbackInfo ci) {
        YAWNING_GEO_ENTITY.remove();
    }

    @Redirect(method = "renderRecursively",
            at = @At(value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/renderer/GeoEntityRenderer;renderCubesOfBone(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
                    remap = false),
            remap = false, require = 0)
    private void yawning$tintGeo(GeoRenderer renderer, PoseStack poseStack, GeoBone bone,
                                 VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                 float red, float green, float blue, float alpha) {
        Entity entity = YAWNING_GEO_ENTITY.get();
        if (entity instanceof LivingEntity living) {
            int eventType = DamageAdaptation.getActiveAdaptationEvent(living);
            if (eventType >= 0 && AdaptationColorConfig.applies(living)) {
                int argb = AdaptationColorConfig.colorFor(eventType);
                if (argb != -1) {
                    red *= ((argb >> 16) & 0xFF) / 255.0F;
                    green *= ((argb >> 8) & 0xFF) / 255.0F;
                    blue *= (argb & 0xFF) / 255.0F;
                    alpha *= ((argb >>> 24) & 0xFF) / 255.0F;
                }
            }
        }
        renderer.renderCubesOfBone(poseStack, bone, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }
}
