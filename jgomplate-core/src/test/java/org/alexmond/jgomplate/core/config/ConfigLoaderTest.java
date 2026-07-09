package org.alexmond.jgomplate.core.config;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

	private final ConfigLoader loader = new ConfigLoader();

	@Test
	void parsesGomplateYamlKeys(@TempDir Path dir) throws Exception {
		Path cfg = dir.resolve("config.yaml");
		Files.writeString(cfg, """
				inputFiles:
				  - in.tmpl
				outputFiles:
				  - out.txt
				missingKey: zero
				experimental: true
				datasources:
				  data:
				    url: file:///data.yaml
				plugins:
				  echo:
				    cmd: /bin/echo
				    args: [hi]
				    pipe: true
				""");

		GomplateConfig config = this.loader.load(cfg.toString(), null);

		assertEquals(List.of("in.tmpl"), config.getInputFiles());
		assertEquals(List.of("out.txt"), config.getOutputFiles());
		assertEquals("zero", config.getMissingKey());
		assertTrue(config.getExperimental());
		assertEquals("file:///data.yaml", config.getDatasources().get("data").getUrl());
		PluginConfig echo = config.getPlugins().get("echo");
		assertEquals("/bin/echo", echo.getCmd());
		assertEquals(List.of("hi"), echo.getArgs());
		assertTrue(echo.getPipe());
	}

	@Test
	void ignoresUnknownKeys(@TempDir Path dir) throws Exception {
		Path cfg = dir.resolve("config.yaml");
		Files.writeString(cfg, "in: hello\nsomeFutureKey: whatever\n");
		GomplateConfig config = this.loader.load(cfg.toString(), null);
		assertEquals("hello", config.getIn());
	}

	@Test
	void explicitConfigFlagWins(@TempDir Path dir) throws Exception {
		Path flag = dir.resolve("flag.yaml");
		Files.writeString(flag, "in: from-flag\n");
		GomplateConfig config = this.loader.load(flag.toString(), dir.resolve("env.yaml").toString());
		assertEquals("from-flag", config.getIn());
	}

	@Test
	void envUsedWhenNoFlag(@TempDir Path dir) throws Exception {
		Path env = dir.resolve("env.yaml");
		Files.writeString(env, "in: from-env\n");
		GomplateConfig config = this.loader.load(null, env.toString());
		assertEquals("from-env", config.getIn());
	}

	@Test
	void missingExplicitFlagIsError(@TempDir Path dir) {
		String missing = dir.resolve("nope.yaml").toString();
		assertThrows(UncheckedIOException.class, () -> this.loader.load(missing, null));
	}

	@Test
	void missingRequiredEnvIsError(@TempDir Path dir) {
		String missing = dir.resolve("nope.yaml").toString();
		assertThrows(UncheckedIOException.class, () -> this.loader.load(null, missing));
	}

	@Test
	void missingDefaultIsSkippedSilently() {
		// No flag, no env → default .gomplate.yaml which does not exist here → empty
		// config.
		GomplateConfig config = this.loader.load(null, null);
		assertNull(config.getIn());
		assertNull(config.getInputFiles());
	}

}
