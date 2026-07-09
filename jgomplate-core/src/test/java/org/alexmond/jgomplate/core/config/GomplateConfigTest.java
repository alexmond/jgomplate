package org.alexmond.jgomplate.core.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GomplateConfigTest {

	@Test
	void overlayReplacesOnlySetFields() {
		GomplateConfig base = new GomplateConfig();
		base.setInputFiles(List.of("a.tmpl"));
		base.setOutputFiles(List.of("a.out"));
		base.setMissingKey("error");

		GomplateConfig cli = new GomplateConfig();
		cli.setInputFiles(List.of("b.tmpl"));

		base.mergeFrom(cli);

		assertEquals(List.of("b.tmpl"), base.getInputFiles(), "set field is overridden");
		assertEquals(List.of("a.out"), base.getOutputFiles(), "unset field is preserved");
		assertEquals("error", base.getMissingKey(), "unset field is preserved");
	}

	@Test
	void overlayWithNullIsNoOp() {
		GomplateConfig base = new GomplateConfig();
		base.setIn("hello");
		base.mergeFrom(null);
		assertEquals("hello", base.getIn());
	}

	@Test
	void overlayCanSetBooleanFlag() {
		GomplateConfig base = new GomplateConfig();
		GomplateConfig cli = new GomplateConfig();
		cli.setExperimental(true);
		base.mergeFrom(cli);
		assertTrue(base.getExperimental());
	}

	@Test
	void overlayMergesDatasourceMapsEntryWise() {
		GomplateConfig base = new GomplateConfig();
		base.setContext(new LinkedHashMap<>(Map.of("a", dsUrl("a.json"))));

		GomplateConfig cli = new GomplateConfig();
		cli.setContext(Map.of("b", dsUrl("b.json")));

		base.mergeFrom(cli);

		assertEquals("a.json", base.getContext().get("a").getUrl(), "base entry kept");
		assertEquals("b.json", base.getContext().get("b").getUrl(), "overlay entry added");
	}

	private static DataSourceConfig dsUrl(String url) {
		DataSourceConfig ds = new DataSourceConfig();
		ds.setUrl(url);
		return ds;
	}

	@Test
	void unsetFieldsStayNull() {
		GomplateConfig config = new GomplateConfig();
		assertNull(config.getIn());
		assertNull(config.getInputFiles());
		assertNull(config.getExperimental());
	}

}
