package org.tdddd.yawning_neko_api.events;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.data.AdaptationDataCapability;
import org.tdddd.yawning_neko_api.data.IAdaptationData;


public class CapabilityEventHandler {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Yawning_neko_api.MODID);

    
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<IAdaptationData>> ADAPTATION_DATA =
            ATTACHMENT_TYPES.register("adaptation_data",
                    () -> AttachmentType.<IAdaptationData>serializable(AdaptationDataCapability::new).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
