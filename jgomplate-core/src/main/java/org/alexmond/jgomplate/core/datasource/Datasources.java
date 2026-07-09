package org.alexmond.jgomplate.core.datasource;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import org.alexmond.jgomplate.core.config.DataSourceConfig;

/**
 * Helpers for turning gomplate's {@code alias=URL} datasource/context arguments into the
 * shared config model, and for resolving a datasource URL to a {@link URI}.
 */
public final class Datasources {

	/** Matches a leading RFC-3986 scheme (e.g. {@code file:}, {@code http:}). */
	private static final Pattern SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");

	private Datasources() {
	}

	/**
	 * Parse an {@code alias=URL} argument (as passed to {@code -c} / {@code -d}). When no
	 * {@code =} is present the whole value is the URL and the alias is derived from the
	 * file's base name without extension, matching gomplate.
	 * @param arg the raw CLI argument
	 * @return an {@code alias -> DataSourceConfig} entry
	 */
	public static Map.Entry<String, DataSourceConfig> parseArg(String arg) {
		int eq = arg.indexOf('=');
		String alias;
		String url;
		if (eq >= 0) {
			alias = arg.substring(0, eq);
			url = arg.substring(eq + 1);
		}
		else {
			url = arg;
			alias = aliasFromUrl(arg);
		}
		DataSourceConfig config = new DataSourceConfig();
		config.setUrl(url);
		return Map.entry(alias, config);
	}

	/**
	 * Derive a datasource alias from a URL: its base name without a file extension.
	 * @param value the URL or path
	 * @return the derived alias
	 */
	public static String aliasFromUrl(String value) {
		String v = value;
		int query = v.indexOf('?');
		if (query >= 0) {
			v = v.substring(0, query);
		}
		int slash = Math.max(v.lastIndexOf('/'), v.lastIndexOf('\\'));
		String base = (slash >= 0) ? v.substring(slash + 1) : v;
		int dot = base.lastIndexOf('.');
		return (dot > 0) ? base.substring(0, dot) : base;
	}

	/**
	 * Resolve a datasource URL to a {@link URI}. A value without a scheme is treated as a
	 * filesystem path and resolved (relative to the working directory) into a
	 * {@code file:} URI.
	 * @param value the URL or path
	 * @return the resolved URI
	 */
	public static URI toUri(String value) {
		String v = value.trim();
		if (SCHEME.matcher(v).matches()) {
			return URI.create(v);
		}
		return Path.of(v).toUri();
	}

}
