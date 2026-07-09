package org.alexmond.jgomplate.functions.ns;

import java.util.HashMap;
import java.util.Map;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code env} namespace (process environment). Reached from templates as
 * {@code env.Getenv}, {@code env.ExpandEnv}, etc. Method names mirror gomplate's Go API
 * (PascalCase).
 *
 * <p>
 * {@code Getenv} is declared variadic by gomplate ({@code Getenv key [default]}); it is
 * exposed here as fixed 1- and 2-arg overloads so both shapes are callable. gomplate's
 * {@code <KEY>_FILE} indirection convention is not implemented.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class EnvNamespace {

	/** gomplate {@code env.Env} — all environment variables as a map. */
	public Map<String, String> Env() {
		return new HashMap<>(System.getenv());
	}

	/** gomplate {@code env.Getenv key} — the variable's value, or empty if unset. */
	public String Getenv(Object key) {
		String value = System.getenv(Values.toString(key));
		return (value != null) ? value : "";
	}

	/**
	 * gomplate {@code env.Getenv key default} — the variable's value, or {@code default}.
	 */
	public String Getenv(Object key, Object def) {
		String value = System.getenv(Values.toString(key));
		return (value != null) ? value : Values.str(def);
	}

	/** gomplate {@code env.HasEnv key} — whether the variable is set. */
	public boolean HasEnv(Object key) {
		return System.getenv(Values.toString(key)) != null;
	}

	/**
	 * gomplate {@code env.ExpandEnv s} — expand {@code $VAR} / {@code ${VAR}} references.
	 */
	public String ExpandEnv(Object s) {
		return expand(Values.toString(s));
	}

	private static String expand(String text) {
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '$' && i + 1 < text.length()) {
				if (text.charAt(i + 1) == '{') {
					int end = text.indexOf('}', i + 2);
					if (end >= 0) {
						out.append(lookup(text.substring(i + 2, end)));
						i = end + 1;
						continue;
					}
				}
				int j = i + 1;
				while (j < text.length() && isNameChar(text.charAt(j))) {
					j++;
				}
				if (j > i + 1) {
					out.append(lookup(text.substring(i + 1, j)));
					i = j;
					continue;
				}
			}
			out.append(c);
			i++;
		}
		return out.toString();
	}

	private static String lookup(String name) {
		String value = System.getenv(name);
		return (value != null) ? value : "";
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

}
