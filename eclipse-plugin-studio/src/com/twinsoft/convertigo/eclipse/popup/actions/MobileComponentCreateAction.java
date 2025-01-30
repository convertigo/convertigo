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

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.wizards.new_mobile.ComponentObjectWizard;

public class MobileComponentCreateAction extends MyAbstractAction {
	private String databaseObjectClassName = null;

	public MobileComponentCreateAction() {
		super();
	}

	MobileComponentCreateAction(String databaseObjectClassName) {
		super();
		this.databaseObjectClassName = databaseObjectClassName;
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			int folderType = -1;
			TreeObject parentTreeObject = null;
			DatabaseObject databaseObject = null;
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				parentTreeObject = explorerView.getFirstSelectedTreeObject();

				if (parentTreeObject instanceof ObjectsFolderTreeObject) {
					ObjectsFolderTreeObject folderTreeObject = (ObjectsFolderTreeObject)parentTreeObject;
					folderType = folderTreeObject.folderType;
					parentTreeObject = folderTreeObject.getParent();
					databaseObject  = (DatabaseObject) parentTreeObject.getObject();
				}
				else {
					databaseObject = (DatabaseObject) parentTreeObject.getObject();
				}

				ComponentObjectWizard newObjectWizard = new ComponentObjectWizard(databaseObject, databaseObjectClassName, folderType);
				WizardDialog wzdlg = new WizardDialog(shell, newObjectWizard);
				wzdlg.setPageSize(850, 650);
				wzdlg.open();
				int result = wzdlg.getReturnCode();
				if ((result != Window.CANCEL) && (newObjectWizard.newBean != null)) {
					postCreate(parentTreeObject, newObjectWizard.newBean);
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to create a new database object '"+ databaseObjectClassName +"'!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	private void postCreate(TreeObject parentTreeObject, DatabaseObject createdDatabaseObject) throws Exception {
		ProjectExplorerView explorerView = getProjectExplorerView();
		explorerView.reloadTreeObject(parentTreeObject);
		explorerView.objectSelected(new CompositeEvent(createdDatabaseObject));
	}

}
