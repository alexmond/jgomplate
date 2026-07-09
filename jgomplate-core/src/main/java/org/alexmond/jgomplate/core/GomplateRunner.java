package org.alexmond.jgomplate.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.alexmond.jgomplate.core.config.GomplateConfig;

/**
 * Drives a render from a resolved {@link GomplateConfig}: resolves the template input(s),
 * renders each through the {@link GomplateEngine}, and writes to the configured
 * output(s). {@code -} is stdin as an input and stdout as an output.
 *
 * <p>
 * Seed scope (issue #7): the inline template ({@code in}), the {@code inputFiles} /
 * {@code outputFiles} lists paired by position, and stdin/stdout. Directory rendering,
 * datasource/context binding, delimiters, missing-key handling and post-exec are wired by
 * follow-up issues; unrelated config keys are accepted but ignored here.
 */
public class GomplateRunner {

	private static final String STDIN_STDOUT = "-";

	private final GomplateEngine engine;

	public GomplateRunner() {
		this(new GomplateEngine());
	}

	public GomplateRunner(GomplateEngine engine) {
		this.engine = engine;
	}

	/**
	 * Render according to {@code config}, using {@code stdin}/{@code stdout} for any
	 * {@code -} input/output.
	 * @param config the fully-resolved configuration (CLI already merged over the file)
	 * @param stdin the stream backing a {@code -} input file
	 * @param stdout the stream backing a {@code -} output file
	 */
	public void run(GomplateConfig config, InputStream stdin, OutputStream stdout) {
		List<String> outputs = (config.getOutputFiles() != null) ? config.getOutputFiles() : List.of();

		if (config.getIn() != null) {
			String rendered = this.engine.render(config.getIn(), Map.of());
			write(target(outputs, 0), rendered, stdout);
			return;
		}

		List<String> inputs = (config.getInputFiles() != null && !config.getInputFiles().isEmpty())
				? config.getInputFiles() : List.of(STDIN_STDOUT);
		String cachedStdin = null;
		for (int i = 0; i < inputs.size(); i++) {
			String source = inputs.get(i);
			String templateText;
			if (STDIN_STDOUT.equals(source)) {
				cachedStdin = (cachedStdin != null) ? cachedStdin : readAll(stdin);
				templateText = cachedStdin;
			}
			else {
				templateText = readFile(Path.of(source));
			}
			write(target(outputs, i), this.engine.render(templateText, Map.of()), stdout);
		}
	}

	private static String target(List<String> outputs, int index) {
		return (index < outputs.size()) ? outputs.get(index) : STDIN_STDOUT;
	}

	private void write(String destination, String rendered, OutputStream stdout) {
		if (STDIN_STDOUT.equals(destination)) {
			try {
				stdout.write(rendered.getBytes(StandardCharsets.UTF_8));
				stdout.flush();
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to write output", ex);
			}
		}
		else {
			try {
				Files.writeString(Path.of(destination), rendered);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to write output file '" + destination + "'", ex);
			}
		}
	}

	private static String readFile(Path path) {
		try {
			return Files.readString(path);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read template '" + path + "'", ex);
		}
	}

	private static String readAll(InputStream stdin) {
		try {
			return new String(stdin.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read stdin", ex);
		}
	}

}
