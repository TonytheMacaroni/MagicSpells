package com.nisovin.magicspells.util.conversion;

public sealed interface ConversionResult<T> permits Valid, Invalid, Skip {

	@SuppressWarnings({"rawtypes", "unchecked"})
	static <T> ConversionResult<T> skip() {
		class Holder {
			static final Skip SKIP = new Skip();
		}

		return Holder.SKIP;
	}

	static <T> ConversionResult<T> invalid() {
		return invalid(true);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static <T> ConversionResult<T> invalid(boolean warn) {
		class Holder {
			static final Invalid WARN = new Invalid(true);
			static final Invalid SILENT = new Invalid(true);
		}

		return warn ? Holder.WARN : Holder.SILENT;
	}

	static <T> ConversionResult<T> valid(T value) {
		return new Valid<>(value);
	}

}
