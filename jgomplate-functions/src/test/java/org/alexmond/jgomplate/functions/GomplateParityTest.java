package org.alexmond.jgomplate.functions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.alexmond.gotmpl4j.GoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parity tests for the implemented gomplate namespaces, with cases ported from gomplate's
 * own Go test suite (conv_test.go, coll_test.go, strings_test.go) so behaviour — not just
 * wiring — is pinned to the upstream {@code gomplate} binary.
 *
 * <p>
 * Cases are batched as {@code template | expected} rows via {@link CsvSource} (delimiter
 * {@code |}, so template commas need no escaping; JUnit's default quote char {@code '}
 * makes {@code ''} an empty expected). Each row renders through the real engine and
 * asserts the rendered string. Cases that need a {@code null} in the data context — which
 * a Go template has no literal for — stay as individual {@link Test}s.
 */
class GomplateParityTest {

	private String render(String text) {
		return new GoTemplate().parse("t", text).render(Map.of());
	}

	private String render(String text, Map<String, Object> data) {
		return new GoTemplate().parse("t", text).render(data);
	}

	/** {@code conv} namespace. Cases mirror gomplate's conv/conv_test.go. */
	@Nested
	class Conv {

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// truthy: "1"/"t"/"true"/"yes" (any case) or a numeric string == 1
				"{{ conv.ToBool \"1\" }}    | true", "{{ conv.ToBool \"t\" }}    | true",
				"{{ conv.ToBool \"T\" }}    | true", "{{ conv.ToBool \"true\" }} | true",
				"{{ conv.ToBool \"True\" }} | true", "{{ conv.ToBool \"TrUe\" }} | true",
				"{{ conv.ToBool \"yes\" }}  | true", "{{ conv.ToBool \"YES\" }}  | true",
				"{{ conv.ToBool \"0x1\" }}  | true", "{{ conv.ToBool \"1.0\" }}  | true",
				"{{ conv.ToBool \"01\" }}   | true",
				// a number is true only when it equals 1
				"{{ conv.ToBool 1 }}        | true", "{{ conv.ToBool 0 }}        | false",
				"{{ conv.ToBool 42 }}       | false",
				// everything else is false — note "on" and "42" are NOT truthy
				"{{ conv.ToBool \"\" }}     | false", "{{ conv.ToBool \"false\" }}| false",
				"{{ conv.ToBool \"foo\" }}  | false", "{{ conv.ToBool \"0xFFFF\" }}| false",
				"{{ conv.ToBool \"010\" }}  | false", "{{ conv.ToBool \"on\" }}   | false",
				"{{ conv.ToBool \"no\" }}   | false", "{{ conv.ToBool \"42\" }}   | false" })
		void toBool(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = { "{{ conv.ToString \"foo\" }} | foo", "{{ conv.ToString 42 }}      | 42",
				"{{ conv.ToString true }}    | true", "{{ conv.ToString 3.14 }}    | 3.14" })
		void toStringScalars(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void toStringNilIsLiteralNil() {
			// gomplate conv.ToString(nil) == "nil"
			Map<String, Object> data = new HashMap<>();
			data.put("n", null);
			assertEquals("nil", render("{{ conv.ToString .n }}", data));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = { "{{ conv.Join (list \"foo\" \"bar\") \",\" }} | foo,bar",
				"{{ conv.Join (list 42 100) \",\" }}          | 42,100" })
		void join(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void joinMixedWithNilElement() {
			// Ported: []any{1, "", true, 3.14, "foo", nil} joined with "," ->
			// "1,,true,3.14,foo,nil"
			Map<String, Object> data = new HashMap<>();
			data.put("l", Arrays.asList(1, "", true, 3.14, "foo", null));
			assertEquals("1,,true,3.14,foo,nil", render("{{ conv.Join .l \",\" }}", data));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = { "{{ conv.Default \"fallback\" \"\" }}      | fallback",
				"{{ conv.Default \"fallback\" \"value\" }} | value" })
		void defaultFallsBackOnEmpty(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void defaultFallsBackOnNil() {
			Map<String, Object> data = new HashMap<>();
			data.put("n", null);
			assertEquals("fallback", render("{{ conv.Default \"fallback\" .n }}", data));
		}

	}

	/**
	 * {@code strings} namespace. Cases mirror gomplate's internal/funcs/strings_test.go.
	 */
	@Nested
	class Strings {

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// ReplaceAll arg order: old new input
				"{{ strings.ReplaceAll \"Orig\" \"Replaced\" \"Orig\" }}     | Replaced",
				"{{ strings.ReplaceAll \"Orig\" \"Replaced\" \"OrigOrig\" }} | ReplacedReplaced",
				"{{ strings.ToUpper \"hello\" }}                             | HELLO",
				"{{ strings.ToLower \"HeLLo\" }}                             | hello",
				"{{ strings.TrimSpace \"  go  \" }}                          | go",
				// \\t \\n are escapes for the Go template lexer, not Java
				"{{ strings.TrimSpace \"\\t\\nfoo\\n\\t\" }}                 | foo",
				// Contains arg order: substr input
				"{{ strings.Contains \"ell\" \"hello\" }}                    | true",
				"{{ strings.Contains \"z\" \"hello\" }}                      | false",
				"{{ strings.HasPrefix \"foo\" \"foobar\" }}                  | true",
				"{{ strings.HasPrefix \"bar\" \"foobar\" }}                  | false",
				"{{ strings.HasSuffix \"bar\" \"foobar\" }}                  | true",
				"{{ strings.HasSuffix \"foo\" \"foobar\" }}                  | false",
				"{{ strings.Repeat 0 \"ab\" }}                               | ''",
				"{{ strings.Repeat 3 \"ab\" }}                               | ababab",
				// Trim/TrimLeft/TrimRight arg order: cutset input
				"{{ strings.Trim \"$\" \"$$foo$$\" }}                        | foo",
				"{{ strings.TrimLeft \"-_\" \"-_fooBAR\" }}                  | fooBAR",
				"{{ strings.TrimRight \"-_\" \"fooBAR-_\" }}                 | fooBAR",
				// TrimPrefix/TrimSuffix arg order: affix input
				"{{ strings.TrimPrefix \"Foo\" \"FooBar\" }}                 | Bar",
				"{{ strings.TrimSuffix \"Bar\" \"FooBar\" }}                 | Foo",
				// Trunc length input — first length chars; <0 keeps the whole string
				"{{ strings.Trunc 3 \"123456789\" }}                        | 123",
				"{{ strings.Trunc -1 \"hello, world\" }}                     | hello, world",
				"{{ strings.Trunc 5 \"\" }}                                  | ''" })
		void strings(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// Split/SplitN arg order: sep [n] input; literal (non-regex) split
				"{{ len (strings.Split \",\" \"a,b,c\") }}          | 3",
				"{{ index (strings.Split \",\" \"a,b,c\") 1 }}      | b",
				"{{ len (strings.SplitN \",\" 2 \"a,b,c\") }}       | 2",
				"{{ index (strings.SplitN \",\" 2 \"a,b,c\") 1 }}   | b,c" })
		void split(String template, String expected) {
			assertEquals(expected, render(template));
		}

	}

	/** {@code math} namespace. Cases mirror gomplate's internal/funcs/math_test.go. */
	@Nested
	class Math {

		@ParameterizedTest
		@CsvSource(delimiter = '|',
				value = { "{{ math.IsInt 42 }}        | true", "{{ math.IsInt 3.14 }}      | false",
						"{{ math.IsInt \"42\" }}     | true", "{{ math.IsInt \"abc\" }}    | false",
						"{{ math.IsFloat 3.14 }}     | true", "{{ math.IsFloat 42 }}       | false",
						"{{ math.IsFloat \"3.14\" }} | true", "{{ math.IsFloat \"42\" }}   | false",
						"{{ math.IsNum 42 }}         | true", "{{ math.IsNum 3.14 }}       | true",
						"{{ math.IsNum \"abc\" }}    | false" })
		void predicates(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// Abs/Sub/Pow keep integer inputs integral; Div is always float
				"{{ math.Abs -5 }}    | 5", "{{ math.Abs -5.5 }}  | 5.5", "{{ math.Abs 0 }}     | 0",
				"{{ math.Sub 1 1 }}   | 0", "{{ math.Sub -5 5 }}  | -10", "{{ math.Sub 5 2.5 }} | 2.5",
				"{{ math.Div 1 2 }}   | 0.5", "{{ math.Div -5 5 }}  | -1", "{{ math.Div 10 4 }}  | 2.5",
				"{{ math.Rem 5 3 }}   | 2", "{{ math.Rem 10 3 }}  | 1", "{{ math.Pow 2 2 }}   | 4",
				"{{ math.Pow 2 10 }}  | 1024", "{{ math.Pow 1.5 2 }} | 2.25",
				// Ceil/Floor round toward +/-inf; Round is half away from zero
				"{{ math.Ceil 4.99 }} | 5", "{{ math.Ceil 4.01 }} | 5", "{{ math.Floor 4.99 }}| 4",
				"{{ math.Round 4.99 }}| 5", "{{ math.Round 4.4 }} | 4", "{{ math.Round 2.5 }} | 3" })
		void arithmetic(String template, String expected) {
			assertEquals(expected, render(template));
		}

	}

	/** {@code coll} namespace. Cases mirror gomplate's coll/coll_test.go. */
	@Nested
	class Coll {

		@ParameterizedTest
		@CsvSource(delimiter = '|',
				value = { "{{ coll.Has (list \"foo\" \"bar\" \"baz\") \"bar\" }}  | true",
						"{{ coll.Has (list \"foo\" \"bar\" \"baz\") 42 }}       | false",
						"{{ coll.Has (list 1 2 42) 42 }}                        | true",
						// on a map, coll.Has tests key presence, not values
						"{{ coll.Has (dict \"foo\" \"bar\") \"foo\" }}          | true",
						"{{ coll.Has (dict \"foo\" \"bar\") \"bar\" }}          | false",
						// Reverse — iterate the reversed list so the assertion is
						// render-agnostic
						"{{ range coll.Reverse (list 1 2 3 4) }}{{ . }}{{ end }} | 4321",
						"{{ range coll.Reverse (list 8) }}{{ . }}{{ end }}       | 8",
						"{{ range coll.Reverse (list) }}x{{ end }}               | ''" })
		void coll(String template, String expected) {
			assertEquals(expected, render(template));
		}

	}

}
