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

import org.alexmond.gotmpl4j.Function;

import org.alexmond.jgomplate.core.config.GomplateConfig;
import org.alexmond.jgomplate.core.datasource.ContextResolver;
import org.alexmond.jgomplate.core.datasource.DatasourceFunctions;

/**
 * Drives a render from a resolved {@link GomplateConfig}: resolves the template input(s),
 * renders each through the {@link GomplateEngine}, and writes to the configured
 * output(s). {@code -} is stdin as an input and stdout as an output.
 *
 * <p>
 * Scope: the inline template ({@code in}), the {@code inputFiles} / {@code outputFiles}
 * lists paired by position, stdin/stdout, the {@code missingKey} behaviour (defaulting to
 * gomplate's {@code error}), {@code context} datasources bound to the template root, and
 * the {@code datasource}/{@code ds}/{@code include} functions over the configured
 * {@code datasources}. Directory rendering, delimiters and post-exec are wired by
 * follow-up issues; unrelated config keys are accepted but ignored here.
 */
public class GomplateRunner {

	private static final String STDIN_STDOUT = "-";

	/** gomplate's CLI default for {@code --missing-key}. */
	private static final String DEFAULT_MISSING_KEY = "error";

	private final GomplateEngine engine;

	private final ContextResolver contextResolver;

	public GomplateRunner() {
		this(new GomplateEngine(), new ContextResolver());
	}

	public GomplateRunner(GomplateEngine engine) {
		this(engine, new ContextResolver());
	}

	public GomplateRunner(GomplateEngine engine, ContextResolver contextResolver) {
		this.engine = engine;
		this.contextResolver = contextResolver;
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
		String missingKey = (config.getMissingKey() != null) ? config.getMissingKey() : DEFAULT_MISSING_KEY;
		Map<String, Object> context = this.contextResolver.resolve(config.getContext());
		Map<String, Function> functions = new DatasourceFunctions(config.getDatasources()).functions();

		if (config.getIn() != null) {
			String rendered = this.engine.render(config.getIn(), context, missingKey, functions);
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
			write(target(outputs, i), this.engine.render(templateText, context, missingKey, functions), stdout);
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
