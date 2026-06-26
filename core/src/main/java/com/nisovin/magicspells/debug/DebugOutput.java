package com.nisovin.magicspells.debug;

/**
 * Specifies when debug output should be displayed.
 */
public enum DebugOutput {

	/**
	 * No debug output should be displayed.
	 */
	SHOW_NONE,
	/**
	 * Debug output should only be displayed if its debug level exceeds the debug level of the output's category.
	 */
	STANDARD,
	/**
	 * All debug output should be displayed.
	 */
	SHOW_ALL

}
