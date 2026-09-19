package org.tdddd.yawning_neko_api.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class AdaptationPriorityRuleLoader implements ResourceManagerReloadListener {
    private static final Gson GSON = new Gson();
    private static final List<PriorityRule> RULES = new ArrayList<>();
    private static final Map<Identifier, Map<Identifier, Identifier>> MAPPING_CACHE = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        RULES.clear();
        MAPPING_CACHE.clear();

        resourceManager.listResources("adaptation_rules",
                path -> path.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (InputStream stream = resource.open();
                 InputStreamReader reader = new InputStreamReader(stream)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root.has("rules") && root.get("rules").isJsonArray()) {
                    for (JsonElement elem : root.getAsJsonArray("rules")) {
                        PriorityRule rule = PriorityRule.fromJson(elem.getAsJsonObject());
                        if (rule != null) {
                            RULES.add(rule);
                        }
                    }
                }
            } catch (Exception e) {
            }
        });

        resourceManager.listResources("entity_adaptation_mappings",
                path -> path.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (InputStream stream = resource.open();
                 InputStreamReader reader = new InputStreamReader(stream)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                String path = id.getPath();
                String fileName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                Identifier mappingId = Identifier.fromNamespaceAndPath(id.getNamespace(), fileName);
                Map<Identifier, Identifier> entityToConfig = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    String entityKey = entry.getKey();
                    JsonElement configElement = entry.getValue();
                    Identifier entityId = Identifier.tryParse(entityKey);
                    if (entityId == null) continue;
                    Identifier configId = null;
                    if (configElement.isJsonPrimitive() && configElement.getAsJsonPrimitive().isString()) {
                        configId = Identifier.tryParse(configElement.getAsString());
                    } else if (configElement.isJsonObject() && configElement.getAsJsonObject().has("config")) {
                        configId = Identifier.tryParse(configElement.getAsJsonObject().get("config").getAsString());
                    }
                    if (configId != null) {
                        entityToConfig.put(entityId, configId);
                    }
                }
                if (!entityToConfig.isEmpty()) {
                    MAPPING_CACHE.put(mappingId, entityToConfig);
                }
            } catch (Exception e) {
            }
        });

        RULES.sort(Comparator.comparingInt(r -> r.priority));
        checkConflicts();
    }

    private static void checkConflicts() {
        Map<Integer, List<PriorityRule>> groups = new HashMap<>();
        for (PriorityRule rule : RULES) {
            groups.computeIfAbsent(rule.priority, k -> new ArrayList<>()).add(rule);
        }
        for (List<PriorityRule> group : groups.values()) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    if (conditionsConflict(group.get(i).condition, group.get(j).condition)) {
                        throw new IllegalStateException(String.format(
                                "Conflict in priority %d: rules '%s' and '%s' can match the same entity",
                                group.get(i).priority,
                                group.get(i).configId != null ? group.get(i).configId : group.get(i).mappingId,
                                group.get(j).configId != null ? group.get(j).configId : group.get(j).mappingId));
                    }
                }
            }
        }
    }

    private static boolean conditionsConflict(Condition a, Condition b) {
        if (a == null || b == null) return true;
        Set<Identifier> aTypes = a.entityTypes;
        Set<Identifier> bTypes = b.entityTypes;
        if (aTypes != null && bTypes != null && Collections.disjoint(aTypes, bTypes)) {
            return false;
        }
        Set<ResourceKey<Level>> aDims = a.dimensions;
        Set<ResourceKey<Level>> bDims = b.dimensions;
        if (aDims != null && bDims != null && Collections.disjoint(aDims, bDims)) {
            return false;
        }
        return true;
    }

    public static Identifier resolveConfigId(LivingEntity entity, Level level) {
        for (PriorityRule rule : RULES) {
            if (rule.matches(entity, level)) {
                if (rule.mappingId != null) {
                    Map<Identifier, Identifier> mapping = MAPPING_CACHE.get(rule.mappingId);
                    if (mapping != null) {
                        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                        Identifier configId = mapping.get(entityId);
                        if (configId != null) {
                            return configId;
                        }
                    }
                } else if (rule.configId != null) {
                    return rule.configId;
                }
            }
        }
        return null;
    }

    public static class PriorityRule {
        public final int priority;
        public final Condition condition;
        public final Identifier configId;
        public final Identifier mappingId;

        private PriorityRule(int priority, Condition condition, Identifier configId, Identifier mappingId) {
            this.priority = priority;
            this.condition = condition;
            this.configId = configId;
            this.mappingId = mappingId;
        }

        public boolean matches(LivingEntity entity, Level level) {
            return condition.matches(entity, level);
        }

        static PriorityRule fromJson(JsonObject obj) {
            int priority = obj.get("priority").getAsInt();
            JsonObject condObj = obj.getAsJsonObject("condition");
            Condition condition = Condition.fromJson(condObj);
            Identifier configId = null;
            Identifier mappingId = null;
            if (obj.has("config")) {
                configId = Identifier.tryParse(obj.get("config").getAsString());
            }
            if (obj.has("mapping")) {
                mappingId = Identifier.tryParse(obj.get("mapping").getAsString());
            }
            if (configId == null && mappingId == null) {
                throw new JsonSyntaxException("Rule must have either 'config' or 'mapping'");
            }
            return new PriorityRule(priority, condition, configId, mappingId);
        }
    }

    private static class Condition {
        final Set<Identifier> entityTypes;
        final Set<ResourceKey<Level>> dimensions;
        final String namePattern;

        Condition(Set<Identifier> entityTypes, Set<ResourceKey<Level>> dimensions,
                  String namePattern) {
            this.entityTypes = entityTypes;
            this.dimensions = dimensions;
            this.namePattern = namePattern;
        }

        boolean matches(LivingEntity entity, Level level) {
            if (entityTypes != null && !entityTypes.isEmpty()) {
                Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (!entityTypes.contains(typeId)) return false;
            }
            if (dimensions != null && !dimensions.isEmpty()) {
                ResourceKey<Level> dimKey = level.dimension();
                if (!dimensions.contains(dimKey)) return false;
            }
            if (namePattern != null && !namePattern.isEmpty()) {
                String customName = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
                if (!Pattern.matches(namePattern, customName)) return false;
            }
            return true;
        }

        static Condition fromJson(JsonObject obj) {
            Set<Identifier> types = null;
            if (obj.has("entity_type")) {
                types = new HashSet<>();
                JsonElement elem = obj.get("entity_type");
                if (elem.isJsonArray()) {
                    for (JsonElement e : elem.getAsJsonArray())
                        types.add(Identifier.tryParse(e.getAsString()));
                } else {
                    types.add(Identifier.tryParse(elem.getAsString()));
                }
            }
            Set<ResourceKey<Level>> dims = null;
            if (obj.has("dimension")) {
                dims = new HashSet<>();
                JsonElement elem = obj.get("dimension");
                if (elem.isJsonArray()) {
                    for (JsonElement e : elem.getAsJsonArray())
                        dims.add(ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(e.getAsString())));
                } else {
                    dims.add(ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(elem.getAsString())));
                }
            }
            String namePattern = obj.has("name_pattern") ? obj.get("name_pattern").getAsString() : null;
            return new Condition(types, dims, namePattern);
        }
    }
}
