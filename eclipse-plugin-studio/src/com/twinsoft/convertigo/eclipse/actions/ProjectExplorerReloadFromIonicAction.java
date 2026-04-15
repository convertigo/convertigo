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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ProjectExplorerReloadFromIonicAction extends MyAbstractAction {

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView projectExplorerView = getProjectExplorerView();
			if (projectExplorerView == null) {
				return;
			}

			TreeObject[] treeObjects = projectExplorerView.getSelectedTreeObjects();
			if (treeObjects == null || treeObjects.length == 0) {
				ConvertigoPlugin.warningMessageBox("Select one or more projects to reload from Ionic.");
				return;
			}

			TreeViewer viewer = projectExplorerView.viewer;
			for (TreeObject treeObject : treeObjects) {
				String projectName = treeObject.getName();
				var report = ProjectExplorerReloadFromIonicHelper.reloadProjectFromIonic(projectName);

				for (String warning : report.warnings) {
					ConvertigoPlugin.logWarning(warning);
				}
			}
			viewer.refresh();
		} catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to reload project(s) from Ionic!");
		} finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	public void init(IViewPart view) {
	}
}
