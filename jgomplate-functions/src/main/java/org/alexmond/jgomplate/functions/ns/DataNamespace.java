package org.alexmond.jgomplate.functions.ns;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
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
 * spaces), {@code ToJSONPretty} indents that canonical JSON the way Go's {@code
 * json.Indent} does, and {@code ToYAML} emits yaml with 2-space indent, sorted keys, and
 * no {@code ---} document marker. The {@code CSV}/{@code CSVByRow}/{@code CSVByColumn}
 * readers and {@code ToCSV} writer follow gomplate's own RFC4180 handling (optional
 * leading delimiter and header arguments; auto-named columns when the header is empty;
 * CRLF output). {@code TOML}/{@code ToTOML} use Jackson's TOML dataformat. Still missing:
 * {@code CUE}/{@code ToCUE} (needs a CUE engine).
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

	private static final TomlMapper TOML_MAPPER = TomlMapper.builder()
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

	/**
	 * gomplate {@code data.ToJSONPretty indent in} — canonical JSON indented the way Go's
	 * {@code json.Indent} does: one item per line, {@code indent} per depth level,
	 * {@code "key": value} spacing, empty containers left compact, no trailing newline.
	 * @param indent the per-level indent string
	 * @param in the value to serialise
	 * @return the indented canonical JSON
	 */
	public String ToJSONPretty(Object indent, Object in) {
		return indentJson(write(JSON_MAPPER, in), Values.str(indent));
	}

	/** gomplate {@code data.ToYAML in} — yaml with 2-space indent and sorted keys. */
	public String ToYAML(Object in) {
		return write(YAML_MAPPER, in);
	}

	/** gomplate {@code data.TOML in} — parse a TOML document into a map. */
	public Map<String, Object> TOML(Object in) {
		return readMap(TOML_MAPPER, Values.toString(in));
	}

	/** gomplate {@code data.ToTOML in} — serialise a value to TOML (sorted keys). */
	public String ToTOML(Object in) {
		return write(TOML_MAPPER, in);
	}

	/**
	 * gomplate {@code data.CSV [delim] [hdr] in} — parse CSV into a row-major
	 * {@code [][]string} with the header line as the first row.
	 */
	public List<List<String>> CSV(Object... args) {
		CsvData d = parseCsv(toStrings(args));
		List<List<String>> out = new ArrayList<>();
		out.add(d.header());
		out.addAll(d.records());
		return out;
	}

	/**
	 * gomplate {@code data.CSVByRow [delim] [hdr] in} — parse CSV into a list of maps,
	 * each data row keyed by the header names.
	 */
	public List<Map<String, String>> CSVByRow(Object... args) {
		CsvData d = parseCsv(toStrings(args));
		List<Map<String, String>> rows = new ArrayList<>();
		for (List<String> record : d.records()) {
			Map<String, String> row = new LinkedHashMap<>();
			for (int i = 0; i < record.size(); i++) {
				row.put(d.header().get(i), record.get(i));
			}
			rows.add(row);
		}
		return rows;
	}

	/**
	 * gomplate {@code data.CSVByColumn [delim] [hdr] in} — parse CSV into a map of
	 * columns, each keyed by the header name.
	 */
	public Map<String, List<String>> CSVByColumn(Object... args) {
		CsvData d = parseCsv(toStrings(args));
		Map<String, List<String>> cols = new LinkedHashMap<>();
		for (List<String> record : d.records()) {
			for (int i = 0; i < record.size(); i++) {
				cols.computeIfAbsent(d.header().get(i), (k) -> new ArrayList<>()).add(record.get(i));
			}
		}
		return cols;
	}

	/**
	 * gomplate {@code data.ToCSV [delim] rows} — serialise a two-dimensional array to
	 * RFC4180 CSV (CRLF line endings, minimal quoting).
	 */
	public String ToCSV(Object... args) {
		String delim = ",";
		Object data;
		if (args.length == 2) {
			delim = Values.toString(args[0]);
			data = args[1];
		}
		else if (args.length == 1) {
			data = args[0];
		}
		else {
			throw new IllegalArgumentException("expected 1 or 2 args, got " + args.length);
		}
		char comma = delim.isEmpty() ? ',' : delim.charAt(0);
		StringBuilder sb = new StringBuilder();
		for (Object rowObj : Values.toList(data)) {
			List<Object> cells = Values.toList(rowObj);
			for (int i = 0; i < cells.size(); i++) {
				if (i > 0) {
					sb.append(comma);
				}
				sb.append(csvField(Values.toString(cells.get(i)), comma));
			}
			sb.append("\r\n");
		}
		return sb.toString();
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

	/**
	 * Reformat compact JSON the way Go's {@code json.Indent} does. String literals are
	 * copied verbatim (escapes respected); structural punctuation drives the layout.
	 */
	private static String indentJson(String compact, String indent) {
		StringBuilder out = new StringBuilder();
		int depth = 0;
		boolean inString = false;
		for (int i = 0; i < compact.length(); i++) {
			char c = compact.charAt(i);
			if (inString) {
				out.append(c);
				if (c == '\\' && i + 1 < compact.length()) {
					out.append(compact.charAt(++i));
				}
				else if (c == '"') {
					inString = false;
				}
				continue;
			}
			char close = (c == '{') ? '}' : ']';
			if (c == '"') {
				inString = true;
				out.append(c);
			}
			else if (c == '{' || c == '[') {
				if (i + 1 < compact.length() && compact.charAt(i + 1) == close) {
					out.append(c).append(close);
					i++;
				}
				else {
					depth++;
					out.append(c).append('\n').append(indent.repeat(depth));
				}
			}
			else if (c == '}' || c == ']') {
				depth--;
				out.append('\n').append(indent.repeat(depth)).append(c);
			}
			else if (c == ',') {
				out.append(c).append('\n').append(indent.repeat(depth));
			}
			else if (c == ':') {
				out.append(c).append(' ');
			}
			else {
				out.append(c);
			}
		}
		return out.toString();
	}

	/**
	 * Resolve gomplate's CSV argument shapes and split into (data records, header),
	 * mirroring {@code parsers.parseCSV}: a {@code null} header means the first row is
	 * the header; an empty header means auto-name columns ({@code A}, {@code B}, …); an
	 * explicit header leaves every row as data.
	 */
	private static CsvData parseCsv(String[] args) {
		String delim = ",";
		String in = "";
		List<String> hdr = null;
		switch (args.length) {
			case 1 -> {
				in = args[0];
			}
			case 2 -> {
				in = args[1];
				if (args[0].length() == 1) {
					delim = args[0];
				}
				else if (args[0].isEmpty()) {
					hdr = new ArrayList<>();
				}
				else {
					hdr = splitList(args[0], delim);
				}
			}
			case 3 -> {
				delim = args[0];
				hdr = splitList(args[1], delim);
				in = args[2];
			}
			default -> throw new IllegalArgumentException("expected 1, 2, or 3 args, got " + args.length);
		}
		List<List<String>> records = parseRecords(in, delim.isEmpty() ? ',' : delim.charAt(0));
		if (!records.isEmpty()) {
			if (hdr == null) {
				hdr = records.remove(0);
			}
			else if (hdr.isEmpty()) {
				for (int i = 0; i < records.get(0).size(); i++) {
					hdr.add(autoIndex(i));
				}
			}
		}
		return new CsvData(records, (hdr != null) ? hdr : new ArrayList<>());
	}

	/** RFC4180 record parser matching Go's {@code encoding/csv} for the common cases. */
	private static List<List<String>> parseRecords(String in, char delim) {
		List<List<String>> records = new ArrayList<>();
		List<String> cur = new ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean inQuotes = false;
		boolean started = false;
		int i = 0;
		while (i < in.length()) {
			char c = in.charAt(i);
			if (inQuotes) {
				if (c == '"' && i + 1 < in.length() && in.charAt(i + 1) == '"') {
					field.append('"');
					i += 2;
				}
				else if (c == '"') {
					inQuotes = false;
					i++;
				}
				else {
					field.append(c);
					i++;
				}
			}
			else if (c == '"' && field.length() == 0) {
				inQuotes = true;
				started = true;
				i++;
			}
			else if (c == delim) {
				cur.add(field.toString());
				field.setLength(0);
				started = true;
				i++;
			}
			else if (c == '\n' || c == '\r') {
				cur.add(field.toString());
				field.setLength(0);
				records.add(cur);
				cur = new ArrayList<>();
				started = false;
				i += (c == '\r' && i + 1 < in.length() && in.charAt(i + 1) == '\n') ? 2 : 1;
			}
			else {
				field.append(c);
				started = true;
				i++;
			}
		}
		if (started || field.length() > 0 || !cur.isEmpty()) {
			cur.add(field.toString());
			records.add(cur);
		}
		return records;
	}

	private static List<String> splitList(String text, String delim) {
		List<String> out = new ArrayList<>();
		int idx = 0;
		int next = text.indexOf(delim, idx);
		while (next >= 0) {
			out.add(text.substring(idx, next));
			idx = next + delim.length();
			next = text.indexOf(delim, idx);
		}
		out.add(text.substring(idx));
		return out;
	}

	/**
	 * gomplate's auto column name: the {@code (i/26 + 1)}-fold repeat of
	 * {@code 'A' + i%26}.
	 */
	private static String autoIndex(int i) {
		return String.valueOf((char) ('A' + i % 26)).repeat(i / 26 + 1);
	}

	private static String csvField(String s, char comma) {
		if (s.isEmpty()) {
			return s;
		}
		boolean quote = s.equals("\\.") || Character.isWhitespace(s.charAt(0));
		for (int i = 0; !quote && i < s.length(); i++) {
			char c = s.charAt(i);
			quote = c == comma || c == '"' || c == '\n' || c == '\r';
		}
		return quote ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
	}

	private static String[] toStrings(Object[] args) {
		String[] out = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			out[i] = Values.toString(args[i]);
		}
		return out;
	}

	private record CsvData(List<List<String>> records, List<String> header) {
	}

}
