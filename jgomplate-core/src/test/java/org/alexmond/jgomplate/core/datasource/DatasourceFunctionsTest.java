package org.alexmond.jgomplate.core.datasource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.alexmond.gotmpl4j.Function;
import org.alexmond.jgomplate.core.config.DataSourceConfig;

class DatasourceFunctionsTest {

	private static DataSourceConfig ds(String url) {
		DataSourceConfig config = new DataSourceConfig();
		config.setUrl(url);
		return config;
	}

	private Function fn(Map<String, DataSourceConfig> sources, String name) {
		return new DatasourceFunctions(sources).functions().get(name);
	}

	@Test
	@SuppressWarnings("unchecked")
	void datasourceReturnsParsedObject(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("data.json");
		Files.writeString(file, "{\"env\":\"prod\"}");
		Object result = fn(Map.of("x", ds(file.toString())), "datasource").invoke("x");
		assertEquals("prod", ((Map<String, Object>) result).get("env"));
	}

	@Test
	void dsIsAnAliasOfDatasource(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("data.yaml");
		Files.writeString(file, "env: staging\n");
		Map<String, Function> functions = new DatasourceFunctions(Map.of("x", ds(file.toString()))).functions();
		assertTrue(functions.containsKey("ds"));
		assertEquals(functions.get("datasource"), functions.get("ds"));
	}

	@Test
	void datasourceIndexesBySubpath(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("data.json");
		Files.writeString(file, "{\"db\":{\"host\":\"localhost\"}}");
		Object result = fn(Map.of("x", ds(file.toString())), "datasource").invoke("x", "db/host");
		assertEquals("localhost", result);
	}

	@Test
	void includeReturnsRawContent(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("note.txt");
		Files.writeString(file, "raw {{ not rendered }}");
		Object result = fn(Map.of("x", ds(file.toString())), "include").invoke("x");
		assertEquals("raw {{ not rendered }}", result);
	}

	@Test
	void undefinedDatasourceThrows() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> fn(Map.of(), "datasource").invoke("missing"));
		assertTrue(ex.getMessage().contains("undefined datasource"));
	}

	@Test
	void unsupportedSchemeThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> fn(Map.of("x", ds("https://example.com/a.json")), "datasource").invoke("x"));
	}

}
