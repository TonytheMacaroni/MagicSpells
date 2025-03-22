package com.nisovin.magicspells.debug;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.EnumMap;
import java.util.Objects;

import org.bukkit.configuration.ConfigurationSection;

public class DebugConfig {

	private final EnumMap<DebugCategory, DebugLevel> overrides;
	private final DebugLevel defaultLevel;
	private final String indentCharacter;
	private final int indent;

	public DebugConfig(ConfigurationSection config) {
		String indentCharacter = config.getString("debug-test.indent-character", " ");
		if (indentCharacter.length() != 1) {
			MagicDebug.warn("Invalid value '%s' for 'indent-character' in 'debug-test' %s. Defaulting to ' '.", indentCharacter, MagicDebug.resolveFullPath());
			indentCharacter = " ";
		}
		this.indentCharacter = " ";

		int indent = config.getInt("debug-test.indent-size", 4);
		if (indent <= 0) {
			MagicDebug.warn("Invalid value '%s' for 'indent-size' in 'debug-test' %s. Defaulting to '4'.", indentCharacter, MagicDebug.resolveFullPath());
			indent = 4;
		}
		this.indent = indent;

		if (config.isBoolean("debug-test")) {
			overrides = new EnumMap<>(DebugCategory.class);
			defaultLevel = config.getBoolean("debug-test") ? DebugLevel.ALL : DebugLevel.NONE;
			return;
		}

		if (config.isString("debug-test")) {
			overrides = new EnumMap<>(DebugCategory.class);

			String levelString = config.getString("debug-test", "");
			DebugLevel defaultLevel;
			try {
				defaultLevel = DebugLevel.valueOf(levelString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid debug level of '%s' for 'debug-test' %s. Defaulting to 'WARNING'.", levelString, MagicDebug.resolveFullPath());
				defaultLevel = DebugLevel.WARNING;
			}
			this.defaultLevel = defaultLevel;

			return;
		}

		ConfigurationSection section = config.getConfigurationSection("debug-test");
		if (section == null) {
			overrides = new EnumMap<>(DebugCategory.class);
			overrides.put(DebugCategory.LOAD, DebugLevel.INFO);
			overrides.put(DebugCategory.UNLOAD, DebugLevel.INFO);

			defaultLevel = DebugLevel.WARNING;

			return;
		}

		String defaultLevelString = config.getString("debug-test.level", "WARNING");
		DebugLevel defaultLevel;
		try {
			defaultLevel = DebugLevel.valueOf(defaultLevelString.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			MagicDebug.warn("Invalid debug level of '%s' for 'level' in 'debug-test' %s. Defaulting to 'WARNING'.", defaultLevelString, MagicDebug.resolveFullPath());
			defaultLevel = DebugLevel.WARNING;
		}

		overrides = new EnumMap<>(DebugCategory.class);
		this.defaultLevel = defaultLevel;

		ConfigurationSection overrides = section.getConfigurationSection("overrides");
		if (overrides == null) return;

		for (String override : overrides.getKeys(false)) {
			DebugCategory category;
			try {
				category = DebugCategory.valueOf(override.toUpperCase().replace("-", "_"));
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid debug category '%s' %s.", override, MagicDebug.resolveFullPath("debug.overrides"));
				continue;
			}

			String levelString = overrides.getString(override, "");
			DebugLevel level;
			try {
				level = DebugLevel.valueOf(levelString.toUpperCase());
			} catch (IllegalArgumentException e) {
				MagicDebug.warn("Invalid debug level of '%s' for category '%s' %s.", levelString, override, MagicDebug.resolveFullPath("debug.overrides"));
				continue;
			}

			this.overrides.put(category, level);
		}
	}

	public DebugConfig(DebugLevel defaultLevel, EnumMap<DebugCategory, DebugLevel> overrides, int indent, String indentCharacter) {
		this.defaultLevel = defaultLevel;
		this.overrides = overrides;
		this.indent = indent;
		this.indentCharacter = indentCharacter;
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

	public DebugLevel getDefaultLevel() {
		return defaultLevel;
	}

	public EnumMap<DebugCategory, DebugLevel> getOverrides() {
		return overrides;
	}

	public boolean suppressDebug(DebugCategory category, DebugLevel level) {
		return overrides.getOrDefault(category, defaultLevel).ordinal() < level.ordinal();
	}

	public boolean isEnabled(DebugCategory category, DebugLevel level) {
		return overrides.getOrDefault(category, defaultLevel).ordinal() >= level.ordinal();
	}

	public boolean isEnhanced(DebugCategory category) {
		return overrides.getOrDefault(category, defaultLevel) == DebugLevel.ALL;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DebugConfig config = (DebugConfig) o;
		return indent == config.indent && Objects.equals(overrides, config.overrides) && defaultLevel == config.defaultLevel && Objects.equals(indentCharacter, config.indentCharacter);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(overrides);
		result = 31 * result + Objects.hashCode(defaultLevel);
		result = 31 * result + Objects.hashCode(indentCharacter);
		result = 31 * result + indent;
		return result;
	}

}
