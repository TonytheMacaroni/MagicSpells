package com.nisovin.magicspells.debug;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public enum DebugLevel {

	NONE(Level.OFF),
	ERROR(Level.SEVERE),
	WARNING(Level.WARNING),
	INFO(Level.INFO),
	ALL(Level.INFO);

	private final Level logLevel;

	DebugLevel(Level logLevel) {
		this.logLevel = logLevel;
	}

	@NotNull
	public Level getLogLevel() {
		return logLevel;
	}

}
