package org.alexmond.jgomplate.core.datasource;

import java.util.LinkedHashMap;
import java.util.Map;

import org.alexmond.jgomplate.core.config.DataSourceConfig;

/**
 * Resolves gomplate's {@code -t alias=URL} partial templates (the {@code templates:}
 * config key) into {@code name -> source} pairs, loaded as raw content so the engine can
 * parse each as a named template invokable via {@code {{ template "name" . }}}.
 *
 * <p>
 * Seed scope: local {@code file} templates only, like {@link ContextResolver}.
 */
public class TemplateResolver {

	private final DatasourceLoader loader;

	public TemplateResolver() {
		this(new DatasourceLoader());
	}

	public TemplateResolver(DatasourceLoader loader) {
		this.loader = loader;
	}

	/**
	 * Load each configured template's raw source.
	 * @param templates the {@code alias -> template datasource} map (may be
	 * {@code null}/empty)
	 * @return a map of template name to raw source
	 * @throws IllegalArgumentException if an entry is malformed or uses an unsupported
	 * scheme
	 */
	public Map<String, String> resolve(Map<String, DataSourceConfig> templates) {
		Map<String, String> out = new LinkedHashMap<>();
		if (templates == null) {
			return out;
		}
		for (Map.Entry<String, DataSourceConfig> entry : templates.entrySet()) {
			String name = entry.getKey();
			out.put(name, this.loader.loadRaw(Datasources.resolve(name, entry.getValue())));
		}
		return out;
	}

}
