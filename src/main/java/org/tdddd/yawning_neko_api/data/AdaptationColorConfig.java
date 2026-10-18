package org.tdddd.yawning_neko_api.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Which entities get the adaptation colour tint, and in which colours.
 *
 * <p>Loaded from {@code data/<namespace>/adaptation_color_models/*.json}. The display duration is
 * fixed at 10 ticks (matching {@link DamageAdaptation#updateClientAdaptationEvent}); the json field
 * is read for validation only. By default only {@code epca:*} is coloured, so a pack has to opt in
 * for vanilla or GeckoLib mobs.</p>
 */
public final class AdaptationColorConfig {
    private static final Gson GSON = new Gson();

    public static final int DEFAULT_PARTIAL = 0xFF33E6FF;   // 0.2, 0.9, 1.0 - cyan
    public static final int DEFAULT_FULL = 0xFFE633E6;      // 0.9, 0.2, 0.9 - purple
    public static final int DURATION_TICKS = 10;

    private static int partialColor = DEFAULT_PARTIAL;
    private static int fullColor = DEFAULT_FULL;
    private static final Set<ResourceLocation> EXPLICIT = new HashSet<>();
    private static final Set<String> NAMESPACES = new HashSet<>();

    private AdaptationColorConfig() {
    }

    public static void reload(ResourceManager resourceManager) {
        partialColor = DEFAULT_PARTIAL;
        fullColor = DEFAULT_FULL;
        EXPLICIT.clear();
        NAMESPACES.clear();

        resourceManager.listResources("adaptation_color_models",
                path -> path.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (InputStream stream = resource.open()) {
                JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
                if (json == null) {
                    return;
                }
                if (json.has("partial") && json.get("partial").isJsonObject()) {
                    partialColor = readColor(json.getAsJsonObject("partial"), partialColor);
                }
                if (json.has("full") && json.get("full").isJsonObject()) {
                    fullColor = readColor(json.getAsJsonObject("full"), fullColor);
                }
                if (json.has("entities") && json.get("entities").isJsonArray()) {
                    for (JsonElement element : json.getAsJsonArray("entities")) {
                        if (!element.isJsonPrimitive()) {
                            continue;
                        }
                        String raw = element.getAsString();
                        if (raw.endsWith(":*")) {
                            NAMESPACES.add(raw.substring(0, raw.length() - 2));
                        } else {
                            ResourceLocation parsed = ResourceLocation.tryParse(raw);
                            if (parsed != null) {
                                EXPLICIT.add(parsed);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static int readColor(JsonObject section, int fallback) {
        if (!section.has("color") || !section.get("color").isJsonPrimitive()) {
            return fallback;
        }
        String raw = section.get("color").getAsString().trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        try {
            long value = Long.parseLong(raw, 16);
            return raw.length() <= 6 ? (int) (0xFF000000L | value) : (int) value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean applies(LivingEntity entity) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) {
            return false;
        }
        return EXPLICIT.contains(entityId) || NAMESPACES.contains(entityId.getNamespace());
    }

    /** -1 when nothing should be drawn, otherwise the ARGB colour for the given event type. */
    public static int colorFor(int eventType) {
        if (eventType < 0) {
            return -1;
        }
        return eventType == 0 ? partialColor : fullColor;
    }
}
