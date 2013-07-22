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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewContentProvider;
import com.twinsoft.convertigo.engine.EngineException;

public class SequenceExecuteSelectedAction extends MyAbstractAction {

	public SequenceExecuteSelectedAction() {
		super();
	}
	
	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	boolean withXslt = false;
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			if ((treeObject != null) && (treeObject instanceof SequenceTreeObject)) {
    				SequenceTreeObject sequenceTreeObject = (SequenceTreeObject)treeObject;
    				openEditors(explorerView, sequenceTreeObject);
    				
    				Sequence sequence = sequenceTreeObject.getObject();
    				ProjectTreeObject projectTreeObject = sequenceTreeObject.getProjectTreeObject();
    				SequenceEditor sequenceEditor = projectTreeObject.getSequenceEditor(sequence);
    				if (sequenceEditor != null) {
    					getActivePage().activate(sequenceEditor);
    					sequenceEditor.getSequenceEditorPart().clearBrowser();
    					sequenceEditor.getDocument(sequence.getName(), isStubRequested(), withXslt);
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to execute the selected sequence!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

	protected boolean isStubRequested() {
		return false;
	}
	
	protected void openEditors(ProjectExplorerView explorerView, TreeObject treeObject) {
		if (treeObject instanceof SequenceTreeObject) {
			SequenceTreeObject sequenceTreeObject = (SequenceTreeObject)treeObject;
			openEditors(explorerView, sequenceTreeObject.getObject().getSteps());
			sequenceTreeObject.openSequenceEditor(); // open sequence editor
		}
	}
	
	private void openEditors(ProjectExplorerView explorerView, List<Step> steps) {
		for (Step step: steps) {
			if (step instanceof SequenceStep) {
				SequenceStep sequenceStep = (SequenceStep)step;
				String projectName = sequenceStep.getProjectName();
				// load project if necessary
				if (!step.getSequence().getProject().getName().equals(projectName))
					loadProject(explorerView, projectName);
				if (step.getSequence().equals(sequenceStep.getSequenceName()))
					return; // avoid sequence recursion
				
				try {
					ProjectTreeObject projectTreeObject = explorerView.getProjectRootObject(projectName);
					Sequence subSequence = projectTreeObject.getObject().getSequenceByName(sequenceStep.getSequenceName());
					SequenceTreeObject subSequenceTreeObject = (SequenceTreeObject)explorerView.findTreeObjectByUserObject(subSequence);
					openEditors(explorerView, subSequenceTreeObject); // recurse on sequence
				} catch (EngineException e) {
					e.printStackTrace();
				}
			}
			else if (step instanceof TransactionStep) {
				TransactionStep transactionStep = (TransactionStep)step;
				String projectName = transactionStep.getProjectName();
				if (!step.getSequence().getProject().getName().equals(projectName))
					loadProject(explorerView, projectName); // load project if necessary
				
				try {
					ProjectTreeObject projectTreeObject = explorerView.getProjectRootObject(projectName);
					Connector connector = projectTreeObject.getObject().getConnectorByName(transactionStep.getConnectorName());
					ConnectorTreeObject connectorTreeObject = (ConnectorTreeObject)explorerView.findTreeObjectByUserObject(connector);
					connectorTreeObject.openConnectorEditor(); // open connector editor
				} catch (EngineException e) {
					e.printStackTrace();
				}
			}
			else if (step instanceof StepWithExpressions) {
				openEditors(explorerView, ((StepWithExpressions)step).getSteps());
			}
		}
	}
	
	private void loadProject(ProjectExplorerView explorerView, String projectName) {
		if (!explorerView.isProjectLoaded(projectName)) {
			TreeObject unloadedProjectTreeObject;
			try {
				unloadedProjectTreeObject = ((ViewContentProvider) explorerView.viewer
						.getContentProvider()).getProjectRootObject(projectName);
				explorerView.loadProject((UnloadedProjectTreeObject)unloadedProjectTreeObject);
				
				try {
					while (!explorerView.isProjectLoaded(projectName))
						Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
				
			} catch (EngineException e) {
				e.printStackTrace();
			}
		}
	}
}
