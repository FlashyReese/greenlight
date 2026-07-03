package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;

/**
 * The policy a server advertised for one client feature.
 *
 * <p>{@code enabled} is the main allow or deny flag. A disabled policy still matters: it can
 * override a lower-priority source that tried to grant the feature.
 *
 * <p>{@code settings} is the feature-specific JSON body. Greenlight does not assign meaning
 * to those keys. The mod that owns the feature decodes them through its
 * {@link PolicyDecoder}.
 *
 * <p>{@code settingsVersion} is also owned by the feature's mod. A typed
 * {@link ClientFeature} denies the feature when this does not match the version registered
 * by the client mod. Raw Greenlight helpers expose enabled policies without decoding this
 * body.
 *
 * @param enabled whether the server is granting this feature
 * @param settingsVersion version of the feature-specific settings body
 * @param settings feature-specific settings JSON
 */
public record FeaturePolicy(boolean enabled, int settingsVersion, JsonObject settings) {
    /**
     * Creates a policy, using an empty settings object when {@code settings} is {@code null}.
     */
    public FeaturePolicy {
        if (settings == null) {
            settings = new JsonObject();
        }
    }
}
