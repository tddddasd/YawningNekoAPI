package org.tdddd.yawning_neko_api;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.tdddd.yawning_neko_api.data.EntityAttackEffectManager;
import org.tdddd.yawning_neko_api.data.EntityImmunityEffectManager;
import org.tdddd.yawning_neko_api.data.EntityUpgradeManager;
import org.tdddd.yawning_neko_api.data.MinimumDamageManager;
import org.tdddd.yawning_neko_api.events.EntityAttackEffectHandler;
import org.tdddd.yawning_neko_api.network.ModNetwork;

@Mod(Yawning_neko_api.MODID)
public class Yawning_neko_api {
    public static final String MODID = "yawning_neko_api";

    public Yawning_neko_api() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        forgeBus.addListener(this::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.register(new EntityAttackEffectHandler());
    }

    private static final MinimumDamageManager MIN_DAMAGE_MANAGER = new MinimumDamageManager();

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
        });
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MIN_DAMAGE_MANAGER);
        event.addListener(new EntityAttackEffectManager());
        event.addListener(new EntityUpgradeManager());
        event.addListener(new EntityImmunityEffectManager());
    }
}
