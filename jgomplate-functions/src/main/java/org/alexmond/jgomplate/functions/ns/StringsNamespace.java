package org.alexmond.jgomplate.functions.ns;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * {@code WordWrap}/{@code Abbrev} port Masterminds' {@code goutils} algorithms verbatim,
 * and {@code Slug} ports gosimple/slug's English pipeline. {@code Trunc}/{@code Split}/
 * {@code WordWrap}/{@code Abbrev} index by character rather than byte, and {@code Slug}
 * approximates {@code unidecode} for non-ASCII input — so ASCII matches gomplate exactly
 * while multibyte/exotic-script input may differ.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class StringsNamespace {

	private static final Pattern NON_ALPHA_NUM = Pattern.compile("[^\\p{L}\\p{N}]+");

	private static final Pattern SPACES = Pattern.compile("\\s+");

	private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

	private static final Pattern SLUG_NON_AUTH = Pattern.compile("[^a-z0-9_-]");

	private static final Pattern SLUG_DASHES = Pattern.compile("-+");

	/**
	 * Latin letters that do not decompose under Unicode NFD; mapped to the same ASCII the
	 * gosimple {@code unidecode} transliteration gomplate uses would produce.
	 */
	private static final Map<Character, String> TRANSLIT = Map.ofEntries(Map.entry('ß', "ss"), Map.entry('æ', "ae"),
			Map.entry('Æ', "AE"), Map.entry('œ', "oe"), Map.entry('Œ', "OE"), Map.entry('ø', "o"), Map.entry('Ø', "O"),
			Map.entry('đ', "d"), Map.entry('Đ', "D"), Map.entry('ð', "d"), Map.entry('Ð', "D"), Map.entry('þ', "th"),
			Map.entry('Þ', "TH"), Map.entry('ł', "l"), Map.entry('Ł', "L"), Map.entry('ı', "i"), Map.entry('ħ', "h"));

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
	 * gomplate {@code strings.WordWrap [width] [lbseq] s} — insert line breaks so no line
	 * exceeds {@code width} (default 80) using {@code lbseq} (default {@code \n}). Long
	 * words are left intact (extended past the limit), matching gomplate.
	 */
	public String WordWrap(Object... args) {
		if (args.length == 0 || args.length > 3) {
			throw new IllegalArgumentException("expected 1, 2, or 3 args, got " + args.length);
		}
		long width = 0;
		String lbSeq = "";
		if (args.length == 2) {
			if (args[0] instanceof String str) {
				lbSeq = str;
			}
			else {
				width = Values.toLong(args[0]);
			}
		}
		else if (args.length == 3) {
			width = Values.toLong(args[0]);
			lbSeq = Values.toString(args[1]);
		}
		String input = Values.toString(args[args.length - 1]);
		int w = (width == 0) ? 80 : (int) width;
		if (w == -1) {
			w = Integer.MAX_VALUE;
		}
		return wrapCustom(input, w, lbSeq.isEmpty() ? "\n" : lbSeq, false);
	}

	/**
	 * gomplate {@code strings.Abbrev [offset] width s} — abbreviate {@code s} with an
	 * ellipsis so the result is at most {@code width} characters, optionally keeping the
	 * {@code offset} position visible. Ports goutils' {@code AbbreviateFull}.
	 */
	public String Abbrev(Object... args) {
		int offset = 0;
		int width;
		String str;
		if (args.length == 2) {
			width = Values.toInt(args[0]);
			str = Values.toString(args[1]);
		}
		else if (args.length == 3) {
			offset = Values.toInt(args[0]);
			width = Values.toInt(args[1]);
			str = Values.toString(args[2]);
		}
		else {
			throw new IllegalArgumentException("abbrev requires a 'width' and 'input' argument");
		}
		if (str.length() <= width) {
			return str;
		}
		return abbreviateFull(str, offset, width);
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
	 * gomplate {@code strings.Slug in} — a URL-friendly slug. Ports gosimple/slug's
	 * English pipeline: substitute {@code &}→{@code and} / {@code @}→{@code at},
	 * transliterate to ASCII, lowercase, replace runs of non-{@code [a-z0-9_-]} with
	 * {@code -}, and trim.
	 *
	 * <p>
	 * ASCII input matches gomplate exactly; non-ASCII transliteration approximates
	 * gosimple's {@code unidecode} via Unicode NFD plus a small Latin map, so common
	 * accented Latin text matches but exotic scripts may differ.
	 */
	public String Slug(Object in) {
		String s = Values.toString(in).strip();
		s = s.replace("&", "and").replace("@", "at");
		s = transliterate(s);
		s = s.toLowerCase(Locale.ROOT);
		s = SLUG_NON_AUTH.matcher(s).replaceAll("-");
		s = SLUG_DASHES.matcher(s).replaceAll("-");
		int start = 0;
		int end = s.length();
		while (start < end && (s.charAt(start) == '-' || s.charAt(start) == '_')) {
			start++;
		}
		while (end > start && (s.charAt(end - 1) == '-' || s.charAt(end - 1) == '_')) {
			end--;
		}
		return s.substring(start, end);
	}

	private static String transliterate(String s) {
		String decomposed = COMBINING_MARKS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("");
		StringBuilder out = new StringBuilder(decomposed.length());
		for (int i = 0; i < decomposed.length(); i++) {
			char c = decomposed.charAt(i);
			String mapped = TRANSLIT.get(c);
			if (mapped != null) {
				out.append(mapped);
			}
			else {
				out.append(c);
			}
		}
		return out.toString();
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

	/** Port of goutils {@code WrapCustom} (char-indexed). */
	private static String wrapCustom(String str, int wrapLength, String newLineStr, boolean wrapLongWords) {
		if (str.isEmpty()) {
			return "";
		}
		int width = (wrapLength < 1) ? 1 : wrapLength;
		int inputLineLength = str.length();
		int offset = 0;
		StringBuilder wrapped = new StringBuilder();
		while (inputLineLength - offset > width) {
			if (str.charAt(offset) == ' ') {
				offset++;
				continue;
			}
			int end = width + offset + 1;
			int spaceToWrapAt = str.substring(offset, end).lastIndexOf(' ') + offset;
			if (spaceToWrapAt >= offset) {
				wrapped.append(str, offset, spaceToWrapAt).append(newLineStr);
				offset = spaceToWrapAt + 1;
			}
			else if (wrapLongWords) {
				wrapped.append(str, offset, width + offset).append(newLineStr);
				offset += width;
			}
			else {
				int longEnd = width + offset;
				int index = str.substring(longEnd).indexOf(' ');
				if (index == -1) {
					wrapped.append(str.substring(offset));
					offset = inputLineLength;
				}
				else {
					spaceToWrapAt = index + longEnd;
					wrapped.append(str, offset, spaceToWrapAt).append(newLineStr);
					offset = spaceToWrapAt + 1;
				}
			}
		}
		wrapped.append(str.substring(offset));
		return wrapped.toString();
	}

	/** Port of goutils {@code AbbreviateFull} (char-indexed). */
	private static String abbreviateFull(String str, int offset, int maxWidth) {
		if (str.isEmpty()) {
			return "";
		}
		if (maxWidth < 4) {
			throw new IllegalArgumentException("Minimum abbreviation width is 4");
		}
		if (str.length() <= maxWidth) {
			return str;
		}
		int off = Math.min(offset, str.length());
		if (str.length() - off < maxWidth - 3) {
			off = str.length() - (maxWidth - 3);
		}
		String marker = "...";
		if (off <= 4) {
			return str.substring(0, maxWidth - 3) + marker;
		}
		if (maxWidth < 7) {
			throw new IllegalArgumentException("Minimum abbreviation width with offset is 7");
		}
		if (off + maxWidth - 3 < str.length()) {
			return marker + abbreviateFull(str.substring(off), 0, maxWidth - 3);
		}
		return marker + str.substring(str.length() - (maxWidth - 3));
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
