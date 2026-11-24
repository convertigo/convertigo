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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;

/**
 * The beans an optional library project brings: a bean of database_objects.xml with requires="project" is offered (the
 * palettes and the creation wizards of the Studios) and documented (the reference documentation) only while that
 * project is loaded, by the engine or opened in a Studio. Without it, nothing tells the bean exists.
 * <p>
 * Only the offer follows the project: an existing object of such a bean still loads, shows, edits and runs as
 * before, whether the project is loaded or not.
 * <p>
 * With {@link ExperimentalFeatures}: two independent conditions, checked side by side. A bean is offered when its
 * required project, if any, is loaded and, if it is experimental, while the experimental features are shown.
 */
public final class RequiredProjects {
	private static volatile Set<String> required;

	private RequiredProjects() {
	}

	/** the project is loaded, by the engine or opened in a Studio (false without an engine) */
	public static boolean isLoaded(String projectName) {
		var app = Engine.theApp;
		var manager = app == null ? null : app.databaseObjectsManager;
		if (manager == null || projectName == null) {
			return false;
		}
		try {
			return manager.getLoadedProjectByName(projectName) != null;
		} catch (RuntimeException e) {
			// a manager not initialized has no project loaded
			return false;
		}
	}

	/** a bean whose required project is not loaded: not offered nor documented */
	public static boolean isHiddenBean(DboBean bean) {
		var project = bean == null ? null : bean.getRequiredProject();
		return project != null && !isLoaded(project);
	}

	/** the projects some bean requires, from database_objects.xml (empty without an engine) */
	public static Set<String> getRequiredProjects() {
		var names = required;
		if (names == null) {
			var set = new TreeSet<String>();
			try {
				for (DboGroup group : Engine.theApp.getDboExplorerManager().getGroups()) {
					for (DboCategory category : group.getCategories()) {
						for (DboBeans beans : category.getBeans()) {
							for (DboBean bean : beans.getBeans()) {
								if (bean.getRequiredProject() != null) {
									set.add(bean.getRequiredProject());
								}
							}
						}
					}
				}
			} catch (Exception e) {
				// not cached: read again once the engine is there
				return Set.of();
			}
			required = names = Collections.unmodifiableSet(set);
		}
		return names;
	}

	/** some bean requires this project: its loading changes what is offered */
	public static boolean isRequired(String projectName) {
		return projectName != null && getRequiredProjects().contains(projectName);
	}

	/** the required projects loaded now, as a key for what caches the offered beans */
	public static String getLoadedKey() {
		return getRequiredProjects().stream().filter(RequiredProjects::isLoaded).collect(Collectors.joining(","));
	}
}
