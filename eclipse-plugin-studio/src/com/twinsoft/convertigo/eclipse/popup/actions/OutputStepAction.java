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

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.AttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class OutputStepAction extends MyAbstractAction {

	protected boolean recurse = false;
	private boolean output = false;

	public OutputStepAction() {
		super();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		if (treeObject instanceof StepTreeObject) {
			Step step = ((StepTreeObject) treeObject).getObject();
			if (step instanceof AttributeStep || step instanceof XMLAttributeStep) {
				action.setText("Attribute always output true.");
				action.setEnabled(false);
			} else {
				output = !step.isOutput();
				String actionText = output ? "Output true":"Output false";
				actionText += recurse ? " recursively":"";
				action.setText(actionText);
			}
		}
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				DatabaseObjectTreeObject treeObject = null;
				TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
				for (int i = treeObjects.length-1 ; i>=0  ; i--) {
					treeObject = (DatabaseObjectTreeObject) treeObjects[i];
					if (treeObject instanceof StepTreeObject) {
						StepTreeObject stepTreeObject = (StepTreeObject)treeObject;
						output((Step)stepTreeObject.getObject());
						stepTreeObject.hasBeenModified(true);

						// Updating the tree
						explorerView.refreshTreeObject(stepTreeObject);

						TreeObjectEvent treeObjectEvent = new TreeObjectEvent(stepTreeObject, "output", !output, output);
						explorerView.fireTreeObjectPropertyChanged(treeObjectEvent);
					}
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to output step!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	private void output(Step step) {
		if (step != null) {
			step.setOutput(output);
			step.hasChanged = true;
			if (recurse && (step instanceof StepWithExpressions)) {
				for (Step child: ((StepWithExpressions)step).getSteps()) {
					output(child);
				}
			}
		}
	}
}
