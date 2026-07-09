package org.alexmond.jgomplate.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.alexmond.jgomplate.core.config.GomplateConfig;

/**
 * Renders a directory tree (gomplate's {@code --input-dir}): walk the input directory,
 * render each file to the output directory (or to a path produced by
 * {@code --output-map}), honouring {@code --include} / {@code --exclude} /
 * {@code --exclude-processing} globs and applying {@code --chmod}.
 *
 * <p>
 * Globs are matched against the file's path relative to the input directory using the
 * platform {@code glob:} syntax ({@code **} spans directories). This is a faithful subset
 * of gomplate's doublestar/gitignore matching — negation and anchoring nuances are not
 * modelled.
 */
public class DirectoryRenderer {

	private static final String DEFAULT_OUTPUT_DIR = ".";

	private final GomplateEngine engine;

	public DirectoryRenderer() {
		this(new GomplateEngine());
	}

	public DirectoryRenderer(GomplateEngine engine) {
		this.engine = engine;
	}

	/**
	 * Render every eligible file under {@code config.inputDir}.
	 * @param config the resolved configuration (must have {@code inputDir} set)
	 * @param context the template root context
	 * @param options the per-render settings (missing-key, functions, partials,
	 * delimiters)
	 */
	public void render(GomplateConfig config, Map<String, Object> context, RenderOptions options) {
		Path inputDir = Path.of(config.getInputDir());
		if (!Files.isDirectory(inputDir)) {
			throw new IllegalArgumentException("input dir is not a directory: " + config.getInputDir());
		}
		List<PathMatcher> includes = matchers(config.getIncludes());
		List<PathMatcher> excludes = matchers(config.getExcludes());
		List<PathMatcher> excludeProcessing = matchers(config.getExcludeProcessing());

		try (Stream<Path> walk = Files.walk(inputDir)) {
			List<Path> files = walk.filter(Files::isRegularFile).toList();
			for (Path file : files) {
				Path relative = inputDir.relativize(file);
				if (skip(relative, includes, excludes)) {
					continue;
				}
				boolean raw = matchesAny(relative, excludeProcessing);
				process(config, context, options, file, relative, raw);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to walk input dir '" + config.getInputDir() + "'", ex);
		}
	}

	private void process(GomplateConfig config, Map<String, Object> context, RenderOptions options, Path file,
			Path relative, boolean raw) {
		String source = readFile(file);
		String rendered = raw ? source : this.engine.render(source, context, options);
		Path output = outputPath(config, context, options, relative);
		writeFile(output, rendered);
		applyChmod(output, config.getChmod());
	}

	private Path outputPath(GomplateConfig config, Map<String, Object> context, RenderOptions options, Path relative) {
		if (config.getOutputMap() != null && !config.getOutputMap().isBlank()) {
			Map<String, Object> data = new HashMap<>(context);
			data.put("in", relative.toString());
			String mapped = this.engine.render(config.getOutputMap(), data, options).trim();
			return Path.of(mapped);
		}
		String outputDir = (config.getOutputDir() != null) ? config.getOutputDir() : DEFAULT_OUTPUT_DIR;
		return Path.of(outputDir).resolve(relative);
	}

	private static boolean skip(Path relative, List<PathMatcher> includes, List<PathMatcher> excludes) {
		if (!includes.isEmpty() && !matchesAny(relative, includes)) {
			return true;
		}
		return matchesAny(relative, excludes);
	}

	private static boolean matchesAny(Path relative, List<PathMatcher> matchers) {
		for (PathMatcher matcher : matchers) {
			if (matcher.matches(relative)) {
				return true;
			}
		}
		return false;
	}

	private static List<PathMatcher> matchers(List<String> globs) {
		List<PathMatcher> matchers = new ArrayList<>();
		if (globs != null) {
			for (String glob : globs) {
				matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
			}
		}
		return matchers;
	}

	private static void applyChmod(Path output, String chmod) {
		if (chmod == null || chmod.isBlank()) {
			return;
		}
		if (!output.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			return;
		}
		try {
			Files.setPosixFilePermissions(output, octalToPermissions(chmod.trim()));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to chmod '" + output + "'", ex);
		}
	}

	private static Set<PosixFilePermission> octalToPermissions(String octal) {
		int mode = Integer.parseInt(octal, 8);
		Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
		PosixFilePermission[] values = PosixFilePermission.values();
		for (int i = 0; i < values.length; i++) {
			if ((mode & (1 << (values.length - 1 - i))) != 0) {
				perms.add(values[i]);
			}
		}
		return perms;
	}

	private static String readFile(Path file) {
		try {
			return Files.readString(file);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read '" + file + "'", ex);
		}
	}

	private static void writeFile(Path output, String content) {
		try {
			Path parent = output.toAbsolutePath().getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.writeString(output, content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write '" + output + "'", ex);
		}
	}

}
