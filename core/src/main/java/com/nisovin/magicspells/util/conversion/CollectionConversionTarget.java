package com.nisovin.magicspells.util.conversion;

import java.util.Collection;
import java.util.function.Supplier;

class CollectionConversionTarget<T, C extends Collection<T>> implements ConversionTarget<T, C> {

	private final Supplier<C> collectionFactory;
	private final Supplier<C> defaultFactory;
	private final boolean nullIfEmpty;

	private C collection;

	public CollectionConversionTarget(Supplier<C> collectionFactory, Supplier<C> defaultFactory, boolean nullIfEmpty) {
		this.collectionFactory = collectionFactory;
		this.defaultFactory = defaultFactory;
		this.nullIfEmpty = nullIfEmpty;
	}

	@Override
	public void add(T value) {
		if (collection == null) collection = collectionFactory.get();

		collection.add(value);
	}

	@Override
	public ConversionResult<C> collect() {
		if (collection == null) {
			if (defaultFactory != null) return ConversionResult.valid(defaultFactory.get());
			return nullIfEmpty ? ConversionResult.validNull() : ConversionResult.valid(collectionFactory.get());
		}

		return ConversionResult.valid(collection);
	}

}
