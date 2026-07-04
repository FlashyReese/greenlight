package me.flashyreese.mods.greenlight.feature;

import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Advanced integration point for delivering server policy to the client.
 *
 * <p>Most mods should rely on the built-in server resource pack source. Implement this
 * only when you have another trusted server channel, such as a custom payload from a
 * server-side mod that can update or revoke policy while the player is connected.
 *
 * <p>Sources are checked by descending {@link #priority()}. For one feature, the highest
 * priority source that declares a {@link FeaturePolicy} wins. A disabled policy counts as a
 * declaration, so it can revoke a grant from a lower-priority source.
 *
 * <p>{@link #fingerprint()} is polled on every Greenlight query. Keep it cheap and stable.
 * {@link #load()} runs only when the fingerprint changes and may do heavier work. If either
 * method throws, Greenlight ignores this source until the fingerprint changes again.
 *
 * <p>A source must only report policies that verifiably came from the server the client is
 * currently connected to. Its fingerprint must change when that server context ends so stale
 * grants cannot survive disconnects.
 */
public interface FeaturePolicySource {
    /**
     * Priority of the built-in server resource pack source.
     *
     * <p>Live sources that can update or revoke policy mid-session, such as custom network
     * payloads, should use a higher priority.
     */
    int RESOURCE_PACK_PRIORITY = 0;

    /**
     * Stable unique ID for this source.
     *
     * @return the source ID
     */
    Identifier id();

    /**
     * Merge priority for this source.
     *
     * @return higher values win over lower values
     */
    int priority();

    /**
     * Cheap value that changes whenever this source's policies should be reloaded.
     *
     * @return a stable fingerprint object compared with {@link Object#equals(Object)}
     */
    Object fingerprint();

    /**
     * Loads all policies currently declared by this source.
     *
     * @return policies keyed by feature ID
     */
    Map<Identifier, FeaturePolicy> load();
}
