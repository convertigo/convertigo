/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.studio.ActionModel;
import com.twinsoft.convertigo.engine.studio.DatabaseObjectsAction;

public class CreateSheetAction extends DatabaseObjectCreateAction {

	public CreateSheetAction() {
		super("com.twinsoft.convertigo.beans.core.Sheet");
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		try {
			boolean enable = true;
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			if (treeObject instanceof ObjectsFolderTreeObject) {
				enable = ((ObjectsFolderTreeObject)treeObject).folderType == ObjectsFolderTreeObject.FOLDER_TYPE_SHEETS;
			}
			else if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject dbo = (DatabaseObject) treeObject.getObject();
				ActionModel actionModel = DatabaseObjectsAction.selectionChanged(getClass().getName(), dbo);
				enable = actionModel.isEnabled;
			}
			action.setEnabled(enable);
		}
		catch (Exception e) {}
	}
	
}
