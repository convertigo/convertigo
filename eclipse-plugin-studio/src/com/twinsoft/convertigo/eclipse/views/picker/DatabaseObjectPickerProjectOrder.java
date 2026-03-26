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

package com.twinsoft.convertigo.eclipse.views.picker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Engine;

public final class DatabaseObjectPickerProjectOrder {

	private DatabaseObjectPickerProjectOrder() {
	}

	public static List<String> getOrderedProjectNames(Project currentProject) {
		List<String> projectNames = new ArrayList<>(Engine.theApp.databaseObjectsManager.getAllProjectNamesList());
		projectNames.sort(String.CASE_INSENSITIVE_ORDER);
		if (currentProject == null) {
			return projectNames;
		}

		String currentProjectName = currentProject.getName();
		if (currentProjectName == null || currentProjectName.isBlank() || !projectNames.contains(currentProjectName)) {
			return projectNames;
		}

		Set<String> referencedProjectNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (Reference reference : currentProject.getReferenceList()) {
			if (reference instanceof ProjectSchemaReference projectSchemaReference) {
				String projectName = projectSchemaReference.getParser().getProjectName();
				if (projectName != null && !projectName.isBlank() && !projectName.equals(currentProjectName)) {
					referencedProjectNames.add(projectName);
				}
			}
		}

		List<String> ordered = new ArrayList<>(projectNames.size());
		ordered.add(currentProjectName);
		referencedProjectNames.stream()
			.filter(projectNames::contains)
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.forEach(ordered::add);
		projectNames.stream()
			.filter(projectName -> !projectName.equals(currentProjectName) && !referencedProjectNames.contains(projectName))
			.forEach(ordered::add);
		return ordered;
	}
}
