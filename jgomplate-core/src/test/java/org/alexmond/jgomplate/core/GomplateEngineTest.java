package org.alexmond.jgomplate.core;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GomplateEngineTest {

	private final GomplateEngine engine = new GomplateEngine();

	@Test
	void rendersGomplateNamespaceThroughEngine() {
		assertEquals("HELLO", engine.render("{{ strings.ToUpper \"hello\" }}"));
	}

	@Test
	void rendersContextData() {
		assertEquals("hi alex", engine.render("hi {{ .name }}", Map.of("name", "alex")));
	}

	@Test
	void rendersSprigBaseline() {
		assertEquals("HELLO", engine.render("{{ \"hello\" | upper }}"));
	}

}
