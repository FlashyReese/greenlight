package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

/**
 * Decodes the raw {@code settings} JSON for one feature into your mod's own policy type.
 *
 * <p>Decoding runs once per policy change, not every time you query the feature. Throwing an
 * exception denies the feature and logs the error. Prefer strict validation so malformed
 * server policy fails closed.
 *
 * @param <T> decoded settings type returned to the client mod
 */
@FunctionalInterface
public interface PolicyDecoder<T> {
    /**
     * Decodes one server policy settings object.
     *
     * @param settings defensive copy of the raw JSON settings object
     * @return decoded settings value, or {@code null} to deny the feature
     * @throws Exception when the settings are malformed or unsupported
     */
    T decode(JsonObject settings) throws Exception;

    /**
     * Adapts a DataFixerUpper {@link Codec} into a Greenlight policy decoder.
     *
     * @param codec codec for the settings type
     * @param <T> decoded settings type
     * @return policy decoder backed by the codec
     */
    static <T> PolicyDecoder<T> fromCodec(Codec<T> codec) {
        return settings -> codec.parse(JsonOps.INSTANCE, settings).getOrThrow();
    }
}
