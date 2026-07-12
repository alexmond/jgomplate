package org.alexmond.jgomplate.functions.ns;

import java.util.List;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code path} namespace — pure, slash-separated (forward-slash) path
 * manipulation, mirroring Go's {@code path} package. Reached from templates as
 * {@code path.Base}, {@code path.Dir}, {@code path.Clean}, … Method names mirror
 * gomplate's Go API (PascalCase).
 *
 * <p>
 * These operate on {@code /}-separated paths regardless of the host OS (unlike
 * {@code filepath}, which is OS-specific) so results are identical everywhere. The
 * algorithms are faithful ports of Go's {@code path.Base/Clean/Dir/Ext/IsAbs/Join/Match/
 * Split}; matching is done on UTF-16 code units, which agrees with Go's rune handling for
 * the ASCII/BMP paths these functions are used with.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class PathNamespace {

	/**
	 * gomplate {@code path.Base in} — the last element; {@code "."} / {@code "/"} edges.
	 */
	public String Base(Object in) {
		String path = Values.toString(in);
		if (path.isEmpty()) {
			return ".";
		}
		int end = path.length();
		while (end > 0 && path.charAt(end - 1) == '/') {
			end--;
		}
		path = path.substring(0, end);
		int slash = path.lastIndexOf('/');
		if (slash >= 0) {
			path = path.substring(slash + 1);
		}
		return path.isEmpty() ? "/" : path;
	}

	/** gomplate {@code path.Clean in} — lexically shortest equivalent path. */
	public String Clean(Object in) {
		return clean(Values.toString(in));
	}

	/** gomplate {@code path.Dir in} — all but the last element, cleaned. */
	public String Dir(Object in) {
		String path = Values.toString(in);
		int slash = path.lastIndexOf('/');
		return clean(path.substring(0, slash + 1));
	}

	/** gomplate {@code path.Ext in} — the file-name extension including the dot. */
	public String Ext(Object in) {
		String path = Values.toString(in);
		for (int i = path.length() - 1; i >= 0 && path.charAt(i) != '/'; i--) {
			if (path.charAt(i) == '.') {
				return path.substring(i);
			}
		}
		return "";
	}

	/** gomplate {@code path.IsAbs in} — whether the path begins with {@code /}. */
	public boolean IsAbs(Object in) {
		String path = Values.toString(in);
		return !path.isEmpty() && path.charAt(0) == '/';
	}

	/**
	 * gomplate {@code path.Join elem…} — join with {@code /} then clean; empties skipped.
	 */
	public String Join(Object... elem) {
		StringBuilder buf = new StringBuilder();
		for (Object e : elem) {
			String s = Values.toString(e);
			if (buf.length() > 0 || !s.isEmpty()) {
				if (buf.length() > 0) {
					buf.append('/');
				}
				buf.append(s);
			}
		}
		return (buf.length() == 0) ? "" : clean(buf.toString());
	}

	/** gomplate {@code path.Match pattern name} — shell pattern match (no {@code /}). */
	public boolean Match(Object pattern, Object name) {
		return match(Values.toString(pattern), Values.toString(name));
	}

	/**
	 * gomplate {@code path.Split in} — split after the final slash into
	 * {@code [dir, file]}.
	 */
	public List<String> Split(Object in) {
		String path = Values.toString(in);
		int slash = path.lastIndexOf('/');
		return List.of(path.substring(0, slash + 1), path.substring(slash + 1));
	}

	// --- faithful ports of Go's path package internals ---

	private static String clean(String path) {
		if (path.isEmpty()) {
			return ".";
		}
		boolean rooted = path.charAt(0) == '/';
		int n = path.length();
		char[] out = new char[n + 1];
		int w = 0;
		int r = 0;
		int dotdot = 0;
		if (rooted) {
			out[w++] = '/';
			r = 1;
			dotdot = 1;
		}
		while (r < n) {
			char c = path.charAt(r);
			if (c == '/') {
				r++;
			}
			else if (c == '.' && (r + 1 == n || path.charAt(r + 1) == '/')) {
				r++;
			}
			else if (c == '.' && r + 1 < n && path.charAt(r + 1) == '.' && (r + 2 == n || path.charAt(r + 2) == '/')) {
				r += 2;
				if (w > dotdot) {
					w--;
					while (w > dotdot && out[w] != '/') {
						w--;
					}
				}
				else if (!rooted) {
					if (w > 0) {
						out[w++] = '/';
					}
					out[w++] = '.';
					out[w++] = '.';
					dotdot = w;
				}
			}
			else {
				if ((rooted && w != 1) || (!rooted && w != 0)) {
					out[w++] = '/';
				}
				for (; r < n && path.charAt(r) != '/'; r++) {
					out[w++] = path.charAt(r);
				}
			}
		}
		return (w == 0) ? "." : new String(out, 0, w);
	}

	private static boolean match(String pattern, String name) {
		while (!pattern.isEmpty()) {
			int[] scan = scanChunk(pattern);
			boolean star = scan[0] != 0;
			String chunk = pattern.substring(scan[1], scan[2]);
			pattern = pattern.substring(scan[2]);
			if (star && chunk.isEmpty()) {
				return name.indexOf('/') < 0;
			}
			String rest = matchChunk(chunk, name);
			if (rest != null && (rest.isEmpty() || !pattern.isEmpty())) {
				name = rest;
			}
			else {
				String advanced = star ? matchStar(chunk, pattern, name) : null;
				if (advanced == null) {
					return false;
				}
				name = advanced;
			}
		}
		return name.isEmpty();
	}

	/**
	 * A leading {@code *} matches any run of non-slash chars; find where the chunk lands.
	 */
	private static String matchStar(String chunk, String pattern, String name) {
		for (int i = 0; i < name.length() && name.charAt(i) != '/'; i++) {
			String t = matchChunk(chunk, name.substring(i + 1));
			if (t != null && !(pattern.isEmpty() && !t.isEmpty())) {
				return t;
			}
		}
		return null;
	}

	/** Returns {@code [star?1:0, chunkStart, restStart]} against the pattern head. */
	private static int[] scanChunk(String pattern) {
		int p = 0;
		boolean star = false;
		while (p < pattern.length() && pattern.charAt(p) == '*') {
			p++;
			star = true;
		}
		boolean inrange = false;
		int i = p;
		scan: for (; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if (c == '\\') {
				if (i + 1 < pattern.length()) {
					i++;
				}
			}
			else if (c == '[') {
				inrange = true;
			}
			else if (c == ']') {
				inrange = false;
			}
			else if (c == '*' && !inrange) {
				break scan;
			}
		}
		return new int[] { star ? 1 : 0, p, i };
	}

	/**
	 * Matches a chunk (no {@code *}) at the head of {@code s}; returns the rest or null.
	 */
	private static String matchChunk(String chunk, String s) {
		boolean failed = false;
		while (!chunk.isEmpty()) {
			if (!failed && s.isEmpty()) {
				failed = true;
			}
			char c = chunk.charAt(0);
			if (c == '[') {
				int r = failed ? 0 : s.charAt(0);
				if (!failed) {
					s = s.substring(1);
				}
				chunk = chunk.substring(1);
				boolean negated = !chunk.isEmpty() && chunk.charAt(0) == '^';
				if (negated) {
					chunk = chunk.substring(1);
				}
				boolean match = false;
				int nrange = 0;
				while (true) {
					if (!chunk.isEmpty() && chunk.charAt(0) == ']' && nrange > 0) {
						chunk = chunk.substring(1);
						break;
					}
					int[] lo = getEsc(chunk);
					chunk = chunk.substring(lo[1]);
					int hi = lo[0];
					if (!chunk.isEmpty() && chunk.charAt(0) == '-') {
						int[] hiEsc = getEsc(chunk.substring(1));
						hi = hiEsc[0];
						chunk = chunk.substring(1 + hiEsc[1]);
					}
					if (lo[0] <= r && r <= hi) {
						match = true;
					}
					nrange++;
				}
				if (match == negated) {
					failed = true;
				}
			}
			else if (c == '?') {
				if (!failed) {
					if (s.charAt(0) == '/') {
						failed = true;
					}
					s = s.substring(1);
				}
				chunk = chunk.substring(1);
			}
			else {
				// a backslash escapes the next char; both compare as a literal
				if (c == '\\') {
					chunk = chunk.substring(1);
					if (chunk.isEmpty()) {
						throw new IllegalArgumentException("syntax error in pattern");
					}
				}
				if (!failed) {
					if (chunk.charAt(0) != s.charAt(0)) {
						failed = true;
					}
					else {
						s = s.substring(1);
					}
				}
				chunk = chunk.substring(1);
			}
		}
		return failed ? null : s;
	}

	/**
	 * Reads one (possibly escaped) char from a class; returns
	 * {@code [charValue, consumed]}.
	 */
	private static int[] getEsc(String chunk) {
		if (chunk.isEmpty() || chunk.charAt(0) == '-' || chunk.charAt(0) == ']') {
			throw new IllegalArgumentException("syntax error in pattern");
		}
		int consumed = 0;
		if (chunk.charAt(0) == '\\') {
			chunk = chunk.substring(1);
			consumed = 1;
			if (chunk.isEmpty()) {
				throw new IllegalArgumentException("syntax error in pattern");
			}
		}
		consumed += 1;
		if (chunk.length() == 1) {
			throw new IllegalArgumentException("syntax error in pattern");
		}
		return new int[] { chunk.charAt(0), consumed };
	}

}
