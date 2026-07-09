package org.alexmond.jgomplate.functions;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.alexmond.gotmpl4j.GoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves the gomplate function pack is discovered through gotmpl4j's
 * {@link java.util.ServiceLoader} and that namespaced calls ({@code strings.ToUpper}, …)
 * resolve via Go-template method calls on the returned namespace objects.
 */
class GomplateNamespaceTest {

	private String render(String text) {
		return new GoTemplate().parse("t", text).render(Map.of());
	}

	@Test
	void stringsNamespaceRenders() {
		assertEquals("HELLO", render("{{ strings.ToUpper \"hello\" }}"));
		assertEquals("go", render("{{ strings.TrimSpace \"  go  \" }}"));
		assertEquals("abcabc", render("{{ strings.Repeat 2 \"abc\" }}"));
	}

	@Test
	void convNamespaceRenders() {
		assertEquals("true", render("{{ conv.ToBool \"yes\" }}"));
		assertEquals("fallback", render("{{ conv.Default \"fallback\" \"\" }}"));
	}

	@Test
	void collNamespaceRenders() {
		// Lists are built with Sprig's variadic `list` builder rather than the variadic
		// `coll.Slice` method: gotmpl4j's executor matches namespace *methods* by exact
		// parameter count (Executor#tryMethodInvoke) and does not yet unpack Java
		// varargs,
		// so a variadic method call resolves to nothing. Native functions like `list` are
		// variadic and work. See the varargs-method-support follow-up.
		assertEquals("a,b,c", render("{{ conv.Join (list \"a\" \"b\" \"c\") \",\" }}"));
		assertEquals("true", render("{{ coll.Has (list \"a\" \"b\") \"b\" }}"));
	}

	@Test
	void sprigBaselineStillWorks() {
		// gomplate assumes the Sprig baseline; confirm a Sprig function coexists with the
		// gomplate namespaces (provider priority 100 vs 200, no collision).
		assertEquals("HELLO", render("{{ \"hello\" | upper }}"));
	}

}
