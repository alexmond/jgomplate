package org.alexmond.jgomplate.functions.ns;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code test} namespace — assertions and value checks that can fail template
 * rendering. Reached from templates as {@code test.Assert}, {@code test.Ternary},
 * {@code test.Required}, {@code test.Kind}, {@code test.IsKind}, {@code test.Fail}.
 * Method names mirror gomplate's Go API (PascalCase).
 *
 * <p>
 * {@code Kind}/{@code IsKind} map Java runtime types onto Go's reflect-kind names
 * ({@code string}, {@code bool}, {@code int}, {@code float64}, {@code slice},
 * {@code map}, {@code invalid} for nil); Go-specific widths ({@code int64} vs
 * {@code int}) and {@code struct}/{@code uint}/{@code complex} kinds have no JVM
 * equivalent, so only the common kinds and the {@code "number"} pseudo-kind are
 * supported.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class TestNamespace {

	/** gomplate {@code test.Ternary tval fval b} — the condition is the LAST argument. */
	public Object Ternary(Object tval, Object fval, Object b) {
		return Values.toBool(b) ? tval : fval;
	}

	/**
	 * gomplate {@code test.Assert [message] value} — throws when {@code value} is falsy.
	 */
	public String Assert(Object... args) {
		if (args.length < 1 || args.length > 2) {
			throw new IllegalArgumentException("wrong number of args: want 1 or 2, got " + args.length);
		}
		if (Values.toBool(args[args.length - 1])) {
			return "";
		}
		String message = (args.length == 2) ? Values.toString(args[0]) : "";
		throw new IllegalStateException(message.isEmpty() ? "assertion failed" : "assertion failed: " + message);
	}

	/** gomplate {@code test.Fail [message]} — always fails rendering. */
	public String Fail(Object... args) {
		if (args.length > 1) {
			throw new IllegalArgumentException("wrong number of args: want 0 or 1, got " + args.length);
		}
		String message = (args.length == 1) ? Values.toString(args[0]) : "";
		throw new IllegalStateException(
				message.isEmpty() ? "template generation failed" : "template generation failed: " + message);
	}

	/**
	 * gomplate {@code test.Required [message] value} — throws when {@code value} is nil
	 * or an empty string; a numeric {@code 0} or {@code false} passes.
	 */
	public Object Required(Object... args) {
		if (args.length < 1 || args.length > 2) {
			throw new IllegalArgumentException("wrong number of args: want 1 or 2, got " + args.length);
		}
		String message = (args.length == 2) ? Values.toString(args[0]) : "";
		Object value = (args.length == 2) ? args[1] : args[0];
		if (message.isEmpty()) {
			message = "can not render template: a required value was not set";
		}
		if (value == null || (value instanceof String s && s.isEmpty())) {
			throw new IllegalStateException(message);
		}
		return value;
	}

	/** gomplate {@code test.Kind arg} — the Go reflect-kind name of the argument. */
	public String Kind(Object arg) {
		return kindOf(arg);
	}

	/**
	 * gomplate {@code test.IsKind kind arg} — whether arg's kind matches (or is numeric).
	 */
	public boolean IsKind(Object kind, Object arg) {
		String want = Values.toString(kind);
		String actual = kindOf(arg);
		if ("number".equals(want) && isNumberKind(actual)) {
			return true;
		}
		return actual.equals(want);
	}

	private static String kindOf(Object arg) {
		if (arg == null) {
			return "invalid";
		}
		if (arg instanceof String) {
			return "string";
		}
		if (arg instanceof Boolean) {
			return "bool";
		}
		if (arg instanceof Float || arg instanceof Double || arg instanceof BigDecimal) {
			return "float64";
		}
		if (arg instanceof Long || arg instanceof Integer || arg instanceof Short || arg instanceof Byte
				|| arg instanceof BigInteger) {
			return "int";
		}
		if (arg instanceof Map) {
			return "map";
		}
		if (arg instanceof List || arg instanceof Object[]) {
			return "slice";
		}
		return "invalid";
	}

	private static boolean isNumberKind(String kind) {
		return "int".equals(kind) || "float64".equals(kind);
	}

}
