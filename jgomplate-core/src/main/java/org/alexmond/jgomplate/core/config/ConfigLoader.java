package org.alexmond.jgomplate.core.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Resolves and parses the jgomplate config file, mirroring gomplate's
 * {@code internal/cmd} precedence:
 *
 * <ol>
 * <li>an explicit {@code --config} path (required — missing is an error),</li>
 * <li>otherwise the {@code $GOMPLATE_CONFIG} environment variable (required),</li>
 * <li>otherwise the default {@code .gomplate.yaml} (optional — missing is skipped
 * silently).</li>
 * </ol>
 */
public class ConfigLoader {

	/** The config file consulted when neither {@code --config} nor the env var is set. */
	public static final String DEFAULT_CONFIG_FILE = ".gomplate.yaml";

	/** Environment variable naming an alternate config file. */
	public static final String CONFIG_ENV = "GOMPLATE_CONFIG";

	private final YAMLMapper yaml = new YAMLMapper();

	/**
	 * Load config following gomplate's precedence, reading the env from the process.
	 * @param configFlag the value of {@code --config}, or {@code null} if not passed
	 * @return the parsed config, or an empty config when the (optional default) file is
	 * absent
	 * @throws UncheckedIOException if a required file is missing or cannot be parsed
	 */
	public GomplateConfig load(String configFlag) {
		return load(configFlag, System.getenv(CONFIG_ENV));
	}

	/**
	 * Load config with the environment value supplied explicitly (testable seam).
	 * @param configFlag the value of {@code --config}, or {@code null} if not passed
	 * @param configEnv the value of {@code $GOMPLATE_CONFIG}, or {@code null}
	 * @return the parsed config, or an empty config when the optional default is absent
	 * @throws UncheckedIOException if a required file is missing or cannot be parsed
	 */
	public GomplateConfig load(String configFlag, String configEnv) {
		String pathStr;
		boolean required;
		if (configFlag != null && !configFlag.isBlank()) {
			pathStr = configFlag;
			required = true;
		}
		else if (configEnv != null && !configEnv.isBlank()) {
			pathStr = configEnv;
			required = true;
		}
		else {
			pathStr = DEFAULT_CONFIG_FILE;
			required = false;
		}

		Path path = Path.of(pathStr);
		if (!Files.exists(path)) {
			if (required) {
				throw new UncheckedIOException(new IOException("config file not found: " + pathStr));
			}
			return new GomplateConfig();
		}
		try {
			GomplateConfig config = this.yaml.readValue(Files.readAllBytes(path), GomplateConfig.class);
			return (config != null) ? config : new GomplateConfig();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read config file '" + pathStr + "'", ex);
		}
	}

}
