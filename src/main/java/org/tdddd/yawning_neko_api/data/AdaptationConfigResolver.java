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

        // Base layer: the mapping for this entity type, else the entity type's own config file.
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        ResourceLocation baseId = null;
        DamageAdaptationConfig base = null;
        if (entityId != null) {
            ResourceLocation mapped = EntityAdaptationMapping.getConfigForEntity(entityId);
            baseId = mapped != null ? mapped : entityId;
            base = DamageAdaptationManager.getDirectConfig(baseId);
        }

        // Rule layer: resolved per entity, so NBT differences between two mobs of the same type matter.
        ResourceLocation ruleId = AdaptationPriorityRuleLoader.resolveConfigId(entity, entity.level());
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
            resolved = rule;                                                   // mapping asked for override
            signature = ruleId + "|override|" + baseId;
        } else {
            resolved = DamageAdaptationConfig.additive(base, rule);            // default: add
            signature = baseId + "|add|" + ruleId;
        }
        if (resolved == null || signature == null) return null;

        final String newSignature = signature;
        entity.getCapability(IAdaptationData.CAPABILITY).ifPresent(data -> {
            if (!newSignature.equals(data.getCurrentConfigId())) {
                data.clearAdaptations();
                // deathCount is the fake-death counter; switching config must not wipe it.
                data.setBrokenAdaptationEndTick(0);
                data.setCurrentConfigId(newSignature);
            }
        });

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
        });
    }

    /**
     * Display helper: the upper bound of the minimum kill count for this entity, i.e. the base layer
     * plus the rule layer (whichever layers exist). Used by Jade to show "current / upper bound".
     */
    public static int resolveMinimumKillCountUpperBound(net.minecraft.world.entity.LivingEntity entity) {
        net.minecraft.resources.ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        DamageAdaptationConfig base = null;
        if (entityId != null) {
            net.minecraft.resources.ResourceLocation mapped = EntityAdaptationMapping.getConfigForEntity(entityId);
            net.minecraft.resources.ResourceLocation baseId = mapped != null ? mapped : entityId;
            base = DamageAdaptationManager.getDirectConfig(baseId);
        }
        net.minecraft.resources.ResourceLocation ruleId =
                AdaptationPriorityRuleLoader.resolveConfigId(entity, entity.level());
        DamageAdaptationConfig rule = ruleId == null ? null : DamageAdaptationManager.getDirectConfig(ruleId);
        // The upper bound is the base value from the json (the rule layer is not added in).
        return base == null ? 0 : base.getMinimumKillCount();
    }
}
