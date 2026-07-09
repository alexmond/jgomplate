package org.alexmond.jgomplate.core;

import java.util.Map;

import org.alexmond.gotmpl4j.Function;

/**
 * The per-render settings threaded from a
 * {@link org.alexmond.jgomplate.core.config.GomplateConfig} through the
 * {@link GomplateEngine}: missing-key mode, extra functions (datasource functions), named
 * partial templates, and custom action delimiters. Bundling them keeps the
 * engine/renderer signatures small as the feature set grows.
 *
 * @param missingKey the Go {@code missingkey} token, or {@code null}/blank for the
 * default
 * @param functions per-render functions merged on top of the auto-discovered providers
 * (may be {@code null})
 * @param partials {@code name -> source} partial templates (may be {@code null})
 * @param leftDelim the left action delimiter, or {@code null}/blank for the default
 * (double open-brace)
 * @param rightDelim the right action delimiter, or {@code null}/blank for the default
 * (double close-brace)
 */
public record RenderOptions(String missingKey, Map<String, Function> functions, Map<String, String> partials,
		String leftDelim, String rightDelim) {

	/**
	 * An options carrier with everything unset (engine defaults).
	 * @return empty options
	 */
	public static RenderOptions none() {
		return new RenderOptions(null, null, null, null, null);
	}

}
