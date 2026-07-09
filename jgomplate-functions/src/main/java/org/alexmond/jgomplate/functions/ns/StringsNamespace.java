package org.alexmond.jgomplate.functions.ns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
 * Still missing from the full gomplate {@code strings} namespace: {@code Slug} (needs a
 * transliterating slug library), and {@code WordWrap}/{@code Abbrev} (need the exact
 * {@code goutils} wrapping/abbreviation algorithms). {@code Trunc}/{@code Split} are
 * character-based rather than byte-based.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class StringsNamespace {

	private static final Pattern NON_ALPHA_NUM = Pattern.compile("[^\\p{L}\\p{N}]+");

	private static final Pattern SPACES = Pattern.compile("\\s+");

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

	/**
	 * gomplate {@code strings.Indent [width] [indent] s} — prefix each line of {@code s}
	 * with {@code indent} repeated {@code width} times. Defaults: {@code width} 1,
	 * {@code indent} a single space. The last argument is always the input string.
	 */
	public String Indent(Object... args) {
		if (args.length == 0 || args.length > 3) {
			throw new IllegalArgumentException("expected 1, 2, or 3 args, got " + args.length);
		}
		String indent = " ";
		int width = 1;
		if (args.length == 2) {
			if (args[0] instanceof String str) {
				indent = str;
			}
			else {
				width = Values.toInt(args[0]);
			}
		}
		else if (args.length == 3) {
			width = Values.toInt(args[0]);
			indent = Values.toString(args[1]);
		}
		return indentString(width, indent, Values.toString(args[args.length - 1]));
	}

	/**
	 * gomplate {@code strings.RuneCount in…} — count of Unicode runes across all args.
	 */
	public int RuneCount(Object... args) {
		StringBuilder sb = new StringBuilder();
		for (Object arg : args) {
			sb.append(Values.toString(arg));
		}
		return (int) sb.toString().codePoints().count();
	}

	/**
	 * gomplate {@code strings.Title s} — upshift the first letter of each word without
	 * lowering the rest (Go {@code cases.Title} with {@code NoLower}).
	 */
	public String Title(Object s) {
		String text = Values.toString(s);
		StringBuilder out = new StringBuilder(text.length());
		boolean startOfWord = true;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				out.append(startOfWord ? Character.toTitleCase(c) : c);
				startOfWord = false;
			}
			else {
				out.append(c);
				startOfWord = true;
			}
		}
		return out.toString();
	}

	/**
	 * gomplate {@code strings.SnakeCase in} — e.g. {@code "Hello, World!"} →
	 * {@code Hello_world}.
	 */
	public String SnakeCase(Object in) {
		return SPACES.matcher(casePrepare(Values.toString(in))).replaceAll("_");
	}

	/**
	 * gomplate {@code strings.KebabCase in} — e.g. {@code "Hello, World!"} →
	 * {@code Hello-world}.
	 */
	public String KebabCase(Object in) {
		return SPACES.matcher(casePrepare(Values.toString(in))).replaceAll("-");
	}

	/**
	 * gomplate {@code strings.CamelCase in} — e.g. {@code "Hello, World!"} →
	 * {@code HelloWorld}.
	 */
	public String CamelCase(Object in) {
		String input = Values.toString(in).strip();
		if (input.isEmpty()) {
			return "";
		}
		String titled = Title(input);
		// restore the first character's original case, as gomplate does
		titled = input.charAt(0) + titled.substring(1);
		return NON_ALPHA_NUM.matcher(titled).replaceAll("");
	}

	/**
	 * gomplate {@code strings.Quote in} — a double-quoted Go string literal ({@code %q}).
	 */
	public String Quote(Object in) {
		String s = Values.toString(in);
		StringBuilder b = new StringBuilder(s.length() + 2);
		b.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> b.append("\\\"");
				case '\\' -> b.append("\\\\");
				case '\n' -> b.append("\\n");
				case '\t' -> b.append("\\t");
				case '\r' -> b.append("\\r");
				default -> b.append(c);
			}
		}
		b.append('"');
		return b.toString();
	}

	/**
	 * gomplate {@code strings.Squote in} — a single-quoted literal; embedded {@code '}
	 * doubled.
	 */
	public String Squote(Object in) {
		return "'" + Values.toString(in).replace("'", "''") + "'";
	}

	/**
	 * gomplate {@code strings.ShellQuote in} — a POSIX shell literal. A list is quoted
	 * element-wise and space-joined.
	 */
	public String ShellQuote(Object in) {
		if (in instanceof Collection<?> || in instanceof Object[]) {
			StringBuilder sb = new StringBuilder();
			List<Object> items = Values.toList(in);
			for (int i = 0; i < items.size(); i++) {
				if (i > 0) {
					sb.append(' ');
				}
				sb.append(shellQuoteOne(Values.toString(items.get(i))));
			}
			return sb.toString();
		}
		return shellQuoteOne(Values.toString(in));
	}

	private static String shellQuoteOne(String s) {
		return "'" + s.replace("'", "'\"'\"'") + "'";
	}

	private static String casePrepare(String raw) {
		String in = raw.strip();
		if (in.isEmpty()) {
			return "";
		}
		String s = in.toLowerCase(Locale.ROOT);
		// restore the original first character (gomplate keeps its case)
		s = in.charAt(0) + s.substring(1);
		return NON_ALPHA_NUM.matcher(s).replaceAll(" ").strip();
	}

	private static String indentString(int width, String indent, String s) {
		if (width <= 0) {
			throw new IllegalArgumentException("width must be > 0");
		}
		if (indent.contains("\n")) {
			throw new IllegalArgumentException("indent must not contain '\\n'");
		}
		String prefix = (width > 1) ? indent.repeat(width) : indent;
		StringBuilder res = new StringBuilder();
		boolean bol = true;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (bol && c != '\n') {
				res.append(prefix);
			}
			res.append(c);
			bol = c == '\n';
		}
		return res.toString();
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
