package org.alexmond.jgomplate.functions;

import java.util.HashMap;
import java.util.Map;

import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.FunctionProvider;
import org.alexmond.gotmpl4j.GoTemplate;
import org.alexmond.jgomplate.functions.ns.Base64Namespace;
import org.alexmond.jgomplate.functions.ns.CollNamespace;
import org.alexmond.jgomplate.functions.ns.ConvNamespace;
import org.alexmond.jgomplate.functions.ns.CryptoNamespace;
import org.alexmond.jgomplate.functions.ns.DataNamespace;
import org.alexmond.jgomplate.functions.ns.MathNamespace;
import org.alexmond.jgomplate.functions.ns.StringsNamespace;
import org.alexmond.jgomplate.functions.ns.UuidNamespace;

/**
 * {@link FunctionProvider} contributing gomplate's namespaced template functions on top
 * of the Go builtins (priority 0) and Sprig (priority 100).
 *
 * <p>
 * Each namespace ({@code strings}, {@code conv}, {@code coll}, …) is registered as a
 * nullary function returning a stateless namespace object; templates then invoke its
 * methods via Go-template method calls — e.g. {@code {{ strings.ToUpper "hi" }}} resolves
 * {@code strings} to the {@link StringsNamespace} instance and calls {@code ToUpper}.
 *
 * <p>
 * Priority is {@code 200} (above Sprig), matching the Helm function pack's slot in the
 * gotmpl4j provider ordering. Discovered automatically via
 * {@link java.util.ServiceLoader} when {@code jgomplate-functions} is on the classpath.
 *
 * @see FunctionProvider
 */
public class GomplateFunctionProvider implements FunctionProvider {

	private static final StringsNamespace STRINGS = new StringsNamespace();

	private static final ConvNamespace CONV = new ConvNamespace();

	private static final CollNamespace COLL = new CollNamespace();

	private static final MathNamespace MATH = new MathNamespace();

	private static final Base64Namespace BASE64 = new Base64Namespace();

	private static final CryptoNamespace CRYPTO = new CryptoNamespace();

	private static final UuidNamespace UUID = new UuidNamespace();

	private static final DataNamespace DATA = new DataNamespace();

	@Override
	public Map<String, Function> getFunctions(GoTemplate template) {
		Map<String, Function> functions = new HashMap<>();
		functions.put("strings", (args) -> STRINGS);
		functions.put("conv", (args) -> CONV);
		functions.put("coll", (args) -> COLL);
		functions.put("math", (args) -> MATH);
		functions.put("base64", (args) -> BASE64);
		functions.put("crypto", (args) -> CRYPTO);
		functions.put("uuid", (args) -> UUID);
		functions.put("data", (args) -> DATA);
		return functions;
	}

	@Override
	public int priority() {
		return 200;
	}

	@Override
	public String name() {
		return "gomplate";
	}

}
