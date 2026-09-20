package org.tdddd.yawning_neko_api.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MinimumDamageManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<Identifier, MinDamageConfig> ENTITY_CONFIGS = new HashMap<>();

    private static MinimumDamageManager instance;

    public MinimumDamageManager() {
        instance = this;
    }

    public static MinimumDamageManager getInstance() {
        return instance;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ENTITY_CONFIGS.clear();

        resourceManager.listResources("minimum_damage", file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        JsonElement element = GSON.fromJson(new InputStreamReader(stream), JsonElement.class);
                        if (element.isJsonObject()) {
                            JsonObject jsonObject = element.getAsJsonObject();
                            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                                Identifier entityId = Identifier.tryParse(entry.getKey());
                                if (entityId != null) {
                                    MinDamageConfig config = MinDamageConfig.fromJson(entry.getValue());
                                    ENTITY_CONFIGS.put(entityId, config);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to load minimum damage config from {}", resourceLocation, e);
                    }
                });
    }

    public float getMinDamage(Entity attacker) {
        if (attacker == null) return 0.0f;
        
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType());
        if (entityId == null) return 0.0f;
        MinDamageConfig config = ENTITY_CONFIGS.get(entityId);
        return config != null ? config.getMinDamage() : 0.0f;
    }

    public boolean hasMinimumDamageConfig(Entity attacker) {
        if (attacker == null) return false;
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType());
        return entityId != null && ENTITY_CONFIGS.containsKey(entityId);
    }
}
