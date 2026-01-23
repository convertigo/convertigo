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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.PlainMessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.NgxConverter;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ConvertMobileUIApplicationToNgxAction extends MyAbstractAction {

	public ConvertMobileUIApplicationToNgxAction() {
		super();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		try {
			boolean enable = false;
			super.selectionChanged(action, selection);

			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			if (treeObject instanceof ProjectTreeObject) {
				Project project = ((ProjectTreeObject)treeObject).getObject();
				IApplicationComponent app = project.getMobileApplication().getApplicationComponent();
				if (app != null) {
					if (app instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
						action.setText("Convert Mobile Application to Ngx");
						enable = true;
					}
					if (app instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
						action.setText("Upgrade Ngx Application");
						enable = ((com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent)app).needsMigration();
					}
				}
			}
			action.setEnabled(enable);
		} catch (Exception e) {}
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				ProjectTreeObject projectTreeObject = treeObject.getProjectTreeObject();
				if (projectTreeObject.getModified()) {
					ConvertigoPlugin.warningMessageBox("Please save project before converting it.");
					return;
				}
				Project project = projectTreeObject.getObject();
				boolean isMobileApplicationProject = project.testAttribute("isMobileApplicationProject", null);
				String dialogTitle = isMobileApplicationProject ? "Converting Mobile Application to NGX" : "Upgrading NGX Application";

				InputDialog dlg = new InputDialog(shell, dialogTitle,
						"Your project '" + project.getName() + "' will be converted to use the new version of the Mobile Builder.\n"
								+ "Please enter a new project name for a converted copy.\n"
								+ "Or convert the current project.",
								project.getName(),
								new IInputValidator() {

					@Override
					public String isValid(String newText) {
						if (newText.isBlank()) {
							return "cannot be blank";
						}
						if (!StringUtils.isNormalized(newText)) {
							return "don't use special character";
						}
						if (newText.equals(project.getName())) {
							return null;
						}
						if (Engine.theApp.databaseObjectsManager.existsProject(newText)) {
							return "a project with that name already exists";
						}
						return null;
					}
				});
				if (dlg.open() == Window.CANCEL) {
					return;
				}
				String projectName = dlg.getValue();
				String taskName = isMobileApplicationProject ? "Converting to NGX" : "Upgrading";

				if (projectName.equals(project.getName())) {
					PlainMessageDialog msg = PlainMessageDialog.getBuilder(shell, "Confirmation")
							.buttonLabels(Arrays.asList("Yes", "No"))
							.image(IconAndMessageDialog.getImage(IconAndMessageDialog.DLG_IMG_MESSAGE_WARNING))
							.message("You are about to modify the current project.\n"
									+ "The operation cannot be undone.\n"
									+ "Please make a backup of your current version before continuing.\n"
									+ "Are you sure you want to convert now?")
							.build();
					int response = msg.open();
					if (response == 0) {
						File projectDir = project.getDirFile();
						explorerView.setSelectedTreeObject(projectTreeObject);
						explorerView.unloadSelectedProjectTreeObject();
						Job.create("Project '" + projectName + "' " + taskName, monitor -> {
							monitor.beginTask(taskName + "..." , IProgressMonitor.UNKNOWN);
							try {
								new NgxConverter(projectDir).convertFile();
							} catch (Exception e) {
								Engine.logStudio.error("Error while "+ taskName, e);
							}
							monitor.beginTask("Open the converted project..." , IProgressMonitor.UNKNOWN);
							display.syncExec(() -> {
								try {
									TreeObject to = explorerView.getProjectRootObject(projectName);
									if (to instanceof UnloadedProjectTreeObject) {
										explorerView.loadProject((UnloadedProjectTreeObject) to);
									} else {
										ConvertigoPlugin.errorMessageBox("Cannot find the '" + projectName + "' project");
									}
								} catch (EngineException e) {
									ConvertigoPlugin.errorMessageBox("Failed to get the '" + projectName + "' project: " + e.getMessage());
								}
							});
						}).schedule();
					}
				} else {

					Job.create("Project '" + projectName + "' " + taskName, monitor -> {
						try {
							monitor.beginTask("Exporting '" + project.getName() + "'", IProgressMonitor.UNKNOWN);
							File car = CarUtils.makeArchive(project, ArchiveExportOption.all);
							monitor.beginTask("Importing '" + projectName + "'", IProgressMonitor.UNKNOWN);
							Project prj = Engine.theApp.databaseObjectsManager.deployProject(car.getAbsolutePath(), projectName, true);
							if (prj == null) {
								return;
							}
							monitor.beginTask(taskName + "..." , IProgressMonitor.UNKNOWN);
							new NgxConverter(prj.getDirFile()).convertFile();
							prj = Engine.theApp.databaseObjectsManager.importProject(Engine.projectYamlFile(projectName), true);
						} catch (Exception e) {
							Engine.logStudio.error("Error while "+ taskName, e);
						}
						monitor.beginTask("Open the converted project..." , IProgressMonitor.UNKNOWN);
					}).schedule();
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to convert or upgrade project!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	@Override
	protected boolean canImpactMobileBuilder(TreeObject ob) {
		return true;
	}
}
