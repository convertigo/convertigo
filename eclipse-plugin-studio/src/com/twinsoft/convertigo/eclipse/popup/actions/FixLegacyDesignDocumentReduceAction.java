/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentViewTreeObject;

public class FixLegacyDesignDocumentReduceAction extends MyAbstractAction {
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		action.setEnabled(selection instanceof IStructuredSelection structured
				&& structured.getFirstElement() instanceof DesignDocumentViewTreeObject view && view.hasLegacyReduce());
	}

	@Override
	public void run() {
		try {
			var explorer = getProjectExplorerView();
			if (explorer == null || !(explorer.getFirstSelectedTreeObject() instanceof DesignDocumentViewTreeObject view)
					|| !view.hasLegacyReduce()) {
				return;
			}
			if (!MessageDialog.openQuestion(getParentShell(), "Fix legacy count reduce",
					"Replace the old default reduce of '" + view.getDocViewName()
					+ "' with a count function that handles CouchDB partial results?\n\n"
					+ "An open reduce editor will be saved and closed. Save the project afterwards to keep the fix.")) {
				return;
			}
			var function = view.getReduceFunction();
			if (function != null && function.closeEditor(true)) {
				if (view.fixLegacyReduce()) {
					explorer.refreshTreeObject(view);
				} else {
					MessageDialog.openInformation(getParentShell(), "Fix legacy count reduce",
							"The reduce was customized in its editor and has not been replaced.");
				}
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to fix the legacy reduce function");
		}
	}
}
