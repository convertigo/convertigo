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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class OpenProjectTestPlatformAction extends MyAbstractAction {

	public OpenProjectTestPlatformAction() {
		super();
	}

	protected Project getSelectedProject(ProjectExplorerView explorerView) {
		if (explorerView != null) {
			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
			Object databaseObject = treeObject == null ? null : treeObject.getObject();

			if (databaseObject instanceof Project) {
				return (Project) databaseObject;
			}
			if (databaseObject instanceof MobileApplication) {
				return ((MobileApplication) databaseObject).getProject();
			}
			if (databaseObject instanceof DatabaseObject) {
				return ((DatabaseObject) databaseObject).getProject();
			}
		}
		return null;
	}

	protected String getProjectUrl(Project project) {
		return EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/dashboard/" + project.getName() + "/backend/";
	}

	protected String getUnableMessage() {
		return "Unable to open the selected project!";
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			Project project = getSelectedProject(explorerView);
			if (project != null) {
				Program.launch(getProjectUrl(project));
			}

		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, getUnableMessage());
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

}
