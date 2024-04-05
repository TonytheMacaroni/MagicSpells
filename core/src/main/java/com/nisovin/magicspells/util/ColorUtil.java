package com.nisovin.magicspells.util;

import java.math.BigInteger;
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
			int value = new BigInteger(hex, 16).intValue();
			return Color.fromARGB(value);
		} catch (IllegalArgumentException e) {
			if (debug) DebugHandler.debugIllegalArgumentException(e);
			return null;
		}
	}

	public static Color getColorFromRGBString(String value) {
		if (value == null) return null;
		String[] splits = value.split(",");
		if (splits.length < 3) return null;

		int red;
		int green;
		int blue;
		try {
			red = Integer.parseInt(splits[0]);
			green = Integer.parseInt(splits[1]);
			blue = Integer.parseInt(splits[2]);
			return Color.fromRGB(red, green, blue);
		} catch (IllegalArgumentException e) {
			DebugHandler.debugIllegalArgumentException(e);
			//TODO determine an appropriate means of logging this
			return null;
		}

	}

}
