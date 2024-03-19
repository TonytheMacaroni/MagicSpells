package com.nisovin.magicspells.debug;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import org.bukkit.configuration.ConfigurationSection;

public class DebugConfig {

	private final Object2ObjectMap<DebugCategory, DebugLevel> levels;
	private final String indentCharacter;
	private final int indent;

	public DebugConfig(ConfigurationSection config) {
		String indentCharacter = config.getString("debug-test.indent-character", " ");
		if (indentCharacter.length() != 1) {
			MagicDebug.warn("Invalid value '%s' for 'indent-character' in 'debug-test' %s. Defaulting to ' '.", indentCharacter, MagicDebug.resolvePath());
			indentCharacter = " ";
		}
		this.indentCharacter = " ";

		int indent = config.getInt("debug-test.indent-size", 4);
		if (indent <= 0) {
			MagicDebug.warn("Invalid value '%s' for 'indent-size' in 'debug-test' %s. Defaulting to '4'.", indentCharacter, MagicDebug.resolvePath());
			indent = 4;
		}
		this.indent = indent;

		if (config.isBoolean("debug-test")) {
			levels = new Object2ObjectArrayMap<>();
			levels.defaultReturnValue(config.getBoolean("debug-test") ? DebugLevel.ALL : DebugLevel.NONE);
			return;
		}

		if (config.isString("debug-test")) {
			levels = new Object2ObjectArrayMap<>();

			String levelString = config.getString("debug-test", "");
			DebugLevel level;
			try {
				level = DebugLevel.valueOf(levelString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid debug level of '%s' for 'debug-test' %s. Defaulting to 'WARNING'.", levelString, MagicDebug.resolvePath());
				level = DebugLevel.WARNING;
			}
			levels.defaultReturnValue(level);

			return;
		}

		ConfigurationSection section = config.getConfigurationSection("debug-test");
		if (section == null) {
			levels = new Object2ObjectArrayMap<>();
			levels.defaultReturnValue(DebugLevel.WARNING);
			return;
		}

		String defaultLevelString = config.getString("debug-test.level", "WARNING");
		DebugLevel defaultLevel;
		try {
			defaultLevel = DebugLevel.valueOf(defaultLevelString.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			MagicDebug.warn("Invalid debug level of '%s' for 'level' in 'debug-test' %s. Defaulting to 'WARNING'.", defaultLevelString, MagicDebug.resolvePath());
			defaultLevel = DebugLevel.WARNING;
		}

		levels = new Object2ObjectArrayMap<>();
		levels.defaultReturnValue(defaultLevel);

		ConfigurationSection overrides = section.getConfigurationSection("overrides");
		if (overrides == null) return;

		for (String override : overrides.getKeys(false)) {
			DebugCategory category;
			try {
				category = DebugCategory.valueOf(override.toUpperCase().replace("-", "_"));
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid debug category '%s' for 'debug.overrides' %s.", override, MagicDebug.resolvePath());
				continue;
			}

			String levelString = overrides.getString(override, "");
			DebugLevel level;
			try {
				level = DebugLevel.valueOf(levelString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid debug level of '%s' for category '%s' for 'debug.overrides' %s.", levelString, override, MagicDebug.resolvePath());
				continue;
			}

			levels.put(category, level);
		}
	}

	public static DebugConfig fromConfig(ConfigurationSection config) {
		return config != null && config.isSet("debug-test") ? new DebugConfig(config) : null;
	}

	public int getIndent() {
		return indent;
	}

	public String getIndentCharacter() {
		return indentCharacter;
	}

	public boolean suppressDebug(DebugCategory category, DebugLevel level) {
		return category != DebugCategory.DEFAULT && levels.get(category).ordinal() < level.ordinal();
	}

	public boolean isEnabled(DebugCategory category) {
		return category == DebugCategory.DEFAULT || levels.get(category).ordinal() >= DebugLevel.INFO.ordinal();
	}

	public boolean isEnhanced(DebugCategory category) {
		return levels.get(category) == DebugLevel.ALL;
	}

}
