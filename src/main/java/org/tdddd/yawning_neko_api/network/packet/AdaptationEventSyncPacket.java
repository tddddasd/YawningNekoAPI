package org.tdddd.yawning_neko_api.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;

/**
 * 同 {@link AdaptationDataSyncPacket}：SimpleChannel 消息 -> CustomPacketPayload。
 */
public class AdaptationEventSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AdaptationEventSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Yawning_neko_api.MODID, "adaptation_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdaptationEventSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(AdaptationEventSyncPacket::encode, AdaptationEventSyncPacket::new);

    private final int entityId;
    private final int eventType;

    public AdaptationEventSyncPacket(int entityId, int eventType) {
        this.entityId = entityId;
        this.eventType = eventType;
    }

    public AdaptationEventSyncPacket(RegistryFriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.eventType = buf.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(eventType);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Entity entity = ctx.player().level().getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                DamageAdaptation.updateClientAdaptationEvent(living, eventType);
            }
        });
    }
}
