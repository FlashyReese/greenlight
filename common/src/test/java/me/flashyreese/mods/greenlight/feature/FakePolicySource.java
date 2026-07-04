package me.flashyreese.mods.greenlight.feature;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory {@link FeaturePolicySource} for tests. Mutating its policies bumps a fresh
 * fingerprint so the engine reloads on the next query, mimicking a live policy change.
 */
final class FakePolicySource implements FeaturePolicySource {
    private final ResourceLocation id;
    private final int priority;
    private final Map<ResourceLocation, FeaturePolicy> policies = new HashMap<>();

    private Object fingerprint = new Object();
    boolean throwOnLoad = false;

    FakePolicySource(String id, int priority) {
        this.id = ResourceLocation.tryParse("greenlight-test:" + id);
        this.priority = priority;
    }

    FakePolicySource put(ResourceLocation feature, FeaturePolicy policy) {
        this.policies.put(feature, policy);
        this.fingerprint = new Object();
        return this;
    }

    void clear() {
        this.policies.clear();
        this.fingerprint = new Object();
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public int priority() {
        return this.priority;
    }

    @Override
    public Object fingerprint() {
        return this.fingerprint;
    }

    @Override
    public Map<ResourceLocation, FeaturePolicy> load() {
        if (this.throwOnLoad) {
            throw new RuntimeException("simulated source failure");
        }
        return new HashMap<>(this.policies);
    }
}
