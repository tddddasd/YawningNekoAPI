package org.tdddd.yawning_neko_api.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityAttackEffectManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<String, EntityAttackConfig> ENTITY_ATTACK_CONFIGS = new HashMap<>();
    private static final Map<String, Map<String, Integer>> EFFECT_LEVEL_CACHE = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ENTITY_ATTACK_CONFIGS.clear();
        EFFECT_LEVEL_CACHE.clear();

        resourceManager.listResources("entity_attack_effects", file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
                        parseAttackConfig(json);
                    } catch (Exception e) {
                        System.err.println("Failed to load entity attack effect data: " + resourceLocation);
                        e.printStackTrace();
                    }
                });
    }

    private void parseAttackConfig(JsonObject json) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String entityId = entry.getKey();
            JsonElement configElement = entry.getValue();

            if (configElement.isJsonObject()) {
                JsonObject configObj = configElement.getAsJsonObject();

                List<AttackEffect> baseEffects = new ArrayList<>();
                if (configObj.has("base_effects") && configObj.get("base_effects").isJsonArray()) {
                    for (JsonElement effectElement : configObj.get("base_effects").getAsJsonArray()) {
                        if (effectElement.isJsonObject()) {
                            JsonObject effectObj = effectElement.getAsJsonObject();
                            AttackEffect effect = new AttackEffect(
                                    effectObj.get("effect").getAsString(),
                                    effectObj.get("duration").getAsInt(),
                                    effectObj.get("amplifier").getAsInt(),
                                    effectObj.get("ambient").getAsBoolean(),
                                    effectObj.has("visibleParticles") ? effectObj.get("visibleParticles").getAsBoolean() : true,
                                    effectObj.has("showIcon") ? effectObj.get("showIcon").getAsBoolean() : true,
                                    effectObj.has("probability") ? effectObj.get("probability").getAsFloat() : 1.0f
                            );
                            baseEffects.add(effect);
                        } else if (effectElement.isJsonArray()) {
                            var effectArray = effectElement.getAsJsonArray();
                            if (effectArray.size() >= 6) {
                                AttackEffect effect = new AttackEffect(
                                        effectArray.get(0).getAsString(),
                                        effectArray.get(1).getAsInt(),
                                        effectArray.get(2).getAsInt(),
                                        effectArray.get(3).getAsBoolean(),
                                        effectArray.get(4).getAsBoolean(),
                                        effectArray.get(5).getAsBoolean(),
                                        effectArray.size() > 6 ? effectArray.get(6).getAsFloat() : 1.0f
                                );
                                baseEffects.add(effect);
                            }
                        }
                    }
                }

                List<EffectUpgrade> upgrades = new ArrayList<>();
                if (configObj.has("upgrades") && configObj.get("upgrades").isJsonArray()) {
                    for (JsonElement upgradeElement : configObj.get("upgrades").getAsJsonArray()) {
                        if (upgradeElement.isJsonObject()) {
                            JsonObject upgradeObj = upgradeElement.getAsJsonObject();

                            String effectFilter = upgradeObj.has("effect_filter") ?
                                    upgradeObj.get("effect_filter").getAsString() : null;

                            Integer maxLevel = upgradeObj.has("max_level") ?
                                    upgradeObj.get("max_level").getAsInt() : null;

                            EffectUpgrade upgrade = new EffectUpgrade(
                                    upgradeObj.get("level").getAsInt(),
                                    upgradeObj.has("probability_multiplier") ? upgradeObj.get("probability_multiplier").getAsFloat() : 1.0f,
                                    upgradeObj.has("level_adjust") ? upgradeObj.get("level_adjust").getAsInt() : 0,
                                    upgradeObj.has("duration_multiplier") ? upgradeObj.get("duration_multiplier").getAsFloat() : 1.0f,
                                    effectFilter,
                                    maxLevel
                            );
                            upgrades.add(upgrade);
                        }
                    }
                }

                EntityAttackConfig config = new EntityAttackConfig(baseEffects, upgrades);
                ENTITY_ATTACK_CONFIGS.put(entityId, config);
            }
        }
    }

    public static int getCachedEffectLevel(String entityId, String effectId) {
        return EFFECT_LEVEL_CACHE.getOrDefault(entityId, new HashMap<>())
                .getOrDefault(effectId, 0);
    }

    public static void setCachedEffectLevel(String entityId, String effectId, int level) {
        EFFECT_LEVEL_CACHE.computeIfAbsent(entityId, k -> new HashMap<>())
                .put(effectId, level);
    }

    public static void clearEntityCache(String entityId) {
        EFFECT_LEVEL_CACHE.remove(entityId);
    }

    public static int getMaxUpgradeLevel(Mob entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        EntityAttackConfig config = ENTITY_ATTACK_CONFIGS.get(entityId);
        return config != null ? config.getMaxUpgradeLevel() : 0;
    }

    public static int getMaxEffectLevel(Mob entity, String effectId) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        EntityAttackConfig config = ENTITY_ATTACK_CONFIGS.get(entityId);
        return config != null ? config.getMaxEffectLevel(effectId) : 0;
    }

    public static List<AttackEffect> getAttackEffects(Mob entity, int upgradeLevel) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return getAttackEffects(entityId, upgradeLevel);
    }

    public static List<AttackEffect> getAttackEffects(String entityId, int upgradeLevel) {
        EntityAttackConfig config = ENTITY_ATTACK_CONFIGS.get(entityId);
        if (config == null) {
            return new ArrayList<>();
        }

        return config.getEffectsForLevel(upgradeLevel, entityId);
    }

    public static List<AttackEffect> getBaseAttackEffects(Mob entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        EntityAttackConfig config = ENTITY_ATTACK_CONFIGS.get(entityId);
        return config != null ? config.getBaseEffects() : new ArrayList<>();
    }

    public static boolean hasAttackEffects(Mob entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return ENTITY_ATTACK_CONFIGS.containsKey(entityId);
    }

    public static Map<String, EntityAttackConfig> getAllAttackConfigs() {
        return new HashMap<>(ENTITY_ATTACK_CONFIGS);
    }

    public static class AttackEffect {
        private final String effectId;
        private final int duration;
        private final int amplifier;
        private final boolean ambient;
        private final boolean visibleParticles;
        private final boolean showIcon;
        private final float probability;

        public AttackEffect(String effectId, int duration, int amplifier, boolean ambient,
                            boolean visibleParticles, boolean showIcon) {
            this(effectId, duration, amplifier, ambient, visibleParticles, showIcon, 1.0f);
        }

        public AttackEffect(String effectId, int duration, int amplifier, boolean ambient,
                            boolean visibleParticles, boolean showIcon, float probability) {
            this.effectId = effectId;
            this.duration = duration;
            this.amplifier = amplifier;
            this.ambient = ambient;
            this.visibleParticles = visibleParticles;
            this.showIcon = showIcon;
            this.probability = probability;
        }

        public String getEffectId() { return effectId; }
        public int getDuration() { return duration; }
        public int getAmplifier() { return amplifier; }
        public boolean isAmbient() { return ambient; }
        public boolean hasVisibleParticles() { return visibleParticles; }
        public boolean shouldShowIcon() { return showIcon; }
        public float getProbability() { return probability; }

        public AttackEffect applyUpgrade(EffectUpgrade upgrade) {
            if (!upgrade.appliesToEffect(effectId)) {
                return this;
            }

            int newDuration = (int)(duration * upgrade.durationMultiplier);
            int newAmplifier = amplifier + upgrade.levelAdjust;

            if (upgrade.maxLevel != null && newAmplifier > upgrade.maxLevel) {
                newAmplifier = upgrade.maxLevel;
            }

            float newProbability = probability * upgrade.probabilityMultiplier;

            return new AttackEffect(
                    effectId, newDuration, newAmplifier, ambient,
                    visibleParticles, showIcon, Math.min(newProbability, 1.0f)
            );
        }
    }

    public static class EffectUpgrade {
        public final int level;
        public final float probabilityMultiplier;
        public final int levelAdjust;
        public final float durationMultiplier;
        public final String effectFilter;
        public final Integer maxLevel;

        public EffectUpgrade(int level, float probabilityMultiplier, int levelAdjust,
                             float durationMultiplier, String effectFilter, Integer maxLevel) {
            this.level = level;
            this.probabilityMultiplier = probabilityMultiplier;
            this.levelAdjust = levelAdjust;
            this.durationMultiplier = durationMultiplier;
            this.effectFilter = effectFilter;
            this.maxLevel = maxLevel;
        }

        public boolean appliesToEffect(String effectId) {
            return effectFilter == null || effectFilter.equals(effectId);
        }
    }

    public static class EntityAttackConfig {
        private final List<AttackEffect> baseEffects;
        private final List<EffectUpgrade> upgrades;

        public EntityAttackConfig(List<AttackEffect> baseEffects, List<EffectUpgrade> upgrades) {
            this.baseEffects = baseEffects;
            this.upgrades = upgrades;
        }

        public List<AttackEffect> getEffectsForLevel(int upgradeLevel, String entityId) {
            List<AttackEffect> result = new ArrayList<>();

            for (AttackEffect baseEffect : baseEffects) {
                AttackEffect upgradedEffect = baseEffect;
                String effectId = baseEffect.getEffectId();

                int effectiveLevel = getCachedEffectLevel(entityId, effectId);
                if (effectiveLevel == 0) {
                    effectiveLevel = upgradeLevel;
                    setCachedEffectLevel(entityId, effectId, effectiveLevel);
                }

                for (EffectUpgrade upgrade : upgrades) {
                    if (upgrade.level <= effectiveLevel && upgrade.appliesToEffect(effectId)) {
                        upgradedEffect = upgradedEffect.applyUpgrade(upgrade);
                    }
                }

                result.add(upgradedEffect);
            }

            return result;
        }

        public static List<AttackEffect> getAttackEffects(Mob entity, int upgradeLevel) {
            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            EntityAttackConfig config = ENTITY_ATTACK_CONFIGS.get(entityId);
            if (config == null) {
                return new ArrayList<>();
            }
            return config.getEffectsForLevel(upgradeLevel, entityId);
        }

        public List<AttackEffect> getBaseEffects() {
            return new ArrayList<>(baseEffects);
        }

        public int getMaxUpgradeLevel() {
            return upgrades.stream()
                    .mapToInt(upgrade -> upgrade.level)
                    .max()
                    .orElse(0);
        }

        public int getMaxEffectLevel(String effectId) {
            return upgrades.stream()
                    .filter(upgrade -> upgrade.appliesToEffect(effectId) && upgrade.maxLevel != null)
                    .mapToInt(upgrade -> upgrade.maxLevel)
                    .max()
                    .orElse(0);
        }
    }
}