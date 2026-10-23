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
    private static final long EVENT_DISPLAY_DURATION_TICKS = 10; // 显示持续时间（游戏刻）
    private static final WeakHashMap<LivingEntity, Long> clientEventExpireTick = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Integer> clientEventType = new WeakHashMap<>();
    /** Client-side adaptation event state keyed by "dimension#entityId", so a respawned or
     *  transiently different entity instance can still read the event that was sent for it. */
    private static final java.util.Map<String, Integer> CLIENT_EVENT_BY_KEY = new java.util.HashMap<>();
    private static final java.util.Map<String, Long> CLIENT_EVENT_EXPIRE_BY_KEY = new java.util.HashMap<>();

    private static String eventKey(net.minecraft.world.entity.Entity entity) {
        return entity.level().dimension().location() + "#" + entity.getId();
    }

    /** Entity-id keyed copy: the packet can record the event before the client entity exists. */
    private static final java.util.Map<Integer, Integer> CLIENT_EVENT_BY_ID = new java.util.HashMap<>();
    private static final java.util.Map<Integer, Long> CLIENT_EVENT_EXPIRE_BY_ID = new java.util.HashMap<>();
    private static net.minecraft.world.level.Level lastClientEventLevel;

    private static void ensureSameLevel(net.minecraft.world.level.Level level) {
        if (level != lastClientEventLevel) {
            lastClientEventLevel = level;
            CLIENT_EVENT_BY_KEY.clear();
            CLIENT_EVENT_EXPIRE_BY_KEY.clear();
            CLIENT_EVENT_BY_ID.clear();
            CLIENT_EVENT_EXPIRE_BY_ID.clear();
            clientEventType.clear();
            clientEventExpireTick.clear();
        }
    }

    /**
     * Records a client-side adaptation event keyed by entity id. This does not need the entity to be
     * resolvable yet, which is what makes freshly spawned mobs work without relogging.
     */
    public static void updateClientAdaptationEventById(int entityId, int eventType,
                                                       net.minecraft.world.level.Level level) {
        ensureSameLevel(level);
        long expireTick = (level == null ? 0L : level.getGameTime()) + 10;
        CLIENT_EVENT_BY_ID.put(entityId, eventType);
        CLIENT_EVENT_EXPIRE_BY_ID.put(entityId, expireTick);
        if (level != null) {
            CLIENT_EVENT_BY_KEY.put(level.dimension().location() + "#" + entityId, eventType);
            CLIENT_EVENT_EXPIRE_BY_KEY.put(level.dimension().location() + "#" + entityId, expireTick);
        }
    }

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

    /**
     * Unambiguous client-side query for the renderer: -1 means no adaptation event is active,
     * 0 is a partial adaptation and 1 a full one. {@link #getLastAdaptationEventType} cannot be
     * used for this because it falls back to 0, which is also the partial-adaptation code.
     */
    /** Diagnostics only: dumps every place the client-side event state could live. */
    public static String debugEventState(LivingEntity entity) {
        return "id=" + entity.getId()
                + " byId=" + CLIENT_EVENT_BY_ID.get(entity.getId())
                + " byIdExpire=" + CLIENT_EVENT_EXPIRE_BY_ID.get(entity.getId())
                + " byKey=" + CLIENT_EVENT_BY_KEY.get(eventKey(entity))
                + " instance=" + clientEventType.get(entity)
                + " now=" + entity.level().getGameTime();
    }

    public static int getActiveAdaptationEvent(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            return -1;
        }
        long now = entity.level().getGameTime();
        ensureSameLevel(entity.level());
        Integer byId = CLIENT_EVENT_BY_ID.get(entity.getId());
        Long byIdExpire = CLIENT_EVENT_EXPIRE_BY_ID.get(entity.getId());
        if (byId != null && byIdExpire != null && byIdExpire > now) {
            return byId;
        }
        if (byIdExpire != null) {
            CLIENT_EVENT_BY_ID.remove(entity.getId());
            CLIENT_EVENT_EXPIRE_BY_ID.remove(entity.getId());
        }
        String key = eventKey(entity);
        Long keyedExpire = CLIENT_EVENT_EXPIRE_BY_KEY.get(key);
        Integer keyedType = CLIENT_EVENT_BY_KEY.get(key);
        if (keyedExpire != null && keyedType != null) {
            if (keyedExpire > now) {
                return keyedType;
            }
            CLIENT_EVENT_EXPIRE_BY_KEY.remove(key);
            CLIENT_EVENT_BY_KEY.remove(key);
        }
        Long expire = clientEventExpireTick.get(entity);
        if (expire == null || expire <= now) {
            clientEventType.remove(entity);
            clientEventExpireTick.remove(entity);
            return -1;
        }
        Integer type = clientEventType.get(entity);
        return type == null ? -1 : type;
    }

    public static void updateClientAdaptationEvent(LivingEntity entity, int eventType) {
        if (entity.level().isClientSide) {
            long expireTick = entity.level().getGameTime() + 10; // 持续10刻
            clientEventExpireTick.put(entity, expireTick);
            clientEventType.put(entity, eventType);
            CLIENT_EVENT_BY_KEY.put(eventKey(entity), eventType);
            CLIENT_EVENT_EXPIRE_BY_KEY.put(eventKey(entity), expireTick);
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