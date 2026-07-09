package org.alexmond.jgomplate.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.springframework.stereotype.Component;

import org.alexmond.jgomplate.core.GomplateEngine;

/**
 * The {@code jgomplate} render command: reads a template (from {@code --file} or the
 * inline {@code --in} string), renders it through the {@link GomplateEngine}, and writes
 * the result to {@code --out} or standard output.
 *
 * <p>
 * A seed subset of gomplate's CLI surface — datasources ({@code -d}), context
 * ({@code -c}), and directory input ({@code --input-dir}) are follow-up work.
 */
@Component
@Command(name = "jgomplate", mixinStandardHelpOptions = true, description = "Render gomplate/Go templates on the JVM.")
public class RenderCommand implements Callable<Integer> {

	@Option(names = { "-f", "--file" }, description = "Template file to render.")
	private Path file;

	@Option(names = { "-i", "--in" }, description = "Inline template string to render.")
	private String inline;

	@Option(names = { "-o", "--out" }, description = "Output file (defaults to stdout).")
	private Path out;

	private final GomplateEngine engine = new GomplateEngine();

	@Override
	public Integer call() throws Exception {
		String templateText;
		if (this.inline != null) {
			templateText = this.inline;
		}
		else if (this.file != null) {
			templateText = Files.readString(this.file);
		}
		else {
			System.err.println("Provide a template with --file <path> or --in <string>.");
			return 2;
		}

		String rendered = this.engine.render(templateText, Map.of());

		if (this.out != null) {
			Files.writeString(this.out, rendered);
		}
		else {
			System.out.print(rendered);
		}
		return 0;
	}

}
