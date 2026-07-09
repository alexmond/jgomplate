package org.alexmond.jgomplate.core.datasource;

import java.net.URI;

/**
 * A named gomplate datasource — the {@code alias=uri} pairs passed on the CLI as
 * {@code -d alias=file.json} and read in templates via {@code (datasource "alias")}.
 *
 * @param alias the name the template references
 * @param uri the location of the data (this seed handles {@code file:} / bare paths)
 */
public record Datasource(String alias, URI uri) {
}
