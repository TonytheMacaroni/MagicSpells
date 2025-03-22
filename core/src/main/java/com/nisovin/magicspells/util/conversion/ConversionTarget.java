package com.nisovin.magicspells.util.conversion;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ConversionTarget<T, R> {

	void add(T value);

	R collect();

	static <T> ConversionTarget<T, Set<T>> set() {
		return new CollectionConversionTarget<>(HashSet::new);
	}

	static <T> ConversionTarget<T, Set<T>> set(Supplier<Set<T>> defaultFactory) {
		return new CollectionConversionTarget<>(HashSet::new, defaultFactory);
	}

	static <T> ConversionTarget<T, List<T>> list() {
		return new CollectionConversionTarget<>(ArrayList::new);
	}

	static <T> ConversionTarget<T, List<T>> list(Supplier<List<T>> defaultFactory) {
		return new CollectionConversionTarget<>(ArrayList::new, defaultFactory);
	}

	static <T, C extends Collection<T>> ConversionTarget<T, C> collection(Supplier<C> collectionFactory) {
		return new CollectionConversionTarget<>(collectionFactory);
	}

	static <T, C extends Collection<T>> ConversionTarget<T, C> collection(Supplier<C> collectionFactory, Supplier<C> defaultFactory) {
		return new CollectionConversionTarget<>(collectionFactory, defaultFactory);
	}

	static <T> ConversionTarget<T, Void> consumer(Consumer<T> consumer) {
		return new ConsumerConversionTarget<>(consumer);
	}

}
