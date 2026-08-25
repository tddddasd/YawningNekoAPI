package org.tdddd.yawning_neko_api.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;

public interface IAdaptationData extends INBTSerializable<CompoundTag> {
    Capability<IAdaptationData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    int getAdaptationLevel(String damageTypeKey);
    void setAdaptationLevel(String damageTypeKey, int level);
    Map<String, Integer> getAllAdaptations();
    void clearAdaptations();
    int getMaxAdaptations();
    void setMaxAdaptations(int max);
    void recordAdaptation(String damageType);
    String getLastDamagedType();
    void setLastDamagedType(String damageType);

    int getDeathCount();
    void setDeathCount(int count);
    void incrementDeathCount();

    String getCurrentConfigId();
    void setCurrentConfigId(String configId);

    long getBrokenAdaptationEndTick();
    void setBrokenAdaptationEndTick(long tick);
    boolean isInBrokenAdaptation(long currentTick);

    long getSpawnTime();
    void setSpawnTime(long tick);

    long getLastHurtTime();
    void setLastHurtTime(long tick);
}