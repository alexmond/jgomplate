package org.alexmond.jgomplate.functions.ns;

import java.util.ArrayList;
import java.util.List;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code coll} namespace (collections). Reached from templates as
 * {@code coll.Slice}, {@code coll.Has}, {@code coll.Reverse}, etc. Method names mirror
 * gomplate's Go API (PascalCase).
 *
 * <p>
 * Seed subset of gomplate's full {@code coll} namespace.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class CollNamespace {

	/**
	 * Build a list from the given items — {@code coll.Slice "a" "b" "c"}.
	 *
	 * <p>
	 * NOTE: variadic namespace <em>methods</em> are not yet callable from templates —
	 * gotmpl4j's executor matches methods by exact parameter count and does not unpack
	 * Java varargs. Until that lands upstream, use Sprig's variadic {@code list} function
	 * to build lists. The method is correct Java and works via direct calls.
	 */
	public List<Object> Slice(Object... items) {
		return new ArrayList<>(List.of((items != null) ? items : new Object[0]));
	}

	/** {@code true} if {@code list} contains {@code item}. */
	public boolean Has(Object list, Object item) {
		for (Object element : Values.toList(list)) {
			if ((element != null) ? element.equals(item) : (item == null)) {
				return true;
			}
		}
		return false;
	}

	/** Return a reversed copy of {@code list}. */
	public List<Object> Reverse(Object list) {
		List<Object> out = new ArrayList<>(Values.toList(list));
		java.util.Collections.reverse(out);
		return out;
	}

}
