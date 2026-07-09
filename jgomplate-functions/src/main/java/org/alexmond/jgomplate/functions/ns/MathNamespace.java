package org.alexmond.jgomplate.functions.ns;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code math} namespace. Reached from templates as {@code math.Add},
 * {@code math.Abs}, etc. Method names mirror gomplate's Go API (PascalCase).
 *
 * <p>
 * The variadic reducers {@code Add}, {@code Mul}, {@code Max}, {@code Min}, and the
 * sequence generator {@code Seq} are exposed as Java varargs; gotmpl4j 1.2.1+ unpacks
 * them into the method call. {@code Add}/{@code Mul} stay integral unless any argument is
 * a float; {@code Max}/{@code Min} take a mandatory first argument plus a variadic tail.
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

	public Object Add(Object... nums) {
		if (anyFloat(nums)) {
			double sum = 0;
			for (Object n : nums) {
				sum += Values.toDouble(n);
			}
			return normalize(sum);
		}
		long sum = 0;
		for (Object n : nums) {
			sum += Values.toLong(n);
		}
		return sum;
	}

	public Object Mul(Object... nums) {
		if (anyFloat(nums)) {
			double product = 1;
			for (Object n : nums) {
				product *= Values.toDouble(n);
			}
			return normalize(product);
		}
		long product = 1;
		for (Object n : nums) {
			product *= Values.toLong(n);
		}
		return product;
	}

	public Object Max(Object first, Object... rest) {
		if (IsFloat(first) || anyFloat(rest)) {
			double m = Values.toDouble(first);
			for (Object n : rest) {
				m = Math.max(m, Values.toDouble(n));
			}
			return normalize(m);
		}
		long m = Values.toLong(first);
		for (Object n : rest) {
			m = Math.max(m, Values.toLong(n));
		}
		return m;
	}

	public Object Min(Object first, Object... rest) {
		if (IsFloat(first) || anyFloat(rest)) {
			double m = Values.toDouble(first);
			for (Object n : rest) {
				m = Math.min(m, Values.toDouble(n));
			}
			return normalize(m);
		}
		long m = Values.toLong(first);
		for (Object n : rest) {
			m = Math.min(m, Values.toLong(n));
		}
		return m;
	}

	/**
	 * gomplate {@code math.Seq [start] end [step]} — an inclusive integer sequence.
	 * {@code start} and {@code step} default to 1; a mis-signed step is corrected and a
	 * zero step yields an empty list, matching gomplate.
	 */
	public List<Long> Seq(Object... args) {
		long start = 1;
		long end;
		long step = 1;
		switch (args.length) {
			case 1 -> {
				end = Values.toLong(args[0]);
			}
			case 2 -> {
				start = Values.toLong(args[0]);
				end = Values.toLong(args[1]);
			}
			case 3 -> {
				start = Values.toLong(args[0]);
				end = Values.toLong(args[1]);
				step = Values.toLong(args[2]);
			}
			default -> throw new IllegalArgumentException("expected 1, 2, or 3 arguments, got " + args.length);
		}
		return seq(start, end, step);
	}

	private static List<Long> seq(long start, long end, long step) {
		List<Long> result = new ArrayList<>();
		if (step == 0) {
			return result;
		}
		if ((end < start && step > 0) || (end > start && step < 0)) {
			step = -step;
		}
		// align the end exactly so the walk terminates
		end -= (end - start) % step;
		long last = start;
		result.add(last);
		while (last != end) {
			last += step;
			result.add(last);
		}
		return result;
	}

	private boolean anyFloat(Object... nums) {
		for (Object n : nums) {
			if (IsFloat(n)) {
				return true;
			}
		}
		return false;
	}

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
