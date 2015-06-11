/**
 * @author julienda
 * @date 23/02/2015
 */
package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

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
			Object ob = treeObject.getObject();
			action.setEnabled(ob instanceof WebServiceReference);
		}
		catch (Exception e) {}
	}
}
