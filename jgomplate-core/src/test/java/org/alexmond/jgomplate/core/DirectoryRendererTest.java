package org.alexmond.jgomplate.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.alexmond.jgomplate.core.config.GomplateConfig;

class DirectoryRendererTest {

	private final DirectoryRenderer renderer = new DirectoryRenderer();

	private static final RenderOptions OPTS = new RenderOptions("error", Map.of(), Map.of(), null, null);

	private GomplateConfig config(Path in, Path out) {
		GomplateConfig config = new GomplateConfig();
		config.setInputDir(in.toString());
		config.setOutputDir(out.toString());
		return config;
	}

	@Test
	void rendersNestedTreeMirroringStructure(@TempDir Path base) throws Exception {
		Path in = Files.createDirectories(base.resolve("in/sub"));
		Files.writeString(base.resolve("in/top.txt"), "{{ \"a\" | upper }}");
		Files.writeString(in.resolve("nested.txt"), "hi {{ \"b\" | upper }}");
		Path out = base.resolve("out");

		this.renderer.render(config(base.resolve("in"), out), Map.of(), OPTS);

		assertEquals("A", Files.readString(out.resolve("top.txt")));
		assertEquals("hi B", Files.readString(out.resolve("sub/nested.txt")));
	}

	@Test
	void excludeSkipsMatchingFiles(@TempDir Path base) throws Exception {
		Path in = Files.createDirectories(base.resolve("in"));
		Files.writeString(in.resolve("keep.txt"), "keep");
		Files.writeString(in.resolve("skip.md"), "skip");
		Path out = base.resolve("out");
		GomplateConfig config = config(in, out);
		config.setExcludes(List.of("*.md"));

		this.renderer.render(config, Map.of(), OPTS);

		assertTrue(Files.exists(out.resolve("keep.txt")));
		assertFalse(Files.exists(out.resolve("skip.md")));
	}

	@Test
	void includeProcessesOnlyMatchingFiles(@TempDir Path base) throws Exception {
		Path in = Files.createDirectories(base.resolve("in"));
		Files.writeString(in.resolve("a.txt"), "a");
		Files.writeString(in.resolve("b.log"), "b");
		Path out = base.resolve("out");
		GomplateConfig config = config(in, out);
		config.setIncludes(List.of("*.txt"));

		this.renderer.render(config, Map.of(), OPTS);

		assertTrue(Files.exists(out.resolve("a.txt")));
		assertFalse(Files.exists(out.resolve("b.log")));
	}

	@Test
	void excludeProcessingCopiesVerbatim(@TempDir Path base) throws Exception {
		Path in = Files.createDirectories(base.resolve("in"));
		Files.writeString(in.resolve("raw.tmpl"), "{{ not_rendered }}");
		Path out = base.resolve("out");
		GomplateConfig config = config(in, out);
		config.setExcludeProcessing(List.of("*.tmpl"));

		this.renderer.render(config, Map.of(), OPTS);

		assertEquals("{{ not_rendered }}", Files.readString(out.resolve("raw.tmpl")));
	}

	@Test
	void outputMapProducesPath(@TempDir Path base) throws Exception {
		Path in = Files.createDirectories(base.resolve("in"));
		Files.writeString(in.resolve("page.md"), "body");
		GomplateConfig config = new GomplateConfig();
		config.setInputDir(in.toString());
		config.setOutputMap(base.resolve("out").toString() + "/{{ .in }}.html");

		this.renderer.render(config, Map.of(), OPTS);

		assertEquals("body", Files.readString(base.resolve("out/page.md.html")));
	}

	@Test
	void chmodAppliesMode(@TempDir Path base) throws Exception {
		Path in = Files.createDirectories(base.resolve("in"));
		Files.writeString(in.resolve("f.txt"), "x");
		Path out = base.resolve("out");
		GomplateConfig config = config(in, out);
		config.setChmod("0640");

		this.renderer.render(config, Map.of(), OPTS);

		Set<PosixFilePermission> perms = Files.getPosixFilePermissions(out.resolve("f.txt"));
		assertTrue(perms.contains(PosixFilePermission.OWNER_READ));
		assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE));
		assertTrue(perms.contains(PosixFilePermission.GROUP_READ));
		assertFalse(perms.contains(PosixFilePermission.OTHERS_READ));
		assertFalse(perms.contains(PosixFilePermission.OWNER_EXECUTE));
	}

}
