package org.alexmond.jgomplate.core.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * A single plugin entry from a {@code .gomplate.yaml} {@code plugins} map, mirroring
 * gomplate's {@code PluginConfig} struct.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginConfig {

	/** The command (executable) to run for this plugin. */
	private String cmd;

	/**
	 * Fixed leading arguments passed to the command before the template-supplied ones.
	 */
	private List<String> args;

	/**
	 * Per-invocation timeout (Go duration string, e.g. {@code 5s}); {@code null} =
	 * default.
	 */
	private String timeout;

	/**
	 * When {@code true}, template args are piped to the command's stdin rather than argv.
	 */
	private Boolean pipe;

}
