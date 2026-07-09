package org.alexmond.jgomplate.core;

import java.util.Locale;
import java.util.Map;

import org.alexmond.gotmpl4j.Function;
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
 * Renders a template string against an in-memory context, with optional overloads for a
 * missing-key mode, extra per-render functions (e.g. the {@code datasource}/{@code ds}/
 * {@code include} functions bound to a CLI invocation), and named partial templates
 * invokable via {@code {{ template "name" . }}}.
 */
public class GomplateEngine {

	/**
	 * Render {@code templateText} against {@code context}.
	 * @param templateText the gomplate/Go template source
	 * @param context the root data exposed as {@code .}; may be empty
	 * @return the rendered output
	 */
	public String render(String templateText, Map<String, Object> context) {
		return render(templateText, context, null);
	}

	/**
	 * Render {@code templateText} against {@code context} with an explicit missing-key
	 * behaviour.
	 * @param templateText the gomplate/Go template source
	 * @param context the root data exposed as {@code .}; may be empty
	 * @param missingKey the Go {@code missingkey} token — one of {@code error},
	 * {@code zero}, {@code default} or {@code invalid}; {@code null}/blank leaves the
	 * engine default (Go's {@code default}: absent keys render as {@code <no value>})
	 * @return the rendered output
	 * @throws IllegalArgumentException if {@code missingKey} is not a recognised token
	 */
	public String render(String templateText, Map<String, Object> context, String missingKey) {
		return render(templateText, context, missingKey, null);
	}

	/**
	 * Render {@code templateText} with an explicit missing-key behaviour and extra
	 * per-render functions (e.g. datasource functions bound to a CLI invocation) merged
	 * on top of the auto-discovered providers.
	 * @param templateText the gomplate/Go template source
	 * @param context the root data exposed as {@code .}; may be empty
	 * @param missingKey the Go {@code missingkey} token, or {@code null}/blank for the
	 * default
	 * @param extraFunctions functions to add for this render (may be {@code null}/empty)
	 * @return the rendered output
	 * @throws IllegalArgumentException if {@code missingKey} is not a recognised token
	 */
	public String render(String templateText, Map<String, Object> context, String missingKey,
			Map<String, Function> extraFunctions) {
		return render(templateText, context, missingKey, extraFunctions, null);
	}

	/**
	 * Render {@code templateText} with extra functions and a set of named partial
	 * templates (backing gomplate's {@code -t}), each invokable from the main template
	 * via {@code {{ template "name" . }}}.
	 * @param templateText the main gomplate/Go template source
	 * @param context the root data exposed as {@code .}; may be empty
	 * @param missingKey the Go {@code missingkey} token, or {@code null}/blank for the
	 * default
	 * @param extraFunctions functions to add for this render (may be {@code null}/empty)
	 * @param partials {@code name -> template source} definitions parsed alongside the
	 * main template (may be {@code null}/empty)
	 * @return the rendered output
	 * @throws IllegalArgumentException if {@code missingKey} is not a recognised token
	 */
	public String render(String templateText, Map<String, Object> context, String missingKey,
			Map<String, Function> extraFunctions, Map<String, String> partials) {
		GoTemplate.Builder builder = GoTemplate.builder();
		if (missingKey != null && !missingKey.isBlank()) {
			builder.option("missingkey=" + missingKey.trim().toLowerCase(Locale.ROOT));
		}
		if (extraFunctions != null && !extraFunctions.isEmpty()) {
			builder.withFunctions(extraFunctions);
		}
		GoTemplate template = builder.build();
		if (partials != null) {
			for (Map.Entry<String, String> partial : partials.entrySet()) {
				template.parse(partial.getKey(), partial.getValue());
			}
		}
		template.parse("template", templateText);
		return template.render("template", (context != null) ? context : Map.of());
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
