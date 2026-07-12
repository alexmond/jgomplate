package org.alexmond.jgomplate.functions.ns;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code time} namespace — the deterministic, offline subset: duration
 * constructors ({@code Nanosecond}…{@code Hour}), {@code ParseDuration}, and
 * {@code Parse} (with a {@link GoTime#Format(Object) Format} method). Reached from
 * templates as {@code time.Second}, {@code time.Parse}, … Method names mirror gomplate's
 * Go API (PascalCase).
 *
 * <p>
 * Durations render in Go's {@code time.Duration.String()} form ({@code 1h30m0s},
 * {@code 1.5s}, {@code 1µs}), not Java's ISO-8601. {@code Parse}/{@code Format} translate
 * Go's reference-time layout strings ({@code 2006-01-02 15:04:05}) to Java
 * {@link DateTimeFormatter} patterns. The clock/zone-dependent functions
 * ({@code Now}/{@code Since}/{@code Until}/{@code ZoneName}/{@code ZoneOffset}) are
 * intentionally omitted — their output is not reproducible.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class TimeNamespace {

	private static final long NANOS_PER_MICRO = 1_000L;

	private static final long NANOS_PER_MILLI = 1_000_000L;

	private static final long NANOS_PER_SECOND = 1_000_000_000L;

	private static final long NANOS_PER_MINUTE = 60_000_000_000L;

	private static final long NANOS_PER_HOUR = 3_600_000_000_000L;

	private static final Pattern DURATION = Pattern.compile("(\\d+\\.?\\d*)(ns|us|µs|ms|s|m|h)");

	/** gomplate {@code time.Nanosecond n} — a duration of n nanoseconds. */
	public GoDuration Nanosecond(Object n) {
		return new GoDuration(Values.toLong(n));
	}

	/** gomplate {@code time.Microsecond n} — a duration of n microseconds. */
	public GoDuration Microsecond(Object n) {
		return new GoDuration(Values.toLong(n) * NANOS_PER_MICRO);
	}

	/** gomplate {@code time.Millisecond n} — a duration of n milliseconds. */
	public GoDuration Millisecond(Object n) {
		return new GoDuration(Values.toLong(n) * NANOS_PER_MILLI);
	}

	/** gomplate {@code time.Second n} — a duration of n seconds. */
	public GoDuration Second(Object n) {
		return new GoDuration(Values.toLong(n) * NANOS_PER_SECOND);
	}

	/** gomplate {@code time.Minute n} — a duration of n minutes. */
	public GoDuration Minute(Object n) {
		return new GoDuration(Values.toLong(n) * NANOS_PER_MINUTE);
	}

	/** gomplate {@code time.Hour n} — a duration of n hours. */
	public GoDuration Hour(Object n) {
		return new GoDuration(Values.toLong(n) * NANOS_PER_HOUR);
	}

	/**
	 * gomplate {@code time.ParseDuration s} — parse a Go duration string (e.g. 1h30m).
	 */
	public GoDuration ParseDuration(Object s) {
		return new GoDuration(parseGoDuration(Values.toString(s)));
	}

	/**
	 * gomplate {@code time.Parse layout value} — parse a time using a Go layout string.
	 */
	public GoTime Parse(Object layout, Object value) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(translateLayout(Values.toString(layout)));
		return new GoTime(formatter.parse(Values.toString(value)));
	}

	private static long parseGoDuration(String s) {
		boolean neg = s.startsWith("-");
		String body = (neg || s.startsWith("+")) ? s.substring(1) : s;
		Matcher matcher = DURATION.matcher(body);
		long total = 0;
		int end = 0;
		boolean any = false;
		while (matcher.find()) {
			if (matcher.start() != end) {
				break;
			}
			double value = Double.parseDouble(matcher.group(1));
			total += Math.round(value * unitNanos(matcher.group(2)));
			end = matcher.end();
			any = true;
		}
		if (!any || end != body.length()) {
			throw new IllegalArgumentException("invalid duration: " + s);
		}
		return neg ? -total : total;
	}

	private static long unitNanos(String unit) {
		return switch (unit) {
			case "ns" -> 1L;
			case "us", "µs" -> NANOS_PER_MICRO;
			case "ms" -> NANOS_PER_MILLI;
			case "s" -> NANOS_PER_SECOND;
			case "m" -> NANOS_PER_MINUTE;
			default -> NANOS_PER_HOUR;
		};
	}

	/** Go reference-time layout tokens, longest first, mapped to Java pattern letters. */
	private static final String[][] LAYOUT_TOKENS = { { "January", "MMMM" }, { "Monday", "EEEE" },
			{ ".000000", ".SSSSSS" }, { ".000", ".SSS" }, { "2006", "yyyy" }, { "Jan", "MMM" }, { "Mon", "EEE" },
			{ "MST", "zzz" }, { "-07:00", "xxx" }, { "-0700", "xx" }, { "15", "HH" }, { "06", "yy" }, { "01", "MM" },
			{ "02", "dd" }, { "03", "hh" }, { "04", "mm" }, { "05", "ss" }, { "PM", "a" }, { "pm", "a" }, { "1", "M" },
			{ "2", "d" }, { "3", "h" }, { "4", "m" }, { "5", "s" } };

	private static String translateLayout(String layout) {
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < layout.length()) {
			String[] match = matchToken(layout, i);
			if (match != null) {
				out.append(match[1]);
				i += match[0].length();
			}
			else {
				char c = layout.charAt(i);
				if (Character.isLetter(c)) {
					out.append('\'').append(c).append('\'');
				}
				else {
					out.append(c);
				}
				i++;
			}
		}
		return out.toString();
	}

	private static String[] matchToken(String layout, int pos) {
		for (String[] token : LAYOUT_TOKENS) {
			if (layout.startsWith(token[0], pos)) {
				return token;
			}
		}
		return null;
	}

	// port of Go's time.Duration.String()
	private static String formatGoDuration(long d) {
		if (d == 0) {
			return "0s";
		}
		boolean neg = d < 0;
		long u = Math.abs(d);
		StringBuilder body = new StringBuilder();
		if (u < NANOS_PER_SECOND) {
			int prec;
			String unit;
			if (u < NANOS_PER_MICRO) {
				prec = 0;
				unit = "ns";
			}
			else if (u < NANOS_PER_MILLI) {
				prec = 3;
				unit = "µs";
			}
			else {
				prec = 6;
				unit = "ms";
			}
			body.append(fmtFrac(u, prec)).append(unit);
		}
		else {
			long secs = u / NANOS_PER_SECOND;
			String frac = fracPart(u % NANOS_PER_SECOND, 9);
			long s = secs % 60;
			long totalMin = secs / 60;
			if (totalMin > 0) {
				long min = totalMin % 60;
				long hours = totalMin / 60;
				if (hours > 0) {
					body.append(hours).append('h');
				}
				body.append(min).append('m');
			}
			body.append(s).append(frac).append('s');
		}
		return neg ? "-" + body : body.toString();
	}

	private static String fmtFrac(long value, int prec) {
		if (prec == 0) {
			return Long.toString(value);
		}
		long scale = (long) Math.pow(10, prec);
		return value / scale + fracPart(value % scale, prec);
	}

	private static String fracPart(long frac, int width) {
		if (frac == 0) {
			return "";
		}
		String digits = String.format("%0" + width + "d", frac);
		int end = digits.length();
		while (end > 0 && digits.charAt(end - 1) == '0') {
			end--;
		}
		return "." + digits.substring(0, end);
	}

	/**
	 * A Go {@code time.Duration}; renders like Go's {@code String()} (e.g.
	 * {@code 1h30m0s}).
	 */
	public static final class GoDuration {

		private final long nanos;

		GoDuration(long nanos) {
			this.nanos = nanos;
		}

		/**
		 * gomplate {@code .Nanoseconds} — the duration as a whole number of nanoseconds.
		 */
		public long Nanoseconds() {
			return this.nanos;
		}

		/**
		 * gomplate {@code .Seconds} — the duration as a floating-point number of seconds.
		 */
		public double Seconds() {
			return (double) this.nanos / NANOS_PER_SECOND;
		}

		/**
		 * gomplate {@code .Minutes} — the duration as a floating-point number of minutes.
		 */
		public double Minutes() {
			return (double) this.nanos / NANOS_PER_MINUTE;
		}

		/** gomplate {@code .Hours} — the duration as a floating-point number of hours. */
		public double Hours() {
			return (double) this.nanos / NANOS_PER_HOUR;
		}

		@Override
		public String toString() {
			return formatGoDuration(this.nanos);
		}

	}

	/**
	 * A parsed Go {@code time.Time}; {@link #Format(Object)} renders it via a Go layout.
	 */
	public static final class GoTime {

		private final TemporalAccessor value;

		GoTime(TemporalAccessor value) {
			this.value = value;
		}

		/** gomplate {@code .Format layout} — render the time using a Go layout string. */
		public String Format(Object layout) {
			return DateTimeFormatter.ofPattern(translateLayout(Values.toString(layout))).format(this.value);
		}

		@Override
		public String toString() {
			return this.value.toString();
		}

	}

}
