package org.alexmond.jgomplate.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.alexmond.jgomplate.core.datasource.Datasource;
import org.alexmond.jgomplate.core.datasource.DatasourceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasourceLoaderTest {

	private final DatasourceLoader loader = new DatasourceLoader();

	@Test
	@SuppressWarnings("unchecked")
	void loadsJson(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config.json");
		Files.writeString(file, "{\"env\":\"prod\",\"replicas\":3}");

		Object loaded = loader.load(new Datasource("cfg", file.toUri()));

		Map<String, Object> map = (Map<String, Object>) loaded;
		assertEquals("prod", map.get("env"));
		assertEquals(3, map.get("replicas"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void loadsYaml(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config.yaml");
		Files.writeString(file, "env: staging\nreplicas: 2\n");

		Object loaded = loader.load(new Datasource("cfg", file.toUri()));

		Map<String, Object> map = (Map<String, Object>) loaded;
		assertEquals("staging", map.get("env"));
		assertEquals(2, map.get("replicas"));
	}

}
