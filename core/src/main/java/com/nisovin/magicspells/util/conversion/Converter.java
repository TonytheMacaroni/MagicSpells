package com.nisovin.magicspells.util.conversion;

import org.jetbrains.annotations.NotNull;

public sealed interface Converter<F, T> permits StringConverter, SectionConverter {

	@NotNull
	ConversionResult<T> convert(F value);

	static <T> StringConverter<T> string(StringConverter<T> converter) {
		return converter;
	}

	static <T> SectionConverter<T> section(SectionConverter<T> converter) {
		return converter;
	}

}
