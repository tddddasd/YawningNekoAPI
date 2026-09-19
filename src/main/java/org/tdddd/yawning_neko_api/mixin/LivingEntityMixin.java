package org.tdddd.yawning_neko_api.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

/**
 * 26.1.2 的伤害入口已重构：
 * <ul>
 *   <li>1.20.1 的 {@code LivingEntity#hurt(DamageSource, float) : boolean} 不存在了；</li>
 *   <li>现在的 {@code Entity#hurt(DamageSource, float) : void} 是 final 的，且只是转发到
 *       {@code Entity#hurtServer(ServerLevel, DamageSource, float) : boolean}；</li>
 *   <li>真正可注入的实现是 {@code LivingEntity#hurtServer(ServerLevel, DamageSource, float)}。</li>
 * </ul>
 * 因此注入点由 {@code hurt} 改为 {@code hurtServer}（javap 校验过 26.1.2 的签名）。
 * 由于该方法在无敌帧检查之后才执行，原“无视无敌帧”的语义改由数据包标签
 * {@code minecraft:bypasses_invulnerability} / {@code minecraft:bypasses_cooldown}
 * （见 {@code data/minecraft/tags/damage_type/}）实现，行为与原版保持一致。
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurtMinimum(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
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
