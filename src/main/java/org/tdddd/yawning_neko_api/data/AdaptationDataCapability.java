package org.tdddd.yawning_neko_api.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AdaptationDataCapability implements IAdaptationData {
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
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag adaptTag = new CompoundTag();
        adaptations.forEach((k, v) -> adaptTag.putInt(k, v));
        tag.put("adaptations", adaptTag);
        tag.putInt("deathCount", deathCount);
        tag.putString("configId", currentConfigId);
        tag.putLong("brokenEnd", brokenAdaptationEndTick);
        tag.putInt("maxAdaptations", maxAdaptations);
        if (lastDamagedType != null) {
            tag.putString("lastDamageType", lastDamagedType);
        }
        if (tag.contains("lastDamageType")) {
            lastDamagedType = tag.getString("lastDamageType");
        } else {
            lastDamagedType = null;
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        adaptations.clear();
        CompoundTag adaptTag = tag.getCompound("adaptations");
        for (String key : adaptTag.getAllKeys()) {
            adaptations.put(key, adaptTag.getInt(key));
        }
        deathCount = tag.getInt("deathCount");
        currentConfigId = tag.getString("configId");
        brokenAdaptationEndTick = tag.getLong("brokenEnd");
        maxAdaptations = tag.getInt("maxAdaptations");
        spawnTime = 0;
        lastHurtTime = 0;
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final IAdaptationData instance = new AdaptationDataCapability();
        private final LazyOptional<IAdaptationData> lazyOptional = LazyOptional.of(() -> instance);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, lazyOptional);
        }

        @Override
        public CompoundTag serializeNBT() { return instance.serializeNBT(); }

        @Override
        public void deserializeNBT(CompoundTag nbt) { instance.deserializeNBT(nbt); }
    }
}