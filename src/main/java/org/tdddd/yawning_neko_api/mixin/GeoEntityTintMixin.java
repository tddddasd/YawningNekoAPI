package org.tdddd.yawning_neko_api.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.yawning_neko_api.data.AdaptationColorConfig;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * GeckoLib path.
 *
 * <p>GeoRenderer is an interface, and a class mixin cannot target an interface, so the colour is
 * applied around GeoEntityRenderer#preRender and restored in doPostRenderCleanup: both are concrete
 * methods of the renderer class (verified with javap against geckolib-forge-1.20.1-4.8.4).</p>
 */
@Pseudo
@OnlyIn(Dist.CLIENT)
@Mixin(targets = "software.bernie.geckolib.renderer.GeoEntityRenderer")
public abstract class GeoEntityTintMixin {

    @Inject(method = "preRender", at = @At("HEAD"), require = 0)
    private void yawning$geoTintPre(PoseStack poseStack, Entity entity, BakedGeoModel model,
                                    MultiBufferSource bufferSource, VertexConsumer buffer,
                                    boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) {
            return;
        }
        int eventType = DamageAdaptation.getActiveAdaptationEvent(living);
        if (eventType < 0 || !AdaptationColorConfig.applies(living)) {
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

    @Inject(method = "doPostRenderCleanup", at = @At("RETURN"), require = 0)
    private void yawning$geoTintPost(CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
