package com.nisovin.magicspells.util.conversion;

import java.util.function.Consumer;

record ConsumerConversionTarget<T>(Consumer<T> consumer) implements ConversionTarget<T, Void> {

	@Override
	public void add(T value) {
		consumer.accept(value);
	}

	@Override
	public Void collect() {
		return null;
	}

}
