package org.alexmond.jgomplate.core.datasource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.alexmond.jgomplate.core.config.DataSourceConfig;

class ContextResolverTest {

	private final ContextResolver resolver = new ContextResolver();

	private static DataSourceConfig ds(String url) {
		DataSourceConfig config = new DataSourceConfig();
		config.setUrl(url);
		return config;
	}

	@Test
	void nullContextsResolveToEmptyMap() {
		assertTrue(this.resolver.resolve(null).isEmpty());
	}

	@Test
	void rootAliasMergesMapIntoRoot(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("data.json");
		Files.writeString(file, "{\"env\":\"prod\",\"replicas\":3}");

		Map<String, Object> root = this.resolver.resolve(Map.of(".", ds(file.toString())));

		assertEquals("prod", root.get("env"));
		assertEquals(3, root.get("replicas"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void namedAliasIsExposedUnderKey(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("data.yaml");
		Files.writeString(file, "env: staging\n");

		Map<String, Object> root = this.resolver.resolve(Map.of("cfg", ds(file.toString())));

		Map<String, Object> cfg = (Map<String, Object>) root.get("cfg");
		assertEquals("staging", cfg.get("env"));
	}

	@Test
	void unsupportedSchemeThrows() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> this.resolver.resolve(Map.of("x", ds("https://example.com/a.json"))));
		assertTrue(ex.getMessage().contains("unsupported scheme"));
	}

	@Test
	void rootMustResolveToMap(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("list.json");
		Files.writeString(file, "[1,2,3]");
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve(Map.of(".", ds(file.toString()))));
	}

	@Test
	void missingUrlThrows() {
		assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve(Map.of("x", new DataSourceConfig())));
	}

}
