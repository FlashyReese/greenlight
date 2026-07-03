package me.flashyreese.mods.greenlight.feature;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static me.flashyreese.mods.greenlight.feature.TestSupport.settings;
import static me.flashyreese.mods.greenlight.feature.TestSupport.uniqueFeature;
import static me.flashyreese.mods.greenlight.feature.TestSupport.uniqueSourceId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientFeatureTest {
    private static final PolicyDecoder<Integer> READ_N = s -> s.get("n").getAsInt();

    private static FakePolicySource newSource() {
        FakePolicySource source = new FakePolicySource(uniqueSourceId(), 5);
        Greenlight.registerSource(source);
        return source;
    }

    private static ClientFeature<Integer> registerFeature(Identifier id) {
        return Greenlight.feature(id).decoder(1, READ_N).register();
    }

    @Test
    void decodesGrantedPolicy() {
        Identifier id = uniqueFeature();
        newSource().put(id, new FeaturePolicy(true, 1, settings("n", 42)));
        ClientFeature<Integer> feature = registerFeature(id);

        assertTrue(feature.isAllowed());
        assertEquals(Optional.of(42), feature.policy());
    }

    @Test
    void deniesWhenNotGranted() {
        ClientFeature<Integer> feature = registerFeature(uniqueFeature());
        assertFalse(feature.isAllowed());
        assertTrue(feature.policy().isEmpty());
    }

    @Test
    void deniesSettingsVersionMismatch() {
        Identifier id = uniqueFeature();
        newSource().put(id, new FeaturePolicy(true, 2, settings("n", 42)));
        ClientFeature<Integer> feature = registerFeature(id);
        assertFalse(feature.isAllowed());
    }

    @Test
    void deniesDisabledPolicy() {
        Identifier id = uniqueFeature();
        newSource().put(id, new FeaturePolicy(false, 1, settings("n", 42)));
        ClientFeature<Integer> feature = registerFeature(id);
        assertFalse(feature.isAllowed());
    }

    @Test
    void failsClosedWhenDecoderThrows() {
        Identifier id = uniqueFeature();
        newSource().put(id, new FeaturePolicy(true, 1, new com.google.gson.JsonObject()));
        ClientFeature<Integer> feature = registerFeature(id);
        assertFalse(feature.isAllowed());
    }

    @Test
    void reflectsLivePolicyChange() {
        Identifier id = uniqueFeature();
        FakePolicySource source = newSource();
        ClientFeature<Integer> feature = registerFeature(id);

        assertTrue(feature.policy().isEmpty());

        source.put(id, new FeaturePolicy(true, 1, settings("n", 1)));
        assertEquals(Optional.of(1), feature.policy());

        source.clear();
        assertTrue(feature.policy().isEmpty());
    }
}
