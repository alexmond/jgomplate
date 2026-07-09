package org.alexmond.jgomplate.cli;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the {@code jgomplate} command-line renderer.
 *
 * <p>
 * Bootstraps the Spring context, then hands the arguments to the Picocli
 * {@link RenderCommand}. The shell exit code is the Picocli execution result.
 */
@SpringBootApplication
public class JgomplateApplication implements ApplicationRunner, ExitCodeGenerator {

	private final IFactory factory;

	private final RenderCommand renderCommand;

	private int exitCode;

	public JgomplateApplication(IFactory factory, RenderCommand renderCommand) {
		this.factory = factory;
		this.renderCommand = renderCommand;
	}

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(JgomplateApplication.class, args)));
	}

	@Override
	public void run(ApplicationArguments args) {
		this.exitCode = new CommandLine(this.renderCommand, this.factory).execute(args.getSourceArgs());
	}

	@Override
	public int getExitCode() {
		return this.exitCode;
	}

}
