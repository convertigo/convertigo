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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Immutable dependency order shared by project bundle producers and consumers. */
public final class ProjectBundlePlan {
	public record ProjectInfo(String name, String version, Set<String> dependencies) {
		public ProjectInfo {
			validateName(name);
			version = version == null ? "" : version;
			if (version.length() > 4096 || dependencies.size() > 1000) throw new IllegalArgumentException("Project metadata is too large");
			dependencies = Collections.unmodifiableSet(new TreeSet<>(dependencies));
			dependencies.forEach(ProjectBundlePlan::validateName);
		}
	}

	private final List<ProjectInfo> projects;

	private ProjectBundlePlan(List<ProjectInfo> projects) {
		this.projects = List.copyOf(projects);
	}

	public List<ProjectInfo> projects() {
		return projects;
	}

	/** Dependencies outside the selection are not imported or fetched by this planner. */
	public static ProjectBundlePlan create(Collection<ProjectInfo> selected) throws IOException {
		if (selected.isEmpty()) throw new IOException("Choose at least one project");
		var byName = new LinkedHashMap<String, ProjectInfo>();
		var portableNames = new TreeSet<String>();
		for (var project : selected) {
			if (byName.putIfAbsent(project.name(), project) != null
					|| !portableNames.add(project.name().toLowerCase(Locale.ROOT))) {
				throw new IOException("The project selection contains a duplicate name: " + project.name());
			}
		}
		var result = new ArrayList<ProjectInfo>();
		var states = new HashMap<String, Integer>();
		for (var name : new TreeSet<>(byName.keySet())) visit(name, byName, states, result);
		return new ProjectBundlePlan(result);
	}

	private static void visit(String name, Map<String, ProjectInfo> projects, Map<String, Integer> states,
			List<ProjectInfo> ordered) throws IOException {
		int state = states.getOrDefault(name, 0);
		if (state == 2) return;
		if (state == 1) throw new IOException("The project selection contains a dependency cycle involving " + name);
		states.put(name, 1);
		var project = projects.get(name);
		for (var dependency : project.dependencies()) {
			if (projects.containsKey(dependency)) visit(dependency, projects, states, ordered);
		}
		states.put(name, 2);
		ordered.add(project);
	}

	public static void validateName(String name) {
		if (name == null || name.isEmpty() || name.length() > 128
				|| !StringUtils.normalize(name).equals(name)
				|| name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")) {
			throw new IllegalArgumentException("Invalid project name");
		}
	}
}
