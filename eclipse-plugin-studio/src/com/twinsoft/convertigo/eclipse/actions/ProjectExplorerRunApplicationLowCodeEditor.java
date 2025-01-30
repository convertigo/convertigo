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

package com.twinsoft.convertigo.eclipse.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ProjectExplorerRunApplicationLowCodeEditor extends MyAbstractAction {

	public ProjectExplorerRunApplicationLowCodeEditor() {
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
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();

				while (treeObject != null && !(treeObject instanceof DatabaseObjectTreeObject)) {
					treeObject = treeObject.getParent();
				}

				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) treeObject;

				databaseObjectTreeObject = (DatabaseObjectTreeObject) explorerView.findTreeObjectByUserObject((DatabaseObject) databaseObjectTreeObject.getObject().getProject().getMobileApplication().getApplicationComponent());
				if (databaseObjectTreeObject instanceof NgxApplicationComponentTreeObject) {
					((NgxApplicationComponentTreeObject) databaseObjectTreeObject).activeEditor(false).launchBuilder(false, false);
				} else if (databaseObjectTreeObject instanceof MobileApplicationComponentTreeObject) {
					((MobileApplicationComponentTreeObject) databaseObjectTreeObject).activeEditor(false).launchBuilder(false, false);
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to open the Application Low Code editor!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}
}
