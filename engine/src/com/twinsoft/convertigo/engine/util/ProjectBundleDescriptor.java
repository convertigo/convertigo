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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo;

/** Reads names and declared references without loading beans or importing references. */
final class ProjectBundleDescriptor {
	private ProjectBundleDescriptor() {
	}

	static ProjectInfo read(Path descriptor) throws Exception {
		if (Files.size(descriptor) > (16L << 20)) throw new IOException("Project descriptor is too large");
		var dependencies = new TreeSet<String>();
		String name, version;
		if (descriptor.getFileName().toString().equals("c8oProject.yaml")) {
			try (var lines = Files.lines(descriptor)) {
				var iterator = lines.iterator();
				while (iterator.hasNext()) {
					String line = iterator.next();
					if (line.length() > (1 << 20) || line.length() - line.stripLeading().length() > 512) {
						throw new IOException("Project descriptor exceeds its structure limits");
					}
				}
			}
			// onlyRoot leaves split sequences and connectors unopened.
			var document = YamlConverter.readYaml(descriptor.toFile(), true);
			Element project = null;
			for (var node = document.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling()) {
				if (node instanceof Element element && element.getAttribute("yaml_key").endsWith(" [core.Project]")) {
					if (project != null) throw new IOException("Multiple project descriptors");
					project = element;
				}
			}
			if (project == null) throw new IOException("Project descriptor not found");
			String key = project.getAttribute("yaml_key");
			name = key.substring(0, key.length() - " [core.Project]".length());
			version = directText(project, "version");
			for (var node = project.getFirstChild(); node != null; node = node.getNextSibling()) {
				if (node instanceof Element element && element.getAttribute("yaml_key").endsWith(" [references.ProjectSchemaReference]")) {
					dependencies.add(reference(directText(element, "projectName")));
				}
			}
		} else {
			org.w3c.dom.Document document;
			try (var input = Files.newInputStream(descriptor)) {
				document = XMLUtils.parseDOMFromInputSource(new org.xml.sax.InputSource(input));
			}
			var nodes = document.getDocumentElement().getElementsByTagName("project");
			if (nodes.getLength() != 1) throw new IOException("Project descriptor not found");
			var project = (Element) nodes.item(0);
			name = property(project, "name");
			version = property(project, "version");
			if (name.isEmpty()) name = project.getAttribute("name");
			if (version.isEmpty()) version = project.getAttribute("version");
			for (var node = project.getFirstChild(); node != null; node = node.getNextSibling()) {
				if (node instanceof Element element && element.getTagName().equals("reference")
						&& element.getAttribute("classname").equals("com.twinsoft.convertigo.beans.references.ProjectSchemaReference")) {
					dependencies.add(reference(property(element, "projectName")));
				}
			}
		}
		return new ProjectInfo(name, version, dependencies);
	}

	private static String reference(String value) throws IOException {
		if (value.isBlank() || value.length() > 8192 || value.contains("${")) throw new IOException("A project reference cannot be resolved from its descriptor");
		return new ProjectUrlParser(value).getProjectName();
	}

	private static String directText(Element parent, String name) {
		for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && element.getTagName().equals(name)) return element.getTextContent();
		}
		return "";
	}

	private static String property(Element parent, String name) {
		for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && element.getTagName().equals("property") && element.getAttribute("name").equals(name)) {
				for (var value = element.getFirstChild(); value != null; value = value.getNextSibling()) {
					if (value instanceof Element child && child.hasAttribute("value")) return child.getAttribute("value");
				}
			}
		}
		return "";
	}
}
