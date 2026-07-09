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
 * Covers input/output ({@code -i}/{@code -f}/{@code -o}, {@code -} = stdin/stdout),
 * {@code --config}, {@code --missing-key}, context ({@code -c}), datasources
 * ({@code -d}), partial templates ({@code -t}), directory rendering ({@code --input-dir}
 * / {@code --output-dir} / {@code --output-map} / {@code --chmod} with
 * {@code --include}/{@code --exclude}/{@code --exclude-processing} globs), and
 * {@code -V}/{@code --experimental}. Custom delimiters and plugins are follow-up work.
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

	@Option(names = { "--input-dir" }, description = "Directory of templates to render recursively.")
	private String inputDir;

	@Option(names = { "--output-dir" }, description = "Output directory for --input-dir (default: .).")
	private String outputDir;

	@Option(names = { "--output-map" }, description = "Template producing each output path (.in = input path).")
	private String outputMap;

	@Option(names = { "--chmod" }, description = "Octal mode applied to written files (e.g. 0644).")
	private String chmod;

	@Option(names = { "--left-delim" }, description = "Left action delimiter (default: {{; env GOMPLATE_LEFT_DELIM).")
	private String leftDelim;

	@Option(names = { "--right-delim" },
			description = "Right action delimiter (default: }}; env GOMPLATE_RIGHT_DELIM).")
	private String rightDelim;

	@Option(names = { "--exclude" }, description = "Glob of files to skip during --input-dir. Repeatable.")
	private List<String> excludes;

	@Option(names = { "--include" }, description = "Glob of files to include during --input-dir. Repeatable.")
	private List<String> includes;

	@Option(names = { "--exclude-processing" },
			description = "Glob of files copied verbatim (not rendered). Repeatable.")
	private List<String> excludeProcessing;

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

		boolean noInput = merged.getIn() == null && (merged.getInputFiles() == null || merged.getInputFiles().isEmpty())
				&& (merged.getInputDir() == null || merged.getInputDir().isBlank());
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
		cli.setInputDir(this.inputDir);
		cli.setOutputDir(this.outputDir);
		cli.setOutputMap(this.outputMap);
		cli.setChmod(this.chmod);
		cli.setExcludes(this.excludes);
		cli.setIncludes(this.includes);
		cli.setExcludeProcessing(this.excludeProcessing);
		cli.setLeftDelim(delimiter(this.leftDelim, "GOMPLATE_LEFT_DELIM"));
		cli.setRightDelim(delimiter(this.rightDelim, "GOMPLATE_RIGHT_DELIM"));
		return cli;
	}

	/** A delimiter flag, falling back to its gomplate environment variable when unset. */
	private static String delimiter(String flag, String envVar) {
		if (flag != null) {
			return flag;
		}
		String env = System.getenv(envVar);
		return (env != null && !env.isBlank()) ? env : null;
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
