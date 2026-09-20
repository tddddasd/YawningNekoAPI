package org.tdddd.yawning_neko_api.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.tdddd.yawning_neko_api.network.packet.AdaptationDataSyncPacket;
import org.tdddd.yawning_neko_api.network.packet.AdaptationEventSyncPacket;


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
        
        PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
    }
}
