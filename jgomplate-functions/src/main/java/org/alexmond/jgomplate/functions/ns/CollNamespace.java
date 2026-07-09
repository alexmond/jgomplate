package org.alexmond.jgomplate.functions.ns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code coll} namespace (collections). Reached from templates as
 * {@code coll.Slice}, {@code coll.Has}, {@code coll.Reverse}, etc. Method names mirror
 * gomplate's Go API (PascalCase).
 *
 * <p>
 * The variadic functions ({@code Slice}, {@code Keys}, {@code Values}, {@code Dict},
 * {@code Merge}, {@code Sort}, {@code Index}, {@code Pick}, {@code Omit},
 * {@code Flatten}, {@code GoSlice}) are callable since gotmpl4j 1.2.1 unpacks varargs
 * methods. {@code JQ} runs the jq language via jackson-jq (gomplate uses gojq — the same
 * jq semantics). Only {@code JSONPath} is unimplemented: gomplate's k8s JSONPath dialect
 * has no Java implementation.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class CollNamespace {

	private static final ObjectMapper JQ_MAPPER = new ObjectMapper();

	private static final Scope JQ_SCOPE = newJqScope();

	/**
	 * Build a list from the given items — {@code coll.Slice "a" "b" "c"}. Callable since
	 * gotmpl4j 1.2.1 unpacks varargs methods.
	 */
	public List<Object> Slice(Object... items) {
		return new ArrayList<>(List.of((items != null) ? items : new Object[0]));
	}

	/**
	 * gomplate {@code coll.Keys m...} — the keys of one or more maps, each map's keys in
	 * sorted order, concatenated left to right.
	 */
	public List<String> Keys(Object... maps) {
		if (maps.length == 0) {
			throw new IllegalArgumentException("need at least one argument");
		}
		List<String> keys = new ArrayList<>();
		for (Object m : maps) {
			keys.addAll(sortedKeys(m));
		}
		return keys;
	}

	/**
	 * gomplate {@code coll.Values m...} — the values of one or more maps, each map's
	 * values in sorted-key order, concatenated left to right.
	 */
	public List<Object> Values(Object... maps) {
		if (maps.length == 0) {
			throw new IllegalArgumentException("need at least one argument");
		}
		List<Object> values = new ArrayList<>();
		for (Object m : maps) {
			Map<String, Object> copy = copyStringMap(m);
			for (String k : sortedKeys(m)) {
				values.add(copy.get(k));
			}
		}
		return values;
	}

	/**
	 * gomplate {@code coll.Dict k1 v1 k2 v2 …} — build a map from alternating key/value
	 * arguments. A trailing key with no value maps to the empty string.
	 */
	public Map<String, Object> Dict(Object... kv) {
		Map<String, Object> dict = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			String key = Values.toString(kv[i]);
			dict.put(key, (i + 1 < kv.length) ? kv[i + 1] : "");
		}
		return dict;
	}

	/**
	 * gomplate {@code coll.Merge dst src…} — deep-merge {@code src} maps into
	 * {@code dst}; left-most values (starting with {@code dst}) win. The inputs are not
	 * modified.
	 */
	public Map<String, Object> Merge(Object dst, Object... srcs) {
		Map<String, Object> result = copyStringMap(dst);
		for (Object src : srcs) {
			result = mergeValues(src, result);
		}
		return result;
	}

	/**
	 * gomplate {@code coll.Sort [key] list} — a sorted copy of {@code list} in natural
	 * order. With a {@code key}, map elements are sorted by that entry's value. The input
	 * is not modified.
	 */
	public List<Object> Sort(Object... args) {
		String key = "";
		Object list;
		if (args.length == 1) {
			list = args[0];
		}
		else if (args.length == 2) {
			key = Values.toString(args[0]);
			list = args[1];
		}
		else {
			throw new IllegalArgumentException("wrong number of args: wanted 1 or 2, got " + args.length);
		}
		List<Object> out = new ArrayList<>(Values.toList(list));
		String sortKey = key;
		out.sort((left, right) -> compareValues(extractKey(left, sortKey), extractKey(right, sortKey)));
		return out;
	}

	/**
	 * gomplate {@code coll.Index idx… item} — index {@code item} by each index in turn
	 * ({@code item} is the last argument). Errors if a map key is absent.
	 */
	public Object Index(Object... args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("wrong number of args: wanted at least 2, got " + args.length);
		}
		Object item = args[args.length - 1];
		for (int i = 0; i < args.length - 1; i++) {
			item = indexOne(item, args[i]);
		}
		return item;
	}

	/**
	 * gomplate {@code coll.Pick key… m} — a copy of map {@code m} keeping only the given
	 * keys ({@code m} is the last argument). A single list argument is expanded.
	 */
	public Map<String, Object> Pick(Object... args) {
		Map<String, Object> in = pickOmitMap(args);
		List<String> keys = pickOmitKeys(args);
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : in.entrySet()) {
			if (keys.contains(entry.getKey())) {
				out.put(entry.getKey(), entry.getValue());
			}
		}
		return out;
	}

	/**
	 * gomplate {@code coll.Omit key… m} — a copy of map {@code m} without the given keys,
	 * or a copy of a list without the given values ({@code m}/list is the last argument).
	 */
	public Object Omit(Object... args) {
		if (args.length <= 1) {
			throw new IllegalArgumentException("wrong number of args: wanted 2 or more, got " + args.length);
		}
		Object last = args[args.length - 1];
		if (isListLike(last)) {
			return omitSlice(args, last);
		}
		Map<String, Object> in = pickOmitMap(args);
		List<String> keys = pickOmitKeys(args);
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : in.entrySet()) {
			if (!keys.contains(entry.getKey())) {
				out.put(entry.getKey(), entry.getValue());
			}
		}
		return out;
	}

	/**
	 * gomplate {@code coll.Flatten [depth] list} — flatten nested lists. Without a depth,
	 * flattens fully; {@code depth} limits how many levels are collapsed.
	 */
	public List<Object> Flatten(Object... args) {
		if (args.length < 1 || args.length > 2) {
			throw new IllegalArgumentException("wrong number of args: wanted 1 or 2, got " + args.length);
		}
		int depth = -1;
		Object list = args[0];
		if (args.length == 2) {
			depth = Values.toInt(args[0]);
			list = args[1];
		}
		return flatten(list, depth);
	}

	/**
	 * gomplate {@code coll.JQ expr in} — run the jq expression {@code expr} against
	 * {@code in}. A single jq output is returned unwrapped; zero or many outputs return a
	 * list. Runs the standard jq language via jackson-jq (gomplate uses gojq), so
	 * ordinary expressions ({@code .a.b}, {@code .items[]}, {@code map(...)},
	 * {@code select(...)}) behave identically.
	 * @param expr the jq expression
	 * @param in the input value (map, list, or scalar)
	 * @return the jq output(s)
	 */
	public Object JQ(Object expr, Object in) {
		try {
			JsonQuery query = JsonQuery.compile(Values.toString(expr), Versions.JQ_1_7);
			JsonNode input = JQ_MAPPER.valueToTree(in);
			List<JsonNode> results = new ArrayList<>();
			query.apply(Scope.newChildScope(JQ_SCOPE), input, results::add);
			if (results.size() == 1) {
				return JQ_MAPPER.convertValue(results.get(0), Object.class);
			}
			List<Object> out = new ArrayList<>(results.size());
			for (JsonNode node : results) {
				out.add(JQ_MAPPER.convertValue(node, Object.class));
			}
			return out;
		}
		catch (JsonQueryException ex) {
			throw new IllegalArgumentException("jq: " + ex.getMessage(), ex);
		}
	}

	private static Scope newJqScope() {
		Scope scope = Scope.newEmptyScope();
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_7, scope);
		return scope;
	}

	/**
	 * gomplate {@code coll.GoSlice item idx…} — Go's {@code slice} builtin: subslice a
	 * list or string. With no indexes returns the whole thing; {@code low}, {@code low
	 * high}, or {@code low high max} (the capacity index applies to lists only and does
	 * not change the visible content). Strings are sliced by character.
	 * @param item the list or string to slice
	 * @param indexes 0–3 slice bounds
	 * @return the subslice
	 */
	public Object GoSlice(Object item, Object... indexes) {
		if (indexes.length > 3) {
			throw new IllegalArgumentException("slice: too many index arguments");
		}
		if (item instanceof String text) {
			if (indexes.length == 3) {
				throw new IllegalArgumentException("slice: cannot 3-index slice a string");
			}
			int low = (indexes.length >= 1) ? Values.toInt(indexes[0]) : 0;
			int high = (indexes.length >= 2) ? Values.toInt(indexes[1]) : text.length();
			return text.substring(low, high);
		}
		if (!isListLike(item)) {
			throw new IllegalArgumentException("slice: can't slice item of type " + typeName(item));
		}
		List<Object> list = Values.toList(item);
		int low = (indexes.length >= 1) ? Values.toInt(indexes[0]) : 0;
		int high = (indexes.length >= 2) ? Values.toInt(indexes[1]) : list.size();
		return new ArrayList<>(list.subList(low, high));
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
		Collections.reverse(out);
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

	private static List<String> sortedKeys(Object m) {
		List<String> keys = new ArrayList<>(copyStringMap(m).keySet());
		Collections.sort(keys);
		return keys;
	}

	/** Deep-merge {@code over} onto {@code base}; {@code over}'s values win. */
	private static Map<String, Object> mergeValues(Object base, Object over) {
		Map<String, Object> def = copyStringMap(base);
		Map<String, Object> src = copyStringMap(over);
		for (Map.Entry<String, Object> entry : src.entrySet()) {
			String k = entry.getKey();
			Object v = entry.getValue();
			if (def.get(k) instanceof Map && v instanceof Map) {
				def.put(k, mergeValues(def.get(k), v));
			}
			else {
				def.put(k, v);
			}
		}
		return def;
	}

	private static Object extractKey(Object element, String key) {
		if (!key.isEmpty() && element instanceof Map<?, ?> map) {
			return map.get(key);
		}
		return element;
	}

	private static int compareValues(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			return Double.compare(Values.toDouble(a), Values.toDouble(b));
		}
		return Values.toString(a).compareTo(Values.toString(b));
	}

	private static Object indexOne(Object item, Object index) {
		if (item instanceof Map<?, ?> map) {
			if (map.containsKey(index)) {
				return map.get(index);
			}
			String key = Values.toString(index);
			if (map.containsKey(key)) {
				return map.get(key);
			}
			throw new IllegalArgumentException("map has no key " + key);
		}
		return Values.toList(item).get(Values.toInt(index));
	}

	private static Map<String, Object> pickOmitMap(Object[] args) {
		Object last = args[args.length - 1];
		if (!(last instanceof Map<?, ?>)) {
			throw new IllegalArgumentException("wrong map type: last argument must be a map");
		}
		return copyStringMap(last);
	}

	private static List<String> pickOmitKeys(Object[] args) {
		if (args.length == 2 && isListLike(args[0])) {
			return toStringList(Values.toList(args[0]));
		}
		List<String> keys = new ArrayList<>();
		for (int i = 0; i < args.length - 1; i++) {
			keys.add(Values.toString(args[i]));
		}
		return keys;
	}

	private static List<Object> omitSlice(Object[] args, Object last) {
		List<Object> values = new ArrayList<>(Arrays.asList(args).subList(0, args.length - 1));
		if (values.size() == 1 && isListLike(values.get(0))) {
			values = Values.toList(values.get(0));
		}
		List<Object> out = new ArrayList<>();
		for (Object v : Values.toList(last)) {
			if (!values.contains(v)) {
				out.add(v);
			}
		}
		return out;
	}

	private static List<Object> flatten(Object list, int depth) {
		List<Object> out = new ArrayList<>();
		if (depth == 0) {
			out.addAll(Values.toList(list));
			return out;
		}
		for (Object v : Values.toList(list)) {
			if (isListLike(v)) {
				out.addAll(flatten(v, depth - 1));
			}
			else {
				out.add(v);
			}
		}
		return out;
	}

	private static List<String> toStringList(List<Object> in) {
		List<String> out = new ArrayList<>();
		for (Object o : in) {
			out.add(Values.toString(o));
		}
		return out;
	}

	private static boolean isListLike(Object o) {
		return o instanceof Collection || o instanceof Object[];
	}

	private static String typeName(Object o) {
		return (o != null) ? o.getClass().getSimpleName() : "nil";
	}

}
