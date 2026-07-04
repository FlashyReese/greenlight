package me.flashyreese.mods.greenlight.feature;

import me.flashyreese.mods.greenlight.feature.spi.GreenlightProvider;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal policy engine behind the public Greenlight API.
 *
 * <p>Mods should interact with {@link Greenlight} and {@link ClientFeature}, not this class.
 *
 * <p>Policies are pulled lazily. Each query checks the cheap source fingerprints. A source's
 * {@code load()} method only runs when its fingerprint changes. When policy changes, the
 * manager rebuilds the merged policy map and bumps a generation counter so feature handles
 * can refresh their decoded caches.
 */
final class ClientFeaturePolicyManager {
    static final Logger LOGGER = LoggerFactory.getLogger("Greenlight");

    private static final Object ERROR_FINGERPRINT = new Object();

    private static final Map<ResourceLocation, ClientFeature<?>> FEATURES = new ConcurrentHashMap<>();
    private static final List<SourceState> SOURCES = new ArrayList<>();
    private static Map<ResourceLocation, FeaturePolicy> mergedPolicies = Map.of();
    private static long generation;

    static {
        SOURCES.add(new SourceState(new ResourcePackPolicySource()));
    }

    private static final class SourceState {
        final FeaturePolicySource source;
        Object fingerprint;
        Map<ResourceLocation, FeaturePolicy> policies = Map.of();

        SourceState(FeaturePolicySource source) {
            this.source = source;
        }
    }

    private ClientFeaturePolicyManager() {
    }

    static void registerFeature(ClientFeature<?> feature) {
        if (FEATURES.putIfAbsent(feature.id(), feature) != null) {
            throw new IllegalStateException("Client feature '" + feature.id() + "' is already registered");
        }
    }

    static synchronized void registerSource(FeaturePolicySource source) {
        for (SourceState state : SOURCES) {
            if (state.source.id().equals(source.id())) {
                throw new IllegalStateException("Feature policy source '" + source.id() + "' is already registered");
            }
        }

        SOURCES.add(new SourceState(source));
        // Highest priority first; stable sort keeps registration order for equal priorities.
        SOURCES.sort(Comparator.comparingInt((SourceState state) -> state.source.priority()).reversed());
        rebuildMergedPolicies();
    }

    static synchronized GreenlightProvider.PolicyQuery query(ResourceLocation featureId) {
        refreshSources();
        return new GreenlightProvider.PolicyQuery(generation, mergedPolicies.get(featureId));
    }

    static synchronized Set<ResourceLocation> getGrantedFeatures() {
        refreshSources();

        Set<ResourceLocation> granted = new HashSet<>();
        for (Map.Entry<ResourceLocation, FeaturePolicy> entry : mergedPolicies.entrySet()) {
            if (entry.getValue().enabled()) {
                granted.add(entry.getKey());
            }
        }
        return Set.copyOf(granted);
    }

    private static void refreshSources() {
        boolean changed = false;

        for (SourceState state : SOURCES) {
            Object fingerprint;
            try {
                fingerprint = state.source.fingerprint();
            } catch (RuntimeException e) {
                fingerprint = ERROR_FINGERPRINT;
            }

            if (Objects.equals(fingerprint, state.fingerprint)) {
                continue;
            }

            state.fingerprint = fingerprint;
            changed = true;

            if (fingerprint == ERROR_FINGERPRINT) {
                LOGGER.warn("Feature policy source {} failed to fingerprint; ignoring it", state.source.id());
                state.policies = Map.of();
                continue;
            }

            try {
                state.policies = Map.copyOf(state.source.load());
            } catch (RuntimeException e) {
                LOGGER.warn("Feature policy source {} failed to load; ignoring it", state.source.id(), e);
                state.policies = Map.of();
            }
        }

        if (changed) {
            rebuildMergedPolicies();
        }
    }

    private static void rebuildMergedPolicies() {
        Map<ResourceLocation, FeaturePolicy> merged = new HashMap<>();
        for (SourceState state : SOURCES) {
            for (Map.Entry<ResourceLocation, FeaturePolicy> entry : state.policies.entrySet()) {
                merged.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        mergedPolicies = Map.copyOf(merged);
        generation++;
    }
}
