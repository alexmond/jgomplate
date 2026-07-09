package org.alexmond.jgomplate.core.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * A single datasource entry from a {@code .gomplate.yaml} {@code datasources} /
 * {@code context} map, mirroring gomplate's {@code DataSource} config struct.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSourceConfig {

	/** The datasource URL (e.g. {@code file:///path/data.yaml}, {@code https://…}). */
	private String url;

	/** Optional HTTP headers to send when resolving the datasource (name → values). */
	private Map<String, List<String>> header;

}
