package me.flashyreese.mods.greenlight.feature;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in policy source for required server resource packs.
 *
 * <p>It reads {@code assets/<feature_namespace>/client_features/v1/<feature_path>.json} from
 * packs that Minecraft marked as downloaded from the current server. Local, built-in, world,
 * and optional packs never grant a policy, even when they contain the same JSON.
 *
 * <p>The fingerprint is the set of trusted server pack IDs. When the server pack goes away,
 * the fingerprint changes and the grant disappears.
 */
final class ResourcePackPolicySource implements FeaturePolicySource {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("greenlight", "server_resource_pack");
    private static final String RESOURCE_ROOT = "client_features/v1";
    private static final String JSON_SUFFIX = ".json";
    private static final int PROTOCOL_VERSION = 1;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return RESOURCE_PACK_PRIORITY;
    }

    @Override
    public Object fingerprint() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null ? collectTrustedServerPackIds(minecraft.getResourcePackRepository()) : List.of();
    }

    @Override
    public Map<ResourceLocation, FeaturePolicy> load() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return Map.of();
        }

        List<String> trustedPackIds = collectTrustedServerPackIds(minecraft.getResourcePackRepository());
        Map<ResourceLocation, FeaturePolicy> policies = scanPolicies(minecraft.getResourceManager(), trustedPackIds);
        if (!policies.isEmpty()) {
            ClientFeaturePolicyManager.LOGGER.info("Loaded server client-feature policies: {}", policies.keySet());
        }
        return policies;
    }

    private static List<String> collectTrustedServerPackIds(PackRepository repository) {
        List<String> trustedPackIds = new ArrayList<>();
        for (Pack pack : repository.getSelectedPacks()) {
            if (pack.getPackSource() == PackSource.SERVER
                    && pack.isRequired()
                    && pack.isFixedPosition()
                    && pack.getDefaultPosition() == Pack.Position.TOP) {
                trustedPackIds.add(pack.getId());
            }
        }
        return trustedPackIds;
    }

    private static Map<ResourceLocation, FeaturePolicy> scanPolicies(ResourceManager resourceManager, List<String> trustedPackIds) {
        if (trustedPackIds.isEmpty()) {
            return Map.of();
        }

        Map<ResourceLocation, FeaturePolicy> policies = new HashMap<>();
        Map<ResourceLocation, List<Resource>> resourceStacks = resourceManager.listResourceStacks(
                RESOURCE_ROOT,
                id -> id.getPath().endsWith(JSON_SUFFIX)
        );

        for (Map.Entry<ResourceLocation, List<Resource>> entry : resourceStacks.entrySet()) {
            ResourceLocation featureId = parseFeatureId(entry.getKey());
            if (featureId == null) {
                continue;
            }

            // The stack is ordered bottom pack first, so walk it in reverse: the topmost
            // trusted server declaration alone decides this feature. If it is unreadable,
            // the feature stays denied rather than falling back to a lower pack.
            List<Resource> stack = entry.getValue();
            for (int i = stack.size() - 1; i >= 0; i--) {
                Resource resource = stack.get(i);
                if (!trustedPackIds.contains(resource.sourcePackId())) {
                    continue;
                }

                FeaturePolicy policy = readPolicy(entry.getKey(), featureId, resource);
                if (policy != null) {
                    policies.put(featureId, policy);
                }
                break;
            }
        }

        return policies;
    }

    private static ResourceLocation parseFeatureId(ResourceLocation resourceId) {
        String path = resourceId.getPath();
        String prefix = RESOURCE_ROOT + "/";
        if (!path.startsWith(prefix) || !path.endsWith(JSON_SUFFIX)) {
            return null;
        }

        String featurePath = path.substring(prefix.length(), path.length() - JSON_SUFFIX.length());
        if (featurePath.isEmpty()) {
            return null;
        }

        return ResourceLocation.tryBuild(resourceId.getNamespace(), featurePath);
    }

    private static FeaturePolicy readPolicy(ResourceLocation resourceId, ResourceLocation featureId, Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            return parsePolicy(GsonHelper.parse(reader), featureId, resource.sourcePackId());
        } catch (IOException | RuntimeException e) {
            ClientFeaturePolicyManager.LOGGER.warn("Ignoring malformed client-feature policy {} from pack '{}'",
                    resourceId, resource.sourcePackId(), e);
            return null;
        }
    }

    /**
     * Validates a policy envelope and returns the parsed policy.
     *
     * <p>Malformed envelopes return {@code null} so the feature fails closed. Package-private
     * for unit tests. {@code source} is only used in warning messages.
     */
    static FeaturePolicy parsePolicy(JsonObject json, ResourceLocation featureId, String source) {
        int protocolVersion = GsonHelper.getAsInt(json, "protocol_version", -1);
        if (protocolVersion != PROTOCOL_VERSION) {
            ClientFeaturePolicyManager.LOGGER.warn("Ignoring client-feature policy {} from '{}': unsupported protocol_version {}",
                    featureId, source, protocolVersion);
            return null;
        }

        String declaredFeature = GsonHelper.getAsString(json, "feature", "");
        if (!featureId.toString().equals(declaredFeature)) {
            ClientFeaturePolicyManager.LOGGER.warn("Ignoring client-feature policy from '{}': declared feature '{}' does not match resource path '{}'",
                    source, declaredFeature, featureId);
            return null;
        }

        int settingsVersion = GsonHelper.getAsInt(json, "settings_version", 1);
        if (settingsVersion < 1) {
            ClientFeaturePolicyManager.LOGGER.warn("Ignoring client-feature policy {} from '{}': invalid settings_version {}",
                    featureId, source, settingsVersion);
            return null;
        }

        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", false);
        JsonObject settings = GsonHelper.getAsJsonObject(json, "settings", new JsonObject());
        return new FeaturePolicy(enabled, settingsVersion, settings);
    }
}
