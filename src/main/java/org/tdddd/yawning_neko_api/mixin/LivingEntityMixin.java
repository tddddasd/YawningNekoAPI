package org.tdddd.yawning_neko_api.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurtMinimum(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(ModDamageTypes.MINIMUM)) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                cir.setReturnValue(false);
                return;
            }
            if (entity.isDeadOrDying()) {
                cir.setReturnValue(false);
                return;
            }
            float newHealth = Math.max(0, entity.getHealth() - amount);
            entity.setHealth(newHealth);
            entity.hurtDuration = 10;
            entity.hurtTime = entity.hurtDuration;
            entity.invulnerableTime = 10;
            if (newHealth <= 0.0F) {
                entity.die(source);
            }
            cir.setReturnValue(true);
        }
    }
}