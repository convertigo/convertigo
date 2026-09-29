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

package com.twinsoft.convertigo.beans.flow;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

/**
 * The Flow sources of a project changed and not saved: the working copies of its files, as the frontend
 * sources, its Engine source and the sources of its Flows. They live beside the objects of the project,
 * not in their XML: a state of the project that must come back whole, as a state of the undo history of
 * the web Studio, takes them with its XML and gives them back once its XML is restored.
 */
public final class FlowWorkingCopies {
	private static final String FILE = "file:";
	private static final String ENGINE = "engine";
	private static final String FLOW = "flow:";

	private FlowWorkingCopies() {
	}

	/**
	 * @return the working copies of the project, by kind and name (a file by its path in the project),
	 *         sorted, so that two states compare
	 */
	public static Map<String, String> of(Project project) throws EngineException {
		var copies = new TreeMap<String, String>();
		var flowEngine = project.getFlowEngine();
		if (flowEngine != null) {
			var root = rootPath(project);
			for (var entry : flowEngine.getSourceDrafts().entrySet()) {
				copies.put(FILE + entry.getKey().substring(root.length() + 1), entry.getValue());
			}
			if (flowEngine.isEngineSourceDirty()) {
				copies.put(ENGINE, flowEngine.getEngineSource());
			}
		}
		for (var sequence : project.getSequencesList()) {
			if (sequence instanceof Flow flow && flow.isFlowSourceDirty()) {
				copies.put(FLOW + flow.getName(), flow.getFlowSource());
			}
		}
		return copies;
	}

	/**
	 * Gives back to a project loaded again, whose working copies were dropped with its previous objects,
	 * those of one of its states; the development viewer follows each frontend source that changes.
	 * @param previous the working copies of the project before it was loaded again
	 */
	public static void restore(Project project, Map<String, String> copies, Map<String, String> previous)
			throws EngineException {
		var flowEngine = project.getFlowEngine();
		var files = new LinkedHashMap<String, String>();
		for (var entry : copies.entrySet()) {
			var key = entry.getKey();
			if (key.startsWith(FILE)) {
				files.put(file(project, key).getPath(), entry.getValue());
			} else if (key.equals(ENGINE) && flowEngine != null) {
				// the saved source is read first: a copy equal to it is not a change
				flowEngine.getEngineSource();
				flowEngine.setEngineSource(entry.getValue());
			} else if (key.startsWith(FLOW)) {
				var name = key.substring(FLOW.length());
				for (var sequence : project.getSequencesList()) {
					if (sequence instanceof Flow flow && flow.getName().equals(name)) {
						flow.setFlowSource(entry.getValue());
					}
				}
			}
		}
		if (flowEngine == null) {
			return;
		}
		if (!files.isEmpty()) {
			flowEngine.setSources(files);
		}
		var changed = new TreeSet<String>();
		for (var map : List.of(copies, previous)) {
			for (var key : map.keySet()) {
				if (key.startsWith(FILE) && !Objects.equals(copies.get(key), previous.get(key))) {
					changed.add(key);
				}
			}
		}
		for (var key : changed) {
			FlowStudioSupport.afterSourceMutation(flowEngine, file(project, key).getPath());
		}
	}

	private static File file(Project project, String key) {
		return new File(project.getDirFile(), key.substring(FILE.length()));
	}

	private static String rootPath(Project project) {
		try {
			return project.getDirFile().getCanonicalPath();
		} catch (Exception e) {
			return project.getDirFile().getAbsolutePath();
		}
	}
}
