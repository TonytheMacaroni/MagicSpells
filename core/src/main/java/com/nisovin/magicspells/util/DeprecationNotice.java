package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DeprecationNotice(@NotNull String reason, @NotNull String replacement, @Nullable String context) {

	public DeprecationNotice(@NotNull String reason, @NotNull String replacement) {
		this(reason, replacement, null);
	}

}
