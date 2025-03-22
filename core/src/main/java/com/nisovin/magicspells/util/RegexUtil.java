package com.nisovin.magicspells.util;

import java.util.regex.Pattern;

public class RegexUtil {

	public static final Pattern DOUBLE_PATTERN = Pattern.compile("[\\x00-\\x20]*[+-]?(NaN|Infinity|((((\\d+)(\\.)?((\\d+)?)([eE][+-]?(\\d+))?)|(\\.(\\d+)([eE][+-]?(\\d+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\d+)))[fFdD]?))[\\x00-\\x20]*");

	public static final Pattern SIMPLE_INT_PATTERN = Pattern.compile("^\\d+$");

	public static final Pattern BLOCK_DATA_SPLIT_PATTERN = Pattern.compile(",(?![^\\[]*])");

}
