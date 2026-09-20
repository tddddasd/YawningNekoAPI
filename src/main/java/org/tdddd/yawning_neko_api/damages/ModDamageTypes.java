package org.tdddd.yawning_neko_api.damages;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.tdddd.yawning_neko_api.Yawning_neko_api;

public class ModDamageTypes {
    
    
    public static final ResourceKey<DamageType> MINIMUM =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Yawning_neko_api.MODID, "minimum"));
}
