package org.alexmond.jgomplate.core.datasource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.alexmond.jgomplate.core.config.DataSourceConfig;

/**
 * Resolves gomplate's {@code context} datasources ({@code -c alias=URL}) into the root
 * template context map. The special alias {@code .} merges its (map) value into the root;
 * any other alias is exposed as {@code .alias}.
 *
 * <p>
 * Seed scope: local {@code file} datasources only (JSON/YAML, as {@link DatasourceLoader}
 * supports). Any other scheme fails with a clear message; remote schemes are follow-up
 * work (issue #2).
 */
public class ContextResolver {

	private static final String ROOT_ALIAS = ".";

	private final DatasourceLoader loader;

	public ContextResolver() {
		this(new DatasourceLoader());
	}

	public ContextResolver(DatasourceLoader loader) {
		this.loader = loader;
	}

	/**
	 * Build the root context map from the configured context datasources.
	 * @param contexts the {@code alias -> datasource} map (may be {@code null}/empty)
	 * @return a mutable map exposed to templates as {@code .}
	 * @throws IllegalArgumentException if an entry is malformed, uses an unsupported
	 * scheme, or the root ({@code .}) datasource does not resolve to a map
	 */
	public Map<String, Object> resolve(Map<String, DataSourceConfig> contexts) {
		Map<String, Object> root = new LinkedHashMap<>();
		if (contexts == null) {
			return root;
		}
		for (Map.Entry<String, DataSourceConfig> entry : contexts.entrySet()) {
			String alias = entry.getKey();
			Object loaded = load(alias, entry.getValue());
			if (ROOT_ALIAS.equals(alias)) {
				if (!(loaded instanceof Map)) {
					throw new IllegalArgumentException("context '.' must resolve to a map, got " + typeName(loaded));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) loaded;
				root.putAll(map);
			}
			else {
				root.put(alias, loaded);
			}
		}
		return root;
	}

	private Object load(String alias, DataSourceConfig config) {
		if (config == null || config.getUrl() == null || config.getUrl().isBlank()) {
			throw new IllegalArgumentException("context '" + alias + "' has no url");
		}
		URI uri = Datasources.toUri(config.getUrl());
		String scheme = (uri.getScheme() != null) ? uri.getScheme().toLowerCase(Locale.ROOT) : null;
		if (!"file".equals(scheme)) {
			throw new IllegalArgumentException("datasource '" + alias + "': unsupported scheme '" + scheme
					+ "' (only local file datasources are supported)");
		}
		return this.loader.load(new Datasource(alias, uri));
	}

	private static String typeName(Object value) {
		return (value != null) ? value.getClass().getSimpleName() : "null";
	}

}
