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
 * 原 1.20.1 是 SimpleChannel 的普通消息类（FriendlyByteBuf + NetworkEvent.Context）。
 * 26.1.2 改为 {@link CustomPacketPayload}：自带 TYPE 与 STREAM_CODEC，处理函数接收
 * {@link IPayloadContext}。写出的数据与顺序保持不变。
 */
public class AdaptationDataSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AdaptationDataSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Yawning_neko_api.MODID, "adaptation_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdaptationDataSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(AdaptationDataSyncPacket::encode, AdaptationDataSyncPacket::new);

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

    public AdaptationDataSyncPacket(RegistryFriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.adaptationLevel = buf.readInt();
        this.damageTypeKeyStr = buf.readUtf();
        this.maxAdaptations = buf.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(adaptationLevel);
        buf.writeUtf(damageTypeKeyStr);
        buf.writeInt(maxAdaptations);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // 原 ctx.get().getSender().level().getEntity(entityId) -> ctx.player().level().getEntity(entityId)
            Entity entity = ctx.player().level().getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                DamageAdaptation.updateClientAdaptationData(living, adaptationLevel, damageTypeKeyStr, maxAdaptations);
            }
        });
    }
}
