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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxPageComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.studio.ActionModel;
import com.twinsoft.convertigo.engine.studio.DatabaseObjectsAction;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EnableNgxPageComponentAction extends MyAbstractAction {

	public EnableNgxPageComponentAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		if (treeObject instanceof NgxPageComponentTreeObject) {
			DatabaseObject dbo = (DatabaseObject) treeObject.getObject();
			ActionModel actionModel = DatabaseObjectsAction.selectionChanged(getClass().getName(), dbo);
			action.setEnabled(actionModel.isEnabled);
		}
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				DatabaseObjectTreeObject treeObject = null;
				PageComponent component = null;

				TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
				for (int i = treeObjects.length-1 ; i>=0  ; i--) {
					treeObject = (DatabaseObjectTreeObject) treeObjects[i];
					if (treeObject instanceof NgxPageComponentTreeObject) {
						NgxPageComponentTreeObject componentTreeObject = GenericUtils.cast(treeObject);
						component = (PageComponent)componentTreeObject.getObject();
						component.setEnabled(true);

						componentTreeObject.setEnabled(true);
						componentTreeObject.hasBeenModified(true);

						TreeObjectEvent treeObjectEvent = new TreeObjectEvent(componentTreeObject, "isEnabled", false, true);
						explorerView.fireTreeObjectPropertyChanged(treeObjectEvent);
					}
				}

				explorerView.refreshSelectedTreeObjects();
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to enable page!");
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
