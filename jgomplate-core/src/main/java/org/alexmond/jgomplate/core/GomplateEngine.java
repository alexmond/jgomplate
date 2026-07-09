package org.alexmond.jgomplate.core;

import java.util.Map;

import org.alexmond.gotmpl4j.GoTemplate;

/**
 * Renders gomplate-style templates on the JVM.
 *
 * <p>
 * A thin façade over gotmpl4j: it builds a {@link GoTemplate} whose function set is the
 * Go builtins plus every {@link org.alexmond.gotmpl4j.FunctionProvider} on the classpath
 * — which, in a jgomplate deployment, means Sprig (priority 100) and the gomplate
 * namespaces (priority 200, from {@code jgomplate-functions}). Callers supply a context
 * map that becomes the template's root data ({@code .}).
 *
 * <p>
 * This seed renders a single template string against an in-memory context. Datasource
 * wiring ({@code datasource}/{@code ds} functions backed by
 * {@link org.alexmond.jgomplate.core.datasource}) and the full gomplate CLI context are
 * follow-up work.
 */
public class GomplateEngine {

	/**
	 * Render {@code templateText} against {@code context}.
	 * @param templateText the gomplate/Go template source
	 * @param context the root data exposed as {@code .}; may be empty
	 * @return the rendered output
	 */
	public String render(String templateText, Map<String, Object> context) {
		return new GoTemplate().parse("template", templateText).render((context != null) ? context : Map.of());
	}

	/**
	 * Render {@code templateText} with no context.
	 * @param templateText the gomplate/Go template source
	 * @return the rendered output
	 */
	public String render(String templateText) {
		return render(templateText, Map.of());
	}

}
