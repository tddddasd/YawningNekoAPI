package org.tdddd.yawning_neko_api.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

import java.util.function.Supplier;

public class AdaptationEventSyncPacket {
    private final int entityId;
    private final int eventType;

    public AdaptationEventSyncPacket(int entityId, int eventType) {
        this.entityId = entityId;
        this.eventType = eventType;
    }

    public AdaptationEventSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.eventType = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(eventType);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client side only: getSender() is null on the client, so the entity has to be resolved
            // through the client level instead of the (absent) sending player.
            Minecraft mc = Minecraft.getInstance();
            // Record by entity id first: a freshly spawned mob may not be resolvable yet, and the
            // renderer reads this key, so the tint works without relogging.
            org.apache.logging.log4j.LogManager.getLogger("yawning-neko-api")
                    .info("event received: id={} type={} level={} gameTime={}",
                            entityId, eventType, mc.level == null ? null : mc.level.dimension().location(),
                            mc.level == null ? -1L : mc.level.getGameTime());
            DamageAdaptation.updateClientAdaptationEventById(entityId, eventType, mc.level);
            if (mc.level != null) {
                Entity entity = mc.level.getEntity(entityId);
                if (entity instanceof LivingEntity living) {
                    DamageAdaptation.updateClientAdaptationEvent(living, eventType);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}