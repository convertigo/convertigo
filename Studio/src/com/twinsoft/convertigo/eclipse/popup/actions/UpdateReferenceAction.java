/**
 * @author julienda
 * @date 23/02/2015
 */
package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.studio.ActionModel;
import com.twinsoft.convertigo.engine.studio.DatabaseObjectsAction;

public class UpdateReferenceAction extends ProjectImportWsReference {

	public UpdateReferenceAction() {
		super();
		updateMode = true;
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
}
