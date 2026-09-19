package org.tdddd.yawning_neko_api.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.tdddd.yawning_neko_api.network.packet.AdaptationDataSyncPacket;
import org.tdddd.yawning_neko_api.network.packet.AdaptationEventSyncPacket;

/**
 * 原 1.20.1 用 {@code NetworkRegistry.newSimpleChannel} + {@code SimpleChannel#registerMessage}。
 *
 * <p>26.1.2 改为 payload 系统：在 {@link RegisterPayloadHandlersEvent} 里用
 * {@link PayloadRegistrar} 注册 {@code CustomPacketPayload} 与其 {@code StreamCodec}，
 * 协议版本号语义不变。同步的数据（实体 id / 适应层数 / 伤害类型键 / 最大适应数）完全一致。
 * 原 {@code public static final SimpleChannel INSTANCE} 字段随 SimpleChannel 一起移除。
 */
public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(AdaptationDataSyncPacket.TYPE, AdaptationDataSyncPacket.STREAM_CODEC,
                AdaptationDataSyncPacket::handle);
        registrar.playToClient(AdaptationEventSyncPacket.TYPE, AdaptationEventSyncPacket.STREAM_CODEC,
                AdaptationEventSyncPacket::handle);
    }

    public static void sendToAllTracking(CustomPacketPayload packet, LivingEntity entity) {
        // 原 PacketDistributor.TRACKING_ENTITY.with(() -> entity)
        PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
    }
}
