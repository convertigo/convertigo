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

import java.util.NoSuchElementException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.studio.ActionModel;
import com.twinsoft.convertigo.engine.studio.DatabaseObjectsAction;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class CreateStubFromXMLAction extends AbstractStubAction {

	public CreateStubFromXMLAction() {
		super();
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		try {
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			
			DatabaseObject dbo = (DatabaseObject) treeObject.getObject();
			ActionModel actionModel = DatabaseObjectsAction.selectionChanged(getClass().getName(), dbo);
			action.setEnabled(actionModel.isEnabled);
		}
		catch (Exception e) {}
	}

	public Document getXML(TreeObject treeObject) throws Exception {
		Document dom = XMLUtils.createDom("java");
		ProjectTreeObject projectTreeObject = treeObject.getProjectTreeObject();
		Object requestable = treeObject.getObject();

		if (requestable instanceof Transaction) {
			Transaction transaction = (Transaction) requestable;
			ConnectorEditor connectorEditor = projectTreeObject.getConnectorEditor((Connector) transaction.getParent());
			dom = connectorEditor != null ? connectorEditor.getLastGeneratedDocument() : null;
		} else if (requestable instanceof Sequence) {
			SequenceEditor sequenceEditor = projectTreeObject.getSequenceEditor((Sequence) requestable);
			dom = sequenceEditor != null ? sequenceEditor.getLastGeneratedDocument() : null;
		}
		if (dom == null) {
			throw new NoSuchElementException("Editor is closed or has none XML generated");
		}
		return dom;
	}
}
