package org.alexmond.jgomplate.sample;

import java.util.Map;

import org.alexmond.jgomplate.core.GomplateEngine;

/**
 * Minimal example of embedding {@link GomplateEngine} to render a gomplate template with
 * the gomplate namespaces ({@code strings.*}) and a context value.
 */
public final class Sample {

	private Sample() {
	}

	public static void main(String[] args) {
		GomplateEngine engine = new GomplateEngine();
		String out = engine.render("Hello, {{ strings.ToUpper .name }}!", Map.of("name", "world"));
		System.out.println(out);
	}

}
