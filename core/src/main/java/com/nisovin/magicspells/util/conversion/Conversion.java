package com.nisovin.magicspells.util.conversion;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.debug.MagicDebug;
import com.nisovin.magicspells.util.ConfigReaderUtil;

public sealed abstract class Conversion<S, R, B extends Conversion<S, R, B>> {

	protected ConversionSource<? extends S> source;
	protected boolean invalidateOnError = false;

	public static <S, T, R> SingleConversion<S, T, R> single() {
		return new SingleConversion<>();
	}

	public static <S, T, R> SingleConversion<S, T, R> single(ConversionSource<? extends S> source, Converter<?, T> converter, ConversionTarget<? super T, R> target) {
		SingleConversion<S, T, R> conversion = new SingleConversion<>();
		return conversion.source(source).converter(converter).target(target);
	}

	public static <S> MultiConversion<S> multi(ConversionSource<? extends S> source) {
		return new MultiConversion<S>().source(source);
	}

	public static <S> MultiConversion<S> multi() {
		return new MultiConversion<>();
	}

	public static <S, T, R> R convert(ConversionSource<? extends S> source, Converter<?, T> converter, ConversionTarget<? super T, R> target) {
		SingleConversion<S, T, R> conversion = new SingleConversion<>();
		return conversion.source(source).converter(converter).target(target).convert().getOrNull();
	}

	static Supplier<String> getTargetTypes(Converter<?, ?> converter) {
		List<String> targetTypes = new ArrayList<>();
		converter.extractTargetTypes(targetTypes::add);

		return getTargetTypes(targetTypes);
	}

	static Supplier<String> getTargetTypes(List<String> targetTypes) {
		if (targetTypes.isEmpty()) return () -> "value";
		if (targetTypes.size() == 1) return targetTypes::getFirst;

		targetTypes.sort(null);

		int size = targetTypes.size();
		if (size == 2) return () -> targetTypes.getFirst() + " or " + targetTypes.getLast();

		targetTypes.set(size - 1, "or " + targetTypes.get(size - 1));
		return () -> String.join(", ", targetTypes);
	}

	@SuppressWarnings("unchecked")
	public B source(ConversionSource<? extends S> source) {
		this.source = source;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B invalidateOnError(boolean invalidateOnError) {
		this.invalidateOnError = invalidateOnError;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B invalidateOnError() {
		this.invalidateOnError = true;
		return (B) this;
	}

	public abstract ConversionResult<R> convert();

	public final static class SingleConversion<S, T, R> extends Conversion<S, R, SingleConversion<S, T, R>> {

		private Converter<?, T> converter;
		private ConversionTarget<? super T, R> target;

		public SingleConversion<S, T, R> converter(Converter<?, T> converter) {
			this.converter = converter;
			return this;
		}

		public SingleConversion<S, T, R> target(ConversionTarget<? super T, R> target) {
			this.target = target;
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public ConversionResult<R> convert() {
			if (source == ConversionSource.empty()) return target.collect();

			Supplier<String> type = getTargetTypes(converter);

			boolean valid = source.forEach(value -> {
				ConverterResult result = switch (converter) {
					case StringConverter<?> stringConverter -> {
						try (var _ = MagicDebug.section("Resolving string-based %s.", type)) {
							if (!(value instanceof String string)) {
								MagicDebug.info("Value is not a string.");
								yield ConverterResult.INVALID;
							}

							yield ((StringConverter<T>) stringConverter).convert(string, target);
						}
					}
					case SectionConverter<?> sectionConverter -> {
						try (var _ = MagicDebug.section("Resolving section-based %s.", type)) {
							if (!(value instanceof Map<?, ?> map)) {
								MagicDebug.info("Value is not a section.");
								yield ConverterResult.INVALID;
							}

							ConfigurationSection section = ConfigReaderUtil.mapToSection(map);
							yield ((SectionConverter<T>) sectionConverter).convert(section, target);
						}
					}
					case MultiConverter<?> multiConverter -> {
						try (var _ = MagicDebug.section("Resolving %s value.", type)) {
							yield ((MultiConverter<T>) multiConverter).convert(value, target);
						}
					}
				};

				switch (result) {
					case VALID -> MagicDebug.info("Valid value found.");
					case INVALID_NO_WARNING -> {
						if (invalidateOnError) return false;
					}
					case INVALID -> {
						MagicDebug.warn("Invalid %s '%s' %s.", type, value, MagicDebug.resolveFullPath());
						if (invalidateOnError) return false;
					}
				}

				return true;
			});

			return valid ? target.collect() : ConversionResult.invalid();
		}

	}

	public final static class MultiConversion<S> extends Conversion<S, Void, MultiConversion<S>> {

		private final List<MultiConversionEntry<?, ?>> targets = new ArrayList<>();

		public <T> MultiConversion<S> target(Converter<?, T> converter, ConversionTarget<? super T, Void> target) {
			targets.add(new MultiConversionEntry<>(target, converter));
			return this;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public ConversionResult<Void> convert() {
			Preconditions.checkState(source != null, "No source specified.");
			Preconditions.checkState(!targets.isEmpty(), "No targets specified.");

			if (source == ConversionSource.empty()) return null;

			boolean valid = source.forEach(value -> {
				for (MultiConversionEntry entry : targets) {
					ConverterResult result = switch (entry.converter) {
						case StringConverter stringConverter -> {
							try (var _ = MagicDebug.section(builder -> builder
								.message("Attempting resolve of string-based %s.", Conversion.getTargetTypes(entry.converter))
								.downgradeWarnings()
							)) {
								if (!(value instanceof String string)) {
									MagicDebug.info("Value is not a string.");
									yield ConverterResult.INVALID;
								}

								yield stringConverter.convert(string, entry.target);
							}
						}
						case SectionConverter sectionConverter -> {
							try (var _ = MagicDebug.section(builder -> builder
								.message("Attempting resolve of section-based %s.", Conversion.getTargetTypes(entry.converter))
								.downgradeWarnings()
							)) {
								if (!(value instanceof Map<?, ?> map)) {
									MagicDebug.info("Value is not a section.");
									yield ConverterResult.INVALID;
								}

								ConfigurationSection section = ConfigReaderUtil.mapToSection(map);
								yield sectionConverter.convert(section, entry.target);
							}
						}
						case MultiConverter multiConverter -> {
							try (var _ = MagicDebug.section("Attempting resolve of %s value.", Conversion.getTargetTypes(entry.converter))) {
								yield multiConverter.convert(value, entry.target);
							}
						}
					};

					if (result == ConverterResult.VALID) {
						MagicDebug.info("Valid value found.");
						return true;
					}
				}

				List<String> targetTypes = new ArrayList<>();
				targets.forEach(target -> target.converter.extractTargetTypes(targetTypes::add));

				MagicDebug.warn("Invalid %s '%s' %s.", getTargetTypes(targetTypes), value, MagicDebug.resolveFullPath());
				return invalidateOnError;
			});

			return valid ? ConversionResult.validNull() : ConversionResult.invalid();
		}

		private record MultiConversionEntry<S, T>(ConversionTarget<? super T, Void> target, Converter<S, T> converter) {

		}

	}

}
