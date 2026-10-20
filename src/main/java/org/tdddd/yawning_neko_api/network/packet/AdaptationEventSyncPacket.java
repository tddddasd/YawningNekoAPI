package org.tdddd.yawning_neko_api.network.packet;

import net.minecraft.network.FriendlyByteBuf;
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
            // The client work lives in its own class, so this packet stays loadable on a dedicated
            // server: no client type appears in this class at all.
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                return;
            }
            org.tdddd.yawning_neko_api.client.AdaptationClientSync.apply(entityId, eventType);
        });
        ctx.get().setPacketHandled(true);
    }
}
