package org.alexmond.jgomplate.functions.ns;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed URL returned by {@code conv.URL}, exposing the same fields Go's
 * {@code url.URL} does so templates can reach them by name —
 * {@code (conv.URL "…").Scheme}, {@code .Host}, {@code .Path}, and so on. Method names
 * mirror Go's API (PascalCase), so gotmpl4j resolves {@code .Scheme} to {@link #Scheme()}
 * exactly as Go resolves the field.
 *
 * <p>
 * Backed by {@link java.net.URI}. {@code User} is exposed as the raw userinfo string
 * rather than Go's {@code *Userinfo} object.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror Go's url.URL API
													// (PascalCase)
public final class ParsedUrl {

	private final URI uri;

	ParsedUrl(URI uri) {
		this.uri = uri;
	}

	/** Go {@code URL.Scheme} — e.g. {@code https}. */
	public String Scheme() {
		return nz(uri.getScheme());
	}

	/** Go {@code URL.Opaque} — the opaque part of a non-hierarchical URL, else empty. */
	public String Opaque() {
		return uri.isOpaque() ? nz(uri.getRawSchemeSpecificPart()) : "";
	}

	/** Go {@code URL.Host} — {@code host} or {@code host:port} (no userinfo). */
	public String Host() {
		String host = nz(uri.getHost());
		if (host.isEmpty()) {
			String authority = nz(uri.getAuthority());
			int at = authority.indexOf('@');
			return (at >= 0) ? authority.substring(at + 1) : authority;
		}
		return (uri.getPort() >= 0) ? host + ":" + uri.getPort() : host;
	}

	/** Go {@code URL.Hostname()} — the host without any port. */
	public String Hostname() {
		String host = Host();
		int colon = host.lastIndexOf(':');
		return (colon >= 0) ? host.substring(0, colon) : host;
	}

	/** Go {@code URL.Port()} — the port without the host, or empty. */
	public String Port() {
		return (uri.getPort() >= 0) ? String.valueOf(uri.getPort()) : "";
	}

	/** Go {@code URL.Path} — the (decoded) path. */
	public String Path() {
		return nz(uri.getPath());
	}

	/** Go {@code URL.RawQuery} — the query string without {@code ?}. */
	public String RawQuery() {
		return nz(uri.getRawQuery());
	}

	/** Go {@code URL.Fragment} — the fragment without {@code #}. */
	public String Fragment() {
		return nz(uri.getFragment());
	}

	/** Go {@code URL.User} — the raw userinfo (e.g. {@code user:pass}), or empty. */
	public String User() {
		return nz(uri.getUserInfo());
	}

	/**
	 * Go {@code URL.Query()} — the query parsed into a map of name → values, form-decoded
	 * (so {@code +} becomes a space), matching {@code url.Values}.
	 */
	public Map<String, List<String>> Query() {
		Map<String, List<String>> values = new LinkedHashMap<>();
		String raw = uri.getRawQuery();
		if (raw == null || raw.isEmpty()) {
			return values;
		}
		for (String pair : raw.split("&")) {
			int eq = pair.indexOf('=');
			String key = (eq >= 0) ? pair.substring(0, eq) : pair;
			String value = (eq >= 0) ? pair.substring(eq + 1) : "";
			values.computeIfAbsent(decode(key), (k) -> new ArrayList<>()).add(decode(value));
		}
		return values;
	}

	/** Go {@code URL.String()} — the reassembled URL. */
	public String String() {
		return uri.toString();
	}

	@Override
	public String toString() {
		return uri.toString();
	}

	private static String decode(String s) {
		return URLDecoder.decode(s, StandardCharsets.UTF_8);
	}

	private static String nz(String s) {
		return (s != null) ? s : "";
	}

}
