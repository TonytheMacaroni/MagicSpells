package com.nisovin.magicspells.util.conversion;

import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

record IterableConversionSource<T, C extends Iterable<T>>(C source) implements ConversionSource<T> {

	@Override
	public @NotNull Iterator<T> iterator() {
		return source.iterator();
	}

}
