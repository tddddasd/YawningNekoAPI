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
            // Client side only: this packet only drives the local tint. The dist guard must come
            // first, because Minecraft and the client classes it pulls in do not exist on a
            // dedicated server and would fail to link the moment this method ran there.
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
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
