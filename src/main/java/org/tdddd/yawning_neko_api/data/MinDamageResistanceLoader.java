package org.tdddd.yawning_neko_api.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per entity multiplier for the health lost through {@code LivingEntity#setHealth}.
 *
 * <p>Data files live in {@code data/<namespace>/min_damage_resistance/<name>.json} and are a flat map of
 * entity id to multiplier, for example {@code {"minecraft:zombie": 0.5}}. The multiplier only applies when
 * the new health is <b>lower</b> than the current one:</p>
 *
 * <ul>
 *   <li>{@code 1.0} (default) - the loss is unchanged;</li>
 *   <li>{@code 0.0} - any {@code setHealth} loss is ignored, so the entity is immune to it;</li>
 *   <li>{@code 2.0} - the loss is doubled.</li>
 * </ul>
 */
public class MinDamageResistanceLoader implements ResourceManagerReloadListener {

    /** Default multiplier: the health loss is unchanged. */
    public static final float DEFAULT_MULTIPLIER = 1.0F;

    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("yawning_neko_api/min_damage_resistance");
    private static final String DIRECTORY = "min_damage_resistance";
    private static final Gson GSON = new Gson();

    private static volatile Map<Identifier, Float> multipliers = Map.of();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<Identifier, Float> loaded = new HashMap<>();
        Map<Identifier, Resource> files = resourceManager.listResources(DIRECTORY,
                path -> path.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = GSON.fromJson(reader, JsonElement.class);
                if (root == null || !root.isJsonObject()) {
                    continue;
                }
                JsonObject object = root.getAsJsonObject();
                for (Map.Entry<String, JsonElement> pair : object.entrySet()) {
                    Identifier entityId = Identifier.tryParse(pair.getKey());
                    if (entityId == null || !pair.getValue().isJsonPrimitive()) {
                        continue;
                    }
                    loaded.put(entityId, Mth.clamp(pair.getValue().getAsFloat(), 0.0F, Float.MAX_VALUE));
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to read min_damage_resistance file {}: {}", entry.getKey(), e.toString());
            }
        }

        multipliers = Map.copyOf(loaded);
        LOGGER.info("Loaded {} min damage resistance entries", loaded.size());
    }

    /** The multiplier configured for this entity, or {@link #DEFAULT_MULTIPLIER}. */
    public static float multiplierFor(LivingEntity entity) {
        if (entity == null) {
            return DEFAULT_MULTIPLIER;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            return DEFAULT_MULTIPLIER;
        }
        Float value = multipliers.get(id);
        return value == null ? DEFAULT_MULTIPLIER : value;
    }

    public static void setMultipliers(Map<Identifier, Float> values) {
        multipliers = Map.copyOf(values);
    }

    public static Optional<Float> configured(Identifier entityId) {
        return Optional.ofNullable(multipliers.get(entityId));
    }
}
