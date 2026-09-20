package org.tdddd.yawning_neko_api.data;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;


public class AdaptationDataCapability implements IAdaptationData {
    
    private static final Codec<Map<String, Integer>> ADAPTATIONS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    private final Map<String, Integer> adaptations = new HashMap<>();
    private int deathCount = 0;
    private String currentConfigId = "";
    private long brokenAdaptationEndTick = 0;
    private int maxAdaptations = 0;
    private String lastDamagedType = null;

    @Override
    public void recordAdaptation(String damageType) {
        adaptations.put(damageType, adaptations.getOrDefault(damageType, 0) + 1);
        this.lastDamagedType = damageType;
    }

    @Override
    public String getLastDamagedType() {
        return lastDamagedType;
    }

    @Override
    public void setLastDamagedType(String damageType) {
        this.lastDamagedType = damageType;
    }

    @Override
    public int getMaxAdaptations() {
        return maxAdaptations;
    }

    @Override
    public void setMaxAdaptations(int max) {
        this.maxAdaptations = max;
    }

    private long spawnTime = 0;
    private long lastHurtTime = 0;

    @Override
    public int getAdaptationLevel(String damageTypeKey) {
        return adaptations.getOrDefault(damageTypeKey, 0);
    }

    @Override
    public void setAdaptationLevel(String damageTypeKey, int level) {
        if (level <= 0) adaptations.remove(damageTypeKey);
        else adaptations.put(damageTypeKey, level);
    }

    @Override
    public Map<String, Integer> getAllAdaptations() { return new HashMap<>(adaptations); }

    @Override
    public void clearAdaptations() { adaptations.clear(); }

    @Override
    public int getDeathCount() { return deathCount; }

    @Override
    public void setDeathCount(int count) { deathCount = count; }

    @Override
    public void incrementDeathCount() { deathCount++; }

    @Override
    public String getCurrentConfigId() { return currentConfigId; }

    @Override
    public void setCurrentConfigId(String configId) { this.currentConfigId = configId; }

    @Override
    public long getBrokenAdaptationEndTick() { return brokenAdaptationEndTick; }

    @Override
    public void setBrokenAdaptationEndTick(long tick) { brokenAdaptationEndTick = tick; }

    @Override
    public boolean isInBrokenAdaptation(long currentTick) {
        return currentTick < brokenAdaptationEndTick;
    }

    @Override
    public long getSpawnTime() { return spawnTime; }

    @Override
    public void setSpawnTime(long tick) { this.spawnTime = tick; }

    @Override
    public long getLastHurtTime() { return lastHurtTime; }

    @Override
    public void setLastHurtTime(long tick) { this.lastHurtTime = tick; }

    
    
    @Override
    public void serialize(ValueOutput output) {
        output.store("adaptations", ADAPTATIONS_CODEC, adaptations);
        output.putInt("deathCount", deathCount);
        output.putString("configId", currentConfigId);
        output.putLong("brokenEnd", brokenAdaptationEndTick);
        output.putInt("maxAdaptations", maxAdaptations);
        if (lastDamagedType != null) {
            output.putString("lastDamageType", lastDamagedType);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        adaptations.clear();
        input.read("adaptations", ADAPTATIONS_CODEC).ifPresent(map -> adaptations.putAll(map));
        deathCount = input.getIntOr("deathCount", 0);
        currentConfigId = input.getStringOr("configId", "");
        brokenAdaptationEndTick = input.getLongOr("brokenEnd", 0L);
        maxAdaptations = input.getIntOr("maxAdaptations", 0);
        spawnTime = 0;
        lastHurtTime = 0;
        
    }
}
