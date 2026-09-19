package org.tdddd.yawning_neko_api.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

public class MinDamageConfig {
    private final float minDamage;

    public MinDamageConfig(float minDamage) {
        this.minDamage = minDamage;
    }

    public float getMinDamage() { return minDamage; }

    /**
     * 从JSON解析配置
     * 支持数字（直接值）或对象（含 min_damage 字段）
     */
    public static MinDamageConfig fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            float minDamage = json.getAsFloat();
            return new MinDamageConfig(minDamage);
        } else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            float minDamage = GsonHelper.getAsFloat(obj, "min_damage", 0.0f);
            return new MinDamageConfig(minDamage);
        }
        return new MinDamageConfig(0.0f);
    }
}
