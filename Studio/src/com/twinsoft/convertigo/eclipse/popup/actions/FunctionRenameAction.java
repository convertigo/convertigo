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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ViewRenameAction.java $
 * $Author: nathalieh $
 * $Revision: 39123 $
 * $Date: 2015-02-05 09:41:39 +0100 (jeu., 05 févr. 2015) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentFilterTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentUpdateTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class FunctionRenameAction extends TreeObjectRenameAction {

	public FunctionRenameAction() {
		super();
		this.type = "function";
	}

	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		action.setEnabled(treeObject instanceof DesignDocumentFilterTreeObject ||
				treeObject instanceof DesignDocumentUpdateTreeObject);
	}
		
}
