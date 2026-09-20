package org.tdddd.yawning_neko_api.data;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class EntityImmunityEffectManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<String, List<String>> ENTITY_IMMUNITIES = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ENTITY_IMMUNITIES.clear();

        resourceManager.listResources("entity_immunity_effects", file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        JsonElement element = GSON.fromJson(new InputStreamReader(stream), JsonElement.class);
                        parseImmunities(element);
                    } catch (Exception e) {
                        System.err.println("Failed to load entity immunity data: " + resourceLocation);
                        e.printStackTrace();
                    }
                });
    }

    
    private void parseImmunities(JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String entityId = entry.getKey();
            JsonElement value = entry.getValue();
            List<String> immunities = new ArrayList<>();

            if (value.isJsonArray()) {
                for (JsonElement e : value.getAsJsonArray()) {
                    if (e.isJsonPrimitive()) {
                        immunities.add(e.getAsString());
                    }
                }
            } else if (value.isJsonObject()) {
                JsonObject configObj = value.getAsJsonObject();
                if (configObj.has("immunities") && configObj.get("immunities").isJsonArray()) {
                    for (JsonElement e : configObj.get("immunities").getAsJsonArray()) {
                        if (e.isJsonPrimitive()) {
                            immunities.add(e.getAsString());
                        }
                    }
                }
                else if (configObj.has("base_effects") && configObj.get("base_effects").isJsonArray()) {
                    for (JsonElement e : configObj.get("base_effects").getAsJsonArray()) {
                        if (e.isJsonObject()) {
                            JsonObject effectObj = e.getAsJsonObject();
                            if (effectObj.has("effect") && effectObj.get("effect").isJsonPrimitive()) {
                                immunities.add(effectObj.get("effect").getAsString());
                            }
                        } else if (e.isJsonPrimitive()) {
                            immunities.add(e.getAsString());
                        } else if (e.isJsonArray()) {
                            JsonArray arr = e.getAsJsonArray();
                            if (arr.size() > 0 && arr.get(0).isJsonPrimitive()) {
                                immunities.add(arr.get(0).getAsString());
                            }
                        }
                    }
                }
            }

            if (!immunities.isEmpty()) {
                ENTITY_IMMUNITIES.put(entityId, immunities);
            }
        }
    }

    public static boolean isImmune(LivingEntity entity, String effectId) {
        
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        List<String> immunities = ENTITY_IMMUNITIES.get(entityId);
        return immunities != null && immunities.contains(effectId);
    }

    public static List<String> getImmunities(Mob entity) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return ENTITY_IMMUNITIES.getOrDefault(entityId, Collections.emptyList());
    }

    public static boolean hasImmunities(Mob entity) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return ENTITY_IMMUNITIES.containsKey(entityId);
    }

    public static Map<String, List<String>> getAllImmunities() {
        return Collections.unmodifiableMap(ENTITY_IMMUNITIES);
    }
}
