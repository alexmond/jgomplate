package org.alexmond.jgomplate.functions.ns;

import java.util.UUID;
import java.util.regex.Pattern;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code uuid} namespace. Reached from templates as {@code uuid.V4},
 * {@code uuid.Nil}, {@code uuid.IsValid}, {@code uuid.Parse}. Method names mirror
 * gomplate's Go API (PascalCase).
 *
 * <p>
 * {@code Parse} accepts the four forms gomplate does — the standard
 * {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}, the {@code urn:uuid:} prefix, the
 * Microsoft {@code {…}} braces, and the raw 32-hex-digit form — and returns the canonical
 * lowercase string. {@code V1} is omitted: the JVM has no built-in time-based (version 1)
 * UUID generator, and a substitute would not be faithful.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class UuidNamespace {

	private static final Pattern CANONICAL = Pattern
		.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

	private static final Pattern RAW_HEX = Pattern.compile("[0-9a-fA-F]{32}");

	/** gomplate {@code uuid.V4} — a random (version 4) UUID. */
	public String V4() {
		return UUID.randomUUID().toString();
	}

	/** gomplate {@code uuid.Nil} — the all-zero nil UUID. */
	public String Nil() {
		return "00000000-0000-0000-0000-000000000000";
	}

	/** gomplate {@code uuid.IsValid in} — whether {@code in} parses as a UUID. */
	public boolean IsValid(Object in) {
		try {
			parse(in);
			return true;
		}
		catch (IllegalArgumentException invalid) {
			return false;
		}
	}

	/** gomplate {@code uuid.Parse in} — parse and return the canonical lowercase form. */
	public String Parse(Object in) {
		return parse(in);
	}

	private static String parse(Object in) {
		String text = Values.toString(in).trim();
		if (text.startsWith("urn:uuid:")) {
			text = text.substring("urn:uuid:".length());
		}
		if (text.startsWith("{") && text.endsWith("}")) {
			text = text.substring(1, text.length() - 1);
		}
		if (RAW_HEX.matcher(text).matches()) {
			text = text.substring(0, 8) + '-' + text.substring(8, 12) + '-' + text.substring(12, 16) + '-'
					+ text.substring(16, 20) + '-' + text.substring(20);
		}
		if (!CANONICAL.matcher(text).matches()) {
			throw new IllegalArgumentException("invalid UUID: " + text);
		}
		return UUID.fromString(text).toString();
	}

}
