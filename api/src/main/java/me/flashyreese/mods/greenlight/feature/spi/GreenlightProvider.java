package me.flashyreese.mods.greenlight.feature.spi;

import me.flashyreese.mods.greenlight.feature.ClientFeature;
import me.flashyreese.mods.greenlight.feature.FeaturePolicy;
import me.flashyreese.mods.greenlight.feature.FeaturePolicySource;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Service provider interface for the Greenlight runtime backend.
 *
 * <p>Consumer mods should not use this directly. Call {@code Greenlight} instead. The full
 * Greenlight mod supplies a provider through {@link java.util.ServiceLoader}. When no
 * provider is present, the public API falls back to default deny.
 */
public interface GreenlightProvider {
    /**
     * Result of a raw policy query.
     *
     * @param generation policy generation used for cache invalidation
     * @param policy policy for the requested feature, or {@code null} when denied
     */
    record PolicyQuery(long generation, FeaturePolicy policy) {
    }

    /**
     * Registers a feature handle with the runtime.
     *
     * @param feature feature to register
     */
    void registerFeature(ClientFeature<?> feature);

    /**
     * Registers an additional policy source.
     *
     * @param source source to register
     */
    void registerSource(FeaturePolicySource source);

    /**
     * Queries the raw policy for one feature.
     *
     * @param featureId feature to query
     * @return raw query result
     */
    PolicyQuery query(ResourceLocation featureId);

    /**
     * Lists currently granted feature IDs.
     *
     * @return granted feature IDs
     */
    Set<ResourceLocation> getGrantedFeatures();
}
