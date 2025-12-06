package com.nisovin.magicspells.util.conversion;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ConversionTarget<T, R> {

	void add(T value);

	ConversionResult<R> collect();

	static <T> ConversionTarget<T, Set<T>> set() {
		return new CollectionConversionTarget<>(HashSet::new, null, false);
	}

	static <T> ConversionTarget<T, Set<T>> set(boolean nullIfEmpty) {
		return new CollectionConversionTarget<>(HashSet::new, null, nullIfEmpty);
	}

	static <T> ConversionTarget<T, Set<T>> set(Supplier<Set<T>> defaultFactory) {
		return new CollectionConversionTarget<>(HashSet::new, defaultFactory, false);
	}

	static <T extends Enum<T>> ConversionTarget<T, Set<T>> enumSet(Class<T> type) {
		return new CollectionConversionTarget<>(() -> EnumSet.noneOf(type), null, false);
	}

	static <T extends Enum<T>> ConversionTarget<T, Set<T>> enumSet(Class<T> type, boolean nullIfEmpty) {
		return new CollectionConversionTarget<>(() -> EnumSet.noneOf(type), null, nullIfEmpty);
	}

	static <T extends Enum<T>> ConversionTarget<T, Set<T>> enumSet(Class<T> type, Supplier<Set<T>> defaultFactory) {
		return new CollectionConversionTarget<>(() -> EnumSet.noneOf(type), defaultFactory, false);
	}

	static <T> ConversionTarget<T, List<T>> list() {
		return new CollectionConversionTarget<>(ArrayList::new, null, false);
	}

	static <T> ConversionTarget<T, List<T>> list(boolean nullIfEmpty) {
		return new CollectionConversionTarget<>(ArrayList::new, null, nullIfEmpty);
	}

	static <T> ConversionTarget<T, List<T>> list(Supplier<List<T>> defaultFactory) {
		return new CollectionConversionTarget<>(ArrayList::new, defaultFactory, false);
	}

	static <T, C extends Collection<T>> ConversionTarget<T, C> collection(Supplier<C> collectionFactory) {
		return new CollectionConversionTarget<>(collectionFactory, null, false);
	}

	static <T, C extends Collection<T>> ConversionTarget<T, C> collection(Supplier<C> collectionFactory, boolean nullIfEmpty) {
		return new CollectionConversionTarget<>(collectionFactory, null, nullIfEmpty);
	}

	static <T, C extends Collection<T>> ConversionTarget<T, C> collection(Supplier<C> collectionFactory, Supplier<C> defaultFactory) {
		return new CollectionConversionTarget<>(collectionFactory, defaultFactory, false);
	}

	static <T> ConversionTarget<T, Void> consumer(Consumer<T> consumer) {
		return new ConsumerConversionTarget<>(consumer);
	}

	static <T> ConversionTarget<T, Void> addTo(Collection<T> collection) {
		return new ConsumerConversionTarget<>(collection::add);
	}

}
