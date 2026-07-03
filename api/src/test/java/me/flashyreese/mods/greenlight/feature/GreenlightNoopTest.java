package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreenlightNoopTest {
    private static final Identifier FEATURE = Identifier.fromNamespaceAndPath("greenlight-test", "noop");

    @Test
    void apiFailsClosedWithoutRuntimeProvider() {
        ClientFeature<Integer> feature = Greenlight
                .feature(FEATURE)
                .decoder(1, settings -> 1)
                .register();

        Greenlight.registerSource(new FeaturePolicySource() {
            @Override
            public Identifier id() {
                return Identifier.fromNamespaceAndPath("greenlight-test", "ignored");
            }

            @Override
            public int priority() {
                return 100;
            }

            @Override
            public Object fingerprint() {
                return new Object();
            }

            @Override
            public Map<Identifier, FeaturePolicy> load() {
                return Map.of(FEATURE, new FeaturePolicy(true, 1, new JsonObject()));
            }
        });

        assertFalse(Greenlight.isAvailable());
        assertFalse(feature.isAllowed());
        assertTrue(feature.policy().isEmpty());
        assertFalse(Greenlight.isAllowed(FEATURE));
        assertTrue(Greenlight.getSettings(FEATURE).isEmpty());
        assertTrue(Greenlight.getGrantedFeatures().isEmpty());
    }
}
