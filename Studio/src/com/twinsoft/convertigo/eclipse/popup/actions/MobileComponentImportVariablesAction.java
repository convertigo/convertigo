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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/StepImportSequenceVariablesAction.java $
 * $Author: fabienb $
 * $Revision: 33546 $
 * $Date: 2013-02-11 15:19:04 +0100 (lun., 11 févr. 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.UIControlVariable;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class MobileComponentImportVariablesAction extends MyAbstractAction {

	public MobileComponentImportVariablesAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		try {
			boolean enable = false;
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject dbo = (DatabaseObject) treeObject.getObject();
				if (dbo instanceof UIDynamicAction) {
					IonBean ionBean = ((UIDynamicAction)dbo).getIonBean();
					if (ionBean != null && ionBean.getName().equals("CallSequenceAction")) {
						enable = true;
					}
				}
			}
			action.setEnabled(enable);
		}
		catch (Exception e) {}
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
    			if ((databaseObject != null) && (databaseObject instanceof UIDynamicAction)) {
    				UIDynamicAction dynAction = (UIDynamicAction)databaseObject;
					IonBean ionBean = ((UIDynamicAction)dynAction).getIonBean();
					if (ionBean != null && ionBean.getName().equals("CallSequenceAction")) {
						Object value = ionBean.getProperty("requestable").getValue();
						if (!value.equals(false)) {
							String target = value.toString();
							if (!target.isEmpty()) {
						    	try {
						    		String projectName = target.substring(0, target.indexOf('.'));
						    		String sequenceName = target.substring(target.indexOf('.')+1);
						    		Project p = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
						    		Sequence sequence = p.getSequenceByName(sequenceName);
						    		
						    		int size = sequence.numberOfVariables();
						    		for (int i=0; i<size; i++) {
						    			RequestableVariable variable = (RequestableVariable) sequence.getVariable(i);
						    			if (variable != null) {
						    				String variableName = variable.getName();
						    				if (dynAction.getVariable(variableName) == null) {
						    					if (!StringUtils.isNormalized(variableName))
						    						throw new EngineException("Variable name is not normalized : \""+variableName+"\".");
						    					
						    					UIControlVariable uiVariable = new UIControlVariable();
						    					uiVariable.setName(variableName);
						    					uiVariable.setComment(variable.getDescription());
						    					uiVariable.setVarSmartType(new MobileSmartSourceType(variable.getDefaultValue().toString()));
						    					dynAction.addUIComponent(uiVariable);

						    					uiVariable.bNew = true;
						    					uiVariable.hasChanged = true;
						    					dynAction.hasChanged = true;
						    				}
						    			}
						    		}
						    		
						    	} catch (Exception e) {}
							}
						}
	    				if (dynAction.hasChanged) {
	    					explorerView.reloadTreeObject(treeObject);
							StructuredSelection structuredSelection = new StructuredSelection(treeObject);
							ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)explorerView, structuredSelection);
	    				}
					}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to add variables to action !");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
