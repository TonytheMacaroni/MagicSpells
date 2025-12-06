package com.nisovin.magicspells.util.conversion;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

public sealed interface Converter<F, T> permits StringConverter, SectionConverter, MultiConverter {

	@NotNull
	ConverterResult convert(F value, ConversionTarget<? super T, ?> target);

	void extractTargetTypes(Consumer<String> targetTypes);

	static <T> StringConverter<T> string(StringConverter<T> converter) {
		return converter;
	}

	static <T> SectionConverter<T> section(SectionConverter<T> converter) {
		return converter;
	}

	@SafeVarargs
	static <T> Converter<?, T> of(Converter<?, T>... converters) {
		return new MultiConverter<>(converters);
	}

}
