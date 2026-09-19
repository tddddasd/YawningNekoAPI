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
                        String path = resourceLocation.getPath();
                        String fileName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                        Identifier configId = Identifier.fromNamespaceAndPath(resourceLocation.getNamespace(), fileName);
                        DamageAdaptationConfig config = DamageAdaptationConfig.fromJson(json);
                        CONFIGS.put(configId, config);
                    } catch (Exception e) {
                    }
                });
        // 原 FMLEnvironment.dist.isClient() -> FMLEnvironment.getDist()
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
}
