package com.nisovin.magicspells.util.conversion;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import net.kyori.adventure.key.Key;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.RegistryAccess;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

public final class Converters {

	public static final StringConverter<Integer> INTEGER = new StringConverter<>() {

		@Override
		public @NotNull ConverterResult convert(String value, ConversionTarget<? super Integer, ?> target) {
			try {
				target.add(Integer.parseInt(value));
				return ConverterResult.VALID;
			} catch (IllegalArgumentException e) {
				return ConverterResult.INVALID;
			}
		}

		@Override
		public void extractTargetTypes(Consumer<String> targetTypes) {
			targetTypes.accept("integer");
		}

	};

	public static final StringConverter<BlockData> BLOCK_DATA = new StringConverter<>() {

		@Override
		public @NotNull ConverterResult convert(String value, ConversionTarget<? super BlockData, ?> target) {
			try {
				target.add(Bukkit.createBlockData(value.toLowerCase()));
				return ConverterResult.VALID;
			} catch (IllegalArgumentException e) {
				return ConverterResult.INVALID;
			}
		}

		@Override
		public void extractTargetTypes(Consumer<String> targetTypes) {
			targetTypes.accept("block data");
		}

	};

	public static final StringConverter<MagicItemData> MAGIC_ITEM_DATA = new StringConverter<>() {

		@Override
		public @NotNull ConverterResult convert(String value, ConversionTarget<? super MagicItemData, ?> target) {
			MagicItemData data = MagicItems.getMagicItemDataFromString(value);
			if (data == null) return ConverterResult.INVALID_NO_WARNING;

			target.add(data);
			return ConverterResult.VALID;
		}

		@Override
		public void extractTargetTypes(Consumer<String> targetTypes) {
			targetTypes.accept("magic item");
		}

	};

	public static final StringConverter<Material> MATERIAL = new StringConverter<>() {

		@Override
		public @NotNull ConverterResult convert(String value, ConversionTarget<? super Material, ?> target) {
			Material material = Material.matchMaterial(value.toLowerCase());
			if (material == null) return ConverterResult.INVALID;

			target.add(material);
			return ConverterResult.VALID;
		}

		@Override
		public void extractTargetTypes(Consumer<String> targetTypes) {
			targetTypes.accept("material");
		}

	};

	public static final StringConverter<Material> MATERIAL_BLOCK = new StringConverter<>() {

		@Override
		public @NotNull ConverterResult convert(String value, ConversionTarget<? super Material, ?> target) {
			Material material = Material.matchMaterial(value.toLowerCase());
			if (material == null || !material.isBlock()) return ConverterResult.INVALID;

			target.add(material);
			return ConverterResult.VALID;
		}

		@Override
		public void extractTargetTypes(Consumer<String> targetTypes) {
			targetTypes.accept("block type");
		}

	};

	public static final StringConverter<Material> MATERIAL_ITEM = new StringConverter<>() {

		@Override
		public @NotNull ConverterResult convert(String value, ConversionTarget<? super Material, ?> target) {
			Material material = Material.matchMaterial(value.toLowerCase());
			if (material == null || !material.isItem()) return ConverterResult.INVALID;

			target.add(material);
			return ConverterResult.VALID;
		}

		@Override
		public void extractTargetTypes(Consumer<String> targetTypes) {
			targetTypes.accept("item type");
		}

	};

	public static final StringConverter<World> WORLD_BY_NAME = new StringConverter<>() {

		@Override
		public @NotNull ConverterResult convert(String value, ConversionTarget<? super World, ?> target) {
			World world = Bukkit.getWorld(value);
			if (world == null) return ConverterResult.INVALID;

			target.add(world);
			return ConverterResult.VALID;
		}

		@Override
		public void extractTargetTypes(Consumer<String> targetTypes) {
			targetTypes.accept("world");
		}

	};

	public static StringConverter<Integer> rangedInteger(int min, String name) {
		return rangedInteger(min, Integer.MAX_VALUE, name);
	}

	public static StringConverter<Integer> rangedInteger(int min, int max, String name) {
		return new StringConverter<>() {

			@Override
			public @NotNull ConverterResult convert(String value, ConversionTarget<? super Integer, ?> target) {
				try {
					int val = Integer.parseInt(value);

					if (val < min) {
						MagicDebug.warn("Invalid %s value '%s' %s - lower than minimum value '%s'.", name, value, MagicDebug.resolveFullPath(), min);
						return ConverterResult.INVALID_NO_WARNING;
					}

					if (val > max) {
						MagicDebug.warn("Invalid %s value '%s' %s - greater than maximum value '%s'.", name, value, MagicDebug.resolveFullPath(), max);
						return ConverterResult.INVALID_NO_WARNING;
					}

					target.add(val);
					return ConverterResult.VALID;
				} catch (IllegalArgumentException e) {
					return ConverterResult.INVALID;
				}
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				targetTypes.accept(name);
			}

		};
	}

	public static <T extends Enum<T>> StringConverter<T> enumConverter(Class<T> type) {
		String name = String.join(" ", StringUtils.splitByCharacterTypeCamelCase(type.getSimpleName()));
		return enumConverter(type, name.toLowerCase());
	}

	public static <T extends Enum<T>> StringConverter<T> enumConverter(Class<T> type, String name) {
		return new StringConverter<>() {

			@Override
			public @NotNull ConverterResult convert(String value, ConversionTarget<? super T, ?> target) {
				try {
					target.add(Enum.valueOf(type, value.toUpperCase()));
					return ConverterResult.VALID;
				} catch (IllegalArgumentException e) {
					return ConverterResult.INVALID;
				}
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				targetTypes.accept(name);
			}

		};
	}

	public static <T extends Enum<T>> StringConverter<T> enumConverter(Class<T> type, Map<String, T> aliases) {
		String name = String.join(" ", StringUtils.splitByCharacterTypeCamelCase(type.getSimpleName()));
		return enumConverter(type, name.toLowerCase(), aliases);
	}

	public static <T extends Enum<T>> StringConverter<T> enumConverter(Class<T> type, String name, Map<String, T> aliases) {
		return new StringConverter<>() {

			@Override
			public @NotNull ConverterResult convert(String value, ConversionTarget<? super T, ?> target) {
				value = value.toUpperCase();

				T alias = aliases.get(value);
				if (alias != null) {
					target.add(alias);
					return ConverterResult.VALID;
				}

				try {
					target.add(Enum.valueOf(type, value));
					return ConverterResult.VALID;
				} catch (IllegalArgumentException e) {
					return ConverterResult.INVALID;
				}
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				targetTypes.accept(name);
			}

		};
	}

	public static <T> StringConverter<T> stringFunction(Function<String, T> function) {
		return stringFunction(function, null, true);
	}

	public static <T> StringConverter<T> stringFunction(Function<String, T> function, String name) {
		return stringFunction(function, name, true);
	}

	public static <T> StringConverter<T> stringFunction(Function<String, T> function, boolean showWarning) {
		return stringFunction(function, null, true);
	}

	public static <T> StringConverter<T> stringFunction(Function<String, T> function, String name, boolean showWarning) {
		return new StringConverter<>() {

			@Override
			public @NotNull ConverterResult convert(String value, ConversionTarget<? super T, ?> target) {
				T converted = function.apply(value);
				if (converted == null) return showWarning ? ConverterResult.INVALID : ConverterResult.INVALID_NO_WARNING;

				target.add(converted);
				return ConverterResult.VALID;
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				if (name != null) targetTypes.accept(name);
			}

		};
	}

	public static <I, T> SequentialConverter<I, T> sequential(@NotNull String type) {
		return new SequentialConverter<>(type);
	}

	public static <T> SequentialConverter<T, T> sequential2(@NotNull String type) {
		return new SequentialConverter<T, T>(type).finisher(Function.identity());
	}

	public static <T> SectionConverter<T> sectionFunction(Function<ConfigurationSection, T> function) {
		return sectionFunction(function, null, true);
	}

	public static <T> SectionConverter<T> sectionFunction(Function<ConfigurationSection, T> function, String name) {
		return sectionFunction(function, name, true);
	}

	public static <T> SectionConverter<T> sectionFunction(Function<ConfigurationSection, T> function, boolean showWarning) {
		return sectionFunction(function, null, true);
	}

	public static <T> SectionConverter<T> sectionFunction(Function<ConfigurationSection, T> function, String name, boolean showWarning) {
		return new SectionConverter<>() {

			@Override
			public @NotNull ConverterResult convert(ConfigurationSection value, ConversionTarget<? super T, ?> target) {
				T converted = function.apply(value);
				if (converted == null) return showWarning ? ConverterResult.INVALID : ConverterResult.INVALID_NO_WARNING;

				target.add(converted);
				return ConverterResult.VALID;
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				if (name != null) targetTypes.accept(name);
			}

		};
	}

	public static <T extends Keyed> StringConverter<Key> registryEntryKey(RegistryKey<T> registryKey) {
		return new StringConverter<>() {

			@Override
			public @NotNull ConverterResult convert(String value, ConversionTarget<? super Key, ?> target) {
				NamespacedKey key = NamespacedKey.fromString(value);
				if (key == null) return ConverterResult.INVALID;

				Registry<@NotNull T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);
				if (registry.get(key) == null) return ConverterResult.INVALID;

				target.add(key);
				return ConverterResult.VALID;
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				targetTypes.accept(registryKey.key().value().replace('_', ' '));
			}

		};
	}

	@SuppressWarnings("UnstableApiUsage")
	public static <T extends Keyed> StringConverter<Key> registryTagKeys(RegistryKey<T> registryKey) {
		return new StringConverter<>() {

			@Override
			public @NotNull ConverterResult convert(String value, ConversionTarget<? super Key, ?> target) {
				if (!value.startsWith("#")) return ConverterResult.INVALID;

				NamespacedKey key = NamespacedKey.fromString(value.substring(1));
				if (key == null) return ConverterResult.INVALID;

				Registry<@NotNull T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);

				TagKey<T> tagKey = TagKey.create(registryKey, key);
				if (!registry.hasTag(tagKey)) return ConverterResult.INVALID;

				Tag<@NotNull T> tag = registry.getTag(tagKey);
				tag.values().forEach(entry -> target.add(entry.key()));

				return ConverterResult.VALID;
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				targetTypes.accept(registryKey.key().value().replace('_', ' ') + " tag");
			}

		};
	}

	public static <T extends Keyed> Converter<?, Key> registryEntryOrTagKeys(RegistryKey<T> registryKey) {
		return Converter.of(registryEntryKey(registryKey), registryTagKeys(registryKey));
	}

	public static <T extends Keyed> StringConverter<T> registryEntry(RegistryKey<T> registryKey) {
		return new StringConverter<>() {

			@Override
			public @NotNull ConverterResult convert(String value, ConversionTarget<? super T, ?> target) {
				NamespacedKey key = NamespacedKey.fromString(value);
				if (key == null) return ConverterResult.INVALID;

				Registry<@NotNull T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);

				T entry = registry.get(key);
				if (entry == null) return ConverterResult.INVALID;

				target.add(entry);
				return ConverterResult.VALID;
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				targetTypes.accept(registryKey.key().value().replace('_', ' '));
			}

		};
	}

	@SuppressWarnings("UnstableApiUsage")
	public static <T extends Keyed> StringConverter<T> registryTag(RegistryKey<T> registryKey) {
		return new StringConverter<>() {

			@Override
			public @NotNull ConverterResult convert(String value, ConversionTarget<? super T, ?> target) {
				if (!value.startsWith("#")) return ConverterResult.INVALID;

				NamespacedKey key = NamespacedKey.fromString(value.substring(1));
				if (key == null) return ConverterResult.INVALID;

				Registry<@NotNull T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);

				TagKey<T> tagKey = TagKey.create(registryKey, key);
				if (!registry.hasTag(tagKey)) return ConverterResult.INVALID;

				Tag<@NotNull T> tag = registry.getTag(tagKey);
				tag.resolve(registry).forEach(target::add);

				return ConverterResult.VALID;
			}

			@Override
			public void extractTargetTypes(Consumer<String> targetTypes) {
				targetTypes.accept(registryKey.key().value().replace('_', ' ') + " tag");
			}

		};
	}

	public static <T extends Keyed> Converter<?, T> registryEntryOrTag(RegistryKey<T> registryKey) {
		return Converter.of(registryEntry(registryKey), registryTag(registryKey));
	}

}
