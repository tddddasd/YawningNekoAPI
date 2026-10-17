package org.tdddd.yawning_neko_api.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.tdddd.yawning_neko_api.Yawning_neko_api;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class EntityAdaptationMapping implements ResourceManagerReloadListener {
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, ResourceLocation> ENTITY_TO_CONFIG_MAP = new HashMap<>();
    /** true = override the base config, false (the default) = add to it. */
    private static final Map<ResourceLocation, Boolean> ENTITY_OVERRIDE_MAP = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ENTITY_TO_CONFIG_MAP.clear();
          ENTITY_OVERRIDE_MAP.clear();

        resourceManager.listResources("entity_adaptation_mappings",
                        file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
                        String sourceNamespace = resourceLocation.getNamespace();
                        parseMappingsFromJson(json, sourceNamespace);
                    } catch (Exception e) {
                    }
                });
    }

    private void parseMappingsFromJson(JsonObject jsonObject, String sourceNamespace) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String entityKey = entry.getKey();
                      // metadata keys ("id", "_comment", ...) are not entity ids
                      if (entityKey.equals("id") || entityKey.startsWith("_")) {
                          continue;
                      }
            JsonElement configElement = entry.getValue();
            ResourceLocation entityId = ResourceLocation.tryParse(entityKey);
            if (entityId == null) {
                continue;
            }

            String configName;
            if (configElement.isJsonPrimitive() && configElement.getAsJsonPrimitive().isString()) {
                configName = configElement.getAsString();
            } else if (configElement.isJsonObject()) {
                JsonElement nestedConfig = configElement.getAsJsonObject().get("config");
                if (nestedConfig != null && nestedConfig.isJsonPrimitive()) {
                    configName = nestedConfig.getAsString();
                } else {
                    continue;
                }
                  JsonElement modeElement = configElement.getAsJsonObject().get("mode");
                  if (modeElement != null && modeElement.isJsonPrimitive()) {
                      ENTITY_OVERRIDE_MAP.put(entityId, "override".equalsIgnoreCase(modeElement.getAsString()));
                  }
            } else {
                continue;
            }

            ResourceLocation configId;
            if (configName.contains(":")) {
                configId = ResourceLocation.tryParse(configName);
            } else {
                configId = new ResourceLocation(sourceNamespace, configName);
            }
            if (configId == null) {
                continue;
            }

            ENTITY_TO_CONFIG_MAP.put(entityId, configId);
        }
    }

    public static ResourceLocation getConfigForEntity(ResourceLocation entityId) {
        return ENTITY_TO_CONFIG_MAP.get(entityId);
    }

    public static boolean hasMapping(ResourceLocation entityId) {
        return ENTITY_TO_CONFIG_MAP.containsKey(entityId);
    }

    /** Missing "mode" means add, per spec. */
    public static boolean isOverride(ResourceLocation entityId) {
        return ENTITY_OVERRIDE_MAP.getOrDefault(entityId, Boolean.FALSE);
    }

    /** Drops the cached mode of one entity (used when a mapping is re-resolved). */
    public static void forgetOverride(ResourceLocation entityId) {
        ENTITY_OVERRIDE_MAP.remove(entityId);
    }
}
