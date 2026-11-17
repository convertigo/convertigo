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

	public record Group(List<ProjectInfo> projects, boolean cyclic) {
		public Group { projects = List.copyOf(projects); }
	}

	private final List<Group> groups;
	private final List<ProjectInfo> projects;

	private ProjectBundlePlan(List<Group> groups) {
		this.groups = List.copyOf(groups);
		this.projects = groups.stream().flatMap(group -> group.projects().stream()).toList();
	}

	public List<ProjectInfo> projects() { return projects; }
	public List<Group> groups() { return groups; }

	public static ProjectBundlePlan create(Collection<ProjectInfo> selected) throws IOException {
		return create(selected, List.of());
	}

	/** Manifest orders only order members of a group; descriptors determine its membership. */
	public static ProjectBundlePlan create(Collection<ProjectInfo> selected, List<List<String>> manifestOrders) throws IOException {
		if (selected.isEmpty()) throw new IOException("Choose at least one project");
		var byName = new LinkedHashMap<String, ProjectInfo>();
		var portableNames = new TreeSet<String>();
		for (var project : selected) {
			if (byName.putIfAbsent(project.name(), project) != null
					|| !portableNames.add(project.name().toLowerCase(Locale.ROOT))) {
				throw new IOException("The project selection contains a duplicate name: " + project.name());
			}
		}
		var components = new Components(byName);
		for (var name : new TreeSet<>(byName.keySet())) if (!components.indices.containsKey(name)) components.visit(name);
		var groups = new ArrayList<Group>();
		var membership = new HashMap<String, Integer>();
		for (var names : components.groups) {
			var rank = new HashMap<String, Integer>();
			for (var order : manifestOrders) {
				if (!order.containsAll(names)) continue;
				for (int i = 0; i < order.size(); i++) rank.putIfAbsent(order.get(i), i);
				break;
			}
			names.sort(java.util.Comparator.comparingInt((String name) -> rank.getOrDefault(name, Integer.MAX_VALUE)).thenComparing(name -> name));
			int id = groups.size();
			for (var name : names) membership.put(name, id);
			boolean cyclic = names.size() > 1 || byName.get(names.get(0)).dependencies().contains(names.get(0));
			groups.add(new Group(names.stream().map(byName::get).toList(), cyclic));
		}
		var dependencies = new ArrayList<Set<Integer>>();
		java.util.Comparator<Integer> order = java.util.Comparator.comparing(id -> groups.get(id).projects().stream().map(ProjectInfo::name).min(String::compareTo).orElseThrow());
		for (int id = 0; id < groups.size(); id++) {
			var required = new TreeSet<Integer>(order);
			for (var project : groups.get(id).projects()) for (var dependency : project.dependencies()) {
				Integer target = membership.get(dependency);
				if (target != null && target != id) required.add(target);
			}
			dependencies.add(required);
		}
		var ids = new TreeSet<Integer>(order);
		for (int i = 0; i < groups.size(); i++) ids.add(i);
		var result = new ArrayList<Group>();
		var visited = new java.util.HashSet<Integer>();
		for (int id : ids) orderGroup(id, groups, dependencies, visited, result);
		return new ProjectBundlePlan(result);
	}

	private static void orderGroup(int id, List<Group> groups, List<Set<Integer>> dependencies,
			Set<Integer> visited, List<Group> result) {
		if (!visited.add(id)) return;
		for (int dependency : dependencies.get(id)) orderGroup(dependency, groups, dependencies, visited, result);
		result.add(groups.get(id));
	}

	/** Tarjan's algorithm, restricted to projects actually present in the selection. */
	private static final class Components {
		final Map<String, ProjectInfo> projects;
		final Map<String, Integer> indices = new HashMap<>(), low = new HashMap<>();
		final java.util.Deque<String> stack = new java.util.ArrayDeque<>();
		final Set<String> onStack = new java.util.HashSet<>();
		final List<List<String>> groups = new ArrayList<>();
		int next;

		Components(Map<String, ProjectInfo> projects) { this.projects = projects; }

		void visit(String name) {
			indices.put(name, next);
			low.put(name, next++);
			stack.push(name);
			onStack.add(name);
			for (var dependency : projects.get(name).dependencies()) {
				if (!projects.containsKey(dependency)) continue;
				if (!indices.containsKey(dependency)) {
					visit(dependency);
					low.put(name, Math.min(low.get(name), low.get(dependency)));
				} else if (onStack.contains(dependency)) low.put(name, Math.min(low.get(name), indices.get(dependency)));
			}
			if (low.get(name).equals(indices.get(name))) {
				var group = new ArrayList<String>();
				String member;
				do { member = stack.pop(); onStack.remove(member); group.add(member); } while (!member.equals(name));
				groups.add(group);
			}
		}
	}

	public static void validateName(String name) {
		if (name == null || name.isEmpty() || name.length() > 128
				|| !StringUtils.normalize(name).equals(name)
				|| name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")) {
			throw new IllegalArgumentException("Invalid project name");
		}
	}
}
