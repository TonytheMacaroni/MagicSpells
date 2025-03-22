package com.nisovin.magicspells.util.conversion;

import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.DebugPath;
import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.ConfigReaderUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: Perhaps make the collection methods take in a single converter, and add a method to compose converters.
//  Additionally, perhaps add a Converter#skippable that uses Section#downgradeWarnings and converting invalid results
//  -> skip results.
public class CollectionUtil {

	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <TValue, TCollection extends Collection<TValue>> TCollection getCollection(
		@NotNull ConfigurationSection config,
		@NotNull String path,
		@NotNull Supplier<@NotNull TCollection> collectionFactory,
		@NotNull Converter<?, TValue> @NotNull ... converters
	) {
		return getCollection(
			config,
			path,
			true,
			false,
			true,
			collectionFactory,
			null,
			converters
		);
	}

	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <TValue, TCollection extends Collection<TValue>> TCollection getCollection(
		@NotNull ConfigurationSection config,
		@NotNull String path,
		boolean nullIfAbsent,
		boolean nullIfInvalid,
		boolean nullIfEmpty,
		@NotNull Supplier<@NotNull TCollection> collectionFactory,
		@Nullable Supplier<@NotNull TCollection> defaultFactory,
		@NotNull Converter<?, TValue> @NotNull ... converters
	) {
		try (var ignored = MagicDebug.section("Resolving collection '%s'.", MagicDebug.resolveShortPath(config, path))
			.pushPaths(path, DebugPath.Type.LIST)
		) {
			List<?> list = config.getList(path);
			if (list == null) {
				if (defaultFactory == null) {
					MagicDebug.info("No values found.");
					return nullIfAbsent || nullIfEmpty ? null : collectionFactory.get();
				}

				TCollection def = defaultFactory.get();
				MagicDebug.info("No values found - using default '%s'.", def);

				return def;
			}

			TCollection collection = collectionFactory.get();

			outer:
			for (int i = 0; i < list.size(); i++) {
				Object object = list.get(i);

				try (var ignored1 = MagicDebug.pushListEntry(i)) {
					for (Converter<?, TValue> converter : converters) {
						ConversionResult<TValue> result = switch (converter) {
							case StringConverter<?> stringConverter -> {
								if (!(object instanceof String string)) yield ConversionResult.skip();
								yield ((StringConverter<TValue>) stringConverter).convert(string);
							}
							case SectionConverter<?> sectionConverter -> {
								if (!(object instanceof Map<?, ?> map)) yield ConversionResult.skip();

								ConfigurationSection section = ConfigReaderUtil.mapToSection(map);
								yield ((SectionConverter<TValue>) sectionConverter).convert(section);
							}
						};

						switch (result) {
							case Valid(TValue value) -> {
								collection.add(value);
								continue outer;
							}
							case Invalid(boolean warn) -> {
								if (warn) MagicDebug.warn("Invalid value '%s' %s.", object, MagicDebug.resolveFullPath());
								if (nullIfInvalid) return null;

								continue outer;
							}
							case Skip() -> {}
						}
					}

					MagicDebug.warn("Invalid value '%s' %s.", object, MagicDebug.resolveFullPath());
					if (nullIfInvalid) return null;
				}
			}

			return !nullIfEmpty || !collection.isEmpty() ? collection : null;
		}
	}

}
