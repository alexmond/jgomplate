package org.alexmond.jgomplate.core.datasource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alexmond.gotmpl4j.Function;

import org.alexmond.jgomplate.core.config.DataSourceConfig;

/**
 * The in-template datasource functions — {@code datasource} (alias {@code ds}) and
 * {@code include} — bound to a render's configured datasources. Each is stateful (it
 * closes over the {@code alias -> config} map and a loader), so unlike the stateless
 * namespace POJOs it is injected per render via
 * {@link org.alexmond.jgomplate.core.GomplateEngine}'s function map rather than a global
 * {@code FunctionProvider}.
 *
 * <p>
 * {@code datasource "alias" [path…]} returns the parsed datasource, optionally indexed by
 * one or more {@code /}-separated path segments. {@code include "alias"} returns the raw
 * (unparsed) content. Parsed datasources are loaded lazily and memoised per instance.
 */
public class DatasourceFunctions {

	private final Map<String, DataSourceConfig> datasources;

	private final DatasourceLoader loader;

	private final Map<String, Object> parsedCache = new HashMap<>();

	public DatasourceFunctions(Map<String, DataSourceConfig> datasources) {
		this(datasources, new DatasourceLoader());
	}

	public DatasourceFunctions(Map<String, DataSourceConfig> datasources, DatasourceLoader loader) {
		this.datasources = (datasources != null) ? datasources : Map.of();
		this.loader = loader;
	}

	/**
	 * The function map to merge into a template's function set.
	 * @return {@code datasource}, {@code ds} and {@code include} bound to this instance
	 */
	public Map<String, Function> functions() {
		Map<String, Function> map = new HashMap<>();
		Function datasource = this::datasource;
		map.put("datasource", datasource);
		map.put("ds", datasource);
		map.put("include", this::include);
		return map;
	}

	private Object datasource(Object... args) {
		String alias = requireAlias(args, "datasource");
		Object data = parsed(alias);
		for (int i = 1; i < args.length; i++) {
			data = index(data, String.valueOf(args[i]), alias);
		}
		return data;
	}

	private Object include(Object... args) {
		String alias = requireAlias(args, "include");
		return this.loader.loadRaw(Datasources.resolve(alias, require(alias)));
	}

	private Object parsed(String alias) {
		return this.parsedCache.computeIfAbsent(alias,
				(key) -> this.loader.load(Datasources.resolve(key, require(key))));
	}

	private DataSourceConfig require(String alias) {
		DataSourceConfig config = this.datasources.get(alias);
		if (config == null) {
			throw new IllegalArgumentException("undefined datasource '" + alias + "'");
		}
		return config;
	}

	private static String requireAlias(Object[] args, String function) {
		if (args.length == 0 || args[0] == null) {
			throw new IllegalArgumentException(function + " requires a datasource alias");
		}
		return String.valueOf(args[0]);
	}

	private static Object index(Object data, String path, String alias) {
		Object current = data;
		for (String segment : path.split("/")) {
			if (segment.isEmpty()) {
				continue;
			}
			current = indexOne(current, segment, alias);
		}
		return current;
	}

	private static Object indexOne(Object current, String segment, String alias) {
		if (current instanceof Map<?, ?> map) {
			return map.get(segment);
		}
		if (current instanceof List<?> list) {
			return list.get(Integer.parseInt(segment));
		}
		throw new IllegalArgumentException(
				"datasource '" + alias + "': cannot index into " + typeName(current) + " with '" + segment + "'");
	}

	private static String typeName(Object value) {
		return (value != null) ? value.getClass().getSimpleName() : "null";
	}

}
