package com.nisovin.magicspells.util.conversion;

import org.jetbrains.annotations.NotNull;
import org.intellij.lang.annotations.Language;

import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.function.Predicate;

import org.bukkit.configuration.ConfigurationSection;

public interface ConversionSource<T> {

	static ConversionSource<String> split(@NotNull String value, @Language("RegExp") @NotNull String pattern) {
		return split(value, Pattern.compile(pattern), true);
	}

	static ConversionSource<String> split(@NotNull String value, @Language("RegExp") @NotNull String pattern, boolean trim) {
		return split(value, Pattern.compile(pattern), trim);
	}

	static ConversionSource<String> split(@NotNull String value, @NotNull Pattern pattern) {
		return split(value, pattern, true);
	}

	static ConversionSource<String> split(@NotNull String value, @NotNull Pattern pattern, boolean trim) {
		List<String> list = Arrays.asList(pattern.split(value));
		if (trim) list.replaceAll(String::trim);

		return new StringConversionSource<>(value, list);
	}

	static ConversionSource<?> listFromConfig(@NotNull ConfigurationSection config, @NotNull String path) {
		List<?> list = config.getList(path);
		if (list == null || list.isEmpty()) return empty();

		return new ListConversionSource<>(config, path, list);
	}

	static ConversionSource<?> empty() {
		class Holder {
			static final ConversionSource<?> EMPTY = _ -> true;
		}

		return Holder.EMPTY;
	}

	boolean forEach(Predicate<T> action);

}
