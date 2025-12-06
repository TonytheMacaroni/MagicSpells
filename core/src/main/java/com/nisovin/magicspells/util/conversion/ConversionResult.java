package com.nisovin.magicspells.util.conversion;

public sealed interface ConversionResult<T> {

	T getOrNull();

	boolean isValid();

	boolean isInvalid();

	record Valid<T>(T value) implements ConversionResult<T> {

		@Override
		public T getOrNull() {
			return value;
		}

		@Override
		public boolean isValid() {
			return true;
		}

		@Override
		public boolean isInvalid() {
			return false;
		}

	}

	record Invalid<T>() implements ConversionResult<T> {

		@Override
		public T getOrNull() {
			return null;
		}

		@Override
		public boolean isValid() {
			return false;
		}

		@Override
		public boolean isInvalid() {
			return true;
		}

	}

	static <T> Valid<T> valid(T value) {
		return new Valid<>(value);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static <T> Valid<T> validNull() {
		class Holder {
			static final Valid VALID = new Valid(null);
		}

		return (Valid<T>) Holder.VALID;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static <T> Invalid<T> invalid() {
		class Holder {
			static final Invalid INVALID = new Invalid();
		}

		return (Invalid<T>) Holder.INVALID;
	}

}
