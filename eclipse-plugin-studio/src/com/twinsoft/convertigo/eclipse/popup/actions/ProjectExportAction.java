/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.awt.Toolkit;
import java.io.File;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.ArchiveExportOptionDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.CarUtils;

public class ProjectExportAction extends MyAbstractAction {

	public ProjectExportAction() {
		super();
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
				ProjectTreeObject projectTreeObject = (ProjectTreeObject) explorerView.getFirstSelectedTreeObject();
				Project project = (Project) projectTreeObject.getObject();
				String projectName = project.getName();

				if (projectTreeObject.hasChanged() && !projectTreeObject.save(true)) {
					return;
				}

				ArchiveExportOptionDialog dlg = new ArchiveExportOptionDialog(shell, project, false);
				if (dlg.open() != Window.OK) {
					return;
				}

				if (!dlg.getVersion().equals(project.getVersion())) {
					project.setVersion(dlg.getVersion());
					project.hasChanged = true;
					projectTreeObject.save(false);
				}

				explorerView.refreshTreeObject(projectTreeObject);

				String projectArchive = projectName + ".car";

				FileDialog fileDialog = new FileDialog(shell, SWT.PRIMARY_MODAL | SWT.SAVE);
				fileDialog.setText("Export a project");
				fileDialog.setFilterExtensions(new String[]{"*.car","*.zip"});
				fileDialog.setFilterNames(new String[]{"Convertigo archives","Convertigo archives as zip"});
				fileDialog.setFilterPath(Engine.PROJECTS_PATH);
				fileDialog.setFileName(projectArchive);

				String filePath = fileDialog.open();
				if (filePath != null) {
					File file = new File(filePath);

					if (file.exists()) {
						if (ConvertigoPlugin.questionMessageBox(shell, "File already exists. Do you want to overwrite?") == SWT.YES) {
							if (!file.delete()) {
								ConvertigoPlugin.warningMessageBox("Error when deleting the file " + file.getName() + "! Please verify access rights!");
								return;
							}
						} else {
							return;
						}
					}

					if (Pattern.matches(".+(\\.zip|\\.car)", file.getName())) {
						CarUtils.makeArchive(file, project, dlg.getArchiveExportOptions());
					}
					else {
						Toolkit.getDefaultToolkit().beep();
						ConvertigoPlugin.logWarning("Wrong file extension!");
					}
				}

				projectTreeObject.getIProject().refreshLocal(IResource.DEPTH_ONE, null);
				explorerView.setFocus();
				explorerView.setSelectedTreeObject(projectTreeObject);
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to export the project!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

}
