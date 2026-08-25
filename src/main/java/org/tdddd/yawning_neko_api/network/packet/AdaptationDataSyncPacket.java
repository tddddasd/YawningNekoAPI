package org.tdddd.yawning_neko_api.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

import java.util.function.Supplier;

public class AdaptationDataSyncPacket {

    private final int entityId;
    private final int adaptationLevel;
    private final String damageTypeKeyStr;
    private final int maxAdaptations;

    public AdaptationDataSyncPacket(int entityId, int adaptationLevel, String damageTypeKeyStr, int maxAdaptations) {
        this.entityId = entityId;
        this.adaptationLevel = adaptationLevel;
        this.damageTypeKeyStr = damageTypeKeyStr;
        this.maxAdaptations = maxAdaptations;
    }

    public AdaptationDataSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.adaptationLevel = buf.readInt();
        this.damageTypeKeyStr = buf.readUtf();
        this.maxAdaptations = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(adaptationLevel);
        buf.writeUtf(damageTypeKeyStr);
        buf.writeInt(maxAdaptations);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = ctx.get().getSender().level().getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                DamageAdaptation.updateClientAdaptationData(living, adaptationLevel, damageTypeKeyStr, maxAdaptations);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}