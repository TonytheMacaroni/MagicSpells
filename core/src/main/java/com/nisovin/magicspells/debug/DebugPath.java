package com.nisovin.magicspells.debug;

import com.google.common.base.Preconditions;

public record DebugPath(String node, Type type, boolean concrete) implements AutoCloseable {

	public DebugPath {
		Preconditions.checkNotNull(node, "Path node cannot be null");
		Preconditions.checkNotNull(type, "Path type cannot be null");
		Preconditions.checkArgument(type != Type.FILE || !concrete, "File paths cannot be concrete");
		Preconditions.checkArgument(concrete || type == Type.SECTION || type == Type.FILE, "Non-file/section paths must be concrete");
	}

	@Override
	public void close() {
		MagicDebug.popPath(this);
	}

	public enum Type {
		FILE,
		SECTION,
		LIST,
		LIST_ENTRY
	}

}
