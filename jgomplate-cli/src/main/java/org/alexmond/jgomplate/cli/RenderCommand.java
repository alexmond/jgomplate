package org.alexmond.jgomplate.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.springframework.stereotype.Component;

import org.alexmond.jgomplate.core.GomplateRunner;
import org.alexmond.jgomplate.core.config.ConfigLoader;
import org.alexmond.jgomplate.core.config.DataSourceConfig;
import org.alexmond.jgomplate.core.config.GomplateConfig;
import org.alexmond.jgomplate.core.datasource.Datasources;

/**
 * The {@code jgomplate} render command. Builds a {@link GomplateConfig} from the flags
 * the user actually passed, overlays it onto the {@code .gomplate.yaml} config file (CLI
 * wins), and renders through the {@link GomplateRunner}.
 *
 * <p>
 * Seed CLI surface (issue #7): inline {@code -i}, template files {@code -f} and output
 * files {@code -o} (repeatable, {@code -} = stdin/stdout), plus {@code --config},
 * {@code -V} and {@code --experimental}. Datasources ({@code -d}), context ({@code -c}),
 * nested templates ({@code -t}), missing-key handling and directory input are wired by
 * follow-up issues.
 */
@Component
@Command(name = "jgomplate", mixinStandardHelpOptions = true, description = "Render gomplate/Go templates on the JVM.")
public class RenderCommand implements Callable<Integer> {

	@Option(names = { "-f", "--file" }, description = "Template file to render ('-' = stdin). Repeatable.")
	private List<String> files;

	@Option(names = { "-i", "--in" }, description = "Inline template string to render.")
	private String inline;

	@Option(names = { "-o", "--out" }, description = "Output file ('-' = stdout). Repeatable, paired with --file.")
	private List<String> outputs;

	@Option(names = { "--config" }, description = "Config file (default: .gomplate.yaml).")
	private String config;

	@Option(names = { "--missing-key" },
			description = "Behaviour on a missing map key: error (default), zero, default, invalid.")
	private String missingKey;

	@Option(names = { "-c", "--context" },
			description = "Context datasource 'alias=URL' bound to '.' (alias '.' = root). Repeatable.")
	private List<String> contexts;

	@Option(names = { "-d", "--datasource" },
			description = "Datasource 'alias=URL' referenced via (ds \"alias\")/include. Repeatable.")
	private List<String> datasources;

	@Option(names = { "-t", "--template" },
			description = "Named partial 'name=URL' invoked via {{ template \"name\" . }}. Repeatable.")
	private List<String> templates;

	@Option(names = { "-V", "--verbose" }, description = "Verbose output.")
	private Boolean verbose;

	@Option(names = { "--experimental" }, description = "Enable experimental features.")
	private Boolean experimental;

	private final ConfigLoader configLoader = new ConfigLoader();

	private final GomplateRunner runner = new GomplateRunner();

	@Override
	public Integer call() throws Exception {
		GomplateConfig fileConfig = this.configLoader.load(this.config);
		GomplateConfig merged = fileConfig.mergeFrom(cliConfig());

		if (Boolean.TRUE.equals(this.verbose)) {
			System.err.println(
					"jgomplate: config=" + ((this.config != null) ? this.config : ConfigLoader.DEFAULT_CONFIG_FILE));
		}

		boolean noInput = merged.getIn() == null
				&& (merged.getInputFiles() == null || merged.getInputFiles().isEmpty());
		if (noInput && System.console() != null) {
			System.err.println("Provide a template with --file <path>, --in <string>, or on stdin.");
			return 2;
		}

		this.runner.run(merged, System.in, System.out);
		return 0;
	}

	/** Assemble a config from the flags actually supplied ({@code null} = not set). */
	private GomplateConfig cliConfig() {
		GomplateConfig cli = new GomplateConfig();
		cli.setIn(this.inline);
		cli.setInputFiles(this.files);
		cli.setOutputFiles(this.outputs);
		cli.setMissingKey(this.missingKey);
		cli.setExperimental(this.experimental);
		cli.setContext(parseDatasources(this.contexts));
		cli.setDatasources(parseDatasources(this.datasources));
		cli.setTemplates(parseDatasources(this.templates));
		return cli;
	}

	/** Parse repeatable {@code alias=URL} arguments into the config's datasource map. */
	private static Map<String, DataSourceConfig> parseDatasources(List<String> args) {
		if (args == null) {
			return null;
		}
		Map<String, DataSourceConfig> map = new LinkedHashMap<>();
		for (String arg : args) {
			Map.Entry<String, DataSourceConfig> entry = Datasources.parseArg(arg);
			map.put(entry.getKey(), entry.getValue());
		}
		return map;
	}

}
