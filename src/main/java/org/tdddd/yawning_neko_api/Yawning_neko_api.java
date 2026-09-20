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
        
        
        
        CapabilityEventHandler.register(modEventBus);
        
        
        modEventBus.addListener(ModNetwork::register);
        
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new EntityAttackEffectHandler());
    }

    private static final MinimumDamageManager MIN_DAMAGE_MANAGER = new MinimumDamageManager();

    
    
    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "minimum_damage"), MIN_DAMAGE_MANAGER);
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "entity_attack_effects"), new EntityAttackEffectManager());
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "entity_effect_upgrades"), new EntityUpgradeManager());
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "entity_immunity_effects"), new EntityImmunityEffectManager());
    }
}
