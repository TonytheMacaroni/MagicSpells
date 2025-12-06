package com.nisovin.magicspells.util.conversion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;
import java.util.regex.Pattern;

import com.nisovin.magicspells.debug.MagicDebug;

import com.google.common.base.Preconditions;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SequentialConverter<I, T> implements StringConverter<T> {

	private final String type;

	private List<ConversionStep<?, I>> conversionSteps = new ArrayList<>();
	private Pattern separator = Pattern.compile(" ");
	private int requiredSteps = -1;
	private int limit = 0;

	private Function<I, T> finisher;
	private Supplier<I> supplier;

	SequentialConverter(@NotNull String type) {
		Preconditions.checkArgument(type != null, "Type name cannot be null");

		this.type = type;
	}

	public SequentialConverter<I, T> finisher(Function<I, T> finisher) {
		this.finisher = finisher;
		return this;
	}

	public SequentialConverter<I, T> supplier(I value) {
		this.supplier = () -> value;
		return this;
	}

	public SequentialConverter<I, T> supplier(Supplier<I> supplier) {
		this.supplier = supplier;
		return this;
	}

	public SequentialConverter<I, T> requiredSteps(int requiredSteps) {
		this.requiredSteps = requiredSteps;
		return this;
	}

	public SequentialConverter<I, T> separator(@RegExp String separator) {
		this.separator = Pattern.compile(separator);
		return this;
	}

	public SequentialConverter<I, T> separator(Pattern separator) {
		this.separator = separator;
		return this;
	}

	public SequentialConverter<I, T> limit(int limit) {
		this.limit = limit;
		return this;
	}

//		public <V> SequentialConverter<I, T> step(Converter<String, V> converter, BiConsumer<V, I> accumulator) {
//			conversionSteps.add(new ConversionStep<V, I>(converter, accumulator));
//			return this;
//		}

	public <V> SequentialConverter<I, T> step(Converter<String, V> converter, BiFunction<V, I, I> accumulator) {
		conversionSteps.add(new ConversionStep<V, I>(converter, accumulator, null));
		return this;
	}

	public <V> SequentialConverter<I, T> step(Converter<String, V> converter, BiFunction<V, I, I> accumulator, String name) {
		conversionSteps.add(new ConversionStep<V, I>(converter, accumulator, name));
		return this;
	}

	@Override
	public @NotNull ConverterResult convert(String value, ConversionTarget<? super T, ?> target) {
		Preconditions.checkState(!conversionSteps.isEmpty(), "At least one conversion step must be specified");
		Preconditions.checkState(finisher != null, "A finisher must be specified");
		Preconditions.checkState(supplier != null, "A supplier must be specified");

		String[] data = separator.split(value, limit);

		if (data.length < requiredSteps) {
			MagicDebug.warn("Invalid %s '%s' %s - too few arguments.", type, value, MagicDebug.resolveFullPath());
			return ConverterResult.INVALID_NO_WARNING;
		}

		if (data.length > conversionSteps.size()) {
			MagicDebug.warn("Invalid %s '%s' %s - too many arguments.", type, value, MagicDebug.resolveFullPath());
			return ConverterResult.INVALID_NO_WARNING;
		}

		ValueConversionTarget<Object> holder = new ValueConversionTarget<>();
		I intermediary = supplier.get();

		for (int i = 0; i < data.length; i++) {
			ConversionStep<?, I> step = conversionSteps.get(i);

			ConverterResult result = step.converter.convert(data[i], holder);
			switch (result) {
				case VALID -> {
					ConversionResult<Object> res = holder.collect();
					intermediary = (I) ((BiFunction) step.accumulator).apply(res.getOrNull(), intermediary);
				}
				case INVALID -> {
					Object name = step.name == null ? Conversion.getTargetTypes(step.converter) : step.name;
					MagicDebug.warn("Invalid %s '%s' %s.", name, data[i], MagicDebug.resolveFullPath());

					return ConverterResult.INVALID_NO_WARNING;
				}
				case INVALID_NO_WARNING -> {
					return ConverterResult.INVALID_NO_WARNING;
				}
			}
		}

		target.add(finisher.apply(intermediary));

		return ConverterResult.VALID;
	}

	@Override
	public void extractTargetTypes(Consumer<String> targetTypes) {
		targetTypes.accept(type);
	}

	public record ConversionStep<V, I>(@NotNull Converter<String, V> converter, @NotNull BiFunction<V, I, I> accumulator, @Nullable String name) {

		public ConversionStep {
			Preconditions.checkArgument(converter != null);
			Preconditions.checkArgument(accumulator != null);
		}

		public ConversionStep(@NotNull Converter<String, V> converter, @NotNull BiConsumer<V, I> accumulator, @Nullable String name) {
			this(converter, (value, intermediary) -> {
				accumulator.accept(value, intermediary);
				return intermediary;
			}, name);
		}

	}

}
