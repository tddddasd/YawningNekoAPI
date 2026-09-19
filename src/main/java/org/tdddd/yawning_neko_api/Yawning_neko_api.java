package org.tdddd.yawning_neko_api;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.tdddd.yawning_neko_api.data.EntityAttackEffectManager;
import org.tdddd.yawning_neko_api.data.EntityImmunityEffectManager;
import org.tdddd.yawning_neko_api.data.EntityUpgradeManager;
import org.tdddd.yawning_neko_api.data.MinimumDamageManager;
import org.tdddd.yawning_neko_api.events.CapabilityEventHandler;
import org.tdddd.yawning_neko_api.events.EntityAttackEffectHandler;
import org.tdddd.yawning_neko_api.network.ModNetwork;

@Mod(Yawning_neko_api.MODID)
public class Yawning_neko_api {
    public static final String MODID = "yawning_neko_api";

    public Yawning_neko_api(IEventBus modEventBus, ModContainer modContainer) {
        // 原 1.20.1：FMLJavaModLoadingContext.get().getModEventBus()
        // 26.1.2：模组总线由构造器注入
        // 数据附件（原 Forge Capability）必须注册到模组总线
        CapabilityEventHandler.register(modEventBus);
        // 原 1.20.1 在 FMLCommonSetupEvent 里执行 ModNetwork.register()；
        // 26.1.2 的负载注册事件本身就是模组总线事件，直接监听即可
        modEventBus.addListener(ModNetwork::register);
        // 原 1.20.1：MinecraftForge.EVENT_BUS
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new EntityAttackEffectHandler());
    }

    private static final MinimumDamageManager MIN_DAMAGE_MANAGER = new MinimumDamageManager();

    // 原 1.20.1：AddReloadListenerEvent（无 key，直接 addListener）
    // 26.1.2：拆分为服务端/客户端两个事件，且每个监听器必须带唯一的 Identifier key
    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "minimum_damage"), MIN_DAMAGE_MANAGER);
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "entity_attack_effects"), new EntityAttackEffectManager());
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "entity_effect_upgrades"), new EntityUpgradeManager());
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "entity_immunity_effects"), new EntityImmunityEffectManager());
    }
}
