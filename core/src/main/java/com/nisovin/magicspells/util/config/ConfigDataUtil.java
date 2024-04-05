package com.nisovin.magicspells.util.config;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import org.bukkit.entity.EntityType;
import org.bukkit.block.data.BlockData;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.debug.DebugCategory;
import com.nisovin.magicspells.handlers.PotionEffectHandler;

public class ConfigDataUtil {

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving integer option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				int value = config.getInt(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Integer> function = FunctionData.build(functionString, Double::intValue, true);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for integer option %s.", functionString, MagicDebug.resolvePath(path));
					return data -> null;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for integer option %s.", config.getString(path), MagicDebug.resolvePath(path));
			else
				MagicDebug.info("No value found.");

			return data -> null;
		}
	}

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path, int def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving integer option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				int value = config.getInt(path, def);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Integer> function = FunctionData.build(functionString, Double::intValue, def, true);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for integer option %s. Defaulting to '%d'.", functionString, MagicDebug.resolvePath(path), def);
					return data -> def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for integer option %s. Defaulting to '%d'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to '%d'.", def);

			return data -> def;
		}
	}

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path, @NotNull ConfigData<Integer> def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving integer option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				int value = config.getInt(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Integer> function = FunctionData.build(config.getString(path), Double::intValue, def, true);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for integer option %s. Defaulting to expression '%s'.", functionString, MagicDebug.resolvePath(path), def);
					return def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for integer option %s. Defaulting to expression '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to expression '%s'.", def);

			return def;
		}
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving long option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path)) {
				long value = config.getLong(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Long> function = FunctionData.build(functionString, Double::longValue);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for long option %s.", functionString, MagicDebug.resolvePath(path));
					return data -> null;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for long option %s.", config.getString(path), MagicDebug.resolvePath(path));
			else
				MagicDebug.info("No value found.");

			return data -> null;
		}
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path, long def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving long option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path)) {
				long value = config.getLong(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Long> function = FunctionData.build(functionString, Double::longValue, def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for long option %s. Defaulting to '%d'.", functionString, MagicDebug.resolvePath(path), def);
					return data -> def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for long option %s. Defaulting to '%d'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to '%d'.", def);

			return data -> def;
		}
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path, @NotNull ConfigData<Long> def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving long option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path)) {
				long value = config.getLong(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Long> function = FunctionData.build(functionString, Double::longValue, def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for long option %s. Defaulting to expression '%s'.", functionString, MagicDebug.resolvePath(path), def);
					return def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for long option %s. Defaulting to expression '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to expression '%s'.", def);

			return def;
		}
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving short option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				short value = (short) config.getInt(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Short> function = FunctionData.build(functionString, Double::shortValue);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for short option %s.", functionString, MagicDebug.resolvePath(path));
					return data -> null;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for short option %s.", config.getString(path), MagicDebug.resolvePath(path));
			else
				MagicDebug.info("No value found.");

			return data -> null;
		}
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path, short def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving short option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				short value = (short) config.getInt(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Short> function = FunctionData.build(functionString, Double::shortValue, def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for short option %s. Defaulting to '%d'.", functionString, MagicDebug.resolvePath(path), def);
					return data -> def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for short option %s. Defaulting to '%d'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to '%d'.", def);

			return data -> def;
		}
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path, @NotNull ConfigData<Short> def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving short option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				short value = (short) config.getInt(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Short> function = FunctionData.build(functionString, Double::shortValue, def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for short option %s. Defaulting to expression '%s'.", functionString, MagicDebug.resolvePath(path), def);
					return def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for short option %s. Defaulting to expression '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to expression '%s'.", def);

			return def;
		}
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving byte option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				byte value = (byte) config.getInt(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Byte> function = FunctionData.build(functionString, Double::byteValue);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for byte option %s.", functionString, MagicDebug.resolvePath(path));
					return data -> null;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for byte option %s.", config.getString(path), MagicDebug.resolvePath(path));
			else
				MagicDebug.info("No value found.");

			return data -> null;
		}
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path, byte def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving byte option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				byte value = (byte) config.getInt(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Byte> function = FunctionData.build(functionString, Double::byteValue, def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for byte option %s. Defaulting to '%d'.", functionString, MagicDebug.resolvePath(path), def);
					return data -> def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for byte option %s. Defaulting to '%d'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to '%d'.", def);

			return data -> def;
		}
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path, @NotNull ConfigData<Byte> def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving byte option '%s'.", getShortPath(path))) {
			if (config.isInt(path)) {
				byte value = (byte) config.getInt(path);
				MagicDebug.info("Resolved value '%d'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Byte> function = FunctionData.build(functionString, Double::byteValue, def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for byte option %s. Defaulting to expression '%s'.", functionString, MagicDebug.resolvePath(path), def);
					return def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for byte option %s. Defaulting to expression '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to expression '%s'.", def);

			return def;
		}
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving double option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
				double value = config.getDouble(path);
				MagicDebug.info("Resolved value '%s'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Double> function = FunctionData.build(functionString, Function.identity());
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for double option %s.", functionString, MagicDebug.resolvePath(path));
					return data -> null;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for double option %s.", config.getString(path), MagicDebug.resolvePath(path));
			else
				MagicDebug.info("No value found.");

			return data -> null;
		}
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path, double def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving double option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
				double value = config.getDouble(path);
				MagicDebug.info("Resolved value '%s'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Double> function = FunctionData.build(functionString, Function.identity(), def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for double option %s. Defaulting to '%s'.", functionString, MagicDebug.resolvePath(path), def);
					return data -> def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for double option %s. Defaulting to '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to '%s'.", def);

			return data -> def;
		}
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path, @NotNull ConfigData<Double> def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving double option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
				double value = config.getDouble(path);
				MagicDebug.info("Resolved value '%s'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Double> function = FunctionData.build(functionString, Function.identity(), def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for double option %s. Defaulting to expression '%s'.", functionString, MagicDebug.resolvePath(path), def);
					return def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for double option %s. Defaulting to expression '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to expression '%s'.", def);

			return def;
		}
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving float option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
				float value = (float) config.getDouble(path);
				MagicDebug.info("Resolved value '%s'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Float> function = FunctionData.build(functionString, Double::floatValue);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for float option %s.", functionString, MagicDebug.resolvePath(path));
					return data -> null;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for float option %s.", config.getString(path), MagicDebug.resolvePath(path));
			else
				MagicDebug.info("No value found.");

			return data -> null;
		}
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path, float def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving float option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
				float value = (float) config.getDouble(path);
				MagicDebug.info("Resolved value '%s'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Float> function = FunctionData.build(functionString, Double::floatValue, def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for float option %s. Defaulting to '%s'.", functionString, MagicDebug.resolvePath(path), def);
					return data -> def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for float option %s. Defaulting to '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to '%s'.", def);

			return data -> def;
		}
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path, @NotNull ConfigData<Float> def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving float option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
				float value = (float) config.getDouble(path);
				MagicDebug.info("Resolved value '%s'.", value);
				return data -> value;
			}

			if (config.isString(path)) {
				String functionString = config.getString(path);

				FunctionData<Float> function = FunctionData.build(functionString, Double::floatValue, def);
				if (function == null) {
					MagicDebug.warn("Invalid expression '%s' for float option %s. Defaulting to expression '%s'.", functionString, MagicDebug.resolvePath(path), def);
					return def;
				}

				MagicDebug.info("Resolved expression '%s'.", functionString);
				return function;
			}

			if (config.isSet(path))
				MagicDebug.warn("Invalid value '%s' found for float option %s. Defaulting to expression '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);
			else
				MagicDebug.info("No value found. Defaulting to expression '%s'.", def);

			return def;
		}
	}

	@NotNull
	public static ConfigData<String> getString(@NotNull ConfigurationSection config, @NotNull String path, @Nullable String def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving string option '%s'.", getShortPath(path))) {
			String value;

			if (config.isString(path)) value = config.getString(path);
			else if (config.isSet(path)) {
				if (def == null) MagicDebug.warn("Invalid value '%s' found for string option %s.", config.getString(path), MagicDebug.resolvePath(path));
				else MagicDebug.warn("Invalid value '%s' found for string option %s. Defaulting to '%s'.", config.getString(path), MagicDebug.resolvePath(path), def);

				value = def;
			} else {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to '%s'.", def);

				value = def;
			}

			if (value == null) return data -> null;

			StringData stringData = new StringData(value);
			if (stringData.isConstant()) {
				MagicDebug.info("Resolved value '%s'.", value);
				return data -> value;
			}

			MagicDebug.info("Resolved expression '%s'.", value);

			List<StringData.PlaceholderData> values = stringData.getValues();
			List<String> fragments = stringData.getFragments();
			if (values.size() == 1 && fragments.size() == 2 && fragments.get(0).isEmpty() && fragments.get(1).isEmpty())
				return values.get(0);

			return stringData;
		}
	}

	@NotNull
	public static ConfigData<String> getString(@Nullable String value) {
		if (value == null) return data -> null;

		StringData stringData = new StringData(value);
		if (stringData.isConstant()) return data -> value;

		List<StringData.PlaceholderData> values = stringData.getValues();
		List<String> fragments = stringData.getFragments();
		if (values.size() == 1 && fragments.size() == 2 && fragments.get(0).isEmpty() && fragments.get(1).isEmpty())
			return values.get(0);

		return stringData;
	}

	@NotNull
	public static ConfigData<Component> getComponent(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Component def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving rich text option '%s'.", getShortPath(path))) {
			ConfigData<String> supplier = getString(config, path, null);
			if (supplier.isConstant()) {
				String value = supplier.get();
				if (value == null) {
					if (def == null) MagicDebug.info("No value found.");
					else MagicDebug.info("No value found. Defaulting to '%s'.", def);

					return data -> def;
				}

				Component component = Util.getMiniMessage(value);
				MagicDebug.info("Resolved value '%s'.", component);
				return data -> component;
			}

			MagicDebug.info("Resolved expression '%s'.", supplier);

			return (VariableConfigData<Component>) data -> {
				String value = supplier.get(data);
				if (value == null) return def;

				return Util.getMiniMessage(value);
			};
		}
	}

	@NotNull
	public static ConfigData<Component> getComponent(@NotNull String value) {
		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) {
			String val = supplier.get();
			if (val == null) return data -> null;

			Component component = Util.getMiniMessage(value);
			return data -> component;
		}

		return (VariableConfigData<Component>) data -> {
			String value1 = supplier.get(data);
			if (value1 == null) return null;

			return Util.getMiniMessage(value1);
		};
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return data -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			if (supplier.isConstant()) {
				Boolean val = parseBoolean(supplier.get());
				return data -> val;
			}

			return (VariableConfigData<Boolean>) data -> parseBoolean(supplier.get(data));
		}

		return data -> null;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path, boolean def) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return data -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			if (supplier.isConstant()) {
				boolean val = parseBoolean(supplier.get(), def);
				return data -> val;
			}

			return (VariableConfigData<Boolean>) data -> parseBoolean(supplier.get(data), def);
		}

		return data -> def;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Boolean> def) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return data -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			if (supplier.isConstant()) {
				Boolean val = parseBoolean(supplier.get());
				if (val == null) return def;

				return data -> val;
			}

			return (VariableConfigData<Boolean>) data -> {
				Boolean val = parseBoolean(supplier.get(data));
				return val == null ? def.get(data) : val;
			};
		}

		return def;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull String value, boolean def) {
		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) {
			boolean val = parseBoolean(value, def);
			return data -> val;
		}

		return (VariableConfigData<Boolean>) data -> parseBoolean(supplier.get(data), def);
	}

	@Nullable
	private static Boolean parseBoolean(@Nullable String value) {
		if (value == null) return null;

		return switch (value.toLowerCase()) {
			case "true" -> true;
			case "false" -> false;
			default -> null;
		};
	}

	private static boolean parseBoolean(@Nullable String value, boolean def) {
		if (value == null) return def;

		return switch (value.toLowerCase()) {
			case "true" -> true;
			case "false" -> false;
			default -> def;
		};
	}

	@NotNull
	public static <T extends Enum<T>> ConfigData<T> getEnum(@NotNull ConfigurationSection config,
															@NotNull String path,
															@NotNull Class<T> type,
															@Nullable T def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving enum option '%s'.", getShortPath(path))) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to '%s'.", def);

				return data -> def;
			}

			try {
				T val = Enum.valueOf(type, value.toUpperCase());
				MagicDebug.info("Resolved value '%s'.", val);

				return data -> val;
			} catch (IllegalArgumentException e) {
				ConfigData<String> supplier = getString(value);
				if (supplier.isConstant()) {
					if (def != null)
						MagicDebug.warn("Invalid value '%s' found for enum option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
					else
						MagicDebug.warn("Invalid value '%s' found for enum option %s.", value, MagicDebug.resolvePath(path));

					return data -> def;
				}

				MagicDebug.info("Resolved expression '%s'.", supplier);

				return (VariableConfigData<T>) data -> {
					String val = supplier.get(data);
					if (val == null) return def;

					try {
						return Enum.valueOf(type, val.toUpperCase());
					} catch (IllegalArgumentException ex) {
						return def;
					}
				};
			}
		}
	}

	public static ConfigData<Material> getMaterial(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Material def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving material option '%s'.", getShortPath(path))) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to '%s'.", def);

				return data -> def;
			}

			Material val = Util.getMaterial(value);
			if (val != null) {
				MagicDebug.info("Resolved value '%s'.", val);
				return data -> val;
			}

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				if (def != null)
					MagicDebug.warn("Invalid value '%s' found for material option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
				else
					MagicDebug.warn("Invalid value '%s' found for material option %s.", value, MagicDebug.resolvePath(path));

				return data -> def;
			}

			MagicDebug.info("Resolved expression '%s'.", supplier);

			return (VariableConfigData<Material>) data -> {
				String materialString = supplier.get(data);
				if (materialString == null) return def;

				Material material = Util.getMaterial(materialString);
				return material == null ? def : material;
			};
		}
	}

	@NotNull
	public static ConfigData<PotionEffectType> getPotionEffectType(@NotNull ConfigurationSection config, @NotNull String path, @Nullable PotionEffectType def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving potion effect type option '%s'.", getShortPath(path))) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to '%s'.", def);

				return data -> def;
			}

			PotionEffectType val = PotionEffectHandler.getPotionEffectType(value);
			if (val != null) {
				MagicDebug.info("Resolved value '%s'.", val);
				return data -> val;
			}

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				if (def != null)
					MagicDebug.warn("Invalid value '%s' found for potion effect type option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
				else
					MagicDebug.warn("Invalid value '%s' found for potion effect type option %s.", value, MagicDebug.resolvePath(path));

				return data -> def;
			}

			MagicDebug.info("Resolved expression '%s'.", supplier);

			return (VariableConfigData<PotionEffectType>) data -> {
				String typeString = supplier.get(data);
				if (typeString == null) return def;

				PotionEffectType type = PotionEffectHandler.getPotionEffectType(typeString);
				return type == null ? def : type;
			};
		}
	}

	@NotNull
	public static ConfigData<Particle> getParticle(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Particle def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving particle option '%s'.", getShortPath(path))) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to '%s'.", def);

				return data -> def;
			}

			Particle val = ParticleUtil.getParticle(value);
			if (val != null) {
				MagicDebug.info("Resolved value '%s'.", val);
				return data -> val;
			}

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				if (def != null)
					MagicDebug.warn("Invalid value '%s' found for particle option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
				else
					MagicDebug.warn("Invalid value '%s' found for particle option %s.", value, MagicDebug.resolvePath(path));

				return data -> def;
			}

			MagicDebug.info("Resolved expression '%s'.", supplier);

			return (VariableConfigData<Particle>) data -> {
				String particleString = supplier.get(data);
				if (particleString == null) return def;

				Particle particle = ParticleUtil.getParticle(particleString);
				return particle == null ? def : particle;
			};
		}
	}

	public static ConfigData<TargetBooleanState> getTargetBooleanState(@NotNull ConfigurationSection config, @NotNull String path, @Nullable TargetBooleanState def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving boolean state option '%s'.", getShortPath(path))) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to '%s'.", def);

				return data -> def;
			}

			TargetBooleanState val = TargetBooleanState.getByName(value);
			if (val != null) {
				MagicDebug.info("Resolved value '%s'.", val);
				return data -> val;
			}

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				if (def != null)
					MagicDebug.warn("Invalid value '%s' found for boolean state option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
				else
					MagicDebug.warn("Invalid value '%s' found for boolean state option %s.", value, MagicDebug.resolvePath(path));

				return data -> def;
			}

			MagicDebug.info("Resolved expression '%s'.", supplier);

			return (VariableConfigData<TargetBooleanState>) data -> {
				String stateString = supplier.get(data);
				if (stateString == null) return def;

				TargetBooleanState state = TargetBooleanState.getByName(stateString);
				return state == null ? def : state;
			};
		}
	}

	public static ConfigData<EntityType> getEntityType(@NotNull ConfigurationSection config, @NotNull String path, @Nullable EntityType def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving entity type option '%s'.", getShortPath(path))) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to '%s'.", def);

				return data -> def;
			}

			EntityType val = MobUtil.getEntityType(value);
			if (val != null) {
				MagicDebug.info("Resolved value '%s'.", val);
				return data -> val;
			}

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				if (def != null)
					MagicDebug.warn("Invalid value '%s' found for entity type option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
				else
					MagicDebug.warn("Invalid value '%s' found for entity type option %s.", value, MagicDebug.resolvePath(path));

				return data -> def;
			}

			MagicDebug.info("Resolved expression '%s'.", supplier);

			return (VariableConfigData<EntityType>) data -> {
				String typeString = supplier.get(data);
				if (typeString == null) return def;

				EntityType type = MobUtil.getEntityType(typeString);
				return type == null ? def : type;
			};
		}
	}

	@NotNull
	public static ConfigData<BlockData> getBlockData(@NotNull ConfigurationSection config, @NotNull String path, @Nullable BlockData def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving block data option '%s'.", getShortPath(path))) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to '%s'.", (Supplier<String>) def::getAsString);

				return data -> def;
			}

			try {
				BlockData val = Bukkit.createBlockData(value.trim().toLowerCase());
				MagicDebug.info("Resolved value '%s'.", (Supplier<String>) val::getAsString);
				return data -> val;
			} catch (IllegalArgumentException e) {
				ConfigData<String> supplier = getString(value);
				if (supplier.isConstant()) {
					if (def != null)
						MagicDebug.warn("Invalid value '%s' found for block data option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def.getAsString());
					else
						MagicDebug.warn("Invalid value '%s' found for block data option %s.", value, MagicDebug.resolvePath(path));

					return data -> def;
				}

				MagicDebug.info("Resolved expression '%s'.", supplier);

				return (VariableConfigData<BlockData>) data -> {
					String val = supplier.get(data);
					if (val == null) return def;

					try {
						return Bukkit.createBlockData(val.trim().toLowerCase());
					} catch (IllegalArgumentException e1) {
						return def;
					}
				};
			}
		}
	}

	public static <T extends Keyed> ConfigData<T> getRegistryEntry(@NotNull ConfigurationSection config, @NotNull String path, @NotNull Registry<T> registry, @Nullable T def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		NamespacedKey key = NamespacedKey.fromString(value);
		if (key != null) {
			T val = registry.get(key);
			if (val != null) return data -> val;
		}

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return new ConfigData<>() {

			@Override
			public T get(@NotNull SpellData data) {
				String val = supplier.get(data);
				if (val == null) return def;

				NamespacedKey key = NamespacedKey.fromString(val);
				if (key == null) return def;

				T entry = registry.get(key);
				return entry == null ? def : entry;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	public static ConfigData<Angle> getAngle(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Angle def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			Angle angle = new Angle(value, false);

			return data -> angle;
		}

		if (config.isString(path)) {
			String string = config.getString(path);

			boolean relative = string.startsWith("~");
			if (relative) {
				string = string.substring(1);

				try {
					float value = string.isEmpty() ? 0 : Float.parseFloat(string);
					Angle angle = new Angle(value, true);

					return data -> angle;
				} catch (NumberFormatException ignored) {
				}
			}

			FunctionData<Float> function = FunctionData.build(string, Double::floatValue);
			if (function == null) return data -> def;

			return data -> new Angle(function.get(data), relative);
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<Vector> getVector(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Vector def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving vector option '%s'.", getShortPath(path))) {
			if (config.isString(path)) {
				String value = config.getString(path);
				if (value == null) {
					if (def == null) {
						MagicDebug.warn("Invalid value found for vector option %s.", MagicDebug.resolvePath(path));
						return data -> null;
					}

					MagicDebug.warn("Invalid value found for vector option %s. Defaulting to '%s'.", MagicDebug.resolvePath(path), def);
					return data -> def.clone();
				}

				String[] vec = value.split(",");
				if (vec.length != 3) {
					if (def == null) {
						MagicDebug.warn("Invalid value '%s' found for vector option %s - too many or too few arguments.", value, MagicDebug.resolvePath(path));
						return data -> null;
					}

					MagicDebug.warn("Invalid value '%s' found for vector option %s - too many or too few arguments. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
					return data -> def.clone();
				}

				try {
					Vector vector = new Vector(Double.parseDouble(vec[0]), Double.parseDouble(vec[1]), Double.parseDouble(vec[2]));
					MagicDebug.info("Resolved value '%s'.", vector);
					return data -> vector.clone();
				} catch (NumberFormatException e) {
					if (def == null) {
						MagicDebug.warn("Invalid value '%s' found for vector option %s.", value, MagicDebug.resolvePath(path));
						return data -> null;
					}

					MagicDebug.warn("Invalid value '%s' found for vector option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
					return data -> def.clone();
				}
			}

			ConfigData<Double> x;
			ConfigData<Double> y;
			ConfigData<Double> z;

			Object obj = config.get(path);
			if (obj instanceof ConfigurationSection section) {
				x = getDouble(section, "x", 0);
				y = getDouble(section, "y", 0);
				z = getDouble(section, "z", 0);
			} else if (obj instanceof List<?> list) {
				if (list.size() != 3) {
					if (def == null) {
						MagicDebug.warn("Invalid value '%s' found for vector option %s - too many or too few arguments.", list, MagicDebug.resolvePath(path));
						return data -> null;
					}

					MagicDebug.warn("Invalid value '%s' found for vector option %s - too many or too few arguments. Defaulting to '%s'.", list, MagicDebug.resolvePath(path), def);
					return data -> def.clone();
				}

				Object xObj = list.get(0);
				Object yObj = list.get(1);
				Object zObj = list.get(2);

				if (xObj instanceof Number number) {
					double val = number.doubleValue();
					x = data -> val;
				} else if (xObj instanceof String string) {
					x = FunctionData.build(string, Function.identity());
				} else x = null;

				if (yObj instanceof Number number) {
					double val = number.doubleValue();
					y = data -> val;
				} else if (yObj instanceof String string) {
					y = FunctionData.build(string, Function.identity());
				} else y = null;

				if (zObj instanceof Number number) {
					double val = number.doubleValue();
					z = data -> val;
				} else if (zObj instanceof String string) {
					z = FunctionData.build(string, Function.identity());
				} else z = null;

				if (x == null || y == null || z == null) {
					if (def == null) {
						if (x == null) {
							MagicDebug.warn("Invalid value '%s' found for x component of vector option %s.", xObj, MagicDebug.resolvePath(path));
							return data -> null;
						}

						if (y == null) {
							MagicDebug.warn("Invalid value '%s' found for y component of vector option %s.", yObj, MagicDebug.resolvePath(path));
							return data -> null;
						}

						MagicDebug.warn("Invalid value '%s' found for z component of vector option %s.", zObj, MagicDebug.resolvePath(path));
						return data -> null;
					}

					if (x == null) {
						MagicDebug.warn("Invalid value '%s' found for x component of vector option %s. Defaulting to '%s'.", xObj, MagicDebug.resolvePath(path), def);
						return data -> def.clone();
					}

					if (y == null) {
						MagicDebug.warn("Invalid value '%s' found for y component of vector option %s. Defaulting to '%s'.", yObj, MagicDebug.resolvePath(path), def);
						return data -> def.clone();
					}

					MagicDebug.warn("Invalid value '%s' found for z component of vector option %s. Defaulting to '%s'.", zObj, MagicDebug.resolvePath(path), def);
					return data -> def.clone();

				}
			} else if (obj != null) {
				if (def == null) {
					MagicDebug.warn("Invalid value '%s' found for vector option %s.", obj, MagicDebug.resolvePath(path));
					return data -> null;
				}

				MagicDebug.warn("Invalid value '%s' found for vector option %s. Defaulting to '%s'.", obj, MagicDebug.resolvePath(path), def);
				return data -> def.clone();
			} else {
				if (def == null) {
					MagicDebug.info("No value found.");
					return data -> null;
				}

				MagicDebug.info("No value found. Defaulting to '%s'.", def);
				return data -> def.clone();
			}

			if (x.isConstant() && y.isConstant() && z.isConstant()) {
				Vector vector = new Vector(x.get(), y.get(), z.get());
				MagicDebug.info("Resolved value '%s'.", vector);
				return data -> vector.clone();
			}

			MagicDebug.info("Resolved expression '{x: '%s', y: '%s', z: '%s'}'.", x, y, z);

			return (VariableConfigData<Vector>) data -> new Vector(x.get(data), y.get(data), z.get(data));
		}
	}

	@NotNull
	public static ConfigData<EulerAngle> getEulerAngle(@NotNull ConfigurationSection config, @NotNull String path, @Nullable EulerAngle def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving euler angle option '%s'.", getShortPath(path))) {
			if (config.isString(path)) {
				String value = config.getString(path);
				if (value == null) {
					if (def == null) {
						MagicDebug.warn("Invalid value found for euler angle option %s.", MagicDebug.resolvePath(path));
						return data -> null;
					}

					MagicDebug.warn("Invalid value found for euler angle option %s. Defaulting to '%s'.", MagicDebug.resolvePath(path), def);
					return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
				}

				String[] ang = value.split(",");
				if (ang.length != 3) {
					if (def == null) {
						MagicDebug.warn("Invalid value '%s' found for euler angle option %s - too many or too few arguments.", value, MagicDebug.resolvePath(path));
						return data -> null;
					}

					MagicDebug.warn("Invalid value '%s' found for euler angle option %s - too many or too few arguments. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
					return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
				}

				try {
					EulerAngle angle = new EulerAngle(Double.parseDouble(ang[0]), Double.parseDouble(ang[1]), Double.parseDouble(ang[2]));
					MagicDebug.info("Resolved value '%s,%s,%s'.", angle.getX(), angle.getY(), angle.getZ());
					return data -> new EulerAngle(angle.getX(), angle.getY(), angle.getZ());
				} catch (NumberFormatException e) {
					if (def == null) {
						MagicDebug.warn("Invalid value '%s' found for euler angle option %s.", value, MagicDebug.resolvePath(path));
						return data -> null;
					}

					MagicDebug.warn("Invalid value '%s' found for euler angle option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), def);
					return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
				}
			}

			ConfigData<Double> x;
			ConfigData<Double> y;
			ConfigData<Double> z;

			Object obj = config.get(path);
			if (obj instanceof ConfigurationSection section) {
				x = getDouble(section, "x", 0);
				y = getDouble(section, "y", 0);
				z = getDouble(section, "z", 0);
			} else if (obj instanceof List<?> list) {
				if (list.size() != 3) {
					if (def == null) {
						MagicDebug.warn("Invalid value '%s' found for euler angle option %s - too many or too few arguments.", list, MagicDebug.resolvePath(path));
						return data -> null;
					}

					MagicDebug.warn("Invalid value '%s' found for euler angle option %s - too many or too few arguments. Defaulting to '%s'.", list, MagicDebug.resolvePath(path), def);
					return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
				}

				Object xObj = list.get(0);
				Object yObj = list.get(1);
				Object zObj = list.get(2);

				if (xObj instanceof Number number) {
					double val = number.doubleValue();
					x = data -> val;
				} else if (xObj instanceof String string) {
					x = FunctionData.build(string, Function.identity());
				} else x = null;

				if (yObj instanceof Number number) {
					double val = number.doubleValue();
					y = data -> val;
				} else if (yObj instanceof String string) {
					y = FunctionData.build(string, Function.identity());
				} else y = null;

				if (zObj instanceof Number number) {
					double val = number.doubleValue();
					z = data -> val;
				} else if (zObj instanceof String string) {
					z = FunctionData.build(string, Function.identity());
				} else z = null;

				if (x == null || y == null || z == null) {
					if (def == null) {
						if (x == null) {
							MagicDebug.warn("Invalid value '%s' found for x component of euler angle option %s.", xObj, MagicDebug.resolvePath(path));
							return data -> null;
						}

						if (y == null) {
							MagicDebug.warn("Invalid value '%s' found for y component of euler angle option %s.", yObj, MagicDebug.resolvePath(path));
							return data -> null;
						}

						MagicDebug.warn("Invalid value '%s' found for z component of euler angle option %s.", zObj, MagicDebug.resolvePath(path));
						return data -> null;
					}

					if (x == null) {
						MagicDebug.warn("Invalid value '%s' found for x component of euler angle option %s. Defaulting to '%s,%s,%s'.", xObj, MagicDebug.resolvePath(path), def.getX(), def.getY(), def.getZ());
						return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
					}

					if (y == null) {
						MagicDebug.warn("Invalid value '%s' found for y component of euler angle option %s. Defaulting to '%s,%s,%s'.", yObj, MagicDebug.resolvePath(path), def.getX(), def.getY(), def.getZ());
						return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
					}

					MagicDebug.warn("Invalid value '%s' found for z component of euler angle option %s. Defaulting to '%s,%s,%s'.", zObj, MagicDebug.resolvePath(path), def.getX(), def.getY(), def.getZ());
					return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());

				}
			} else if (obj != null) {
				if (def == null) {
					MagicDebug.warn("Invalid value '%s' found for euler angle option %s.", obj, MagicDebug.resolvePath(path));
					return data -> null;
				}

				MagicDebug.warn("Invalid value '%s' found for euler angle option %s. Defaulting to '%s,%s,%s'.", obj, MagicDebug.resolvePath(path), def.getX(), def.getY(), def.getZ());
				return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
			} else {
				if (def == null) {
					MagicDebug.info("No value found.");
					return data -> null;
				}

				MagicDebug.info("No value found. Defaulting to '%s,%s,%s'.", def.getX(), def.getY(), def.getZ());
				return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
			}

			if (x.isConstant() && y.isConstant() && z.isConstant()) {
				EulerAngle angle = new EulerAngle(x.get(), y.get(), z.get());
				MagicDebug.info("Resolved value '%s,%s,%s'.", angle.getX(), angle.getY(), angle.getZ());
				return data -> new EulerAngle(angle.getX(), angle.getY(), angle.getZ());
			}

			MagicDebug.info("Resolved expression '{x: '%s', y: '%s', z: '%s'}'.", x, y, z);

			return (VariableConfigData<EulerAngle>) data -> new EulerAngle(x.get(data), y.get(data), z.get(data));
		}
	}

	public static ConfigData<Color> getColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Color def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving color option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isString(path)) {
				String value = config.getString(path);
				if (value == null) {
					if (def == null)
						MagicDebug.warn("Invalid value found for color option %s.", MagicDebug.resolvePath(path));
					else
						MagicDebug.warn("Invalid value found for color option %s. Defaulting to '%s'.", MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asRGB()));

					return data -> def;
				}

				ConfigData<String> supplier = getString(value);
				if (supplier.isConstant()) {
					Color color = ColorUtil.getColorFromHexString(value, false);
					if (color == null) {
						if (def == null)
							MagicDebug.warn("Invalid value '%s' found for color option %s.", value, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid value '%s' found for color option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asRGB()));

						return data -> def;
					}

					return data -> color;
				}

				MagicDebug.info("Resolved expression '%s'.", supplier);

				return (VariableConfigData<Color>) data -> {
					Color color = ColorUtil.getColorFromHexString(supplier.get(data), false);
					return color == null ? def : color;
				};
			}

			if (config.isConfigurationSection(path)) {
				ConfigurationSection section = config.getConfigurationSection(path);
				if (section == null) {
					if (def == null)
						MagicDebug.warn("Invalid value found for color option %s.", MagicDebug.resolvePath(path));
					else
						MagicDebug.warn("Invalid value found for color option %s. Defaulting to '%s'.", MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asRGB()));

					return data -> def;
				}

				ConfigData<Integer> red = getInteger(section, "red", 0);
				ConfigData<Integer> green = getInteger(section, "green", 0);
				ConfigData<Integer> blue = getInteger(section, "blue", 0);

				if (red.isConstant() && green.isConstant() && blue.isConstant()) {
					int r = red.get();
					if (r < 0 || r > 255) {
						if (def == null)
							MagicDebug.warn("Invalid red value '%d' found for color option %s.", r, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid value red value '%d' found for color option %s. Defaulting to '%s'.", r, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asRGB()));

						return data -> def;
					}

					int g = green.get();
					if (g < 0 || g > 255) {
						if (def == null)
							MagicDebug.warn("Invalid green value '%d' found for color option %s.", g, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid value green value '%d' found for color option %s. Defaulting to '%s'.", g, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asRGB()));

						return data -> def;
					}

					int b = blue.get();
					if (b < 0 || b > 255) {
						if (def == null)
							MagicDebug.warn("Invalid blue value '%d' found for color option %s.", b, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid blue red value '%d' found for color option %s. Defaulting to '%s'.", b, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asRGB()));

						return data -> def;
					}

					Color c = Color.fromRGB(r, g, b);
					MagicDebug.info("Resolved value '%s'.", (Supplier<String>) () -> Integer.toHexString(c.asRGB()));

					return data -> c;
				}

				MagicDebug.info("Resolved expression '{red: '%s', green: '%s', blue: '%s'}'.", red, green, blue);

				return (VariableConfigData<Color>) data -> {
					Integer r = red.get(data);
					Integer g = green.get(data);
					Integer b = blue.get(data);
					if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
						return def;

					return Color.fromRGB(r, g, b);
				};
			}

			if (config.isSet(path)) {
				if (def == null) MagicDebug.warn("Invalid value '%s' found for color option %s.", config.getString(path), MagicDebug.resolvePath(path));
				else MagicDebug.warn("Invalid value '%s' found for color option %s. Defaulting to value '%s'.", config.getString(path), MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asRGB()));
			} else {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to value '%s'.", (Supplier<String>) () -> Integer.toHexString(def.asRGB()));
			}

			return data -> def;
		}
	}

	public static ConfigData<Color> getARGBColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Color def) {
		try (var ignored = MagicDebug.section(DebugCategory.OPTIONS, "Resolving color option '%s'.", getShortPath(path))) {
			if (config.isInt(path) || config.isString(path)) {
				String value = config.getString(path);
				if (value == null) {
					if (def == null)
						MagicDebug.warn("Invalid value found for color option %s.", MagicDebug.resolvePath(path));
					else
						MagicDebug.warn("Invalid value found for color option %s. Defaulting to '%s'.", MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asARGB()));

					return data -> def;
				}

				ConfigData<String> supplier = getString(value);
				if (supplier.isConstant()) {
					Color color = ColorUtil.getColorFromARGHexString(value, false);
					if (color == null) {
						if (def == null)
							MagicDebug.warn("Invalid value '%s' found for color option %s.", value, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid value '%s' found for color option %s. Defaulting to '%s'.", value, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asARGB()));

						return data -> def;
					}

					return data -> color;
				}

				MagicDebug.info("Resolved expression '%s'.", supplier);

				return (VariableConfigData<Color>) data -> {
					Color color = ColorUtil.getColorFromARGHexString(supplier.get(data), false);
					return color == null ? def : color;
				};
			}

			if (config.isConfigurationSection(path)) {
				ConfigurationSection section = config.getConfigurationSection(path);
				if (section == null) {
					if (def == null)
						MagicDebug.warn("Invalid value found for color option %s.", MagicDebug.resolvePath(path));
					else
						MagicDebug.warn("Invalid value found for color option %s. Defaulting to '%s'.", MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asARGB()));

					return data -> def;
				}

				ConfigData<Integer> alpha = getInteger(section, "alpha", 255);
				ConfigData<Integer> red = getInteger(section, "red", 0);
				ConfigData<Integer> green = getInteger(section, "green", 0);
				ConfigData<Integer> blue = getInteger(section, "blue", 0);

				if (alpha.isConstant() && red.isConstant() && green.isConstant() && blue.isConstant()) {
					Integer a = alpha.get();
					if (a < 0 || a > 255) {
						if (def == null)
							MagicDebug.warn("Invalid alpha value '%d' found for color option %s.", a, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid value red value '%d' found for color option %s. Defaulting to '%s'.", a, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asARGB()));

						return data -> def;
					}

					int r = red.get();
					if (r < 0 || r > 255) {
						if (def == null)
							MagicDebug.warn("Invalid red value '%d' found for color option %s.", r, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid value red value '%d' found for color option %s. Defaulting to '%s'.", r, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asARGB()));

						return data -> def;
					}

					int g = green.get();
					if (g < 0 || g > 255) {
						if (def == null)
							MagicDebug.warn("Invalid green value '%d' found for color option %s.", g, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid value green value '%d' found for color option %s. Defaulting to '%s'.", g, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asARGB()));

						return data -> def;
					}

					int b = blue.get();
					if (b < 0 || b > 255) {
						if (def == null)
							MagicDebug.warn("Invalid blue value '%d' found for color option %s.", b, MagicDebug.resolvePath(path));
						else
							MagicDebug.warn("Invalid blue red value '%d' found for color option %s. Defaulting to '%s'.", b, MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asARGB()));

						return data -> def;
					}

					Color c = Color.fromARGB(a, r, g, b);
					MagicDebug.info("Resolved value '%s'.", (Supplier<String>) () -> Integer.toHexString(c.asARGB()));

					return data -> c;
				}

				MagicDebug.info("Resolved expression '{alpha: '%s', red: '%s', green: '%s', blue: '%s'}'.", alpha, red, green, blue);

				return (VariableConfigData<Color>) data -> {
					Integer a = alpha.get(data);
					Integer r = red.get(data);
					Integer g = green.get(data);
					Integer b = blue.get(data);
					if (a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
						return def;

					return Color.fromARGB(a, r, g, b);
				};
			}

			if (config.isSet(path)) {
				if (def == null) MagicDebug.warn("Invalid value '%s' found for color option %s.", config.getString(path), MagicDebug.resolvePath(path));
				else MagicDebug.warn("Invalid value '%s' found for color option %s. Defaulting to value '%s'.", config.getString(path), MagicDebug.resolvePath(path), (Supplier<String>) () -> Integer.toHexString(def.asARGB()));
			} else {
				if (def == null) MagicDebug.info("No value found.");
				else MagicDebug.info("No value found. Defaulting to value '%s'.", (Supplier<String>) () -> Integer.toHexString(def.asARGB()));
			}

			return data -> def;
		}
	}

	@NotNull
	public static ConfigData<DustOptions> getDustOptions(@NotNull ConfigurationSection config,
														 @NotNull String colorPath,
														 @NotNull String sizePath,
														 @Nullable DustOptions def) {
		ConfigData<Color> color = getColor(config, colorPath, def == null ? null : def.getColor());
		ConfigData<Float> size = def == null ? getFloat(config, sizePath) : getFloat(config, sizePath, def.getSize());

		if (color.isConstant() && size.isConstant()) {
			Color c = color.get();
			if (c == null) return data -> def;

			Float s = size.get();
			if (s == null) return data -> def;

			DustOptions options = new DustOptions(c, s);
			return data -> options;
		}

		return (VariableConfigData<DustOptions>) data -> {
			Color c = color.get(data);
			if (c == null) return def;

			Float s = size.get(data);
			if (s == null) return def;

			return new DustOptions(c, s);
		};
	}

	@NotNull
	public static ConfigData<DustTransition> getDustTransition(@NotNull ConfigurationSection config,
															   @NotNull String colorPath,
															   @NotNull String toColorPath,
															   @NotNull String sizePath,
															   @Nullable DustTransition def) {
		ConfigData<Color> color = getColor(config, colorPath, def == null ? null : def.getColor());
		ConfigData<Color> toColor = getColor(config, toColorPath, def == null ? null : def.getToColor());
		ConfigData<Float> size = def == null ? getFloat(config, sizePath) : getFloat(config, sizePath, def.getSize());

		if (color.isConstant() && toColor.isConstant() && size.isConstant()) {
			Color c = color.get();
			if (c == null) return data -> def;

			Color tc = toColor.get();
			if (tc == null) return data -> def;

			Float s = size.get();
			if (s == null) return data -> def;

			DustTransition transition = new DustTransition(c, tc, s);
			return data -> transition;
		}

		return (VariableConfigData<DustTransition>) data -> {
			Color c = color.get(data);
			if (c == null) return def;

			Color tc = toColor.get(data);
			if (tc == null) return def;

			Float s = size.get(data);
			if (s == null) return def;

			return new DustTransition(c, tc, s);
		};
	}

	private static Supplier<String> getShortPath(String option) {
		return () -> {
			int index = option.lastIndexOf('.');
			if (index == -1) return option;

			return option.substring(index + 1);
		};
	}

}
