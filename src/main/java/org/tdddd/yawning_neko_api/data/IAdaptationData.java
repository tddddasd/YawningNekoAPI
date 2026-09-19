package org.tdddd.yawning_neko_api.data;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.Map;

/**
 * 原 1.20.1 版本继承 {@code INBTSerializable<CompoundTag>}，并在此声明
 * {@code Capability<IAdaptationData> CAPABILITY}。
 *
 * <p>26.1.2 已移除 Forge Capability 与 {@code INBTSerializable}：
 * <ul>
 *   <li>能力改为数据附件，类型声明在
 *       {@link org.tdddd.yawning_neko_api.events.CapabilityEventHandler#ADAPTATION_DATA}
 *       （通过 {@code IAttachmentHolder#getData/setData} 读写）；</li>
 *   <li>序列化改为 {@link ValueIOSerializable}（{@code ValueOutput}/{@code ValueInput}）。</li>
 * </ul>
 */
public interface IAdaptationData extends ValueIOSerializable {
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
