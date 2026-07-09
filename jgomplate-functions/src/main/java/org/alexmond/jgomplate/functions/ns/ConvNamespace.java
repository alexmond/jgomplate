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
		return Values.toString(in);
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
		return items.stream().map(Values::toString).collect(Collectors.joining(Values.str(sep)));
	}

	/** gomplate {@code conv.ToInt64 in} — coerce to a 64-bit integer. */
	public long ToInt64(Object in) {
		return Values.toLong(in);
	}

	/** gomplate {@code conv.ToInt in} — coerce to an integer. */
	public int ToInt(Object in) {
		return Math.toIntExact(Values.toLong(in));
	}

	/** gomplate {@code conv.ToFloat64 in} — coerce to a floating-point number. */
	public double ToFloat64(Object in) {
		return Values.toDouble(in);
	}

	/**
	 * gomplate {@code conv.ParseInt s base bitSize} — parse a signed integer in the given
	 * base ({@code 0} auto-detects {@code 0x}/{@code 0}-octal/decimal). {@code bitSize}
	 * is accepted for signature compatibility; the JVM always uses 64-bit longs.
	 * @param s the string to parse
	 * @param base the numeric base, or {@code 0} to auto-detect
	 * @param bitSize accepted for gomplate compatibility, otherwise unused
	 * @return the parsed value
	 */
	public long ParseInt(Object s, Object base, Object bitSize) {
		String text = Values.str(s).trim();
		int radix = Values.toInt(base);
		return (radix == 0) ? Long.decode(text) : Long.parseLong(text, radix);
	}

	/**
	 * gomplate {@code conv.ParseFloat s bitSize} — parse a floating-point number.
	 * {@code bitSize} is accepted for signature compatibility.
	 * @param s the string to parse
	 * @param bitSize accepted for gomplate compatibility, otherwise unused
	 * @return the parsed value
	 */
	public double ParseFloat(Object s, Object bitSize) {
		return Double.parseDouble(Values.str(s).trim());
	}

	/**
	 * gomplate {@code conv.ParseUint s base bitSize} — parse an unsigned integer in the
	 * given base ({@code 0} auto-detects). {@code bitSize} is accepted for signature
	 * compatibility.
	 * @param s the string to parse
	 * @param base the numeric base, or {@code 0} to auto-detect
	 * @param bitSize accepted for gomplate compatibility, otherwise unused
	 * @return the parsed value
	 */
	public long ParseUint(Object s, Object base, Object bitSize) {
		String text = Values.str(s).trim();
		int radix = Values.toInt(base);
		return (radix == 0) ? Long.decode(text) : Long.parseUnsignedLong(text, radix);
	}

	/** gomplate {@code conv.Atoi s} — parse a base-10 integer. */
	public int Atoi(Object s) {
		return Integer.parseInt(Values.str(s).trim());
	}

}
