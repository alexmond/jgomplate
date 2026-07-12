package org.alexmond.jgomplate.functions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mindrot.jbcrypt.BCrypt;

import org.alexmond.gotmpl4j.GoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// ToInt64/ToInt: base-0 strings, float truncation, bool coercion
				"{{ conv.ToInt64 \"42\" }}     | 42", "{{ conv.ToInt64 \"0x1A\" }}   | 26",
				"{{ conv.ToInt64 3.99 }}       | 3", "{{ conv.ToInt64 true }}       | 1",
				"{{ conv.ToInt \"42\" }}       | 42", "{{ conv.ToInt 3.99 }}         | 3",
				// ToFloat64: fractional values (whole-float rendering is engine-defined)
				"{{ conv.ToFloat64 \"3.14\" }} | 3.14", "{{ conv.ToFloat64 \"1.5\" }}  | 1.5",
				// Parse*: explicit base; bitSize accepted but unused on the JVM
				"{{ conv.ParseInt \"FF\" 16 64 }}   | 255", "{{ conv.ParseInt \"-42\" 10 64 }} | -42",
				"{{ conv.ParseInt \"0x1A\" 0 64 }}  | 26", "{{ conv.ParseFloat \"3.14\" 64 }} | 3.14",
				"{{ conv.ParseUint \"FF\" 16 64 }}  | 255", "{{ conv.Atoi \"42\" }}         | 42",
				"{{ conv.Atoi \"-7\" }}         | -7" })
		void numericCoercions(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = { // element-wise variadic coercers → a list
				"{{ range conv.ToInt64s \"42\" \"15\" 3.9 }}{{ . }} {{ end }}   | '42 15 3 '",
				"{{ range conv.ToInts \"7\" 8 }}{{ . }} {{ end }}              | '7 8 '",
				"{{ range conv.ToFloat64s \"3.14\" \"1.5\" }}{{ . }} {{ end }}  | '3.14 1.5 '",
				"{{ range conv.ToStrings 42 true \"x\" }}{{ . }} {{ end }}      | '42 true x '",
				"{{ range conv.ToBools 1 \"true\" 0 \"no\" }}{{ . }} {{ end }}  | 'true true false false '",
				// empty input → empty list
				"{{ range conv.ToInt64s }}x{{ end }}                          | ''",
				// len confirms arity is preserved
				"{{ len (conv.ToStrings \"a\" \"b\" \"c\") }}                    | 3" })
		void pluralCoercers(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void url() {
			// conv.URL parses into a Go-url-shaped object; fields are read by name
			String u = "{{ $u := conv.URL \"https://example.com:443/foo/bar?baz=qux#quux\" }}";
			assertEquals("https example.com:443 example.com 443 /foo/bar baz=qux quux",
					render(u + "{{ $u.Scheme }} {{ $u.Host }} {{ $u.Hostname }} {{ $u.Port }} "
							+ "{{ $u.Path }} {{ $u.RawQuery }} {{ $u.Fragment }}"));
			// parenthesised-call field access works too
			assertEquals("https", render("{{ (conv.URL \"https://x/p\").Scheme }}"));
			// .Query parses into name → values (form-decoded)
			assertEquals("qux", render("{{ index (index (conv.URL \"https://x/p?baz=qux\").Query \"baz\") 0 }}"));
			// userinfo is exposed as the raw string
			assertEquals("alice", render("{{ (conv.URL \"https://alice@example.com/p\").User }}"));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// base-0 parsing: a leading 0 is octal, 0x is hex
				"{{ conv.ToInt64 \"010\" }}     | 8", "{{ conv.ToInt \"010\" }}       | 8",
				"{{ conv.ToInt64 \"0xFFFF\" }}  | 65535",
				// gomplate strips thousands separators before parsing
				"{{ conv.ToInt64 \"4,096\" }}   | 4096", "{{ conv.ToInt \"4,096\" }}     | 4096",
				"{{ conv.ToInt64 \"-4,096.00\" }} | -4096", "{{ conv.ToFloat64 \"1,000.34\" }} | 1000.34",
				// float strings truncate toward zero; bool → 0/1
				"{{ conv.ToInt64 \"42.0\" }}    | 42", "{{ conv.ToInt64 \"3.5\" }}     | 3",
				"{{ conv.ToInt64 false }}       | 0", "{{ conv.ToInt false }}         | 0" })
		void numericSpecialForms(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// a truthy value passes through Default unchanged
				"{{ conv.Default \"D\" 1 }} | 1",
				// bool and nested-list elements format through Join
				"{{ conv.Join (list true false) \",\" }} | true,false",
				// extra ToString scalars
				"{{ conv.ToString \"\" }}   | ''", "{{ conv.ToString -127 }} | -127" })
		void defaultJoinToStringExtra(String template, String expected) {
			assertEquals(expected, render(template));
		}

		// NOTE: gomplate's conv.Default returns the default for any Go-template-falsy
		// input (0, false, empty list/map), via template.IsTrue. jgomplate currently
		// only falls back on "" / nil, so `conv.Default "D" 0` yields 0, not "D".
		// Tracked as a parity gap (#23); add the truthiness vectors once it is fixed.

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

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// case conversions — vectors from gomplate strings.TestCaseFuncs
				"{{ strings.SnakeCase \"Hello, World!\" }} | Hello_world",
				"{{ strings.KebabCase \"Hello, World!\" }} | Hello-world",
				"{{ strings.CamelCase \"Hello, World!\" }} | HelloWorld",
				"{{ strings.SnakeCase \"foo  bar\" }}      | foo_bar",
				"{{ strings.CamelCase \"foo  bar\" }}      | fooBar",
				"{{ strings.CamelCase \"grüne Straße\" }}  | grüneStraße",
				// Title upshifts word-initials without lowering the rest (NoLower)
				"{{ strings.Title \"hello world\" }}       | Hello World",
				"{{ strings.Title \"hello WORLD\" }}       | Hello WORLD",
				// RuneCount concatenates its args, then counts runes
				"{{ strings.RuneCount \"hello\" }}         | 5", "{{ strings.RuneCount \"a\" \"bc\" }}     | 3",
				"{{ strings.RuneCount \"héllo\" }}         | 5",
				// Quote — Go %q double-quoting
				"{{ strings.Quote \"foo\" }}               | \"foo\"",
				// Indent: 2-arg (indent string OR width int) and 3-arg (width, indent)
				"{{ strings.Indent \"  \" \"foo\" }}       | '  foo'",
				"{{ strings.Indent 2 \"foo\" }}            | '  foo'",
				"{{ strings.Indent 2 \"-\" \"foo\" }}      | --foo",
				// Abbrev — vectors from gomplate strings.TestAbbrev (ports goutils)
				"{{ strings.Abbrev 3 \"foo\" }}            | foo", "{{ strings.Abbrev 2 6 \"foobar\" }}       | foobar",
				"{{ strings.Abbrev 6 9 \"foobarbazquxquux\" }} | ...baz...",
				"{{ strings.Abbrev 9 \"The quick brown fox\" }} | The qu...",
				// Slug — vectors from gomplate strings.TestSlug (ports gosimple/slug)
				"{{ strings.Slug \"Hello, World!\" }}      | hello-world",
				"{{ strings.Slug \"foo@example.com\" }}    | fooatexample-com",
				"{{ strings.Slug \"rock & roll!\" }}       | rock-and-roll",
				"{{ strings.Slug \"100%\" }}               | 100",
				// non-ASCII Latin transliteration (ü→u, ß→ss)
				"{{ strings.Slug \"grüne Straße\" }}       | grune-strasse" })
		void caseQuoteIndent(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void indentMultiline() {
			// gomplate strings.TestIndent: each line prefixed, blank lines untouched
			assertEquals("  hello\n  world\n  !",
					render("{{ strings.Indent 1 \"  \" .s }}", Map.of("s", "hello\nworld\n!")));
			assertEquals("  foo\n", render("{{ strings.Indent 1 \"  \" .s }}", Map.of("s", "foo\n")));
		}

		@Test
		void shellQuote() {
			// vectors from gomplate strings.TestShellQuote
			assertEquals("''", render("{{ strings.ShellQuote \"\" }}"));
			assertEquals("'foo'", render("{{ strings.ShellQuote \"foo\" }}"));
			assertEquals("'hello \"world\"'", render("{{ strings.ShellQuote \"hello \\\"world\\\"\" }}"));
			assertEquals("'it'\"'\"'s its'", render("{{ strings.ShellQuote \"it's its\" }}"));
		}

		@Test
		void squote() {
			assertEquals("'foo'", render("{{ strings.Squote \"foo\" }}"));
			assertEquals("'it''s'", render("{{ strings.Squote \"it's\" }}"));
			assertEquals("''", render("{{ strings.Squote \"\" }}"));
			assertEquals("'123.4'", render("{{ strings.Squote 123.4 }}"));
		}

		@Test
		void wordWrap() {
			// default width 80 leaves a short line alone
			assertEquals("hello world", render("{{ strings.WordWrap \"hello world\" }}"));
			// width wraps at the last space within the limit
			assertEquals("The quick\nbrown fox", render("{{ strings.WordWrap 10 \"The quick brown fox\" }}"));
			// 3-arg form: width, then a custom line-break sequence
			assertEquals("The quick|brown fox", render("{{ strings.WordWrap 10 \"|\" \"The quick brown fox\" }}"));
			// a word longer than the width is left intact (wrapLongWords = false)
			assertEquals("a\nlongword\nb", render("{{ strings.WordWrap 4 \"a longword b\" }}"));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// RuneCount counts code points, not UTF-16 units — emoji are 1 each
				"{{ strings.RuneCount \"\" }}          | 0", "{{ strings.RuneCount \"foo\" \"bar\" }} | 6",
				"{{ strings.RuneCount \"😂😂\" }}       | 2",
				// Title: word-initials up, rest untouched; punctuation is a boundary
				"{{ strings.Title \"foo\" }}           | Foo", "{{ strings.Title \"foo bar\" }}     | Foo Bar",
				"{{ strings.Title \"foo,bar&baz\" }}   | Foo,Bar&Baz", "{{ strings.Title \"FOO\" }}         | FOO",
				// Quote coerces non-strings and escapes embedded quotes (Squote cases,
				// whose output is single-quote-wrapped, live in squote() — CsvSource's
				// quote char is ' so they cannot be expressed as a row here)
				"{{ strings.Quote \"\" }}              | \"\"",
				// Trim edges: empty cutset is a no-op
				"{{ strings.TrimLeft \"\" \"foo\" }}   | foo", "{{ strings.TrimRight \"\" \"foo\" }} | foo",
				// Trunc: 0 → empty; length >= len → whole string
				"{{ strings.Trunc 0 \"hello, world\" }}  | ''",
				"{{ strings.Trunc 12 \"hello, world\" }} | hello, world",
				"{{ strings.Trunc 42 \"hello, world\" }} | hello, world" })
		void runeCountTitleQuoteTrimTruncExtra(String template, String expected) {
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

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = { "{{ math.Add 1 2 }}         | 3", "{{ math.Add 1 2 3 4 5 }} | 15",
				"{{ math.Add 8 }}           | 8", "{{ math.Add 1 2.5 }}     | 3.5", "{{ math.Add 1 2 3 4.0 }}   | 10",
				"{{ math.Add -1 -2 -3 }}  | -6",
				// Mul: integral unless any arg is a float
				"{{ math.Mul 2 3 4 }}       | 24", "{{ math.Mul 5 }}         | 5", "{{ math.Mul 2 2.5 }}       | 5",
				"{{ math.Mul -2 3 }}      | -6",
				// Max / Min: mandatory first arg + variadic tail
				"{{ math.Max 1 2 3 }}       | 3", "{{ math.Max 5 }}         | 5", "{{ math.Max 1 5.5 2 }}     | 5.5",
				"{{ math.Min 3 2 1 }}     | 1", "{{ math.Min 5 }}           | 5", "{{ math.Min 3 1.5 2 }}   | 1.5" })
		void reducers(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|',
				value = { "{{ range math.Seq 3 }}{{.}} {{end}}       | '1 2 3 '",
						"{{ range math.Seq 2 4 }}{{.}} {{end}}     | '2 3 4 '",
						"{{ range math.Seq 1 6 2 }}{{.}} {{end}}   | '1 3 5 '",
						"{{ range math.Seq 5 1 }}{{.}} {{end}}     | '5 4 3 2 1 '",
						"{{ range math.Seq 1 10 0 }}{{.}} {{end}}  | ''" })
		void seq(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// predicates coerce strings: base-0 int, float, NaN
				"{{ math.IsInt \"052\" }}   | true", "{{ math.IsInt \"0xff\" }}  | true",
				"{{ math.IsInt \"-42\" }}   | true", "{{ math.IsInt true }}      | false",
				"{{ math.IsFloat \"-3.14\" }} | true", "{{ math.IsFloat \"0.00\" }}  | true",
				"{{ math.IsNum \"0xff\" }}    | true", "{{ math.IsNum \"\" }}        | false" })
		void predicateCoercion(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// Ceil toward +inf, Floor toward -inf, Round half away from zero —
				// negatives
				"{{ math.Ceil 42.1 }}  | 43", "{{ math.Ceil -1.9 }}  | -1", "{{ math.Floor 42.1 }} | 42",
				"{{ math.Floor -1.9 }} | -2", "{{ math.Round -1.9 }} | -2", "{{ math.Round -3.5 }} | -4",
				"{{ math.Round -4.5 }} | -5", "{{ math.Abs 3.14 }}   | 3.14", "{{ math.Abs -1.9 }}   | 1.9" })
		void roundingNegatives(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// reducers coerce string/hex/bool args
				"{{ math.Max \"14\" \"0xff\" -5 }} | 255", "{{ math.Min \"14\" \"0xff\" -5 }} | -5",
				"{{ math.Mul \"-5\" 5 }} | -25", "{{ math.Mul 14 \"2\" }} | 28",
				// Seq single-arg counts 1→n (descending here); step never overshoots the
				// end
				"{{ range math.Seq 0 }}{{.}} {{end}}     | '1 0 '",
				"{{ range math.Seq 0 5 2 }}{{.}} {{end}} | '0 2 4 '" })
		void reducerCoercionAndSeqEdges(String template, String expected) {
			assertEquals(expected, render(template));
		}

	}

	/**
	 * {@code base64} namespace. Cases mirror gomplate's internal/funcs/base64_test.go.
	 */
	@Nested
	class Base64 {

		@ParameterizedTest
		@CsvSource(delimiter = '|',
				value = { "{{ base64.Encode \"hello world\" }}          | aGVsbG8gd29ybGQ=",
						"{{ base64.Encode \"\" }}                       | ''",
						"{{ base64.Decode \"aGVsbG8gd29ybGQ=\" }}       | hello world",
						// standard decode fails on '_', falls back to the URL-safe
						// alphabet
						"{{ base64.Decode \"aGVsbG8_\" }}               | hello?",
						// round-trip through Encode/Decode
						"{{ base64.Decode (base64.Encode \"foo bar\") }} | foo bar",
						// DecodeBytes returns raw bytes; Encode re-encodes the byte[]
						// directly
						"{{ base64.Encode (base64.DecodeBytes \"aGVsbG8=\") }} | aGVsbG8=" })
		void base64(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// RFC 4648 §10 padding ladder
				"{{ base64.Encode \"f\" }}      | Zg==", "{{ base64.Encode \"fo\" }}     | Zm8=",
				"{{ base64.Encode \"foo\" }}    | Zm9v", "{{ base64.Encode \"foob\" }}   | Zm9vYg==",
				"{{ base64.Encode \"fooba\" }}  | Zm9vYmE=", "{{ base64.Encode \"foobar\" }} | Zm9vYmFy",
				"{{ base64.Decode \"Zg==\" }}   | f", "{{ base64.Decode \"Zm8=\" }}   | fo",
				"{{ base64.Decode \"Zm9vYg==\" }} | foob" })
		void encodeLadder(String template, String expected) {
			assertEquals(expected, render(template));
		}

	}

	/**
	 * {@code crypto} namespace (hash digests). Vectors are the NIST "abc" test vectors.
	 */
	@Nested
	class Crypto {

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = { "{{ crypto.SHA1 \"abc\" }}   | a9993e364706816aba3e25717850c26c9cd0d89d",
				"{{ crypto.SHA224 \"abc\" }} | 23097d223405d8228642a477bda255b32aadbce4bda0b3f7e36c9da7",
				"{{ crypto.SHA256 \"abc\" }} | ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
				"{{ crypto.SHA384 \"abc\" }} | cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
				"{{ crypto.SHA512 \"abc\" }} | ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
				"{{ crypto.SHA512_224 \"abc\" }} | 4634270f707b6a54daae7530460842e20e37ed265ceee9a43e8924aa",
				"{{ crypto.SHA512_256 \"abc\" }} | 53048e2681941ef99b2e29b76b4c7dabe4c2d0c634fc6d46e0e2f13107e7af23" })
		void shaDigests(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void sha256OfEmptyString() {
			assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
					render("{{ crypto.SHA256 \"\" }}"));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// RFC 6070 PBKDF2-HMAC-SHA1 vectors (SHA1 is gomplate's default)
				"{{ crypto.PBKDF2 \"password\" \"salt\" 1 20 }}        | 0c60c80f961f0e71f3a9b524af6012062fe037a6",
				"{{ crypto.PBKDF2 \"password\" \"salt\" 2 20 }}        | ea6c014dc72d6f8ccd1ed92ace1d41f0d8de8957",
				"{{ crypto.PBKDF2 \"password\" \"salt\" 1 20 \"SHA1\" }}   | 0c60c80f961f0e71f3a9b524af6012062fe037a6",
				"{{ crypto.PBKDF2 \"password\" \"salt\" 1 32 \"SHA256\" }} | 120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b" })
		void pbkdf2(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void bcryptDefaultCostRoundTrips() {
			// Bcrypt is salted/non-deterministic — assert the format and that it
			// verifies.
			String hash = render("{{ crypto.Bcrypt \"secret\" }}");
			assertTrue(hash.startsWith("$2a$10$"), hash);
			assertTrue(BCrypt.checkpw("secret", hash), "hash must verify");
		}

		@Test
		void bcryptExplicitCostRoundTrips() {
			String hash = render("{{ crypto.Bcrypt 6 \"secret\" }}");
			assertTrue(hash.startsWith("$2a$06$"), hash);
			assertTrue(BCrypt.checkpw("secret", hash), "hash must verify");
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// IEEE 802.11i test vectors, iter=4096, keylen in bytes → 2× hex chars
				"{{ crypto.PBKDF2 \"password\" \"IEEE\" 4096 32 }} "
						+ "| f42c6fc52df0ebef9ebb4b90b38a5f902e83fe1b135a70e23aed762e9710a12e",
				"{{ crypto.PBKDF2 \"ThisIsAPassword\" \"ThisIsASSID\" 4096 32 }} "
						+ "| 0dc0d6eb90555ed6419756b9a15ec3e3209b63df707dd508d14581f8982721af",
				"{{ crypto.PBKDF2 \"password\" \"IEEE\" 4096 64 \"SHA512\" }} | c16f4cb6d03e23614399dee5e7f676fb1"
						+ "da0eb9471b6a74a6c5bc934c6ec7d2ab7028fbb1000b1beb97f17646045d8144792352f6676d13b20a4c03754903d7e" })
		void pbkdf2IEEE(String template, String expected) {
			assertEquals(expected, render(template));
		}

		// NOTE: gomplate clamps a bcrypt cost < 4 up to the default cost (10) — Go's
		// bcrypt.GenerateFromPassword silently substitutes DefaultCost. jgomplate's
		// jBCrypt rejects cost < 4, so `crypto.Bcrypt 0 "secret"` yields <no value>.
		// Tracked as a parity gap (#23); add the clamp vector once it is fixed.

		@Test
		void bcryptAcceptsMinimumCost() {
			String hash = render("{{ crypto.Bcrypt 4 \"secret\" }}");
			assertTrue(hash.startsWith("$2a$04$"), hash);
			assertTrue(BCrypt.checkpw("secret", hash), "hash must verify");
		}

	}

	/**
	 * {@code env} namespace. JGOMPLATE_TEST_VAR=hello is set for the test JVM (surefire
	 * config) so the value-present path is deterministic; the unset/default paths need no
	 * setup.
	 */
	@Nested
	class Env {

		@ParameterizedTest
		@CsvSource(delimiter = '|',
				value = { "{{ env.Getenv \"JGOMPLATE_TEST_VAR\" }}          | hello",
						"{{ env.Getenv \"JGOMPLATE_UNSET_XYZ\" }}         | ''",
						"{{ env.Getenv \"JGOMPLATE_UNSET_XYZ\" \"fb\" }}  | fb",
						"{{ env.Getenv \"JGOMPLATE_TEST_VAR\" \"fb\" }}   | hello",
						"{{ env.HasEnv \"JGOMPLATE_TEST_VAR\" }}          | true",
						"{{ env.HasEnv \"JGOMPLATE_UNSET_XYZ\" }}         | false",
						"{{ index (env.Env) \"JGOMPLATE_TEST_VAR\" }}     | hello",
						// ExpandEnv: $VAR and ${VAR}; unset expands to empty
						"{{ env.ExpandEnv \"v=$JGOMPLATE_TEST_VAR!\" }}   | v=hello!",
						"{{ env.ExpandEnv \"v=${JGOMPLATE_TEST_VAR}!\" }} | v=hello!",
						"{{ env.ExpandEnv \"x=$JGOMPLATE_UNSET_XYZ!\" }}  | x=!",
						"{{ env.ExpandEnv \"no vars here\" }}             | no vars here" })
		void envLookup(String template, String expected) {
			assertEquals(expected, render(template));
		}

	}

	/** {@code data} namespace (structured-data parse/serialise). */
	@Nested
	class Data {

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// parse then index
				"{{ index (data.JSON \"{\\\"a\\\":1,\\\"b\\\":2}\") \"a\" }}   | 1",
				"{{ index (data.JSONArray \"[10,20,30]\") 1 }}                  | 20",
				"{{ index (data.YAML \"foo: bar\") \"foo\" }}                    | bar",
				"{{ index (data.YAMLArray \"- 10\\n- 20\") 1 }}                 | 20",
				// ToJSON is canonical: keys sorted, compact
				"{{ data.ToJSON (data.JSON \"{\\\"b\\\":2,\\\"a\\\":1}\") }}     | {\"a\":1,\"b\":2}",
				"{{ data.ToJSON (data.JSONArray \"[2,1,3]\") }}                 | [2,1,3]",
				// cross-format: YAML in, canonical JSON out (sorted)
				"{{ data.ToJSON (data.YAML \"b: 2\\na: 1\") }}                  | {\"a\":1,\"b\":2}",
				// ToYAML (trimmed to drop the trailing newline)
				"{{ strings.TrimSpace (data.ToYAML (data.JSON \"{\\\"a\\\":1}\")) }} | a: 1" })
		void parseAndSerialise(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void toJSONPretty() {
			// Go json.Indent: sorted keys, per-level indent, "key": value, no trailing
			// newline.
			assertEquals("{\n  \"a\": 1,\n  \"b\": 2\n}",
					render("{{ data.ToJSONPretty \"  \" (data.JSON \"{\\\"b\\\":2,\\\"a\\\":1}\") }}"));
		}

		@Test
		void toJSONPrettyArrayAndEmpty() {
			assertEquals("[\n  1,\n  2\n]", render("{{ data.ToJSONPretty \"  \" (data.JSONArray \"[1,2]\") }}"));
			// an empty object stays compact, like Go's json.Indent
			assertEquals("{}", render("{{ data.ToJSONPretty \"  \" (data.JSON \"{}\") }}"));
		}

		@Test
		void csvRead() {
			// first line is the header; CSV keeps it as row 0
			Map<String, Object> d = Map.of("in", "a,b\n1,2\n3,4");
			assertEquals("b", render("{{ index (index (data.CSV .in) 0) 1 }}", d));
			assertEquals("1", render("{{ index (index (data.CSV .in) 1) 0 }}", d));
			assertEquals("1", render("{{ index (index (data.CSVByRow .in) 0) \"a\" }}", d));
			// column "b" across the two data rows → [2, 4]
			assertEquals("4", render("{{ index (index (data.CSVByColumn .in) \"b\") 1 }}", d));
		}

		@Test
		void csvDelimiterAndHeaderArgs() {
			// single-char first arg → delimiter
			assertEquals("1", render("{{ index (index (data.CSV \";\" .in) 1) 0 }}", Map.of("in", "a;b\n1;2")));
			// empty header arg → auto-named columns A, B, …; first line stays data
			Map<String, Object> d = Map.of("in", "1,2\n3,4");
			assertEquals("1", render("{{ index (index (data.CSVByRow \"\" .in) 0) \"A\" }}", d));
			assertEquals("2", render("{{ index (index (data.CSVByRow \"\" .in) 0) \"B\" }}", d));
			// explicit header (3-arg) → every line is data
			assertEquals("x", render("{{ index (index (data.CSV \",\" \"x,y\" \"1,2\") 0) 0 }}"));
			assertEquals("1", render("{{ index (index (data.CSV \",\" \"x,y\" \"1,2\") 1) 0 }}"));
		}

		@Test
		void toCsv() {
			// RFC4180 output — CRLF terminators, minimal quoting
			assertEquals("a,b\r\n1,2\r\n", render("{{ data.ToCSV (list (list \"a\" \"b\") (list \"1\" \"2\")) }}"));
			assertEquals("a;b\r\n", render("{{ data.ToCSV \";\" (list (list \"a\" \"b\")) }}"));
			// a cell containing the delimiter is quoted
			assertEquals("\"a,b\",c\r\n", render("{{ data.ToCSV (list (list \"a,b\" \"c\")) }}"));
		}

		@Test
		void toml() {
			// parse scalars
			assertEquals("bar", render("{{ index (data.TOML \"foo = \\\"bar\\\"\") \"foo\" }}"));
			assertEquals("42", render("{{ index (data.TOML \"n = 42\") \"n\" }}"));
			// cross-format: TOML in, canonical JSON out (keys sorted)
			assertEquals("{\"a\":1,\"b\":2}", render("{{ data.ToJSON (data.TOML \"b = 2\\na = 1\") }}"));
			// ToTOML emits sorted keys; Jackson uses TOML literal (single-quoted) strings
			assertEquals("foo = 'bar'",
					render("{{ strings.TrimSpace (data.ToTOML (data.JSON \"{\\\"foo\\\":\\\"bar\\\"}\")) }}"));
			// round-trip through ToTOML → TOML
			assertEquals("9", render("{{ index (data.TOML (data.ToTOML (data.JSON \"{\\\"x\\\":9}\"))) \"x\" }}"));
		}

	}

	/** {@code uuid} namespace. Cases mirror gomplate's internal/funcs/uuid_test.go. */
	@Nested
	class Uuid {

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				"{{ uuid.Nil }}                                             | 00000000-0000-0000-0000-000000000000",
				"{{ uuid.IsValid \"00000000-0000-0000-0000-000000000000\" }} | true",
				"{{ uuid.IsValid \"a987fbc9-4bed-4078-8f07-9141ba07c9f3\" }} | true",
				"{{ uuid.IsValid \"not-a-uuid\" }}                           | false",
				"{{ uuid.IsValid \"12345\" }}                                | false",
				// Parse returns the canonical lowercase form
				"{{ uuid.Parse \"A987FBC9-4BED-4078-8F07-9141BA07C9F3\" }}   | a987fbc9-4bed-4078-8f07-9141ba07c9f3",
				// Microsoft braces, raw hex, and urn:uuid: forms all decode
				"{{ uuid.Parse \"{a987fbc9-4bed-4078-8f07-9141ba07c9f3}\" }} | a987fbc9-4bed-4078-8f07-9141ba07c9f3",
				"{{ uuid.Parse \"a987fbc94bed40788f079141ba07c9f3\" }}       | a987fbc9-4bed-4078-8f07-9141ba07c9f3",
				"{{ uuid.Parse \"urn:uuid:a987fbc9-4bed-4078-8f07-9141ba07c9f3\" }} | a987fbc9-4bed-4078-8f07-9141ba07c9f3" })
		void nilIsValidParse(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void v4HasVersion4Format() {
			// V4 is random, so assert the RFC 4122 v4 shape rather than an exact value.
			String out = render("{{ uuid.V4 }}");
			assertTrue(out.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"),
					"not a v4 UUID: " + out);
		}

	}

	/**
	 * {@code random} namespace. Output is non-deterministic — assert shape, not bytes.
	 */
	@Nested
	class Random {

		@Test
		void asciiAlphaAlphaNumShape() {
			// gomplate ASCII = printable ASCII; Alpha/AlphaNum are ASCII (Go POSIX
			// classes)
			assertTrue(render("{{ random.ASCII 100 }}").matches("[\\x20-\\x7e]{100}"));
			assertTrue(render("{{ random.Alpha 50 }}").matches("[A-Za-z]{50}"));
			assertTrue(render("{{ random.AlphaNum 50 }}").matches("[A-Za-z0-9]{50}"));
			// count 0 yields an empty string for the bounded generators
			assertEquals("", render("{{ random.ASCII 0 }}"));
		}

		@Test
		void stringDefaultAndSpecAndBounds() {
			// default set is [a-zA-Z0-9_.-]
			assertTrue(render("{{ random.String 40 }}").matches("[a-zA-Z0-9_.-]{40}"));
			// a regex-style character set
			assertTrue(render("{{ random.String 16 \"[a-f0-9]\" }}").matches("[a-f0-9]{16}"));
			// a 1-wide code-point range is deterministic — only that char
			assertEquals("aaaa", render("{{ random.String 4 \"a\" \"a\" }}"));
		}

		@Test
		void itemPicksFromList() {
			assertEquals("only", render("{{ random.Item (list \"only\") }}"));
			String pick = render("{{ random.Item (list \"a\" \"b\" \"c\") }}");
			assertTrue(pick.equals("a") || pick.equals("b") || pick.equals("c"), pick);
		}

	}

	/** {@code regexp} namespace. Cases mirror gomplate's regexp/regexp_test.go. */
	@Nested
	class Regexp {

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// Find: leftmost match
				"{{ regexp.Find \"[a-z]+\" \"foo bar baz\" }}       | foo",
				// FindAll: 2-arg (all) and 3-arg (capped at n)
				"{{ len (regexp.FindAll \"[a-z]+\" \"foo bar baz\") }}   | 3",
				"{{ index (regexp.FindAll \"[a-z]+\" 2 \"foo bar baz\") 1 }} | bar",
				"{{ len (regexp.FindAll \"[0-9]+\" \"abc\") }}          | 0",
				// Match: partial match anywhere; anchors work
				"{{ regexp.Match \"^[a-z]+$\" \"abc\" }}     | true",
				"{{ regexp.Match \"^[a-z]+$\" \"abc1\" }} | false",
				"{{ regexp.Match \"[0-9]\" \"a1b\" }}        | true",
				// Replace: $-expansion; ${1} (braced numeric) is shimmed to Java's $1
				"{{ regexp.Replace \"a(x*)b\" \"T\" \"-ab-axxb-\" }}    | -T-T-",
				"{{ regexp.Replace \"a(x*)b\" \"$1\" \"-ab-axxb-\" }}   | --xx-",
				"{{ regexp.Replace \"a(x*)b\" \"${1}W\" \"-ab-axxb-\" }} | -W-xxW-",
				// ReplaceLiteral: no expansion, $1 is literal
				"{{ regexp.ReplaceLiteral \"a(x*)b\" \"$1\" \"-ab-axxb-\" }} | -$1-$1-",
				// Split: n<0 all, n>0 caps, n==0 empty
				"{{ len (regexp.Split \"[ ]+\" \"foo  bar baz\") }}   | 3",
				"{{ index (regexp.Split \",\" \"a,b,c\") 1 }} | b", "{{ len (regexp.Split \",\" 2 \"a,b,c\") }}   | 2",
				"{{ index (regexp.Split \",\" 2 \"a,b,c\") 1 }} | b,c",
				"{{ len (regexp.Split \",\" 0 \"a,b,c\") }}   | 0",
				// QuoteMeta: Go's metacharacter set, not \\Q..\\E
				"{{ regexp.QuoteMeta \"a.b\" }}   | a\\.b", "{{ regexp.QuoteMeta \"1+1=2\" }} | 1\\+1=2" })
		void regexpFuncs(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void namedGroupReplaceViaRe2Syntax() {
			// RE2 (?P<name>) is rewritten to Java (?<name>); ${name} refs work in both
			assertEquals("bar foo", render(
					"{{ regexp.Replace \"(?P<first>[a-z]+) (?P<last>[a-z]+)\" \"${last} ${first}\" \"foo bar\" }}"));
		}

	}

	/** {@code test} namespace. Cases mirror gomplate's internal/funcs/test_test.go. */
	@Nested
	class TestNs {

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// Ternary: condition is the LAST arg; ToBool decides
				"{{ test.Ternary \"foo\" 42 false }} | 42", "{{ test.Ternary \"foo\" 42 \"yes\" }} | foo",
				"{{ test.Ternary false true true }} | false",
				// Kind: Java runtime types mapped to Go reflect-kind names (42.0
				// collapses
				// to int in the engine's number model, so a fractional float pins
				// float64)
				"{{ test.Kind \"foo\" }}      | string", "{{ test.Kind true }}         | bool",
				"{{ test.Kind 42 }}           | int", "{{ test.Kind 3.14 }}         | float64",
				"{{ test.Kind (list 1 2) }}   | slice", "{{ test.Kind (dict \"a\" 1) }} | map",
				// IsKind: exact kinds plus the "number" pseudo-kind
				"{{ test.IsKind \"number\" 42 }}   | true", "{{ test.IsKind \"number\" 3.14 }} | true",
				"{{ test.IsKind \"number\" \"foo\" }} | false", "{{ test.IsKind \"string\" \"foo\" }} | true",
				"{{ test.IsKind \"bool\" true }}   | true", "{{ test.IsKind \"int\" 42 }}      | true",
				// Required passes through a set value — 0 is set, not empty
				"{{ test.Required \"foo\" }} | foo", "{{ test.Required 0 }}       | 0",
				// Assert on a truthy value renders nothing
				"{{ test.Assert true }}     | ''", "{{ test.Assert \"msg\" true }} | ''" })
		void testFuncs(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void kindOfNilIsInvalid() {
			Map<String, Object> data = new HashMap<>();
			data.put("n", null);
			assertEquals("invalid", render("{{ test.Kind .n }}", data));
		}

		@Test
		void requiredReturnsSetValue() {
			// a present value passes through Required unchanged
			assertEquals("prod", render("{{ test.Required \"env\" .env }}", Map.of("env", "prod")));
		}

		@Test
		void failPathsCurrentlyRenderNoValue() {
			// ENGINE GAP (gotmpl4j#137): gomplate ABORTS rendering on test.Assert(false),
			// test.Fail, and test.Required(unset). gotmpl4j swallows the function's
			// thrown
			// error and renders <no value> instead (same limitation as crypto.Bcrypt's
			// error path). This pins the CURRENT behaviour so the test flips to failing —
			// a signal to restore the abort assertions — once #137 lands.
			assertEquals("<no value>", render("{{ test.Assert false }}"));
			assertEquals("<no value>", render("{{ test.Assert \"boom\" false }}"));
			assertEquals("<no value>", render("{{ test.Fail }}"));
			assertEquals("<no value>", render("{{ test.Fail \"nope\" }}"));
			assertEquals("<no value>", render("{{ test.Required \"\" }}"));
			Map<String, Object> data = new HashMap<>();
			data.put("n", null);
			assertEquals("<no value>", render("{{ test.Required \"need it\" .n }}", data));
		}

	}

	/** {@code path} namespace. Cases mirror gomplate's internal/funcs/path_test.go. */
	@Nested
	class Path {

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// pure, slash-separated (Go path package) — OS-independent
				"{{ path.Base \"foo/bar\" }}                 | bar", "{{ path.Base \"/foo/bar\" }} | bar",
				"{{ path.Clean \"/foo/bar/../baz\" }}        | /foo/baz",
				"{{ path.Dir \"foo/bar\" }}                  | foo", "{{ path.Ext \"/foo/bar/baz.txt\" }} | .txt",
				"{{ path.IsAbs \"foo/bar\" }}                | false", "{{ path.IsAbs \"/foo/bar\" }} | true",
				"{{ path.Join \"foo\" \"bar\" \"baz\" \"..\" \"qux\" }} | foo/bar/qux",
				"{{ path.Match \"*.txt\" \"foo.json\" }}     | false", "{{ path.Match \"*.txt\" \"foo.txt\" }} | true",
				// Split returns [dir, file]; dir keeps the trailing slash
				"{{ index (path.Split \"/foo/bar/baz\") 0 }} | /foo/bar/",
				"{{ index (path.Split \"/foo/bar/baz\") 1 }} | baz",
				// extra Match cases: ? matches one non-slash char, [] a class, * stops at
				// /
				"{{ path.Match \"foo?bar\" \"fooxbar\" }}    | true",
				"{{ path.Match \"foo?bar\" \"foo/bar\" }} | false",
				"{{ path.Match \"[a-c]oo\" \"boo\" }}        | true",
				"{{ path.Match \"*.txt\" \"a/b.txt\" }} | false" })
		void pathFuncs(String template, String expected) {
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
						"{{ range coll.Reverse (list) }}x{{ end }}               | ''",
						// Append/Prepend arg order: v list
						"{{ range coll.Append 3 (list 1 2) }}{{ . }}{{ end }}    | 123",
						"{{ range coll.Prepend 0 (list 1 2) }}{{ . }}{{ end }}   | 012",
						// Uniq keeps first-seen order
						"{{ range coll.Uniq (list 1 2 2 3 1) }}{{ . }}{{ end }}  | 123" })
		void coll(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|',
				value = { "{{ index (coll.Set \"b\" 2 (dict \"a\" 1)) \"b\" }}       | 2",
						"{{ index (coll.Set \"b\" 2 (dict \"a\" 1)) \"a\" }}       | 1",
						"{{ index (coll.Unset \"a\" (dict \"a\" 1 \"b\" 2)) \"b\" }} | 2",
						"{{ len (coll.Unset \"a\" (dict \"a\" 1 \"b\" 2)) }}       | 1" })
		void setAndUnset(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = { "{{ range coll.Slice 1 2 3 }}{{ . }}{{ end }}                 | 123",
				// Keys/Values: per-map sorted-key order
				"{{ range coll.Keys (dict \"c\" 3 \"a\" 1 \"b\" 2) }}{{ . }}{{ end }}     | abc",
				"{{ range coll.Values (dict \"c\" 30 \"a\" 10 \"b\" 20) }}{{ . }} {{ end }} | '10 20 30 '",
				// Dict: alternating pairs; a trailing bare key maps to ""
				"{{ index (coll.Dict \"a\" 1 \"b\" 2) \"b\" }}                   | 2",
				"{{ index (coll.Dict \"a\" 1 \"x\") \"x\" }}                     | ''",
				// Merge: left-most (dst) wins; keys only in src are pulled in
				"{{ index (coll.Merge (dict \"a\" 1) (dict \"a\" 2 \"b\" 3)) \"a\" }} | 1",
				"{{ index (coll.Merge (dict \"a\" 1) (dict \"a\" 2 \"b\" 3)) \"b\" }} | 3" })
		void mapBuilders(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|',
				value = { "{{ range coll.Sort (list 3 1 2) }}{{ . }}{{ end }}          | 123",
						"{{ range coll.Sort (list \"b\" \"a\" \"c\") }}{{ . }}{{ end }}          | abc",
						// Index: indexes come first, the item is the last argument
						"{{ coll.Index 1 (list \"a\" \"b\" \"c\") }}                     | b",
						"{{ coll.Index \"x\" (dict \"x\" 42) }}                          | 42",
						"{{ coll.Index 0 \"y\" (list (dict \"y\" 9)) }}                  | 9",
						// Pick/Omit: keys first, map last
						"{{ len (coll.Pick \"a\" \"c\" (dict \"a\" 1 \"b\" 2 \"c\" 3)) }}   | 2",
						"{{ index (coll.Pick \"a\" \"c\" (dict \"a\" 1 \"b\" 2 \"c\" 3)) \"a\" }} | 1",
						"{{ len (coll.Omit \"a\" (dict \"a\" 1 \"b\" 2 \"c\" 3)) }}        | 2",
						"{{ coll.Has (coll.Omit \"a\" (dict \"a\" 1 \"b\" 2)) \"a\" }}    | false",
						// Flatten: full by default, or limited by an explicit depth
						"{{ range coll.Flatten (list 1 (list 2 3) (list 4 (list 5))) }}{{ . }}{{ end }} | 12345",
						"{{ len (coll.Flatten 1 (list 1 (list 2 (list 3)))) }}           | 3" })
		void sortIndexPickOmitFlatten(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@Test
		void jq() {
			// a single jq output is unwrapped
			assertEquals("bar", render("{{ coll.JQ \".foo\" (dict \"foo\" \"bar\") }}"));
			assertEquals("42", render("{{ coll.JQ \".a.b\" (dict \"a\" (dict \"b\" 42)) }}"));
			// builtins: length, keys (sorted)
			assertEquals("3", render("{{ coll.JQ \"length\" (list 1 2 3) }}"));
			assertEquals("ab", render("{{ range coll.JQ \"keys\" (dict \"b\" 1 \"a\" 2) }}{{ . }}{{ end }}"));
			// a transformed array is one output → index into it
			assertEquals("4", render("{{ index (coll.JQ \"map(. * 2)\" (list 1 2 3)) 1 }}"));
			// the iterator .[] yields many outputs → a list to range over
			assertEquals("123", render("{{ range coll.JQ \".[]\" (list 1 2 3) }}{{ . }}{{ end }}"));
			// pipe
			assertEquals("2", render("{{ coll.JQ \".items | length\" (dict \"items\" (list 5 6)) }}"));
		}

		@Test
		void goSlice() {
			// Go's slice builtin: low..high on a list
			assertEquals("2", render("{{ index (coll.GoSlice (list 1 2 3 4 5) 1 3) 0 }}"));
			assertEquals("2", render("{{ len (coll.GoSlice (list 1 2 3 4 5) 1 3) }}"));
			// low-only keeps the tail
			assertEquals("345", render("{{ range coll.GoSlice (list 1 2 3 4 5) 2 }}{{ . }}{{ end }}"));
			// no indexes → the whole list
			assertEquals("abc", render("{{ range coll.GoSlice (list \"a\" \"b\" \"c\") }}{{ . }}{{ end }}"));
			// strings slice by character
			assertEquals("el", render("{{ coll.GoSlice \"hello\" 1 3 }}"));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// Sort a list of maps by a key (sort-key first, list last)
				"{{ range coll.Sort \"name\" (list (dict \"name\" \"Bart\" \"age\" 12) "
						+ "(dict \"name\" \"Maggie\" \"age\" 1) (dict \"name\" \"Lisa\" \"age\" 6)) }}{{ .name }} {{ end }} "
						+ "| 'Bart Lisa Maggie '",
				"{{ range coll.Sort \"age\" (list (dict \"name\" \"Bart\" \"age\" 12) "
						+ "(dict \"name\" \"Maggie\" \"age\" 1) (dict \"name\" \"Lisa\" \"age\" 6)) }}{{ .name }} {{ end }} "
						+ "| 'Maggie Lisa Bart '" })
		void sortByKey(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// coll.Omit filters a list too (values to drop first, list last)
				"{{ range coll.Omit \"b\" (list \"a\" \"b\" \"c\") }}{{.}}{{end}}     | ac",
				"{{ range coll.Omit \"b\" \"c\" (list \"a\" \"b\" \"c\") }}{{.}}{{end}} | a",
				// empty-string key is a real key for Pick/Omit on maps
				"{{ index (coll.Pick \"\" (dict \"foo\" \"bar\" \"\" \"baz\")) \"\" }}  | baz",
				"{{ coll.Has (coll.Omit \"\" (dict \"foo\" \"bar\" \"\" \"baz\")) \"\" }} | false" })
		void omitSliceAndEmptyKey(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// Keys/Values over several maps: each map sorted by key, then
				// concatenated
				"{{ range coll.Keys (dict \"foo\" 1 \"bar\" 2) (dict \"baz\" 3 \"qux\" 4) }}{{.}} {{end}} "
						+ "| 'bar foo baz qux '",
				"{{ range coll.Values (dict \"foo\" 1 \"bar\" 2) (dict \"baz\" 3 \"qux\" 4) }}{{.}} {{end}} "
						+ "| '2 1 3 4 '",
				// Uniq is type-aware: 1 and \"1\" are distinct, true dedups
				"{{ len (coll.Uniq (list 1 2 3 1 true false true \"1\" 2)) }} | 6" })
		void keysValuesMultiMapAndUniq(String template, String expected) {
			assertEquals(expected, render(template));
		}

		@ParameterizedTest
		@CsvSource(delimiter = '|', value = {
				// three-map merge: left-most (dst) wins per key; later maps only add new
				// keys
				"{{ index (coll.Merge (dict \"a\" 4 \"c\" 5) (dict \"a\" 1 \"b\" 2 \"c\" 3) "
						+ "(dict \"a\" 1 \"b\" 2 \"c\" 3 \"d\" 4)) \"a\" }} | 4",
				"{{ index (coll.Merge (dict \"a\" 4 \"c\" 5) (dict \"a\" 1 \"b\" 2 \"c\" 3) "
						+ "(dict \"a\" 1 \"b\" 2 \"c\" 3 \"d\" 4)) \"c\" }} | 5",
				"{{ index (coll.Merge (dict \"a\" 4 \"c\" 5) (dict \"a\" 1 \"b\" 2 \"c\" 3) "
						+ "(dict \"a\" 1 \"b\" 2 \"c\" 3 \"d\" 4)) \"b\" }} | 2",
				"{{ index (coll.Merge (dict \"a\" 4 \"c\" 5) (dict \"a\" 1 \"b\" 2 \"c\" 3) "
						+ "(dict \"a\" 1 \"b\" 2 \"c\" 3 \"d\" 4)) \"d\" }} | 4" })
		void mergeThreeMaps(String template, String expected) {
			assertEquals(expected, render(template));
		}

	}

}
