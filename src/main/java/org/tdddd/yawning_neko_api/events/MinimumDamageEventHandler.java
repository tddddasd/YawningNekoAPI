package org.tdddd.yawning_neko_api.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.yawning_neko_api.data.MinimumDamageManager;

@Mod.EventBusSubscriber
public class MinimumDamageEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
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