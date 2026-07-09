package org.alexmond.jgomplate.functions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Small argument-coercion helpers shared by the gomplate namespace classes. Template
 * arguments arrive as loosely-typed {@link Object}s (Go-template numbers are {@code Long}
 * / {@code Double}, everything renders through {@code toString}), so the namespaces take
 * {@code Object} parameters and normalize here rather than relying on reflective
 * parameter-type matching.
 */
public final class Values {

	private Values() {
	}

	/** Render a value the way Go's template engine would stringify it. */
	public static String str(Object value) {
		return (value != null) ? String.valueOf(value) : "";
	}

	/**
	 * Convert to a string the way gomplate's {@code conv.ToString} does: {@code null}
	 * becomes the literal {@code "nil"}, a byte array is decoded as UTF-8, and everything
	 * else is stringified. This differs from {@link #str(Object)} (which maps
	 * {@code null} to the empty string) and is used where gomplate routes values through
	 * {@code conv.ToString} — {@code conv.ToString} itself and {@code conv.Join}.
	 * @param value the value to convert
	 * @return the gomplate {@code conv.ToString} rendering
	 */
	public static String toString(Object value) {
		if (value == null) {
			return "nil";
		}
		if (value instanceof byte[] bytes) {
			return new String(bytes, StandardCharsets.UTF_8);
		}
		return String.valueOf(value);
	}

	/** Coerce to an {@code int}, accepting numbers or numeric strings. */
	public static int toInt(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		return Integer.parseInt(str(value).trim());
	}

	/**
	 * Coerce to a {@code long} the way gomplate's {@code conv.ToInt64} does: booleans map
	 * to {@code 1}/{@code 0}, numbers truncate, and strings parse in base 0 (so
	 * {@code 0x} hex and leading-{@code 0} octal are honoured).
	 * @param value the value to coerce
	 * @return the {@code long} value
	 */
	public static long toLong(Object value) {
		if (value instanceof Boolean bool) {
			return bool ? 1L : 0L;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		String text = str(value).trim().replace(",", "");
		try {
			return Long.decode(text);
		}
		catch (NumberFormatException ignored) {
			// gomplate's strToInt64 falls back to a float parse, then truncates
			return (long) Double.parseDouble(text);
		}
	}

	/**
	 * Coerce to a {@code double} the way gomplate's {@code conv.ToFloat64} does: booleans
	 * map to {@code 1}/{@code 0}, numbers widen, and strings parse as an integer (base 0)
	 * or a float.
	 * @param value the value to coerce
	 * @return the {@code double} value
	 */
	public static double toDouble(Object value) {
		if (value instanceof Boolean bool) {
			return bool ? 1.0d : 0.0d;
		}
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		String text = str(value).trim().replace(",", "");
		try {
			return Long.decode(text);
		}
		catch (NumberFormatException ignored) {
			return Double.parseDouble(text);
		}
	}

	/**
	 * Coerce to a {@code boolean} using gomplate's {@code conv.ToBool} rules: real
	 * booleans pass through; a number is true only when it equals {@code 1}; a string is
	 * true only when it is (case-insensitively) {@code "1"}, {@code "t"}, {@code "true"},
	 * or {@code "yes"}, or parses as a number equal to {@code 1}; everything else (nil,
	 * {@code "on"}, {@code "42"}, other kinds) is false.
	 * @param value the value to coerce
	 * @return the gomplate boolean interpretation
	 */
	public static boolean toBool(Object value) {
		if (value instanceof Boolean bool) {
			return bool;
		}
		if (value instanceof Number number) {
			return number.doubleValue() == 1.0d;
		}
		if (value instanceof String text) {
			return strToBool(text);
		}
		return false;
	}

	private static boolean strToBool(String value) {
		String text = value.toLowerCase(Locale.ROOT);
		if (text.equals("1") || text.equals("t") || text.equals("true") || text.equals("yes")) {
			return true;
		}
		Double parsed = tryParseNumber(text);
		return (parsed != null) && (parsed == 1.0d);
	}

	private static Double tryParseNumber(String text) {
		try {
			// Long.decode understands 0x hex, leading-0 octal, and signed decimal — the
			// same set gomplate's strToFloat64 accepts before its plain-decimal fallback.
			return (double) Long.decode(text);
		}
		catch (NumberFormatException ignored) {
			// fall through to a float parse
		}
		try {
			return Double.parseDouble(text);
		}
		catch (NumberFormatException ignored) {
			return null;
		}
	}

	/**
	 * Normalize a list-ish argument (List, array, or single value) into a {@link List}.
	 */
	public static List<Object> toList(Object value) {
		List<Object> out = new ArrayList<>();
		if (value == null) {
			return out;
		}
		if (value instanceof Collection<?> collection) {
			out.addAll(collection);
		}
		else if (value instanceof Object[] array) {
			out.addAll(Arrays.asList(array));
		}
		else {
			out.add(value);
		}
		return out;
	}

}
