package org.alexmond.jgomplate.functions.ns;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

	/**
	 * gomplate {@code coll.Has}: for a map, whether {@code item} is a key; for a list or
	 * array, whether {@code item} is an element (by value equality).
	 * @param in the map, list, or array to test
	 * @param item the key or element to look for
	 * @return {@code true} when present
	 */
	public boolean Has(Object in, Object item) {
		if (in instanceof Map<?, ?> map) {
			return map.containsKey(item);
		}
		for (Object element : Values.toList(in)) {
			if (Objects.equals(element, item)) {
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

	/**
	 * gomplate {@code coll.Append v list} — a copy of {@code list} with {@code v}
	 * appended.
	 */
	public List<Object> Append(Object v, Object list) {
		List<Object> out = new ArrayList<>(Values.toList(list));
		out.add(v);
		return out;
	}

	/**
	 * gomplate {@code coll.Prepend v list} — a copy of {@code list} with {@code v} at the
	 * front.
	 */
	public List<Object> Prepend(Object v, Object list) {
		List<Object> out = new ArrayList<>();
		out.add(v);
		out.addAll(Values.toList(list));
		return out;
	}

	/** gomplate {@code coll.Uniq list} — remove duplicates, keeping first-seen order. */
	public List<Object> Uniq(Object list) {
		List<Object> out = new ArrayList<>();
		for (Object element : Values.toList(list)) {
			if (!out.contains(element)) {
				out.add(element);
			}
		}
		return out;
	}

	/**
	 * gomplate {@code coll.Set key value m} — a copy of map {@code m} with {@code key}
	 * set to {@code value}.
	 * @param key the key to set
	 * @param value the value to associate
	 * @param m the source map
	 * @return the updated map copy
	 */
	public Map<String, Object> Set(Object key, Object value, Object m) {
		Map<String, Object> out = copyStringMap(m);
		out.put(Values.str(key), value);
		return out;
	}

	/**
	 * gomplate {@code coll.Unset key m} — a copy of map {@code m} without {@code key}.
	 * @param key the key to remove
	 * @param m the source map
	 * @return the updated map copy
	 */
	public Map<String, Object> Unset(Object key, Object m) {
		Map<String, Object> out = copyStringMap(m);
		out.remove(Values.str(key));
		return out;
	}

	private static Map<String, Object> copyStringMap(Object m) {
		Map<String, Object> out = new LinkedHashMap<>();
		if (m instanceof Map<?, ?> map) {
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				out.put(String.valueOf(entry.getKey()), entry.getValue());
			}
		}
		return out;
	}

}
