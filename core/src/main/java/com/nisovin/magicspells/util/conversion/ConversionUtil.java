package com.nisovin.magicspells.util.conversion;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.ConfigReaderUtil;

public class ConversionUtil {

	// TODO: Support multiple targets. Allow targets to specify if conversion should end.
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <S, T, R> R convert(ConversionSource<S> source, ConversionTarget<T, R> target, Converter<?, T>... converters) {
		if (source == ConversionSource.empty()) return target.collect();

		Iterator<S> iterator = source.iterator();
		int index = 0;

		outer:
		while (iterator.hasNext()) {
			S sourceValue = iterator.next();

			try (var ignored1 = MagicDebug.pushListEntry(index)) {
				for (Converter<?, T> converter : converters) {
					ConversionResult<T> result = switch (converter) {
						case StringConverter<?> stringConverter -> {
							if (!(sourceValue instanceof String string)) yield ConversionResult.skip();
							yield ((StringConverter<T>) stringConverter).convert(string);
						}
						case SectionConverter<?> sectionConverter -> {
							if (!(sourceValue instanceof Map<?, ?> map)) yield ConversionResult.skip();

							ConfigurationSection section = ConfigReaderUtil.mapToSection(map);
							yield ((SectionConverter<T>) sectionConverter).convert(section);
						}
					};

					switch (result) {
						case Valid(T value) -> {
							target.add(value);
							continue outer;
						}
						case Invalid(boolean warn) -> {
							if (warn) MagicDebug.warn("Invalid value '%s' %s.", sourceValue, MagicDebug.resolveFullPath());
//							if (nullIfInvalid) return null;

							continue outer;
						}
						case Skip() -> {}
					}
				}

				MagicDebug.warn("Invalid value '%s' %s.", sourceValue, MagicDebug.resolveFullPath());
//				if (nullIfInvalid) return null;
			}

			index++;
		}

		return target.collect();
	}

	// TODO: Support multiple targets. Allow targets to specify if conversion should end.
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <S> void convert(ConversionSource<S> source, Conversion<?, Void>... conversions) {
		if (source == ConversionSource.empty()) return;

		Iterator<S> iterator = source.iterator();
		int index = 0;

		outer:
		while (iterator.hasNext()) {
			S sourceValue = iterator.next();

			try (var ignored1 = MagicDebug.pushListEntry(index)) {
				for (var conversion : conversions) {
					for (Converter<?, ?> converter : conversion.converters()) {
						ConversionResult<?> result = switch (converter) {
							case StringConverter<?> stringConverter -> {
								if (!(sourceValue instanceof String string)) yield ConversionResult.skip();
								yield stringConverter.convert(string);
							}
							case SectionConverter<?> sectionConverter -> {
								if (!(sourceValue instanceof Map<?, ?> map)) yield ConversionResult.skip();

								ConfigurationSection section = ConfigReaderUtil.mapToSection(map);
								yield sectionConverter.convert(section);
							}
						};

						switch (result) {
							case Valid(Object value) -> {
								((ConversionTarget) conversion.target()).add(value);
								continue outer;
							}
							case Invalid(boolean warn) -> {
								if (warn) MagicDebug.warn("Invalid value '%s' %s.", sourceValue, MagicDebug.resolveFullPath());
//							    if (nullIfInvalid) return null;

								continue outer;
							}
							case Skip() -> {}
						}
					}

					MagicDebug.warn("Invalid value '%s' %s.", sourceValue, MagicDebug.resolveFullPath());
//				    if (nullIfInvalid) return null;
				}
			}

			index++;
		}
	}

}
