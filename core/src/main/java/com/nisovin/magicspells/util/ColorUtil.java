package com.nisovin.magicspells.util;

import java.util.regex.Pattern;

import org.bukkit.Color;

import com.nisovin.magicspells.handlers.DebugHandler;

public class ColorUtil {

	public static final Pattern HEX_PATTERN = Pattern.compile("&(#\\w{6})");

	public static Color getColorFromHexString(String hex, boolean debug) {
		if (hex == null) return null;

		if (hex.startsWith("#")) hex = hex.substring(1);

		try {
			int value = Integer.parseInt(hex, 16);
			return Color.fromRGB(value);
		} catch (IllegalArgumentException e) {
			if (debug) DebugHandler.debugIllegalArgumentException(e);
			return null;
		}
	}

	public static Color getColorFromHexString(String hex) {
		return getColorFromHexString(hex, true);
	}

	public static Color getColorFromARGHexString(String hex, boolean debug) {
		if (hex == null) return null;

		if (hex.startsWith("#")) hex = hex.substring(1);

		try {
			int value = Integer.parseUnsignedInt(hex, 16);
			return Color.fromARGB(value);
		} catch (IllegalArgumentException e) {
			if (debug) DebugHandler.debugIllegalArgumentException(e);
			return null;
		}
	}

}
