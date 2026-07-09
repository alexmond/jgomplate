package org.alexmond.jgomplate.functions.ns;

import java.util.Locale;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code strings} namespace. Reached from templates as {@code strings.ToUpper},
 * {@code strings.TrimSpace}, etc. — the method names deliberately mirror gomplate's Go
 * API (PascalCase), so the checkstyle {@code MethodName} rule is suppressed for this
 * package.
 *
 * <p>
 * This is a seed subset; the full gomplate {@code strings} namespace has ~30 functions.
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

	public String Trim(Object s, Object cutset) {
		String text = Values.str(s);
		String cut = Values.str(cutset);
		int start = 0;
		int end = text.length();
		while (start < end && cut.indexOf(text.charAt(start)) >= 0) {
			start++;
		}
		while (end > start && cut.indexOf(text.charAt(end - 1)) >= 0) {
			end--;
		}
		return text.substring(start, end);
	}

	public String Repeat(Object count, Object s) {
		return Values.str(s).repeat(Math.max(0, Values.toInt(count)));
	}

	public String ReplaceAll(Object old, Object replacement, Object s) {
		return Values.str(s).replace(Values.str(old), Values.str(replacement));
	}

	public boolean Contains(Object substr, Object s) {
		return Values.str(s).contains(Values.str(substr));
	}

}
