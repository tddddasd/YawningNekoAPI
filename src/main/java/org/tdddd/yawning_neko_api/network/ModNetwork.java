package org.tdddd.yawning_neko_api.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.network.packet.AdaptationDataSyncPacket;
import org.tdddd.yawning_neko_api.network.packet.AdaptationEventSyncPacket;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Yawning_neko_api.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, AdaptationDataSyncPacket.class,
                AdaptationDataSyncPacket::encode,
                AdaptationDataSyncPacket::new,
                AdaptationDataSyncPacket::handle);

        INSTANCE.registerMessage(1, AdaptationEventSyncPacket.class,
                AdaptationEventSyncPacket::encode,
                AdaptationEventSyncPacket::new,
                AdaptationEventSyncPacket::handle);
    }

    public static void sendToAllTracking(Object packet, LivingEntity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }
}
