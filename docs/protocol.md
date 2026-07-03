# Greenlight Policy Files

This page is for server owners building resource packs and mod developers documenting their
own Greenlight features.

## Terms

- **Feature ID**: the namespaced ID of a mod feature, such as `examplemod:cave_tint`.
- **Policy file**: the JSON file a server ships for one feature.
- **Grant**: an enabled, trusted policy that the client mod can understand.
- **Settings**: the feature-specific JSON body. Greenlight stores it, but the owning mod
  decides what the keys mean.

## How Greenlight Trusts a Server Policy

1. The server sends a required resource pack using `resource-pack` and
   `require-resource-pack=true` in `server.properties`, or the equivalent server API.
2. Minecraft marks downloaded server packs with `PackSource.SERVER`. Greenlight only trusts
   selected packs that are server-sourced, required, fixed in position, and placed at the
   top of the pack stack.
3. Local packs, world packs, built-in packs, and normal optional packs do not grant
   features, even if they contain byte-for-byte identical JSON.
4. When the server pack is removed on disconnect, login failure, or pack removal, the grant
   disappears and the client returns to default deny.

This is a client-side permission signal, not enforcement. Greenlight helps compatible mods
respect a server's policy. It cannot stop a modified client from ignoring that policy.

## Policy File Location

Each feature gets one JSON file under the namespace of the mod that owns the feature:

```text
assets/<feature_namespace>/client_features/v1/<feature_path>.json
```

For feature `examplemod:cave_tint`:

```text
assets/examplemod/client_features/v1/cave_tint.json
```

If a client does not have the mod that owns a feature, no feature code will act on that
policy. A server pack can safely include policies for many optional client mods. Greenlight
diagnostics may still list the granted feature ID when the policy file itself is valid.

## Envelope Format

```json
{
  "protocol_version": 1,
  "feature": "examplemod:cave_tint",
  "enabled": true,
  "settings_version": 1,
  "settings": {
    "...": "feature-defined body"
  }
}
```

Fields:

- `protocol_version` is required and must be `1`. It versions the Greenlight envelope, not
  the feature's own settings.
- `feature` is required and must match the feature ID from the file path.
- `enabled` defaults to `false`. Use `true` to grant the feature. Use `false` to explicitly
  revoke it from a lower-priority source.
- `settings_version` defaults to `1`. This is owned by the feature's mod. A typed
  `ClientFeature` handle denies the grant if the version does not match the decoder it
  registered.
- `settings` defaults to an empty object. The owning mod decodes this body.

Malformed files fail closed. Greenlight logs the problem and treats the feature as denied.

## Settings Versioning for Mod Developers

`protocol_version` belongs to Greenlight. `settings_version` belongs to your feature.

Register exactly one settings version for each feature handle:

```java
Greenlight.feature(Identifier.fromNamespaceAndPath("examplemod", "cave_tint"))
        .decoder(1, CaveTintPolicy::fromJson)
        .register();
```

Use additive changes when possible. New fields should be optional, and missing values should
mean the most restrictive behavior. Bump `settings_version` only when older clients would
misread the new shape.

One policy file has one `settings_version`, so a server targets one schema generation for a
feature. To support mixed client versions, keep the change additive or register a new
feature ID.

## Multiple Packs and Policy Sources

If several trusted server packs declare the same feature, the topmost trusted declaration in
the pack stack wins. If that topmost declaration is unreadable, Greenlight denies the
feature instead of falling back to a lower pack.

The required resource pack is the built-in policy source at priority `0`. Advanced
integrations can register additional sources, such as a custom network payload from a
server-side mod. For each feature, the highest-priority source that declares a policy wins.
A disabled policy counts as a declaration and can revoke a lower-priority grant.

## Practical Notes

- Do not put secrets in policy JSON. Resource packs are visible to clients.
- Keep feature IDs stable. They are the public contract between your mod docs and server
  packs.
- Document the `settings` keys your feature supports. Server owners should not need to read
  your code to write a policy.
