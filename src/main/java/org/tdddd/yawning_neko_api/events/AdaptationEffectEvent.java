package org.tdddd.yawning_neko_api.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

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
        PARTIAL_ADAPTATION,   
        FULL_ADAPTATION       
    }

    @Override
    public boolean isCancelable() {
        return false;
    }
}