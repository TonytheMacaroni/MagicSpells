package com.nisovin.magicspells.util.conversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.NotNull;

public interface ConversionSource<T> extends Iterable<T> {

	static ConversionSource<String> split(@NotNull String value, @NotNull String pattern) {
		return new IterableConversionSource<>(Arrays.asList(value.split(pattern)));
	}

	static ConversionSource<String> split(@NotNull String value, @NotNull Pattern pattern) {
		return new IterableConversionSource<>(Arrays.asList(pattern.split(value)));
	}

	static <T> ConversionSource<T> iterable(@NotNull Iterable<T> collection) {
		return new IterableConversionSource<>(collection);
	}

	static ConversionSource<?> listFromConfig(@NotNull ConfigurationSection config, @NotNull String path) {
		List<?> list = config.getList(path);
		if (list == null) return empty();

		return new IterableConversionSource<>(list);
	}

	static ConversionSource<?> empty() {
		class Holder {
			static final ConversionSource<?> EMPTY = new IterableConversionSource<>(Collections.emptyList());
		}

		return Holder.EMPTY;
	}

}
