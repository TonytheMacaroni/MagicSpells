package com.nisovin.magicspells.util.conversion;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.ConfigReaderUtil;

public record MultiConverter<T>(Converter<?, T>[] converters) implements Converter<Object, T> {

	@SuppressWarnings("unchecked")
	@Override
	public @NotNull ConverterResult convert(Object value, ConversionTarget<? super T, ?> target) {
		for (Converter<?, T> converter : converters) {
			ConverterResult result = switch (converter) {
				case StringConverter<?> stringConverter -> {
					try (var _ = MagicDebug.section(builder -> builder
						.message("Attempting resolve of string-based %s.", Conversion.getTargetTypes(converter))
						.downgradeWarnings()
					)) {
						if (!(value instanceof String string)) {
							MagicDebug.info("Value is not a string.");
							yield ConverterResult.INVALID;
						}

						yield ((StringConverter<T>) stringConverter).convert(string, target);
					}
				}
				case SectionConverter<?> sectionConverter -> {
					try (var _ = MagicDebug.section(builder -> builder
						.message("Attempting resolve of section-based %s.", Conversion.getTargetTypes(converter))
						.downgradeWarnings()
					)) {
						if (!(value instanceof Map<?, ?> map)) {
							MagicDebug.info("Value is not a section.");
							yield ConverterResult.INVALID;
						}

						ConfigurationSection section = ConfigReaderUtil.mapToSection(map);
						yield ((SectionConverter<T>) sectionConverter).convert(section, target);
					}
				}
				case MultiConverter<?> multiConverter -> {
					try (var _ = MagicDebug.section("Attempting resolve of %s value.", Conversion.getTargetTypes(converter))) {
						yield ((MultiConverter<T>) multiConverter).convert(value, target);
					}
				}
			};

			if (result == ConverterResult.VALID) return result;
		}

		return ConverterResult.INVALID;
	}

	@Override
	public void extractTargetTypes(Consumer<String> targetTypes) {
		for (Converter<?, T> converter : converters)
			converter.extractTargetTypes(targetTypes);
	}

}
