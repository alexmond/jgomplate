package org.alexmond.jgomplate.core.datasource;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.alexmond.jgomplate.core.config.DataSourceConfig;

class DatasourcesTest {

	@Test
	void parseArgSplitsAliasAndUrl() {
		Map.Entry<String, DataSourceConfig> entry = Datasources.parseArg("cfg=./data.json");
		assertEquals("cfg", entry.getKey());
		assertEquals("./data.json", entry.getValue().getUrl());
	}

	@Test
	void parseArgWithoutEqualsDerivesAlias() {
		Map.Entry<String, DataSourceConfig> entry = Datasources.parseArg("/etc/app/config.yaml");
		assertEquals("config", entry.getKey());
		assertEquals("/etc/app/config.yaml", entry.getValue().getUrl());
	}

	@Test
	void parseArgKeepsRootAlias() {
		assertEquals(".", Datasources.parseArg(".=data.json").getKey());
	}

	@Test
	void aliasFromUrlStripsPathAndExtension() {
		assertEquals("data", Datasources.aliasFromUrl("dir/sub/data.json"));
		assertEquals("data", Datasources.aliasFromUrl("data.yaml"));
		assertEquals("noext", Datasources.aliasFromUrl("noext"));
	}

	@Test
	void toUriKeepsExplicitScheme() {
		assertEquals("https", Datasources.toUri("https://example.com/a.json").getScheme());
		assertEquals("file", Datasources.toUri("file:///tmp/a.json").getScheme());
	}

	@Test
	void toUriTreatsBarePathAsFile() {
		URI uri = Datasources.toUri("relative/data.json");
		assertEquals("file", uri.getScheme());
	}

}
