package me.flashyreese.mods.greenlight.forge;

import net.minecraftforge.fml.common.Mod;

/**
 * Greenlight is a pure library: its NeoForge module ships no Java at all, because NeoForge accepts a
 * mods.toml entry with no matching class. Forge does not, and refuses the jar with "has mods that
 * were not found", so this empty entry point exists solely to satisfy that requirement.
 */
@Mod("greenlight")
public class GreenlightForgeMod {
}
