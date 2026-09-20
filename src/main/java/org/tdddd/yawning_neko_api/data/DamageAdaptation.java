package org.tdddd.yawning_neko_api.data;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.tdddd.yawning_neko_api.Yawning_neko_api;
import org.tdddd.yawning_neko_api.events.AdaptationEffectEvent;
import org.tdddd.yawning_neko_api.network.ModNetwork;
import org.tdddd.yawning_neko_api.network.packet.AdaptationDataSyncPacket;
import org.tdddd.yawning_neko_api.network.packet.AdaptationEventSyncPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Yawning_neko_api.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DamageAdaptation {
    private static final WeakHashMap<LivingEntity, Long> spawnTimeMap = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Long> lastHurtTimeMap = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Long> delayedHurtTimeMap = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Map<String, Integer>> clientAdaptationData = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, String> clientLastDamageTypeKey = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Boolean> clientLastDamageTypeAdaptable = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, String> serverLastDamageTypeKey = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Integer> lastAdaptationEventType = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Long> lastAdaptationEventTime = new WeakHashMap<>();
    private static final long EVENT_DISPLAY_DURATION_TICKS = 10; 
    private static final WeakHashMap<LivingEntity, Long> clientEventExpireTick = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Integer> clientEventType = new WeakHashMap<>();

    private static final WeakHashMap<LivingEntity, Integer> clientMaxAdaptations = new WeakHashMap<>();

    private static String damageTypeKeyToString(Object key) {
        if (key instanceof ResourceLocation loc) {
            return loc.toString();
        } else if (key instanceof TagKey<?> tag) {
            return "#" + tag.location().toString();
        }
        return null;
    }

    private static Object stringToDamageTypeKey(String str) {
        if (str == null) return null;
        if (str.startsWith("#")) {
            String tagName = str.substring(1);
            return TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(tagName));
        } else {
            return new ResourceLocation(str);
        }
    }

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(new ResourceLocation(Yawning_neko_api.MODID, "adaptation"),
                    new AdaptationDataCapability.Provider());
        }
    }

    public static DamageAdaptationConfig getEntityConfig(LivingEntity entity) {
        if (entity.level().isClientSide) return null;
        return AdaptationConfigResolver.getConfig(entity);
    }

    public static boolean isInvulnerable(LivingEntity entity) {
        DamageAdaptationConfig config = getEntityConfig(entity);
        if (config == null) return false;

        long currentTime = entity.level().getGameTime();

        int minKillDuration = config.getMinimumKillDuration();
        if (minKillDuration > 0) {
            Long spawnTime = spawnTimeMap.get(entity);
            if (spawnTime != null && currentTime - spawnTime < minKillDuration) {
                return true;
            }
        }

        int minAttackInterval = config.getMinimumAttackInterval();
        if (minAttackInterval > 0) {
            Long delayedHurt = delayedHurtTimeMap.get(entity);
            if (delayedHurt != null && currentTime - delayedHurt < minAttackInterval) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInBrokenAdaptation(LivingEntity entity) {
        IAdaptationData data = entity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
        if (data == null) return false;
        return data.isInBrokenAdaptation(entity.level().getGameTime());
    }

    public static float getRemainingBrokenAdaptationTime(LivingEntity entity) {
        IAdaptationData data = entity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
        if (data == null) return 0;
        long currentTick = entity.level().getGameTime();
        long remaining = Math.max(0, data.getBrokenAdaptationEndTick() - currentTick);
        return remaining / 20.0f;
    }

    public static int getDeathCount(LivingEntity entity) {
        IAdaptationData data = entity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
        return data == null ? 0 : data.getDeathCount();
    }

    public static void recordDeath(LivingEntity entity) {
        IAdaptationData data = entity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
        if (data != null) data.incrementDeathCount();
    }

    private static float getDamageMultiplier(LivingEntity entity, DamageType damageType) {
        DamageAdaptationConfig config = getEntityConfig(entity);
        if (config == null) return 1.0f;

        Object damageTypeKey = getDamageTypeKey(entity, damageType, config);
        if (damageTypeKey == null) return 1.0f;

        return config.getDamageMultipliers().getOrDefault(damageTypeKey, 1.0f);
    }

    private static Object getDamageTypeKey(LivingEntity entity, DamageType damageType, DamageAdaptationConfig config) {
        ResourceLocation damageTypeId = entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getKey(damageType);
        if (damageTypeId == null) return null;

        var registry = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var holder = registry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId));

        for (Object key : config.getDamageMultipliers().keySet()) {
            if (key instanceof ResourceLocation loc && damageTypeId.equals(loc)) {
                return key;
            } else if (key instanceof TagKey<?> tag && holder.isPresent() && holder.get().is((TagKey<DamageType>) tag)) {
                return key;
            }
        }
        return damageTypeId;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        DamageAdaptationConfig config = getEntityConfig(entity);

        if (isInvulnerable(entity)) {
            event.setCanceled(true);
            return;
        }
        if (config == null) return;

        DamageType damageType = event.getSource().type();
        Object damageTypeKey = getDamageTypeKey(entity, damageType, config);

        float multiplier = getDamageMultiplier(entity, damageType);
        float originalDamage = event.getAmount();
        float multipliedDamage = originalDamage * multiplier;

        boolean shouldTriggerBroken = false;
        if (multipliedDamage > 0 && damageTypeKey != null && config.shouldTriggerBrokenAdaptation(damageTypeKey, multipliedDamage)) {
            shouldTriggerBroken = true;
            IAdaptationData data = entity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
            if (data != null) {
                long durationTicks = (long) (config.getBrokenAdaptationDuration() * 20);
                data.setBrokenAdaptationEndTick(entity.level().getGameTime() + durationTicks);
            }
        }

        if (multiplier < 0 && originalDamage < 0) {
            float finalDamage = Math.abs(originalDamage) * Math.abs(multiplier);
            event.setAmount(finalDamage);
            recordHurtTime(entity);
            scheduleDelayedHurtTime(entity);
            processDamageAdaptation(entity, config, event, finalDamage, shouldTriggerBroken, damageTypeKey);
        } else if (multiplier < 0 && originalDamage > 0) {
            float healing = Math.abs(multipliedDamage);
            handleNegativeDamage(entity, event.getSource(), -healing);
            event.setCanceled(true);
        } else if (multiplier > 0 && originalDamage < 0) {
            event.setAmount(multipliedDamage);
            handleNegativeDamage(entity, event.getSource(), multipliedDamage);
            event.setCanceled(true);
        } else {
            event.setAmount(multipliedDamage);
            recordHurtTime(entity);
            scheduleDelayedHurtTime(entity);
            processDamageAdaptation(entity, config, event, multipliedDamage, shouldTriggerBroken, damageTypeKey);
        }

        String keyStr = damageTypeKeyToString(damageTypeKey);
        if (keyStr != null) {
            serverLastDamageTypeKey.put(entity, keyStr);
        }
    }

    public static int getLastAdaptationEventType(LivingEntity entity) {
        if (entity.level().isClientSide) {
            Long expireTick = clientEventExpireTick.get(entity);
            if (expireTick == null) return 0;
            if (entity.level().getGameTime() > expireTick) {
                clientEventExpireTick.remove(entity);
                clientEventType.remove(entity);
                return 0;
            }
            return clientEventType.getOrDefault(entity, 0);
        } else {
            Long time = lastAdaptationEventTime.get(entity);
            if (time == null) return 0;
            if (entity.level().getGameTime() - time > EVENT_DISPLAY_DURATION_TICKS) {
                lastAdaptationEventType.remove(entity);
                lastAdaptationEventTime.remove(entity);
                return 0;
            }
            return lastAdaptationEventType.getOrDefault(entity, 0);
        }
    }

    private static void setLastAdaptationEventType(LivingEntity entity, int type) {
        lastAdaptationEventType.put(entity, type);
        lastAdaptationEventTime.put(entity, entity.level().getGameTime());
        if (!entity.level().isClientSide) {
            ModNetwork.sendToAllTracking(new AdaptationEventSyncPacket(entity.getId(), type), entity);
        }
    }

    private static void processDamageAdaptation(LivingEntity entity, DamageAdaptationConfig config,
                                                LivingHurtEvent event, float currentDamage,
                                                boolean isBrokenTriggered, Object damageTypeKey) {
        IAdaptationData data = entity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
        if (data == null) return;

        long currentTick = entity.level().getGameTime();
        if (data.isInBrokenAdaptation(currentTick) || isBrokenTriggered) {
            event.setAmount(currentDamage);
            return;
        }

        String keyStr = damageTypeKeyToString(damageTypeKey);
        if (keyStr == null || !config.isDamageTypeAdaptable(damageTypeKey)) {
            event.setAmount(currentDamage);
            return;
        }

        int oldAdapt = data.getAdaptationLevel(keyStr);
        int newAdapt = oldAdapt;
        boolean canAdapt = canAdaptDamageType(entity, config, damageTypeKey, data);
        float chance = config.getAdaptationProbability();
        if (canAdapt && oldAdapt < config.getMaxAdaptations() && entity.getRandom().nextFloat() < chance) {
            newAdapt++;
            data.setAdaptationLevel(keyStr, newAdapt);
            syncAdaptationData(entity, keyStr, newAdapt, config.getMaxAdaptations());
        }

        float reduction = 1.0f - (newAdapt * config.getAdaptationPercentagePerInjury());
        float finalDamage = currentDamage * reduction;
        event.setAmount(finalDamage);

        if (!entity.level().isClientSide) {
            int maxAdapt = data.getMaxAdaptations();
            if (newAdapt >= maxAdapt) {
                MinecraftForge.EVENT_BUS.post(new AdaptationEffectEvent(entity, AdaptationEffectEvent.Type.FULL_ADAPTATION));
                setLastAdaptationEventType(entity, 2);
            } else if (newAdapt > oldAdapt) {
                MinecraftForge.EVENT_BUS.post(new AdaptationEffectEvent(entity, AdaptationEffectEvent.Type.PARTIAL_ADAPTATION));
                setLastAdaptationEventType(entity, 1);
            }
        }
    }

    private static boolean canAdaptDamageType(LivingEntity entity, DamageAdaptationConfig config,
                                              Object damageTypeKey, IAdaptationData data) {
        if (config.getMaxAdaptableDamageTypes() == -1) return true;
        String keyStr = damageTypeKeyToString(damageTypeKey);
        long adaptedCount = data.getAllAdaptations().entrySet().stream()
                .filter(e -> e.getValue() > 0 && config.isDamageTypeAdaptable(stringToDamageTypeKey(e.getKey())))
                .count();
        if (data.getAdaptationLevel(keyStr) > 0) return true;
        return adaptedCount < config.getMaxAdaptableDamageTypes();
    }

    private static void handleNegativeDamage(LivingEntity entity, DamageSource source, float amount) {
        if (amount < 0) {
            entity.heal(-amount);
        }
    }

    public static void recordHurtTime(LivingEntity entity) {
        lastHurtTimeMap.put(entity, entity.level().getGameTime());
    }

    private static void scheduleDelayedHurtTime(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            entity.level().getServer().execute(() -> {
                if (entity.isAlive()) {
                    delayedHurtTimeMap.put(entity, entity.level().getGameTime());
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DamageAdaptationConfig config = getEntityConfig(entity);
        if (config == null) return;

        int minKills = config.getMinimumKillCount();
        if (minKills <= 0) return;

        IAdaptationData data = entity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
        if (data == null) return;

        int currentDeaths = data.getDeathCount();
        currentDeaths++;
        data.setDeathCount(currentDeaths);
        long currentTime = entity.level().getGameTime();

        if (currentDeaths < minKills) {
            event.setCanceled(true);
            entity.setHealth(entity.getMaxHealth());
            spawnTimeMap.put(entity, currentTime);

            
            if (!entity.level().isClientSide && entity.level() instanceof ServerLevel serverLevel) {
                RandomSource random = entity.getRandom();
                int count = 7 + random.nextInt(6);
                AABB bb = entity.getBoundingBox();
                for (int i = 0; i < count; i++) {
                    double x = bb.minX + random.nextDouble() * (bb.maxX - bb.minX);
                    double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
                    double z = bb.minZ + random.nextDouble() * (bb.maxZ - bb.minZ);
                    DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.2F), 1.0F);
                    serverLevel.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0.1);
                }
            }
        } else {
            data.setDeathCount(0);
            spawnTimeMap.remove(entity);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            DamageAdaptationConfig config = getEntityConfig(living);
            if (config != null) {
                spawnTimeMap.put(living, living.level().getGameTime());
                living.getCapability(IAdaptationData.CAPABILITY).ifPresent(data -> {
                    if (data.getMaxAdaptations() == 0) {
                        data.setMaxAdaptations(config.getMaxAdaptations());
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            onEntityRemoved(living);
        }
    }

    public static void onEntityRemoved(LivingEntity entity) {
        spawnTimeMap.remove(entity);
        lastHurtTimeMap.remove(entity);
        delayedHurtTimeMap.remove(entity);
        clientAdaptationData.remove(entity);
        clientLastDamageTypeKey.remove(entity);
        clientLastDamageTypeAdaptable.remove(entity);
        clientEventExpireTick.remove(entity);
        clientEventType.remove(entity);
        clientMaxAdaptations.remove(entity);
    }

    private static void syncAdaptationData(LivingEntity entity, String damageTypeKeyStr, int level, int maxAdaptations) {
        if (!entity.level().isClientSide) {
            ModNetwork.sendToAllTracking(new AdaptationDataSyncPacket(entity.getId(), level, damageTypeKeyStr, maxAdaptations), entity);
        }
    }

    public static void updateClientAdaptationData(LivingEntity entity, int adaptationLevel, String damageTypeKeyStr, int maxAdaptations) {
        if (entity.level().isClientSide) {
            Map<String, Integer> entityMap = clientAdaptationData.computeIfAbsent(entity, k -> new HashMap<>());
            entityMap.put(damageTypeKeyStr, adaptationLevel);
            clientLastDamageTypeKey.put(entity, damageTypeKeyStr);
            clientMaxAdaptations.put(entity, maxAdaptations);

            DamageAdaptationConfig config = getEntityConfig(entity);
            if (config != null) {
                Object keyObj = stringToDamageTypeKey(damageTypeKeyStr);
                boolean adaptable = keyObj != null && config.isDamageTypeAdaptable(keyObj);
                clientLastDamageTypeAdaptable.put(entity, adaptable);
            } else {
                clientLastDamageTypeAdaptable.put(entity, true);
            }
        }
    }

    public static int getClientMaxAdaptations(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return clientMaxAdaptations.getOrDefault(entity, 0);
        }
        return 0;
    }

    public static void updateClientAdaptationEvent(LivingEntity entity, int eventType) {
        if (entity.level().isClientSide) {
            long expireTick = entity.level().getGameTime() + 10; 
            clientEventExpireTick.put(entity, expireTick);
            clientEventType.put(entity, eventType);
        }
    }

    private static int getAdaptationDataInternal(LivingEntity entity) {
        if (entity.level().isClientSide) {
            String lastKey = clientLastDamageTypeKey.get(entity);
            if (lastKey != null) {
                Boolean adaptable = clientLastDamageTypeAdaptable.get(entity);
                if (adaptable == null || !adaptable) return 0;
                Map<String, Integer> map = clientAdaptationData.get(entity);
                return map == null ? 0 : map.getOrDefault(lastKey, 0);
            }
            return 0;
        } else {
            String lastKey = serverLastDamageTypeKey.get(entity);
            if (lastKey != null) {
                IAdaptationData data = entity.getCapability(IAdaptationData.CAPABILITY).orElse(null);
                if (data != null) {
                    return data.getAdaptationLevel(lastKey);
                }
            }
            return 0;
        }
    }

    public static int getAdaptationData(LivingEntity entity) {
        return getAdaptationDataInternal(entity);
    }

    public static boolean shouldShowAdaptationEffect(LivingEntity entity) {
        return getAdaptationData(entity) > 0 && entity.hurtTime > 0;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DamageAdaptationManager());
        event.addListener(new EntityAdaptationMapping());
        event.addListener(new AdaptationPriorityRuleLoader());
    }
}