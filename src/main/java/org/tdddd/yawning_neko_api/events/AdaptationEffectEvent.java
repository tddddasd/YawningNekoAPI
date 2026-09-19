package org.tdddd.yawning_neko_api.events;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/**
 * 事件本身保持原样；26.1.2 的 NeoForge 事件总线已移除 {@code Event#isCancelable()}，
 * 需要可取消的事件改为实现 {@code ICancellableEvent}。本事件依旧不可取消。
 */
public class AdaptationEffectEvent extends Event {
    private final LivingEntity entity;
    private final Type effectType;

    public AdaptationEffectEvent(LivingEntity entity, Type effectType) {
        this.entity = entity;
        this.effectType = effectType;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public Type getEffectType() {
        return effectType;
    }

    public enum Type {
        PARTIAL_ADAPTATION,   // 部分适应
        FULL_ADAPTATION       // 完全适应
    }
}
