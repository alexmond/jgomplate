package org.alexmond.jgomplate.functions.ns;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code strings} namespace. Reached from templates as {@code strings.ToUpper},
 * {@code strings.TrimSpace}, etc. — the method names deliberately mirror gomplate's Go
 * API (PascalCase), so the checkstyle {@code MethodName} rule is suppressed for this
 * package.
 *
 * <p>
 * gomplate's affix/separator argument comes <em>first</em> and the operated-on string
 * <em>last</em> (pipeline-friendly): {@code strings.Trim cutset s},
 * {@code strings.HasPrefix prefix s}, {@code strings.Split sep s}. Signatures here match
 * that order exactly.
 *
 * <p>
 * Seed subset of the full ~30-function gomplate {@code strings} namespace. Still missing:
 * the case-format helpers ({@code CamelCase}/{@code SnakeCase}/{@code KebabCase}/
 * {@code Slug}), {@code Title} (needs Unicode title-casing), the quoting helpers
 * ({@code Quote}/{@code Squote}/{@code ShellQuote}), and the variadic ones
 * ({@code Indent}/{@code WordWrap}/{@code RuneCount}/{@code Abbrev}).
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class StringsNamespace {

	public String ToUpper(Object s) {
		return Values.str(s).toUpperCase(Locale.ROOT);
	}

	public String ToLower(Object s) {
		return Values.str(s).toLowerCase(Locale.ROOT);
	}

	public String TrimSpace(Object s) {
		return Values.str(s).strip();
	}

	public boolean Contains(Object substr, Object s) {
		return Values.str(s).contains(Values.str(substr));
	}

	public boolean HasPrefix(Object prefix, Object s) {
		return Values.str(s).startsWith(Values.str(prefix));
	}

	public boolean HasSuffix(Object suffix, Object s) {
		return Values.str(s).endsWith(Values.str(suffix));
	}

	public String Repeat(Object count, Object s) {
		return Values.str(s).repeat(Math.max(0, Values.toInt(count)));
	}

	public String ReplaceAll(Object old, Object replacement, Object s) {
		return Values.str(s).replace(Values.str(old), Values.str(replacement));
	}

	/**
	 * gomplate {@code strings.Trim cutset s} — strip leading and trailing cutset runes.
	 */
	public String Trim(Object cutset, Object s) {
		return trimSet(Values.str(s), Values.str(cutset), true, true);
	}

	/** gomplate {@code strings.TrimLeft cutset s}. */
	public String TrimLeft(Object cutset, Object s) {
		return trimSet(Values.str(s), Values.str(cutset), true, false);
	}

	/** gomplate {@code strings.TrimRight cutset s}. */
	public String TrimRight(Object cutset, Object s) {
		return trimSet(Values.str(s), Values.str(cutset), false, true);
	}

	/** gomplate {@code strings.TrimPrefix prefix s} — drop {@code prefix} if present. */
	public String TrimPrefix(Object prefix, Object s) {
		String text = Values.str(s);
		String pre = Values.str(prefix);
		return text.startsWith(pre) ? text.substring(pre.length()) : text;
	}

	/** gomplate {@code strings.TrimSuffix suffix s} — drop {@code suffix} if present. */
	public String TrimSuffix(Object suffix, Object s) {
		String text = Values.str(s);
		String suf = Values.str(suffix);
		return text.endsWith(suf) ? text.substring(0, text.length() - suf.length()) : text;
	}

	/**
	 * gomplate {@code strings.Trunc length s} — first {@code length} chars ({@code <0} =
	 * all).
	 */
	public String Trunc(Object length, Object s) {
		String text = Values.str(s);
		int len = Values.toInt(length);
		return (len < 0 || len >= text.length()) ? text : text.substring(0, len);
	}

	/** gomplate {@code strings.Split sep s} — literal (non-regex) split. */
	public List<String> Split(Object sep, Object s) {
		return splitLiteral(Values.str(s), Values.str(sep), -1);
	}

	/**
	 * gomplate {@code strings.SplitN sep n s} — literal split into at most {@code n}
	 * parts.
	 */
	public List<String> SplitN(Object sep, Object n, Object s) {
		return splitLiteral(Values.str(s), Values.str(sep), Values.toInt(n));
	}

	private static String trimSet(String text, String cutset, boolean left, boolean right) {
		int start = 0;
		int end = text.length();
		while (left && start < end && cutset.indexOf(text.charAt(start)) >= 0) {
			start++;
		}
		while (right && end > start && cutset.indexOf(text.charAt(end - 1)) >= 0) {
			end--;
		}
		return text.substring(start, end);
	}

	/**
	 * Go {@code strings.Split}/{@code SplitN} semantics: literal separator, trailing
	 * empty fields preserved. {@code limit < 0} splits everything; {@code limit == 0}
	 * yields an empty list; {@code limit > 0} caps the result at {@code limit} parts (the
	 * last part keeps the remainder). An empty separator splits into individual runes.
	 */
	private static List<String> splitLiteral(String text, String sep, int limit) {
		List<String> parts = new ArrayList<>();
		if (limit == 0) {
			return parts;
		}
		if (sep.isEmpty()) {
			int[] runes = text.codePoints().toArray();
			for (int i = 0; i < runes.length; i++) {
				if (limit > 0 && parts.size() == limit - 1) {
					parts.add(new String(runes, i, runes.length - i));
					return parts;
				}
				parts.add(new String(Character.toChars(runes[i])));
			}
			return parts;
		}
		int idx = 0;
		while (!(limit > 0 && parts.size() == limit - 1)) {
			int next = text.indexOf(sep, idx);
			if (next < 0) {
				break;
			}
			parts.add(text.substring(idx, next));
			idx = next + sep.length();
		}
		parts.add(text.substring(idx));
		return parts;
	}

}
