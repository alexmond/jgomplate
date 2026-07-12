package org.alexmond.jgomplate.functions.ns;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code random} namespace — ASCII, Alpha, AlphaNum, String, Item. Reached from
 * templates as {@code random.ASCII}, … Method names mirror gomplate's Go API
 * (PascalCase).
 *
 * <p>
 * Output is drawn from {@link java.security.SecureRandom}, so it will never reproduce
 * gomplate's exact bytes — only the length, character set, and ranges match. gomplate's
 * built-in classes are ASCII (Go's {@code [[:alpha:]]}/{@code [[:alnum:]]} are ASCII),
 * and character sets are filtered to Unicode "graphic" code points, matching Go's
 * {@code unicode.IsGraphic}. A user-supplied {@code String} pattern is matched over the
 * Basic Multilingual Plane (Go scans the full range; astral code points via an explicit
 * bounds pair still work).
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class RandomNamespace {

	private static final SecureRandom RNG = new SecureRandom();

	/** gomplate's default String set — {@code [a-zA-Z0-9_.-]}. */
	private static final int[] DEFAULT_SET = codePoints(
			"-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz");

	private static final int[] ALPHA = codePoints("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");

	private static final int[] ALPHANUM = codePoints("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");

	private static final int BMP_MAX = 0xFFFF;

	private static final Map<String, int[]> CHARSET_CACHE = new ConcurrentHashMap<>();

	/**
	 * gomplate {@code random.ASCII count} — printable ASCII (space through {@code ~}).
	 */
	public String ASCII(Object count) {
		return rnd(Values.toInt(count), filterRange(' ', '~'));
	}

	/** gomplate {@code random.Alpha count} — ASCII letters. */
	public String Alpha(Object count) {
		return rnd(Values.toInt(count), ALPHA);
	}

	/** gomplate {@code random.AlphaNum count} — ASCII letters and digits. */
	public String AlphaNum(Object count) {
		return rnd(Values.toInt(count), ALPHANUM);
	}

	/**
	 * gomplate {@code random.String count [set|lower upper]} — default set, a regex-style
	 * character set, or an inclusive lower/upper code-point (or single-char) range.
	 */
	public String String(Object... args) {
		if (args.length == 0 || args.length > 3) {
			throw new IllegalArgumentException("wrong number of args: want 1 to 3, got " + args.length);
		}
		int c = Values.toInt(args[0]);
		if (c == 0) {
			throw new IllegalArgumentException("count must be greater than 0");
		}
		if (args.length == 1) {
			return rnd(c, DEFAULT_SET);
		}
		if (args.length == 2) {
			return rnd(c, matchChars(Values.toString(args[1])));
		}
		int[] bounds = bounds(args[1], args[2]);
		return rnd(c, filterRange(bounds[0], bounds[1]));
	}

	/** gomplate {@code random.Item items} — a random element of the list. */
	public Object Item(Object items) {
		List<Object> list = Values.toList(items);
		if (list.isEmpty()) {
			throw new IllegalArgumentException("expected a non-empty array or slice");
		}
		return (list.size() == 1) ? list.get(0) : list.get(RNG.nextInt(list.size()));
	}

	private static String rnd(int count, int[] chars) {
		if (chars.length == 0) {
			throw new IllegalArgumentException("no usable characters in the given range");
		}
		StringBuilder buf = new StringBuilder(Math.max(count, 0));
		for (int i = 0; i < count; i++) {
			buf.appendCodePoint(chars[RNG.nextInt(chars.length)]);
		}
		return buf.toString();
	}

	/** Lower/upper bounds from two single-char strings, or two integer code points. */
	private static int[] bounds(Object lower, Object upper) {
		if (lower instanceof String ls && upper instanceof String us) {
			if (ls.codePointCount(0, ls.length()) == 1 && us.codePointCount(0, us.length()) == 1) {
				return new int[] { ls.codePointAt(0), us.codePointAt(0) };
			}
			return new int[] { Integer.decode(ls), Integer.decode(us) };
		}
		return new int[] { Values.toInt(lower), Values.toInt(upper) };
	}

	private static int[] filterRange(int lower, int upper) {
		int[] tmp = new int[Math.max(0, upper - lower + 1)];
		int len = 0;
		for (int cp = lower; cp <= upper; cp++) {
			if (isGraphic(cp)) {
				tmp[len++] = cp;
			}
		}
		int[] out = new int[len];
		System.arraycopy(tmp, 0, out, 0, len);
		return out;
	}

	private static int[] matchChars(String spec) {
		return CHARSET_CACHE.computeIfAbsent(spec, (s) -> {
			Pattern pattern = Pattern.compile(translatePosix(s));
			int[] tmp = new int[BMP_MAX + 1];
			int len = 0;
			for (int cp = 0; cp <= BMP_MAX; cp++) {
				if (isGraphic(cp) && pattern.matcher(new String(Character.toChars(cp))).matches()) {
					tmp[len++] = cp;
				}
			}
			int[] out = new int[len];
			System.arraycopy(tmp, 0, out, 0, len);
			return out;
		});
	}

	/**
	 * Rewrite Go/POSIX bracket classes ({@code [:alpha:]}) to Java's {@code \p{Alpha}}
	 * form.
	 */
	private static String translatePosix(String spec) {
		return spec.replace("[:alpha:]", "\\p{Alpha}")
			.replace("[:alnum:]", "\\p{Alnum}")
			.replace("[:digit:]", "\\p{Digit}")
			.replace("[:upper:]", "\\p{Upper}")
			.replace("[:lower:]", "\\p{Lower}")
			.replace("[:punct:]", "\\p{Punct}")
			.replace("[:space:]", "\\s")
			.replace("[:print:]", "\\p{Print}")
			.replace("[:graph:]", "\\p{Graph}")
			.replace("[:xdigit:]", "\\p{XDigit}")
			.replace("[:word:]", "\\w")
			.replace("[:cntrl:]", "\\p{Cntrl}");
	}

	/**
	 * Go's {@code unicode.IsGraphic}: categories L, M, N, P, S, and the space separator
	 * Zs.
	 */
	private static boolean isGraphic(int cp) {
		return switch (Character.getType(cp)) {
			case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
					Character.MODIFIER_LETTER, Character.OTHER_LETTER, Character.NON_SPACING_MARK,
					Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK, Character.DECIMAL_DIGIT_NUMBER,
					Character.LETTER_NUMBER, Character.OTHER_NUMBER, Character.DASH_PUNCTUATION,
					Character.START_PUNCTUATION, Character.END_PUNCTUATION, Character.CONNECTOR_PUNCTUATION,
					Character.OTHER_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
					Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL,
					Character.SPACE_SEPARATOR ->
				true;
			default -> false;
		};
	}

	private static int[] codePoints(String s) {
		return s.codePoints().toArray();
	}

}
