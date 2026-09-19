package org.tdddd.yawning_neko_api.damages;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.tdddd.yawning_neko_api.Yawning_neko_api;

public class ModDamageTypes {
    // 伤害类型依旧由数据包注册表驱动（data/yawning_neko_api/damage_type/minimum.json），
    // 26.1.2 只是把 ResourceLocation 更名为 Identifier，注册方式不变。
    public static final ResourceKey<DamageType> MINIMUM =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Yawning_neko_api.MODID, "minimum"));
}
