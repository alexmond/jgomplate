package org.alexmond.jgomplate.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.alexmond.jgomplate.core.config.DataSourceConfig;
import org.alexmond.jgomplate.core.config.GomplateConfig;

class GomplateRunnerTest {

	private final GomplateRunner runner = new GomplateRunner();

	private static final InputStream NO_STDIN = new ByteArrayInputStream(new byte[0]);

	@Test
	void inlineTemplateToStdout() {
		GomplateConfig config = new GomplateConfig();
		config.setIn("{{ strings.ToUpper \"hi\" }}");
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		this.runner.run(config, NO_STDIN, out);

		assertEquals("HI", out.toString(StandardCharsets.UTF_8));
	}

	@Test
	void stdinToStdout() {
		GomplateConfig config = new GomplateConfig();
		config.setInputFiles(List.of("-"));
		InputStream stdin = new ByteArrayInputStream("{{ \"x\" | upper }}".getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		this.runner.run(config, stdin, out);

		assertEquals("X", out.toString(StandardCharsets.UTF_8));
	}

	@Test
	void defaultsToStdinWhenNoInput() {
		GomplateConfig config = new GomplateConfig();
		InputStream stdin = new ByteArrayInputStream("plain".getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		this.runner.run(config, stdin, out);

		assertEquals("plain", out.toString(StandardCharsets.UTF_8));
	}

	@Test
	void fileInputToFileOutput(@TempDir Path dir) throws Exception {
		Path in = dir.resolve("in.tmpl");
		Path out = dir.resolve("out.txt");
		Files.writeString(in, "hello {{ \"world\" }}");
		GomplateConfig config = new GomplateConfig();
		config.setInputFiles(List.of(in.toString()));
		config.setOutputFiles(List.of(out.toString()));

		this.runner.run(config, NO_STDIN, new ByteArrayOutputStream());

		assertEquals("hello world", Files.readString(out));
	}

	@Test
	void defaultsToErrorMissingKey() {
		GomplateConfig config = new GomplateConfig();
		config.setIn("{{ .missing }}");
		assertThrows(RuntimeException.class, () -> this.runner.run(config, NO_STDIN, new ByteArrayOutputStream()));
	}

	@Test
	void missingKeyOverrideFromConfig() {
		GomplateConfig config = new GomplateConfig();
		config.setIn("[{{ .missing }}]");
		config.setMissingKey("zero");
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		this.runner.run(config, NO_STDIN, out);

		assertEquals("[]", out.toString(StandardCharsets.UTF_8));
	}

	@Test
	void contextDatasourceBoundToRoot(@TempDir Path dir) throws Exception {
		Path data = dir.resolve("data.json");
		Files.writeString(data, "{\"name\":\"alex\"}");
		GomplateConfig config = new GomplateConfig();
		config.setIn("hi {{ .name }}");
		config.setContext(Map.of(".", dsConfig(data.toString())));
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		this.runner.run(config, NO_STDIN, out);

		assertEquals("hi alex", out.toString(StandardCharsets.UTF_8));
	}

	@Test
	void namedContextReachableByAlias(@TempDir Path dir) throws Exception {
		Path data = dir.resolve("data.yaml");
		Files.writeString(data, "env: prod\n");
		GomplateConfig config = new GomplateConfig();
		config.setIn("{{ .cfg.env }}");
		config.setContext(Map.of("cfg", dsConfig(data.toString())));
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		this.runner.run(config, NO_STDIN, out);

		assertEquals("prod", out.toString(StandardCharsets.UTF_8));
	}

	private static DataSourceConfig dsConfig(String url) {
		DataSourceConfig config = new DataSourceConfig();
		config.setUrl(url);
		return config;
	}

	@Test
	void datasourceFunctionReachableInTemplate(@TempDir Path dir) throws Exception {
		Path data = dir.resolve("db.json");
		Files.writeString(data, "{\"host\":\"localhost\"}");
		GomplateConfig config = new GomplateConfig();
		config.setIn("{{ (ds \"db\").host }}");
		config.setDatasources(Map.of("db", dsConfig(data.toString())));
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		this.runner.run(config, NO_STDIN, out);

		assertEquals("localhost", out.toString(StandardCharsets.UTF_8));
	}

	@Test
	void includeFunctionReturnsRawContent(@TempDir Path dir) throws Exception {
		Path data = dir.resolve("snippet.txt");
		Files.writeString(data, "verbatim");
		GomplateConfig config = new GomplateConfig();
		config.setIn("[{{ include \"s\" }}]");
		config.setDatasources(Map.of("s", dsConfig(data.toString())));
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		this.runner.run(config, NO_STDIN, out);

		assertEquals("[verbatim]", out.toString(StandardCharsets.UTF_8));
	}

	@Test
	void multipleFilesPairedByPosition(@TempDir Path dir) throws Exception {
		Path in1 = dir.resolve("a.tmpl");
		Path in2 = dir.resolve("b.tmpl");
		Path out1 = dir.resolve("a.out");
		Path out2 = dir.resolve("b.out");
		Files.writeString(in1, "{{ \"a\" | upper }}");
		Files.writeString(in2, "{{ \"b\" | upper }}");
		GomplateConfig config = new GomplateConfig();
		config.setInputFiles(List.of(in1.toString(), in2.toString()));
		config.setOutputFiles(List.of(out1.toString(), out2.toString()));

		this.runner.run(config, NO_STDIN, new ByteArrayOutputStream());

		assertEquals("A", Files.readString(out1));
		assertEquals("B", Files.readString(out2));
	}

}
