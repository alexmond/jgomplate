package org.alexmond.jgomplate.functions.ns;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code data} namespace (structured-data parse/serialise). Reached from
 * templates as {@code data.JSON}, {@code data.ToYAML}, etc. Method names mirror
 * gomplate's Go API (PascalCase).
 *
 * <p>
 * Output matches gomplate's: {@code ToJSON} emits canonical JSON (map keys sorted, no
 * spaces) and {@code ToYAML} emits yaml with 2-space indent, sorted keys, and no
 * {@code ---} document marker. Seed subset: {@code ToJSONPretty} (needs Go's {@code
 * json.Indent} formatting), {@code TOML}/{@code ToTOML}, the variadic {@code CSV}
 * readers, and {@code CUE} are follow-up work.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class DataNamespace {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
		.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

	private static final YAMLMapper YAML_MAPPER = YAMLMapper.builder()
		.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
		.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
		.build();

	/** gomplate {@code data.JSON in} — parse a JSON object into a map. */
	public Map<String, Object> JSON(Object in) {
		return readMap(JSON_MAPPER, Values.toString(in));
	}

	/** gomplate {@code data.JSONArray in} — parse a JSON array into a list. */
	public List<Object> JSONArray(Object in) {
		return readList(JSON_MAPPER, Values.toString(in));
	}

	/** gomplate {@code data.YAML in} — parse a YAML object into a map. */
	public Map<String, Object> YAML(Object in) {
		return readMap(YAML_MAPPER, Values.toString(in));
	}

	/** gomplate {@code data.YAMLArray in} — parse a YAML array into a list. */
	public List<Object> YAMLArray(Object in) {
		return readList(YAML_MAPPER, Values.toString(in));
	}

	/** gomplate {@code data.ToJSON in} — canonical JSON (sorted keys, compact). */
	public String ToJSON(Object in) {
		return write(JSON_MAPPER, in);
	}

	/** gomplate {@code data.ToYAML in} — yaml with 2-space indent and sorted keys. */
	public String ToYAML(Object in) {
		return write(YAML_MAPPER, in);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readMap(ObjectMapper mapper, String text) {
		try {
			return mapper.readValue(text, Map.class);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("could not parse as an object: " + ex.getOriginalMessage(), ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Object> readList(ObjectMapper mapper, String text) {
		try {
			return mapper.readValue(text, List.class);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("could not parse as an array: " + ex.getOriginalMessage(), ex);
		}
	}

	private static String write(ObjectMapper mapper, Object in) {
		try {
			return mapper.writeValueAsString(in);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("could not marshal: " + ex.getOriginalMessage(), ex);
		}
	}

}
