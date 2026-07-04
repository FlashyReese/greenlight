package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static me.flashyreese.mods.greenlight.feature.TestSupport.settings;
import static me.flashyreese.mods.greenlight.feature.TestSupport.uniqueFeature;
import static me.flashyreese.mods.greenlight.feature.TestSupport.uniqueSourceId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyStoreTest {
    private static FakePolicySource source(int priority) {
        FakePolicySource source = new FakePolicySource(uniqueSourceId(), priority);
        Greenlight.registerSource(source);
        return source;
    }

    @Test
    void higherPrioritySourceWins() {
        ResourceLocation id = uniqueFeature();
        source(1).put(id, new FeaturePolicy(true, 1, settings("n", 1)));
        source(2).put(id, new FeaturePolicy(true, 1, settings("n", 2)));

        assertEquals(2, Greenlight.getSettings(id).orElseThrow().get("n").getAsInt());
    }

    @Test
    void disabledHigherPriorityRevokesLowerGrant() {
        ResourceLocation id = uniqueFeature();
        source(1).put(id, new FeaturePolicy(true, 1, settings("n", 1)));
        source(2).put(id, new FeaturePolicy(false, 1, new JsonObject()));

        assertFalse(Greenlight.isAllowed(id));
        assertFalse(Greenlight.getGrantedFeatures().contains(id));
    }

    @Test
    void failsClosedWhenSourceThrows() {
        ResourceLocation id = uniqueFeature();
        FakePolicySource source = source(5);
        source.put(id, new FeaturePolicy(true, 1, settings("n", 1)));
        source.throwOnLoad = true;

        assertFalse(Greenlight.isAllowed(id));
    }

    @Test
    void getGrantedFeaturesListsEnabledOnly() {
        ResourceLocation enabled = uniqueFeature();
        ResourceLocation disabled = uniqueFeature();
        FakePolicySource source = source(5);
        source.put(enabled, new FeaturePolicy(true, 1, new JsonObject()));
        source.put(disabled, new FeaturePolicy(false, 1, new JsonObject()));

        assertTrue(Greenlight.getGrantedFeatures().contains(enabled));
        assertFalse(Greenlight.getGrantedFeatures().contains(disabled));
    }

    @Test
    void getSettingsReturnsDefensiveCopy() {
        ResourceLocation id = uniqueFeature();
        source(5).put(id, new FeaturePolicy(true, 1, settings("n", 5)));

        JsonObject first = Greenlight.getSettings(id).orElseThrow();
        first.addProperty("n", 999);

        assertEquals(5, Greenlight.getSettings(id).orElseThrow().get("n").getAsInt());
    }
}
