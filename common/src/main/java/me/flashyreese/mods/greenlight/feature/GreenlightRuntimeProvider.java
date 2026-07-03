package me.flashyreese.mods.greenlight.feature;

import me.flashyreese.mods.greenlight.feature.spi.GreenlightProvider;
import net.minecraft.resources.Identifier;

import java.util.Set;

public final class GreenlightRuntimeProvider implements GreenlightProvider {
    public GreenlightRuntimeProvider() {
    }

    @Override
    public void registerFeature(ClientFeature<?> feature) {
        ClientFeaturePolicyManager.registerFeature(feature);
    }

    @Override
    public void registerSource(FeaturePolicySource source) {
        ClientFeaturePolicyManager.registerSource(source);
    }

    @Override
    public PolicyQuery query(Identifier featureId) {
        return ClientFeaturePolicyManager.query(featureId);
    }

    @Override
    public Set<Identifier> getGrantedFeatures() {
        return ClientFeaturePolicyManager.getGrantedFeatures();
    }
}
