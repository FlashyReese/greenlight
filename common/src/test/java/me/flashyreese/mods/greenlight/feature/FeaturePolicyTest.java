package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static me.flashyreese.mods.greenlight.feature.TestSupport.settings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FeaturePolicyTest {
    @Test
    void nullSettingsBecomeEmptyObject() {
        FeaturePolicy policy = new FeaturePolicy(true, 1, null);
        assertNotNull(policy.settings());
        assertEquals(new JsonObject(), policy.settings());
    }

    @Test
    void retainsProvidedFields() {
        JsonObject s = settings("n", 3);
        FeaturePolicy policy = new FeaturePolicy(false, 2, s);
        assertEquals(false, policy.enabled());
        assertEquals(2, policy.settingsVersion());
        assertEquals(s, policy.settings());
    }
}
