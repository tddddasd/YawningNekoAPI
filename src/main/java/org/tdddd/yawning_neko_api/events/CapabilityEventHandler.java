package org.tdddd.yawning_neko_api.events;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.data.AdaptationDataCapability;
import org.tdddd.yawning_neko_api.data.IAdaptationData;

/**
 * 原 1.20.1 版：监听 {@code AttachCapabilitiesEvent<Entity>}，为每个 LivingEntity 挂上
 * {@code AdaptationDataCapability.Provider}。
 *
 * <p>26.1.2 用数据附件（data attachment）取代 Capability：附件类型注册进
 * {@code NeoForgeRegistries.Keys.ATTACHMENT_TYPES}，默认值惰性创建，不再需要“附加事件”。
 * 注册发生在模组总线（mod event bus）上，故由 {@link Yawning_neko_api} 构造器调用
 * {@link #register(IEventBus)}。
 */
public class CapabilityEventHandler {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Yawning_neko_api.MODID);

    /** 组件名沿用 1.20.1 的能力 id：{@code yawning_neko_api:adaptation_data}。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<IAdaptationData>> ADAPTATION_DATA =
            ATTACHMENT_TYPES.register("adaptation_data",
                    () -> AttachmentType.<IAdaptationData>serializable(AdaptationDataCapability::new).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
