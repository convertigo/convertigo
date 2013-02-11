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

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class StepExportVariablesToSequenceAction extends MyAbstractAction {

	public StepExportVariablesToSequenceAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();
    			if ((databaseObject != null) && (databaseObject instanceof RequestableStep)) {
    				RequestableStep requestableStep = (RequestableStep)databaseObject;
    				requestableStep.exportVariableDefinition();
    				
    				Sequence sequence = requestableStep.getSequence();
    				if (sequence.hasChanged) {
    					SequenceTreeObject sequenceTreeObject = (SequenceTreeObject) explorerView.findTreeObjectByUserObject(sequence);
    					explorerView.reloadTreeObject(sequenceTreeObject);
    					explorerView.setSelectedTreeObject(sequenceTreeObject);
						StructuredSelection structuredSelection = new StructuredSelection(sequenceTreeObject);
						ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)explorerView, structuredSelection);
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to export step variables to main sequence!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
