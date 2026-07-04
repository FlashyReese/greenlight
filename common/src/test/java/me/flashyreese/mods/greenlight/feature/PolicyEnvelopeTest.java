package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static me.flashyreese.mods.greenlight.feature.TestSupport.envelope;
import static me.flashyreese.mods.greenlight.feature.TestSupport.settings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEnvelopeTest {
    private static final ResourceLocation FEATURE = ResourceLocation.tryParse("greenlight-test:cave_tint");
    private static final String FEATURE_ID = "greenlight-test:cave_tint";

    private static FeaturePolicy parse(JsonObject json) {
        return ResourcePackPolicySource.parsePolicy(json, FEATURE, "test");
    }

    @Test
    void acceptsValidEnvelope() {
        FeaturePolicy policy = parse(envelope(1, FEATURE_ID, true, 1, settings("n", 5)));
        assertNotNull(policy);
        assertTrue(policy.enabled());
        assertEquals(1, policy.settingsVersion());
        assertEquals(5, policy.settings().get("n").getAsInt());
    }

    @Test
    void rejectsUnsupportedProtocolVersion() {
        assertNull(parse(envelope(2, FEATURE_ID, true, 1, settings("n", 5))));
    }

    @Test
    void rejectsMissingProtocolVersion() {
        JsonObject json = envelope(1, FEATURE_ID, true, 1, settings("n", 5));
        json.remove("protocol_version");
        assertNull(parse(json));
    }

    @Test
    void rejectsFeatureIdMismatch() {
        assertNull(parse(envelope(1, "greenlight-test:other", true, 1, settings("n", 5))));
    }

    @Test
    void defaultsSettingsVersionToOneWhenAbsent() {
        FeaturePolicy policy = parse(envelope(1, FEATURE_ID, true, null, settings("n", 5)));
        assertNotNull(policy);
        assertEquals(1, policy.settingsVersion());
    }

    @Test
    void rejectsSettingsVersionBelowOne() {
        assertNull(parse(envelope(1, FEATURE_ID, true, 0, settings("n", 5))));
    }

    @Test
    void defaultsEnabledToFalseWhenAbsent() {
        JsonObject json = envelope(1, FEATURE_ID, true, 1, settings("n", 5));
        json.remove("enabled");
        FeaturePolicy policy = parse(json);
        assertNotNull(policy);
        assertFalse(policy.enabled());
    }

    @Test
    void defaultsSettingsToEmptyObjectWhenAbsent() {
        FeaturePolicy policy = parse(envelope(1, FEATURE_ID, true, 1, null));
        assertNotNull(policy);
        assertEquals(new JsonObject(), policy.settings());
    }
}
