package org.tdddd.yawning_neko_api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

/**
 * Client half of {@code AdaptationEventSyncPacket}.
 *
 * <p>This lives in its own class on purpose: the packet class is loaded on a dedicated server too, and
 * a {@code Minecraft} reference inside it would make the server fail to verify the class.</p>
 */
public final class AdaptationClientSync {

    private AdaptationClientSync() {
    }

    public static void apply(int entityId, int eventType) {
        Minecraft mc = Minecraft.getInstance();
        DamageAdaptation.updateClientAdaptationEventById(entityId, eventType, mc.level);
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof LivingEntity living) {
            DamageAdaptation.updateClientAdaptationEvent(living, eventType);
        }
    }
}
