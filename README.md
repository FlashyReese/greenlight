# Greenlight

Greenlight is a client-side library mod for Minecraft. It gives servers a shared way to
allow gameplay-affecting client features, and gives compatible client mods one API for
checking that permission.

Greenlight does not add a menu, a feature toggle, or any visible gameplay change by itself.
Install it when another mod says it supports or requires Greenlight.

## How It Works

A server can authorize a feature by shipping a small policy file in its required resource
pack. Greenlight only trusts policy from the current server's required pack. Local resource
packs with the same files do not grant anything.

When the player leaves the server, the grants go away and compatible mods should return to
their default-deny behavior.

Greenlight is not anti-cheat. It helps honest clients, modpacks, and server communities use
one opt-in format instead of every mod inventing its own handshake.

## Documentation

The detailed documentation lives in the GitHub Wiki:

- [Home](https://github.com/FlashyReese/greenlight/wiki)
- [Server Owner Guide](https://github.com/FlashyReese/greenlight/wiki/Server-Owner-Guide)
- [Mod Developer Guide](https://github.com/FlashyReese/greenlight/wiki/Mod-Developer-Guide)
- [Policy Files](https://github.com/FlashyReese/greenlight/wiki/Policy-Files)

## API Package

```java
me.flashyreese.mods.greenlight.feature
```

Minimal feature registration:

```java
public static final ClientFeature<CaveTintPolicy> CAVE_TINT = Greenlight
        .feature(ResourceLocation.tryParse("examplemod:cave_tint"))
        .decoder(1, CaveTintPolicy::fromJson)
        .register();
```

Query when the feature would apply:

```java
Optional<CaveTintPolicy> policy = CAVE_TINT.policy();
if (policy.isEmpty()) {
    return;
}
```

## License

LGPL-3.0-or-later. See [LICENSE](LICENSE).
