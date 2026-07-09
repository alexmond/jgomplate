package org.alexmond.jgomplate.core.datasource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Reads a {@link Datasource} into a parsed object graph (Map / List / scalar) suitable
 * for exposing to a template.
 *
 * <p>
 * Seed scope: local files parsed by extension — {@code .json} via Jackson, {@code .yaml}
 * / {@code .yml} via the YAML dataformat. Remote datasources (http, vault, consul, aws,
 * …) and the {@code Content-Type}-based negotiation gomplate does are follow-up work.
 */
public class DatasourceLoader {

	private final ObjectMapper json = new ObjectMapper();

	private final YAMLMapper yaml = new YAMLMapper();

	/**
	 * Load and parse a datasource.
	 * @param datasource the datasource to read
	 * @return the parsed value (typically a {@code Map<String, Object>})
	 * @throws UncheckedIOException if the file cannot be read
	 * @throws IllegalArgumentException if the format is not recognized
	 */
	public Object load(Datasource datasource) {
		Path path = Path.of(datasource.uri().getSchemeSpecificPart());
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		try {
			byte[] bytes = Files.readAllBytes(path);
			if (name.endsWith(".json")) {
				return json.readValue(bytes, Object.class);
			}
			if (name.endsWith(".yaml") || name.endsWith(".yml")) {
				return yaml.readValue(bytes, Object.class);
			}
			throw new IllegalArgumentException("Unsupported datasource format: " + name);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read datasource '" + datasource.alias() + "'", ex);
		}
	}

}
