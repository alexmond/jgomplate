package org.alexmond.jgomplate.functions.ns;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code math} namespace. Reached from templates as {@code math.Add},
 * {@code math.Abs}, etc. Method names mirror gomplate's Go API (PascalCase).
 *
 * <p>
 * Only the unary/binary functions are exposed: gomplate's {@code Add}, {@code Mul},
 * {@code Max}, {@code Min}, and {@code Seq} are variadic, and variadic namespace methods
 * are not yet callable through gotmpl4j (it matches methods by exact parameter count and
 * does not unpack Java varargs). Build sums/products with those once upstream varargs
 * support lands.
 *
 * <p>
 * Numeric type is preserved the way gomplate does: {@code Abs}/{@code Sub}/{@code Pow}
 * stay integral when their inputs are integral, {@code Div} is always floating-point, and
 * {@code Rem} is integer. Whole-valued results are returned as a {@code long} so they
 * render without a trailing {@code .0}, matching Go's rendering of an integral
 * {@code float64}.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class MathNamespace {

	public boolean IsInt(Object n) {
		if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte
				|| n instanceof BigInteger) {
			return true;
		}
		if (n instanceof String text) {
			try {
				Long.decode(text.trim());
				return true;
			}
			catch (NumberFormatException ignored) {
				return false;
			}
		}
		return false;
	}

	public boolean IsFloat(Object n) {
		if (n instanceof Float || n instanceof Double || n instanceof BigDecimal) {
			return true;
		}
		if (n instanceof String text) {
			try {
				Double.parseDouble(text.trim());
			}
			catch (NumberFormatException ignored) {
				return false;
			}
			// a string that also parses as an integer is an int, not a float
			return !IsInt(n);
		}
		return false;
	}

	public boolean IsNum(Object n) {
		return IsInt(n) || IsFloat(n);
	}

	public Object Abs(Object n) {
		double m = Math.abs(Values.toDouble(n));
		if (IsInt(n)) {
			return (long) m;
		}
		return normalize(m);
	}

	public Object Sub(Object a, Object b) {
		if (IsFloat(a) || IsFloat(b)) {
			return normalize(Values.toDouble(a) - Values.toDouble(b));
		}
		return Values.toLong(a) - Values.toLong(b);
	}

	public Object Div(Object a, Object b) {
		double dividend = Values.toDouble(b);
		if (dividend == 0) {
			throw new IllegalArgumentException("error: division by 0");
		}
		return normalize(Values.toDouble(a) / dividend);
	}

	public long Rem(Object a, Object b) {
		return Values.toLong(a) % Values.toLong(b);
	}

	public Object Pow(Object a, Object b) {
		double r = Math.pow(Values.toDouble(a), Values.toDouble(b));
		if (IsFloat(a)) {
			return normalize(r);
		}
		return (long) r;
	}

	public Object Ceil(Object n) {
		return normalize(Math.ceil(Values.toDouble(n)));
	}

	public Object Floor(Object n) {
		return normalize(Math.floor(Values.toDouble(n)));
	}

	public Object Round(Object n) {
		double d = Values.toDouble(n);
		// gomplate uses Go's math.Round — round half away from zero, unlike Java's
		// Math.round (half up toward +inf).
		double r = Math.signum(d) * Math.floor(Math.abs(d) + 0.5);
		return normalize(r);
	}

	/**
	 * Render a whole-valued double as a {@code long} (so it prints without {@code .0})
	 * and leave fractional or non-finite values as a {@code double} — mirroring how Go
	 * renders a {@code float64}.
	 */
	private static Object normalize(double value) {
		if (Double.isFinite(value) && value == Math.floor(value) && Math.abs(value) < 9.007199254740992E15) {
			return (long) value;
		}
		return value;
	}

}
