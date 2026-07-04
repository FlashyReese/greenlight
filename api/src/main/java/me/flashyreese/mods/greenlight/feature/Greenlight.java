package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import me.flashyreese.mods.greenlight.feature.spi.GreenlightProvider;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Main API for Minecraft client mods that want to honor Greenlight server policy.
 *
 * <p>Use this when your mod has a gameplay-affecting client feature that should stay off on
 * multiplayer unless the server has explicitly allowed it. Servers usually publish that
 * permission through a server resource pack at
 * {@code assets/<feature_namespace>/client_features/v1/<feature_path>.json}. Advanced
 * integrations can also provide policies through {@link FeaturePolicySource}.
 *
 * <p>Greenlight does not enable your feature by itself. Your mod still owns the setting,
 * the UI, and the final behavior. Treat a grant as permission plus server limits, then clamp
 * the player's local config before applying the feature.
 *
 * <p>Feature owner checklist:
 * <ul>
 *   <li>Default deny on multiplayer when no trusted policy is present.</li>
 *   <li>Query at the point of use, such as each render event or gameplay hook.</li>
 *   <li>Do not cache grants across worlds or servers.</li>
 *   <li>Remember that this helps honest clients. It is not an anti-cheat.</li>
 * </ul>
 *
 * <p>Typical registration during client init:
 * <pre>{@code
 * public static final ClientFeature<MyPolicy> CAVE_TINT = Greenlight
 *         .feature(Identifier.fromNamespaceAndPath("examplemod", "cave_tint"))
 *         .decoder(1, MyPolicy::fromJson) // or PolicyDecoder.fromCodec(MyPolicy.CODEC)
 *         .register();
 *
 * // at application time:
 * Optional<MyPolicy> policy = CAVE_TINT.policy();
 * }</pre>
 */
public final class Greenlight {
    private static final Logger LOGGER = LoggerFactory.getLogger("Greenlight");
    private static final GreenlightProvider NOOP_PROVIDER = new NoopProvider();
    private static final GreenlightProvider PROVIDER = loadProvider();

    private Greenlight() {
    }

    /**
     * Whether the full Greenlight runtime mod is installed.
     *
     * <p>If a mod only ships the small Greenlight API for optional compatibility, this returns
     * {@code false}. The API remains safe to call in that state: feature registration is
     * accepted, source registration is ignored, and every query returns denied.
     *
     * @return {@code true} when the full Greenlight runtime provider is loaded
     */
    public static boolean isAvailable() {
        return PROVIDER != NOOP_PROVIDER;
    }

    /**
     * Starts registering a Greenlight-controlled feature.
     *
     * <p>The ID namespace should be your mod ID. The matching server policy file lives at
     * {@code assets/<namespace>/client_features/v1/<path>.json}.
     *
     * @param featureId namespaced ID for the feature
     * @return a builder for registering the feature
     */
    public static ClientFeature.Builder<JsonObject> feature(Identifier featureId) {
        return new ClientFeature.Builder<>(featureId, 1, JsonObject::deepCopy);
    }

    /**
     * Registers an additional way to receive server policy.
     *
     * <p>Most client mods do not need this. Use it for integrations such as a custom payload
     * from a server mod that can update or revoke policy while connected. Source IDs must be
     * unique. See {@link FeaturePolicySource} for priority and trust rules.
     *
     * @param source source to register
     */
    public static void registerSource(FeaturePolicySource source) {
        PROVIDER.registerSource(source);
    }

    /**
     * Checks whether a feature is currently granted without decoding its settings.
     *
     * <p>For repeated checks from your own mod, prefer a registered {@link ClientFeature};
     * handles validate {@code settings_version} and cache decoded settings until the policy
     * changes. This raw helper only checks whether an enabled policy exists.
     *
     * @param featureId feature to check
     * @return {@code true} when the feature is currently granted
     */
    public static boolean isAllowed(Identifier featureId) {
        GreenlightProvider.PolicyQuery query = query(featureId);
        return query.policy() != null && query.policy().enabled();
    }

    /**
     * Returns the raw settings JSON for a granted feature.
     *
     * <p>The returned object is a defensive copy. The result is empty when the feature is not
     * granted. This raw helper does not validate {@code settings_version}; use a registered
     * {@link ClientFeature} when you need typed settings.
     *
     * @param featureId feature to inspect
     * @return raw settings JSON, or empty when the feature is denied
     */
    public static Optional<JsonObject> getSettings(Identifier featureId) {
        GreenlightProvider.PolicyQuery query = query(featureId);
        if (query.policy() == null || !query.policy().enabled()) {
            return Optional.empty();
        }

        return Optional.of(query.policy().settings().deepCopy());
    }

    /**
     * IDs of every feature the current server currently grants.
     *
     * <p>This is mainly useful for diagnostics screens, logs, and developer tools. The set can
     * include feature IDs from valid policy files even when no loaded mod registered a handle
     * for that ID.
     *
     * @return granted feature IDs
     */
    public static Set<Identifier> getGrantedFeatures() {
        return PROVIDER.getGrantedFeatures();
    }

    static void registerFeature(ClientFeature<?> feature) {
        PROVIDER.registerFeature(feature);
    }

    static GreenlightProvider.PolicyQuery query(Identifier featureId) {
        return PROVIDER.query(featureId);
    }

    static void logDecodeFailure(Identifier featureId, Exception e) {
        LOGGER.warn("Failed to decode policy settings for client feature {}; treating it as denied", featureId, e);
    }

    static void logUnknownSchemaVersion(Identifier featureId, int settingsVersion) {
        LOGGER.warn("Server policy for client feature {} uses unknown settings_version {}; treating it as denied", featureId, settingsVersion);
    }

    private static GreenlightProvider loadProvider() {
        try {
            for (GreenlightProvider provider : ServiceLoader.load(GreenlightProvider.class)) {
                return provider;
            }
        } catch (ServiceConfigurationError e) {
            LOGGER.warn("Failed to load Greenlight runtime provider; falling back to default-deny API mode", e);
        }

        return NOOP_PROVIDER;
    }

    private static final class NoopProvider implements GreenlightProvider {
        private static final PolicyQuery EMPTY_QUERY = new PolicyQuery(0, null);

        @Override
        public void registerFeature(ClientFeature<?> feature) {
        }

        @Override
        public void registerSource(FeaturePolicySource source) {
        }

        @Override
        public PolicyQuery query(Identifier featureId) {
            return EMPTY_QUERY;
        }

        @Override
        public Set<Identifier> getGrantedFeatures() {
            return Set.of();
        }
    }
}
