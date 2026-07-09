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

class TemplateResolverTest {

	private final TemplateResolver resolver = new TemplateResolver();

	private static DataSourceConfig ds(String url) {
		DataSourceConfig config = new DataSourceConfig();
		config.setUrl(url);
		return config;
	}

	@Test
	void nullTemplatesResolveToEmptyMap() {
		assertTrue(this.resolver.resolve(null).isEmpty());
	}

	@Test
	void loadsRawTemplateSource(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("greeting.tmpl");
		Files.writeString(file, "Hi {{ . }}");

		Map<String, String> resolved = this.resolver.resolve(Map.of("greeting", ds(file.toString())));

		assertEquals("Hi {{ . }}", resolved.get("greeting"));
	}

	@Test
	void unsupportedSchemeThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> this.resolver.resolve(Map.of("x", ds("https://example.com/t.tmpl"))));
	}

}
