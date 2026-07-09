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

	}

}
