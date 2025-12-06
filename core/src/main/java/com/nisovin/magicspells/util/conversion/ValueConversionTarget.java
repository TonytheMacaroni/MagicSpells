package com.nisovin.magicspells.util.conversion;

public class ValueConversionTarget<T> implements ConversionTarget<T, T> {

	private T value;

	@Override
	public void add(T value) {
		this.value = value;
	}

	@Override
	public ConversionResult<T> collect() {
		return ConversionResult.valid(value);
	}

}
