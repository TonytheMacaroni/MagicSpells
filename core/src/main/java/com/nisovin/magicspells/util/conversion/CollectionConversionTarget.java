package com.nisovin.magicspells.util.conversion;

import java.util.Collection;
import java.util.function.Supplier;

class CollectionConversionTarget<T, C extends Collection<T>> implements ConversionTarget<T, C> {

	private final Supplier<C> collectionFactory;
	private final Supplier<C> defaultFactory;

	private C collection;

	public CollectionConversionTarget(Supplier<C> collectionFactory, Supplier<C> defaultFactory) {
		this.collectionFactory = collectionFactory;
		this.defaultFactory = defaultFactory;
	}

	public CollectionConversionTarget(Supplier<C> collectionFactory) {
		this(collectionFactory, null);
	}

	@Override
	public void add(T value) {
		if (collection == null) collection = collectionFactory.get();

		collection.add(value);
	}

	@Override
	public C collect() {
		if (collection == null)
			return defaultFactory == null ? collectionFactory.get() : defaultFactory.get();

		return collection;
	}

}
