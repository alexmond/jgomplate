package org.alexmond.jgomplate.functions.ns;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code conv} namespace (type conversion / defaulting). Reached from templates
 * as {@code conv.ToBool}, {@code conv.Default}, {@code conv.Join}, etc. Method names
 * mirror gomplate's Go API (PascalCase).
 *
 * <p>
 * The plural {@code To…s} forms are element-wise variadic coercers ({@code conv.ToInt64s
 * 42 15} → {@code [42 15]}), callable since gotmpl4j 1.2.1 unpacks varargs methods. Each
 * argument is coerced independently by the matching singular. Still missing: {@code URL}
 * (returns a parsed-URL object).
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

	/** gomplate {@code conv.ToBools in…} — coerce each argument to a boolean. */
	public List<Boolean> ToBools(Object... in) {
		List<Boolean> out = new ArrayList<>(in.length);
		for (Object v : in) {
			out.add(Values.toBool(v));
		}
		return out;
	}

	/** gomplate {@code conv.ToStrings in…} — coerce each argument to a string. */
	public List<String> ToStrings(Object... in) {
		List<String> out = new ArrayList<>(in.length);
		for (Object v : in) {
			out.add(Values.toString(v));
		}
		return out;
	}

	/** gomplate {@code conv.ToInt64s in…} — coerce each argument to a 64-bit integer. */
	public List<Long> ToInt64s(Object... in) {
		List<Long> out = new ArrayList<>(in.length);
		for (Object v : in) {
			out.add(Values.toLong(v));
		}
		return out;
	}

	/** gomplate {@code conv.ToInts in…} — coerce each argument to an integer. */
	public List<Integer> ToInts(Object... in) {
		List<Integer> out = new ArrayList<>(in.length);
		for (Object v : in) {
			out.add(Math.toIntExact(Values.toLong(v)));
		}
		return out;
	}

	/** gomplate {@code conv.ToFloat64s in…} — coerce each argument to a float. */
	public List<Double> ToFloat64s(Object... in) {
		List<Double> out = new ArrayList<>(in.length);
		for (Object v : in) {
			out.add(Values.toDouble(v));
		}
		return out;
	}

}
