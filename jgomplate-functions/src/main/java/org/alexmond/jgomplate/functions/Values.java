package org.alexmond.jgomplate.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

	/** Coerce to an {@code int}, accepting numbers or numeric strings. */
	public static int toInt(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		return Integer.parseInt(str(value).trim());
	}

	/**
	 * Coerce to a {@code boolean} using gomplate's rules: real booleans pass through;
	 * strings {@code "true"/"1"/"yes"/"on"} (case-insensitive) are true; a non-zero
	 * number is true; everything else is false.
	 */
	public static boolean toBool(Object value) {
		if (value instanceof Boolean bool) {
			return bool;
		}
		if (value instanceof Number number) {
			return number.doubleValue() != 0;
		}
		String text = str(value).trim().toLowerCase(java.util.Locale.ROOT);
		return text.equals("true") || text.equals("1") || text.equals("yes") || text.equals("on");
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
