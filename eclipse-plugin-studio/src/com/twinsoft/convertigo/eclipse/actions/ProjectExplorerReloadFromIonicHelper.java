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

package com.twinsoft.convertigo.eclipse.actions;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.NgxIonicRoundTripConverter;
import com.twinsoft.convertigo.engine.util.NgxIonicRoundTripConverter.ImportReport;

public class ProjectExplorerReloadFromIonicHelper {
	private static final long LOAD_TIMEOUT_MS = 15000;
	private static final long LOAD_POLL_MS = 100;

	private ProjectExplorerReloadFromIonicHelper() {
	}

	public static ImportReport reloadProjectFromIonic(String projectName) throws Exception {
		var data = prepareLoadedProject(projectName);
		ImportReport report = NgxIonicRoundTripConverter.importFromIonic(data.project);
		waitForReloadJob(reloadProject(data.view, data.projectTreeObject), projectName);
		return report;
	}

	public static ImportReport importProjectFromIonic(String projectName, String ionicTargetPath) throws Exception {
		var data = prepareLoadedProject(projectName);
		File target = ionicTargetPath == null || ionicTargetPath.isBlank() ? null : new File(ionicTargetPath);
		ImportReport report = NgxIonicRoundTripConverter.importFromIonic(data.project, target);
		refreshProject(data.view, data.projectTreeObject);
		return report;
	}

	private static LoadedProjectData prepareLoadedProject(String projectName) throws Exception {
		if (projectName == null || projectName.isBlank()) {
			throw new IllegalArgumentException("Project name is required");
		}

		File projectDir = new File(Engine.projectDir(projectName));
		File ionicDir = new File(projectDir, "_private/ionic");
		if (!ionicDir.exists()) {
			throw new IllegalStateException("Missing " + ionicDir.getAbsolutePath());
		}

		ProjectExplorerView view = requireProjectExplorerView();
		TreeObject root = getProjectRootObject(view, projectName);
		if (root == null) {
			loadMissingProjectRoot(view, projectName);
			root = waitForProjectRoot(view, projectName);
		} else if (root instanceof UnloadedProjectTreeObject unloaded) {
			loadProject(view, unloaded);
			root = waitForProjectRoot(view, projectName);
		}

		if (!(root instanceof ProjectTreeObject projectTreeObject)) {
			throw new IllegalStateException("Project not loaded in Project Explorer: " + projectName);
		}

		if (projectTreeObject.getModified()) {
			projectTreeObject.save(false);
		}

		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
		if (project == null) {
			throw new EngineException("Project not loaded: " + projectName);
		}

		return new LoadedProjectData(view, projectTreeObject, project);
	}

	private static ProjectExplorerView requireProjectExplorerView() throws Exception {
		AtomicReference<ProjectExplorerView> ref = new AtomicReference<>();
		AtomicReference<Throwable> error = new AtomicReference<>();
		ConvertigoPlugin.syncExec(() -> {
			try {
				var plugin = ConvertigoPlugin.getDefault();
				if (plugin == null) {
					throw new IllegalStateException("Project Explorer view not available");
				}
				var view = plugin.getProjectExplorerView();
				if (view == null) {
					throw new IllegalStateException("Project Explorer view not available");
				}
				ref.set(view);
			} catch (Throwable t) {
				error.set(t);
			}
		});
		if (error.get() != null) {
			throw asException(error.get());
		}
		return ref.get();
	}

	private static TreeObject getProjectRootObject(ProjectExplorerView view, String projectName) throws Exception {
		AtomicReference<TreeObject> ref = new AtomicReference<>();
		AtomicReference<Throwable> error = new AtomicReference<>();
		ConvertigoPlugin.syncExec(() -> {
			try {
				ref.set(view.getProjectRootObject(projectName));
			} catch (Throwable t) {
				error.set(t);
			}
		});
		if (error.get() != null) {
			throw asException(error.get());
		}
		return ref.get();
	}

	private static void loadMissingProjectRoot(ProjectExplorerView view, String projectName) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		ConvertigoPlugin.syncExec(() -> {
			try {
				view.importProjectTreeObject(projectName);
			} catch (CoreException e) {
				error.set(e);
			} catch (Throwable t) {
				error.set(t);
			}
		});
		if (error.get() != null) {
			throw asException(error.get());
		}
	}

	private static void loadProject(ProjectExplorerView view, UnloadedProjectTreeObject unloaded) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		ConvertigoPlugin.syncExec(() -> {
			try {
				view.loadProject(unloaded, false);
			} catch (Throwable t) {
				error.set(t);
			}
		});
		if (error.get() != null) {
			throw asException(error.get());
		}
	}

	private static TreeObject waitForProjectRoot(ProjectExplorerView view, String projectName) throws Exception {
		long deadline = System.currentTimeMillis() + LOAD_TIMEOUT_MS;
		TreeObject root = null;
		while (System.currentTimeMillis() < deadline) {
			root = getProjectRootObject(view, projectName);
			if (root instanceof ProjectTreeObject) {
				return root;
			}
			Thread.sleep(LOAD_POLL_MS);
		}
		throw new IllegalStateException("Timed out while waiting for project to load: " + projectName);
	}

	private static Job reloadProject(ProjectExplorerView view, ProjectTreeObject projectTreeObject) throws Exception {
		AtomicReference<Job> job = new AtomicReference<>();
		AtomicReference<Throwable> error = new AtomicReference<>();
		ConvertigoPlugin.syncExec(() -> {
			try {
				job.set(view.reloadProject(projectTreeObject));
			} catch (Throwable t) {
				error.set(t);
			}
		});
		if (error.get() != null) {
			throw asException(error.get());
		}
		return job.get();
	}

	private static void waitForReloadJob(Job job, String projectName) throws Exception {
		if (job == null || Display.getCurrent() != null) {
			return;
		}
		try {
			job.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EngineException("Interrupted while waiting for project reload: " + projectName, e);
		}
		IStatus status = job.getResult();
		if (status != null && status.matches(IStatus.ERROR | IStatus.CANCEL)) {
			Throwable exception = status.getException();
			throw exception == null
				? new EngineException(status.getMessage())
				: new EngineException(status.getMessage(), exception);
		}
	}

	private static void refreshProject(ProjectExplorerView view, ProjectTreeObject projectTreeObject) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		ConvertigoPlugin.syncExec(() -> {
			try {
				view.reloadTreeObject(projectTreeObject);
			} catch (Throwable t) {
				error.set(t);
			}
		});
		if (error.get() != null) {
			throw asException(error.get());
		}
	}

	private static Exception asException(Throwable throwable) {
		return throwable instanceof Exception exception ? exception : new Exception(throwable);
	}

	private static class LoadedProjectData {
		final ProjectExplorerView view;
		final ProjectTreeObject projectTreeObject;
		final Project project;

		LoadedProjectData(ProjectExplorerView view, ProjectTreeObject projectTreeObject, Project project) {
			this.view = view;
			this.projectTreeObject = projectTreeObject;
			this.project = project;
		}
	}
}
