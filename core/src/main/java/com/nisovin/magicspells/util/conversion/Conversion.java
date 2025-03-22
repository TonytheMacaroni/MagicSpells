package com.nisovin.magicspells.util.conversion;

public record Conversion<T, R>(ConversionTarget<T, R> target, Converter<?, T>... converters) {

	@SafeVarargs
	public Conversion {

	}

	@SafeVarargs
	public static <T, R> Conversion<T, R> of(ConversionTarget<T, R> target, Converter<?, T>... converters) {
		return new Conversion<>(target, converters);
	}

}
