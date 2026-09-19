package org.tdddd.yawning_neko_api.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.data.MinimumDamageManager;

/**
 * 原 1.20.1 监听 {@code LivingAttackEvent}（发生在无敌帧检查之前）。
 * 26.1.2 已移除 LivingAttackEvent，最接近的替代是
 * {@link LivingIncomingDamageEvent}（发生在无敌帧检查之后、伤害结算之前）。
 * 这里保留原有的“直接改血量 + 触发无敌帧”逻辑，不取消事件。
 */
@EventBusSubscriber(modid = Yawning_neko_api.MODID)
public class MinimumDamageEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        LivingEntity attacker = getAttacker(source.getEntity());
        if (attacker == null) return;

        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return;

        MinimumDamageManager manager = MinimumDamageManager.getInstance();
        if (manager == null) return;

        float minDamage = manager.getMinDamage(attacker);
        if (minDamage <= 0) return;

        float newHealth = Math.max(0, target.getHealth() - minDamage);
        target.setHealth(newHealth);

        target.hurtDuration = 10;
        target.hurtTime = target.hurtDuration;
        target.invulnerableTime = 10;

        if (newHealth <= 0.0F) {
            target.die(source);
        }
    }

    private static LivingEntity getAttacker(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) return owner;
        return null;
    }
}
