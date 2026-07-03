package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import org.junit.jupiter.api.Test;

import static me.flashyreese.mods.greenlight.feature.TestSupport.settings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyDecoderTest {
    private static final Codec<Integer> N_CODEC = Codec.INT.fieldOf("n").codec();

    @Test
    void fromCodecDecodesValidSettings() throws Exception {
        PolicyDecoder<Integer> decoder = PolicyDecoder.fromCodec(N_CODEC);
        assertEquals(7, decoder.decode(settings("n", 7)));
    }

    @Test
    void fromCodecThrowsOnInvalidSettings() {
        PolicyDecoder<Integer> decoder = PolicyDecoder.fromCodec(N_CODEC);
        assertThrows(Exception.class, () -> decoder.decode(new JsonObject()));
    }
}
