package org.tdddd.yawning_neko_api.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.tdddd.yawning_neko_api.data.MinDamageResistanceLoader;

/**
 * Scales the health a {@code setHealth} call takes away.
 *
 * <p>Only a <b>reduction</b> is scaled: healing, or setting the same value, passes through untouched, so
 * nothing about regeneration or damage in general changes. The multiplier comes from
 * {@link MinDamageResistanceLoader}.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySetHealthMixin {

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float yna$scaleSetHealthLoss(float health) {
        LivingEntity self = (LivingEntity) (Object) this;
        float current = self.getHealth();
        if (health >= current) {
            // healing or unchanged: the config only talks about lost health
            return health;
        }
        float multiplier = MinDamageResistanceLoader.multiplierFor(self);
        if (multiplier == MinDamageResistanceLoader.DEFAULT_MULTIPLIER) {
            return health;
        }
        float loss = current - health;
        return current - loss * multiplier;
    }
}
