package org.alexmond.jgomplate.core;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MissingKeyTest {

	private final GomplateEngine engine = new GomplateEngine();

	private static final Map<String, Object> CTX = Map.of("name", "x");

	@Test
	void errorModeThrowsOnMissingKey() {
		assertThrows(RuntimeException.class, () -> this.engine.render("{{ .missing }}", CTX, "error"));
	}

	@Test
	void zeroModeRendersEmpty() {
		assertEquals("", this.engine.render("{{ .missing }}", CTX, "zero"));
	}

	@Test
	void defaultModeRendersNoValue() {
		assertEquals("<no value>", this.engine.render("{{ .missing }}", CTX, "default"));
	}

	@Test
	void invalidModeIsTreatedAsDefault() {
		assertEquals("<no value>", this.engine.render("{{ .missing }}", CTX, "invalid"));
	}

	@Test
	void blankModeLeavesEngineDefault() {
		assertEquals("<no value>", this.engine.render("{{ .missing }}", CTX, ""));
		assertEquals("<no value>", this.engine.render("{{ .missing }}", CTX, null));
	}

	@Test
	void modeIsCaseInsensitive() {
		assertEquals("", this.engine.render("{{ .missing }}", CTX, "ZERO"));
	}

	@Test
	void unknownModeThrowsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> this.engine.render("{{ .x }}", CTX, "bogus"));
	}

	@Test
	void presentKeysRenderNormallyInErrorMode() {
		assertEquals("x", this.engine.render("{{ .name }}", CTX, "error"));
	}

}
