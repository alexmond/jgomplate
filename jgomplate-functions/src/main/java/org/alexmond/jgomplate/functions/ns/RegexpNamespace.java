package org.alexmond.jgomplate.functions.ns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code regexp} namespace — Find, FindAll, Match, Replace, ReplaceLiteral,
 * Split, QuoteMeta. Reached from templates as {@code regexp.Find}, … Method names mirror
 * gomplate's Go API (PascalCase).
 *
 * <p>
 * gomplate uses Go's RE2 engine; this delegates to {@link java.util.regex} with a small
 * compatibility shim for the syntax that differs: RE2 named groups {@code (?P<name>)} are
 * rewritten to Java's {@code (?<name>)}, and RE2 braced numeric replacement refs
 * {@code $}{@code {1}} are rewritten to Java's {@code $1}. {@code QuoteMeta} escapes Go's
 * exact metacharacter set (not {@code \Q…\E}), and {@code Split}'s {@code n} argument is
 * mapped onto Java's split-limit semantics. Common character classes ({@code [a-z]},
 * {@code \s}, {@code \w}) are identical across both engines.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class RegexpNamespace {

	/** Go's regexp metacharacters, each escaped with a backslash by {@code QuoteMeta}. */
	private static final String META = "\\.+*?()|[]{}^$";

	/** gomplate {@code regexp.Find re input} — the leftmost match, or "" if none. */
	public String Find(Object re, Object input) {
		Matcher matcher = compile(re).matcher(Values.toString(input));
		return matcher.find() ? matcher.group() : "";
	}

	/**
	 * gomplate {@code regexp.FindAll re [n] input} — up to n matches (all when n is
	 * negative).
	 */
	public List<String> FindAll(Object... args) {
		String re;
		int n;
		String input;
		if (args.length == 2) {
			re = Values.toString(args[0]);
			n = -1;
			input = Values.toString(args[1]);
		}
		else if (args.length == 3) {
			re = Values.toString(args[0]);
			n = Values.toInt(args[1]);
			input = Values.toString(args[2]);
		}
		else {
			throw new IllegalArgumentException("wrong number of args: want 2 or 3, got " + args.length);
		}
		List<String> out = new ArrayList<>();
		if (n == 0) {
			return out;
		}
		Matcher matcher = compile(re).matcher(input);
		int from = 0;
		while (from <= input.length() && matcher.find(from)) {
			out.add(matcher.group());
			if (n > 0 && out.size() == n) {
				break;
			}
			from = (matcher.end() == matcher.start()) ? matcher.end() + 1 : matcher.end();
		}
		return out;
	}

	/** gomplate {@code regexp.Match re input} — whether the pattern matches anywhere. */
	public boolean Match(Object re, Object input) {
		return compile(re).matcher(Values.toString(input)).find();
	}

	/** gomplate {@code regexp.QuoteMeta in} — backslash-escape Go's metacharacters. */
	public String QuoteMeta(Object in) {
		String s = Values.toString(in);
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (META.indexOf(c) >= 0) {
				buf.append('\\');
			}
			buf.append(c);
		}
		return buf.toString();
	}

	/**
	 * gomplate {@code regexp.Replace re replacement input} — replace all, with
	 * $-expansion.
	 */
	public String Replace(Object re, Object replacement, Object input) {
		String repl = translateReplacement(Values.toString(replacement));
		return compile(re).matcher(Values.toString(input)).replaceAll(repl);
	}

	/**
	 * gomplate {@code regexp.ReplaceLiteral re replacement input} — replace all,
	 * verbatim.
	 */
	public String ReplaceLiteral(Object re, Object replacement, Object input) {
		String repl = Matcher.quoteReplacement(Values.toString(replacement));
		return compile(re).matcher(Values.toString(input)).replaceAll(repl);
	}

	/**
	 * gomplate {@code regexp.Split re [n] input} — split by the pattern (all when n is
	 * negative).
	 */
	public List<String> Split(Object... args) {
		String re;
		int n;
		String input;
		if (args.length == 2) {
			re = Values.toString(args[0]);
			n = -1;
			input = Values.toString(args[1]);
		}
		else if (args.length == 3) {
			re = Values.toString(args[0]);
			n = Values.toInt(args[1]);
			input = Values.toString(args[2]);
		}
		else {
			throw new IllegalArgumentException("wrong number of args: want 2 or 3, got " + args.length);
		}
		if (n == 0) {
			return new ArrayList<>();
		}
		int limit = (n < 0) ? -1 : n;
		return new ArrayList<>(Arrays.asList(compile(re).split(input, limit)));
	}

	private static Pattern compile(Object re) {
		return Pattern.compile(translatePattern(Values.toString(re)));
	}

	/** RE2 named-group syntax {@code (?P<name>)} → Java {@code (?<name>)}. */
	private static String translatePattern(String re) {
		return re.replace("(?P<", "(?<");
	}

	/**
	 * RE2 braced numeric refs {@code $}{@code {1}} → Java {@code $1} (Java braces are
	 * names).
	 */
	private static String translateReplacement(String repl) {
		return repl.replaceAll("\\$\\{(\\d+)\\}", "\\$$1");
	}

}
