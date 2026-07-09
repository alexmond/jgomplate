package org.alexmond.jgomplate.functions.ns;

import java.util.List;
import java.util.stream.Collectors;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code conv} namespace (type conversion / defaulting). Reached from templates
 * as {@code conv.ToBool}, {@code conv.Default}, {@code conv.Join}, etc. Method names
 * mirror gomplate's Go API (PascalCase).
 *
 * <p>
 * Seed subset of gomplate's full {@code conv} namespace.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class ConvNamespace {

	public boolean ToBool(Object in) {
		return Values.toBool(in);
	}

	public String ToString(Object in) {
		return Values.str(in);
	}

	/**
	 * Return {@code in} unless it is empty/absent, in which case return {@code def}.
	 * Mirrors gomplate {@code conv.Default def in}.
	 * @param def the fallback value
	 * @param in the candidate value
	 * @return {@code in} when non-empty, otherwise {@code def}
	 */
	public Object Default(Object def, Object in) {
		if (in == null || Values.str(in).isEmpty()) {
			return def;
		}
		return in;
	}

	public String Join(Object list, Object sep) {
		List<Object> items = Values.toList(list);
		return items.stream().map(Values::str).collect(Collectors.joining(Values.str(sep)));
	}

}
