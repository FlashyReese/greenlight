package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import me.flashyreese.mods.greenlight.feature.spi.GreenlightProvider;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * A handle for one Greenlight-controlled feature in your client mod.
 *
 * <p>Create one with {@link Greenlight#feature(Identifier)} during client init, then keep it
 * somewhere your feature code can reach. Query the handle when the feature would apply, such
 * as during a render event, tick hook, or option check. Do not store an allowed result across
 * worlds or servers.
 *
 * <p>An empty policy means the server has not granted this feature in a form your mod
 * understands. In that case, behave like vanilla on multiplayer.
 *
 * @param <T> decoded settings type returned by your policy decoder
 */
public final class ClientFeature<T> {
    private final Identifier id;
    private final int schemaVersion;
    private final PolicyDecoder<T> decoder;

    private long cachedGeneration = Long.MIN_VALUE;
    private Optional<T> cachedPolicy = Optional.empty();

    ClientFeature(Identifier id, int schemaVersion, PolicyDecoder<T> decoder) {
        this.id = id;
        this.schemaVersion = schemaVersion;
        this.decoder = decoder;
    }

    /**
     * The namespaced ID for this feature.
     *
     * @return the feature ID registered by your mod
     */
    public Identifier id() {
        return this.id;
    }

    /**
     * Whether the current server grants this feature in a form this mod understands.
     *
     * <p>This is equivalent to {@code policy().isPresent()}. Disabled policies, missing
     * policies, settings version mismatches, and decoder failures all count as denied.
     *
     * @return {@code true} when this feature is currently granted and decoded successfully
     */
    public boolean isAllowed() {
        return this.policy().isPresent();
    }

    /**
     * Returns decoded server settings for this feature.
     *
     * <p>The result is empty when the feature is not granted, when the server used a different
     * {@code settings_version}, or when your decoder rejected the JSON. The decode result is
     * cached until the policy changes.
     *
     * @return decoded server settings, or empty when the feature is denied
     */
    public synchronized Optional<T> policy() {
        GreenlightProvider.PolicyQuery query = Greenlight.query(this.id);
        if (query.generation() != this.cachedGeneration) {
            this.cachedGeneration = query.generation();
            this.cachedPolicy = this.decodePolicy(query.policy());
        }

        return this.cachedPolicy;
    }

    private Optional<T> decodePolicy(FeaturePolicy policy) {
        if (policy == null || !policy.enabled()) {
            return Optional.empty();
        }

        if (policy.settingsVersion() != this.schemaVersion) {
            Greenlight.logUnknownSchemaVersion(this.id, policy.settingsVersion());
            return Optional.empty();
        }

        try {
            // Decoders get a copy so they cannot mutate the engine's cached policy.
            return Optional.ofNullable(this.decoder.decode(policy.settings().deepCopy()));
        } catch (Exception e) {
            Greenlight.logDecodeFailure(this.id, e);
            return Optional.empty();
        }
    }

    /**
     * Builder for {@link ClientFeature} handles.
     *
     * <p>A handle declares exactly one settings schema version. The server declares the
     * version it is sending with {@code settings_version} in the policy file. A mismatch
     * denies the feature.
     *
     * <p>Keep settings changes additive when older clients should keep working. Missing
     * values should mean the most restrictive behavior. Bump the version only for breaking
     * changes.
     *
     * @param <T> decoded settings type returned by the current decoder
     */
    public static final class Builder<T> {
        private final Identifier id;
        private final int schemaVersion;
        private final PolicyDecoder<T> decoder;

        Builder(Identifier id, int schemaVersion, PolicyDecoder<T> decoder) {
            this.id = id;
            this.schemaVersion = schemaVersion;
            this.decoder = decoder;
        }

        /**
         * Sets the settings version and decoder for this feature.
         *
         * <p>Without a decoder, the handle exposes a defensive copy of the raw {@link JsonObject}
         * at version {@code 1}. See {@link PolicyDecoder} for decoder failure behavior.
         * {@link PolicyDecoder#fromCodec} adapts a DataFixerUpper codec.
         *
         * @param schemaVersion settings version this client understands
         * @param decoder decoder for the policy {@code settings} object
         * @param <U> decoded settings type returned by the decoder
         * @return a builder with the supplied version and decoder
         */
        public <U> Builder<U> decoder(int schemaVersion, PolicyDecoder<U> decoder) {
            if (schemaVersion < 1) {
                throw new IllegalArgumentException("Schema versions start at 1, got " + schemaVersion);
            }

            return new Builder<>(this.id, schemaVersion, decoder);
        }

        /**
         * Registers the feature and returns its handle.
         *
         * <p>When the full Greenlight runtime is available, each feature ID can only be
         * registered once. A duplicate registration throws so mod conflicts are visible during
         * startup. In API-only optional mode, registration is accepted but no runtime state is
         * kept.
         *
         * @return the registered feature handle
         */
        public ClientFeature<T> register() {
            ClientFeature<T> feature = new ClientFeature<>(this.id, this.schemaVersion, this.decoder);
            Greenlight.registerFeature(feature);
            return feature;
        }
    }
}
