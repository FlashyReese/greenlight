package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared helpers. The policy engine keeps global static state with no reset hook and rejects
 * duplicate IDs, so every test mints fresh feature and source IDs instead of resetting.
 */
final class TestSupport {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestSupport() {
    }

    static Identifier uniqueFeature() {
        return Identifier.fromNamespaceAndPath("greenlight-test", "feature-" + COUNTER.incrementAndGet());
    }

    static String uniqueSourceId() {
        return "source-" + COUNTER.incrementAndGet();
    }

    static JsonObject settings(String key, int value) {
        JsonObject json = new JsonObject();
        json.addProperty(key, value);
        return json;
    }

    static JsonObject envelope(int protocolVersion, String feature, boolean enabled, Integer settingsVersion, JsonObject settings) {
        JsonObject json = new JsonObject();
        json.addProperty("protocol_version", protocolVersion);
        json.addProperty("feature", feature);
        json.addProperty("enabled", enabled);
        if (settingsVersion != null) {
            json.addProperty("settings_version", settingsVersion);
        }
        if (settings != null) {
            json.add("settings", settings);
        }
        return json;
    }
}
