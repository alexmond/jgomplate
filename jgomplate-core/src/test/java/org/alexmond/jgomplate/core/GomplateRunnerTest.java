package org.alexmond.jgomplate.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		Files.writeString(in, "hello {{ .missing | default \"world\" }}");
		GomplateConfig config = new GomplateConfig();
		config.setInputFiles(List.of(in.toString()));
		config.setOutputFiles(List.of(out.toString()));

		this.runner.run(config, NO_STDIN, new ByteArrayOutputStream());

		assertEquals("hello world", Files.readString(out));
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
