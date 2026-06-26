package com.nisovin.magicspells.debug;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// TODO: Transition code over to scopes
@NullMarked
public record DebugScope(
	Component header,
	DebugCategory category,
	DebugLevel level,
	@Nullable DebugConfig config,
	DebugOutput output,
	boolean supressWarnings
	// TODO: The other fields being a part of the scope don't make sense. Should be tracked outside of the scope.
	//  Output may not make sense to have its own field; then again, it likely depends on the parent scope, so computing
	//  it on the fly may be intrusive.
) {



}
