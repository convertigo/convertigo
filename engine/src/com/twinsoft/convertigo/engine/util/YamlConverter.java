/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

/** Project format dispatch. Legacy parsing and rendering remain deliberately isolated. */
public class YamlConverter {
	public enum Format { LEGACY, STANDARD }
	public static final String STANDARD_MIN_VERSION = "8.5.0";
	private static final String FORMAT_DATA = "convertigo.yaml.format";
	private static final UserDataHandler COPY_FORMAT = new UserDataHandler() {
		@Override public void handle(short operation, String key, Object data, Node source, Node destination) {
			if (destination != null) destination.setUserData(key, data, this);
		}
	};

	private YamlConverter() { }

	public static Format getFormat(File yaml) throws IOException {
		try (var reader = Files.newBufferedReader(yaml.toPath(), StandardCharsets.UTF_8)) {
			String first = reader.readLine();
			// Standard tools may insert a document start, comments, quotes or flow style.
			// No such first line is a valid legacy project header.
			return first != null && (first.startsWith("↑") || first.startsWith("↓"))
					? Format.LEGACY : Format.STANDARD;
		}
	}

	public static Format getFormat(Document document) {
		return document.getUserData(FORMAT_DATA) instanceof Format format ? format : Format.LEGACY;
	}

	public static void setFormat(Document document, Format format) {
		document.setUserData(FORMAT_DATA, format, COPY_FORMAT);
	}

	public static void copyFormat(Document source, Document target) {
		setFormat(target, getFormat(source));
	}

	/**
	 * The format a save or an export writes: standard for a project read in it, or whose beans require engine
	 * {@value #STANDARD_MIN_VERSION} or later (no older engine opens it anyway); else the legacy format it was read with.
	 *
	 * @param format the format the project was read with
	 * @param minVersion the minimum version its beans require, as the shrink computes it
	 */
	public static Format savedFormat(Format format, String minVersion) {
		return format == Format.STANDARD || VersionUtils.compare(minVersion, STANDARD_MIN_VERSION) >= 0
				? Format.STANDARD : Format.LEGACY;
	}

	/** Keep the migration suffix while imposing the file format's compatibility floor. */
	public static String minimumVersion(String version, Format format) {
		if (format == Format.STANDARD && VersionUtils.compare(version, STANDARD_MIN_VERSION) < 0) {
			int suffix = version.indexOf(".m");
			return STANDARD_MIN_VERSION + (suffix < 0 ? ".m006" : version.substring(suffix));
		}
		return version;
	}

	public static Document readYaml(File yaml) throws Exception { return readYaml(yaml, false); }

	public static Document readYaml(File yaml, boolean onlyRoot) throws Exception {
		Format format = getFormat(yaml);
		Document document = format == Format.LEGACY ? LegacyYamlConverter.readYaml(yaml, onlyRoot)
				: StandardYamlConverter.readYaml(yaml, onlyRoot);
		setFormat(document, format);
		return document;
	}

	public static void writeYaml(Document document, File yaml) throws IOException {
		writeYaml(document, yaml, null);
	}

	public static void writeYaml(Document document, File yaml, File subdir) throws IOException {
		if (getFormat(document) == Format.STANDARD) {
			StandardYamlConverter.writeYaml(document, yaml, subdir);
		} else {
			LegacyYamlConverter.writeYaml(document, yaml, subdir);
		}
	}

	/** Search snippets are not project documents and retain their compact legacy notation. */
	public static String toYaml(Element element) throws IOException { return LegacyYamlConverter.toYaml(element); }
	public static String toYaml(Element element, Set<File> files) throws IOException {
		return LegacyYamlConverter.toYaml(element, files);
	}
	static boolean needsQuote(String text) { return LegacyYamlConverter.needsQuote(text); }

	public static String projectName(Document document) throws IOException {
		for (Node node = document.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && element.getTagName().equals("bean")) {
				String key = element.getAttribute("yaml_key");
				if (key.endsWith(" [core.Project]")) return key.substring(0, key.length() - " [core.Project]".length());
			}
		}
		throw new IOException("Missing project bean in YAML document");
	}
}
