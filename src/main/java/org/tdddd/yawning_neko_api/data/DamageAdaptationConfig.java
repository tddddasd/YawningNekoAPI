package org.tdddd.yawning_neko_api.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DamageAdaptationConfig {
    /** 键既可能是 Identifier（具体伤害类型），也可能是 TagKey<DamageType>（# 标签）。 */
    private final Map<Object, Float> damageMultipliers;
    private final float adaptationProbability;
    private final int maxAdaptations;
    private final float adaptationPercentagePerInjury;
    private final int minimumKillDuration;
    private final int minimumAttackInterval;
    private final int minimumKillCount;

    private final float brokenAdaptationDuration;

    private final int maxAdaptableDamageTypes;

    private final Set<Object> nonAdaptableDamageTypes;

    private final float maxMultiplierAbs;
    public DamageAdaptationConfig(Map<Object, Float> damageMultipliers, float adaptationProbability,
                                  int maxAdaptations, float adaptationPercentagePerInjury,
                                  int minimumKillDuration, int minimumAttackInterval, int minimumKillCount,
                                  float brokenAdaptationDuration, int maxAdaptableDamageTypes) {
        this.damageMultipliers = damageMultipliers;
        this.adaptationProbability = adaptationProbability;
        this.maxAdaptations = maxAdaptations;
        this.adaptationPercentagePerInjury = adaptationPercentagePerInjury;
        this.minimumKillDuration = minimumKillDuration;
        this.minimumAttackInterval = minimumAttackInterval;
        this.minimumKillCount = minimumKillCount;
        this.brokenAdaptationDuration = brokenAdaptationDuration;
        this.maxAdaptableDamageTypes = maxAdaptableDamageTypes;

        float maxAbs = 0.0f;
        this.nonAdaptableDamageTypes = new HashSet<>();

        for (Map.Entry<Object, Float> entry : damageMultipliers.entrySet()) {
            float multiplier = entry.getValue();
            if (Math.abs(multiplier) > maxAbs) {
                maxAbs = Math.abs(multiplier);
            }

            if (multiplier >= 1.0f || multiplier < 0.0f) {
                nonAdaptableDamageTypes.add(entry.getKey());
            }
        }
        this.maxMultiplierAbs = maxAbs;
    }

    public Map<Object, Float> getDamageMultipliers() { return damageMultipliers; }
    public float getAdaptationProbability() { return adaptationProbability; }
    public int getMaxAdaptations() { return maxAdaptations; }
    public float getAdaptationPercentagePerInjury() { return adaptationPercentagePerInjury; }
    public int getMinimumKillDuration() { return minimumKillDuration; }
    public int getMinimumAttackInterval() { return minimumAttackInterval; }
    public int getMinimumKillCount() { return minimumKillCount; }

    public float getBrokenAdaptationDuration() { return brokenAdaptationDuration; }
    public float getMaxMultiplierAbs() { return maxMultiplierAbs; }
    public int getMaxAdaptableDamageTypes() { return maxAdaptableDamageTypes; }
    public Set<Object> getNonAdaptableDamageTypes() { return nonAdaptableDamageTypes; }

    public boolean isDamageTypeAdaptable(Object damageType) {
        if (!damageMultipliers.containsKey(damageType)) {
            return true;
        }
        return !nonAdaptableDamageTypes.contains(damageType);
    }

    public boolean shouldTriggerBrokenAdaptation(Object damageType, float damageAmount) {
        if (!damageMultipliers.containsKey(damageType)) {
            return false;
        }

        Float multiplier = damageMultipliers.get(damageType);
        if (multiplier == null) {
            return false;
        }

        return multiplier != 0.0f &&
                Math.abs(multiplier) >= maxMultiplierAbs - 0.001f && // 使用容差比较浮点数
                damageAmount > 0.0f;
    }

    public static DamageAdaptationConfig fromJson(JsonObject json) {
        Map<Object, Float> damageMultipliers = new HashMap<>();

        if (json.has("damage") && json.get("damage").isJsonObject()) {
            JsonObject damageObj = json.getAsJsonObject("damage");
            for (Map.Entry<String, JsonElement> entry : damageObj.entrySet()) {
                String damageType = entry.getKey();
                float multiplier = entry.getValue().getAsFloat();

                if (damageType.startsWith("#")) {
                    String tagName = damageType.substring(1);
                    damageMultipliers.put(TagKey.create(Registries.DAMAGE_TYPE, Identifier.parse(tagName)), multiplier);
                } else {
                    damageMultipliers.put(Identifier.parse(damageType), multiplier);
                }
            }
        }

        JsonObject adaptabilityObj = json.getAsJsonObject("damage_adaptability");
        float probability = adaptabilityObj.get("damage_adaptability_probability").getAsFloat();
        int times = adaptabilityObj.get("damage_adaptability_times").getAsInt();
        float percentage = adaptabilityObj.get("adaptation_percentage_per_injury").getAsFloat();
        int killDuration = adaptabilityObj.get("minimum_kill_duration").getAsInt();
        int attackInterval = adaptabilityObj.get("minimum_attack_interval").getAsInt();
        int killCount = 0;
        if (adaptabilityObj.has("minimum_kill_count")) {
            killCount = adaptabilityObj.get("minimum_kill_count").getAsInt();
        }

        float brokenAdaptationDuration = 3.0f;
        if (adaptabilityObj.has("broken_adaptation_duration")) {
            brokenAdaptationDuration = adaptabilityObj.get("broken_adaptation_duration").getAsFloat();
        }

        int maxAdaptableDamageTypes = 0;
        if (adaptabilityObj.has("max_adaptable_damage_types")) {
            maxAdaptableDamageTypes = adaptabilityObj.get("max_adaptable_damage_types").getAsInt();
        }

        return new DamageAdaptationConfig(damageMultipliers, probability, times,
                percentage, killDuration, attackInterval, killCount,
                brokenAdaptationDuration, maxAdaptableDamageTypes);
    }
}
