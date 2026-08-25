package org.tdddd.yawning_neko_api.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

public class AdaptationConfigResolver {
    public static ResourceLocation resolveConfigId(LivingEntity entity, Level level) {
        ResourceLocation ruleId = AdaptationPriorityRuleLoader.resolveConfigId(entity, level);
        if (ruleId != null) return ruleId;

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return null;

        ResourceLocation mappedId = EntityAdaptationMapping.getConfigForEntity(entityId);
        if (mappedId != null) return mappedId;

        return DamageAdaptationManager.hasConfig(entityId) ? entityId : null;
    }

    public static DamageAdaptationConfig getConfig(LivingEntity entity) {
        if (entity.level().isClientSide) return null;

        ResourceLocation configId = resolveConfigId(entity, entity.level());
        if (configId == null) return null;

        DamageAdaptationConfig config = DamageAdaptationManager.getDirectConfig(configId);
        if (config == null) return null;

        entity.getCapability(IAdaptationData.CAPABILITY).ifPresent(data -> {
            String currentRecorded = data.getCurrentConfigId();
            String newIdStr = configId.toString();
            if (!newIdStr.equals(currentRecorded)) {
                data.clearAdaptations();
                data.setDeathCount(0);
                data.setBrokenAdaptationEndTick(0);
                data.setCurrentConfigId(newIdStr);
            }
        });

        return config;
    }

    public static void refreshAllEntitiesConfig() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.execute(() -> {
            for (Level level : server.getAllLevels()) {
                if (level instanceof ServerLevel serverLevel) {
                    for (net.minecraft.world.entity.Entity entity : serverLevel.getAllEntities()) {
                        if (entity instanceof LivingEntity living) {
                            refreshEntityConfig(living);
                        }
                    }
                }
            }
        });
    }

    public static void refreshEntityConfig(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        ResourceLocation newConfigId = resolveConfigId(entity, entity.level());
        if (newConfigId == null) return;

        DamageAdaptationConfig newConfig = DamageAdaptationManager.getDirectConfig(newConfigId);
        if (newConfig == null) return;

        entity.getCapability(IAdaptationData.CAPABILITY).ifPresent(data -> {
            String oldConfigId = data.getCurrentConfigId();
            String newConfigIdStr = newConfigId.toString();

            if (!newConfigIdStr.equals(oldConfigId)) {
                data.clearAdaptations();
                data.setDeathCount(0);
                data.setBrokenAdaptationEndTick(0);
                data.setCurrentConfigId(newConfigIdStr);
                data.setMaxAdaptations(newConfig.getMaxAdaptations());
            } else {
                int oldMax = data.getMaxAdaptations();
                int newMax = newConfig.getMaxAdaptations();
                if (oldMax != newMax) {
                    data.setMaxAdaptations(newMax);
                    for (var entry : data.getAllAdaptations().entrySet()) {
                        if (entry.getValue() > newMax) {
                            data.setAdaptationLevel(entry.getKey(), newMax);
                        }
                    }
                }
            }
        });
    }
}