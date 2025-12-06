package com.nisovin.magicspells.util.conversion;

import java.util.function.Predicate;

import com.nisovin.magicspells.debug.MagicDebug;

record StringConversionSource<T>(String original, Iterable<T> elements) implements ConversionSource<T> {

	@Override
	public boolean forEach(Predicate<T> action) {
		try (var _ = MagicDebug.section("Resolving values from string '%s'.", original)) {
			for (T element : elements) {
			    try (var _ = MagicDebug.section("Resolving value '%s'.", element)) {
					if (!action.test(element))
						return false;
				}
			}

			return true;
		}
	}

}
