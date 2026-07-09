package org.alexmond.jgomplate.core.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * The unified jgomplate configuration, shared by the CLI and the {@code .gomplate.yaml}
 * config file. Field names mirror gomplate's {@code Config} struct YAML keys verbatim so
 * a gomplate config file loads unchanged.
 *
 * <p>
 * Every field is nullable: {@code null} means "unset". This lets {@link #mergeFrom} treat
 * a partially-populated config (e.g. one built from the CLI flags the user actually
 * passed) as an overlay whose set fields win over the file config, matching gomplate's
 * {@code Config.MergeFrom} precedence (CLI over file).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GomplateConfig {

	/** Inline template string ({@code -i}/{@code --in}). */
	private String in;

	/** Template input files ({@code -f}/{@code --file}); {@code -} means stdin. */
	private List<String> inputFiles;

	/** Input directory to render recursively ({@code --input-dir}). */
	private String inputDir;

	/** Glob patterns to exclude when rendering a directory ({@code --exclude}). */
	private List<String> excludes;

	/** Globs copied verbatim (not rendered) during directory rendering. */
	private List<String> excludeProcessing;

	/** Output directory for directory rendering ({@code --output-dir}). */
	private String outputDir;

	/** Template producing the output path per input ({@code --output-map}). */
	private String outputMap;

	/**
	 * Output files ({@code -o}/{@code --out}), paired with {@code inputFiles}; {@code -}
	 * = stdout.
	 */
	private List<String> outputFiles;

	/** Octal mode applied to written output files ({@code --chmod}). */
	private String chmod;

	/**
	 * Left action delimiter ({@code --left-delim}); gomplate default is a double
	 * open-brace.
	 */
	private String leftDelim;

	/**
	 * Right action delimiter ({@code --right-delim}); gomplate default is a double
	 * close-brace.
	 */
	private String rightDelim;

	/**
	 * Behaviour on a missing map key ({@code --missing-key}):
	 * {@code error}/{@code zero}/{@code default}/{@code invalid}.
	 */
	private String missingKey;

	/** Post-render command run against each output ({@code --post-exec}). */
	private List<String> postExec;

	/** Default per-plugin timeout (Go duration string). */
	private String pluginTimeout;

	/** Pipe rendered output into the trailing {@code --exec-pipe} command. */
	private Boolean execPipe;

	/** Opt in to experimental gomplate features ({@code --experimental}). */
	private Boolean experimental;

	/** Named datasources ({@code -d}/{@code --datasource}). */
	private Map<String, DataSourceConfig> datasources;

	/**
	 * Named context datasources eagerly bound to the template root
	 * ({@code -c}/{@code --context}).
	 */
	private Map<String, DataSourceConfig> context;

	/** Named nested templates ({@code -t}/{@code --template}). */
	private Map<String, DataSourceConfig> templates;

	/** Named plugins exposed as template functions ({@code --plugin}). */
	private Map<String, PluginConfig> plugins;

	/**
	 * Overlay {@code other} onto this config: every non-{@code null} field of
	 * {@code other} replaces the corresponding field here. Mirrors gomplate's
	 * {@code Config.MergeFrom}, used to let CLI flags (the overlay) win over the config
	 * file (the base).
	 * @param other the overlay whose set fields take precedence; {@code null} is a no-op
	 * @return this config, mutated in place, for chaining
	 */
	public GomplateConfig mergeFrom(GomplateConfig other) {
		if (other == null) {
			return this;
		}
		if (other.in != null) {
			this.in = other.in;
		}
		if (other.inputFiles != null) {
			this.inputFiles = other.inputFiles;
		}
		if (other.inputDir != null) {
			this.inputDir = other.inputDir;
		}
		if (other.excludes != null) {
			this.excludes = other.excludes;
		}
		if (other.excludeProcessing != null) {
			this.excludeProcessing = other.excludeProcessing;
		}
		if (other.outputDir != null) {
			this.outputDir = other.outputDir;
		}
		if (other.outputMap != null) {
			this.outputMap = other.outputMap;
		}
		if (other.outputFiles != null) {
			this.outputFiles = other.outputFiles;
		}
		if (other.chmod != null) {
			this.chmod = other.chmod;
		}
		if (other.leftDelim != null) {
			this.leftDelim = other.leftDelim;
		}
		if (other.rightDelim != null) {
			this.rightDelim = other.rightDelim;
		}
		if (other.missingKey != null) {
			this.missingKey = other.missingKey;
		}
		if (other.postExec != null) {
			this.postExec = other.postExec;
		}
		if (other.pluginTimeout != null) {
			this.pluginTimeout = other.pluginTimeout;
		}
		if (other.execPipe != null) {
			this.execPipe = other.execPipe;
		}
		if (other.experimental != null) {
			this.experimental = other.experimental;
		}
		if (other.datasources != null) {
			this.datasources = other.datasources;
		}
		if (other.context != null) {
			this.context = other.context;
		}
		if (other.templates != null) {
			this.templates = other.templates;
		}
		if (other.plugins != null) {
			this.plugins = other.plugins;
		}
		return this;
	}

}
