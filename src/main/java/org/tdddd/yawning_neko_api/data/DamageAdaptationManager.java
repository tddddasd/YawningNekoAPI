package org.tdddd.yawning_neko_api.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class DamageAdaptationManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new Gson();
    private static final Map<Identifier, DamageAdaptationConfig> CONFIGS = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        CONFIGS.clear();
        resourceManager.listResources("damage_adaptation_configs",
                        file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
                        Identifier configId = jsonId(json, resourceLocation);
                        DamageAdaptationConfig config = DamageAdaptationConfig.fromJson(json);
                        CONFIGS.put(configId, config);
                    } catch (Exception e) {
                    }
                });
        
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            AdaptationConfigResolver.refreshAllEntitiesConfig();
        }
    }

    public static DamageAdaptationConfig getConfig(Identifier entityId) {
        return CONFIGS.get(entityId);
    }

    public static boolean hasConfig(Identifier entityId) {
        Identifier configId = EntityAdaptationMapping.getConfigForEntity(entityId);
        if (configId != null) {
            return CONFIGS.containsKey(configId);
        }
        return CONFIGS.containsKey(entityId);
    }

    public static DamageAdaptationConfig getDirectConfig(Identifier configId) {
        return CONFIGS.get(configId);
    }

    /** Reads the explicit "id" from the json, falling back to the file name for old packs. */
    private static Identifier jsonId(com.google.gson.JsonObject json, Identifier file) {
        if (json.has("id") && json.get("id").isJsonPrimitive()) {
            Identifier explicit = Identifier.tryParse(json.get("id").getAsString());
            if (explicit != null) {
                return explicit;
            }
        }
        String path = file.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
        return Identifier.fromNamespaceAndPath(file.getNamespace(), fileName);
    }
}
