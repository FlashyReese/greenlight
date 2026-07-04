package me.flashyreese.mods.greenlight.testmod;

import me.flashyreese.mods.greenlight.feature.ClientFeature;
import me.flashyreese.mods.greenlight.feature.Greenlight;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreenlightTestmod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("greenlight-testmod");

    public record SamplePolicy(int max) {
    }

    public static final ClientFeature<SamplePolicy> SAMPLE = Greenlight
            .feature(ResourceLocation.tryParse("greenlight-test:sample"))
            .decoder(1, json -> new SamplePolicy(GsonHelper.getAsInt(json, "max", 0)))
            .register();

    // Only ever declared by this mod's own (client-side) resources, never by a server pack.
    // Useful for manual checks that client-side packs cannot grant a feature.
    public static final ClientFeature<SamplePolicy> LOCAL_ONLY = Greenlight
            .feature(ResourceLocation.tryParse("greenlight-test:local_only"))
            .decoder(1, json -> new SamplePolicy(GsonHelper.getAsInt(json, "max", 0)))
            .register();

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.info("joined; Greenlight granted features = {}", Greenlight.getGrantedFeatures());
            SAMPLE.policy().ifPresentOrElse(
                    policy -> LOGGER.info("sample feature GRANTED, max = {}", policy.max()),
                    () -> LOGGER.info("sample feature NOT granted (default-deny)"));
        });
        LOGGER.info("initialized; sample feature 'greenlight-test:sample' registered");
    }
}
