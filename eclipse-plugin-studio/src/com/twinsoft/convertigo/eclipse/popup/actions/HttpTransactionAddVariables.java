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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.HttpTransactionVariablesDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class HttpTransactionAddVariables extends MyAbstractAction {
	protected String databaseObjectClassName = null;

	public HttpTransactionAddVariables() {
		super();
	}

	public HttpTransactionAddVariables(String databaseObjectClassName) {
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
			TreeObject parentTreeObject = null;
			AbstractHttpTransaction databaseObject = null;
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				parentTreeObject = explorerView.getFirstSelectedTreeObject();

				if (parentTreeObject.getObject() instanceof AbstractHttpTransaction) {
					databaseObject = (AbstractHttpTransaction) parentTreeObject.getObject();
					HttpTransactionVariablesDialog couchVariablesDialog = new HttpTransactionVariablesDialog(shell, databaseObject);
					couchVariablesDialog.open();
					explorerView.reloadTreeObject(parentTreeObject);
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
}
