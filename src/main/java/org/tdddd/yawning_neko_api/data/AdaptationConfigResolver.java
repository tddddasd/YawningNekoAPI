package org.tdddd.yawning_neko_api.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.tdddd.yawning_neko_api.events.CapabilityEventHandler;

public class AdaptationConfigResolver {
    public static Identifier resolveConfigId(LivingEntity entity, Level level) {
        Identifier ruleId = AdaptationPriorityRuleLoader.resolveConfigId(entity, level);
        if (ruleId != null) return ruleId;

        
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) return null;

        Identifier mappedId = EntityAdaptationMapping.getConfigForEntity(entityId);
        if (mappedId != null) return mappedId;

        return DamageAdaptationManager.hasConfig(entityId) ? entityId : null;
    }

    public static DamageAdaptationConfig getConfig(LivingEntity entity) {
        if (entity.level().isClientSide()) return null;

        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Identifier baseId = null;
        DamageAdaptationConfig base = null;
        if (entityId != null) {
            Identifier mapped = EntityAdaptationMapping.getConfigForEntity(entityId);
            baseId = mapped != null ? mapped : entityId;
            base = DamageAdaptationManager.getDirectConfig(baseId);
        }

        Identifier ruleId = AdaptationPriorityRuleLoader.resolveConfigId(entity, entity.level());
        DamageAdaptationConfig rule = ruleId == null ? null : DamageAdaptationManager.getDirectConfig(ruleId);

        DamageAdaptationConfig resolved;
        String signature;
        if (rule == null) {
            resolved = base;
            signature = baseId == null ? null : baseId.toString();
        } else if (base == null) {
            resolved = rule;
            signature = ruleId.toString();
        } else if (entityId != null && EntityAdaptationMapping.isOverride(entityId)) {
            resolved = rule;
            signature = ruleId + "|override|" + baseId;
        } else {
            resolved = DamageAdaptationConfig.additive(base, rule);
            signature = baseId + "|add|" + ruleId;
        }
        if (resolved == null || signature == null) return null;

        IAdaptationData data = entity.getData(CapabilityEventHandler.ADAPTATION_DATA);
        if (!signature.equals(data.getCurrentConfigId())) {
            data.clearAdaptations();
            // deathCount is the fake-death counter; switching config must not wipe it.
            data.setBrokenAdaptationEndTick(0);
            data.setCurrentConfigId(signature);
        }
        return resolved;
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
        if (entity.level().isClientSide()) return;

        Identifier newConfigId = resolveConfigId(entity, entity.level());
        if (newConfigId == null) return;

        DamageAdaptationConfig newConfig = DamageAdaptationManager.getDirectConfig(newConfigId);
        if (newConfig == null) return;

        IAdaptationData data = entity.getData(CapabilityEventHandler.ADAPTATION_DATA);
        String oldConfigId = data.getCurrentConfigId();
        String newConfigIdStr = newConfigId.toString();

        if (!newConfigIdStr.equals(oldConfigId)) {
            data.clearAdaptations();
            // deathCount is the fake-death counter; switching config must not wipe it.
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
    }

}
