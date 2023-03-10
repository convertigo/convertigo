/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public abstract class MyAbstractAction extends Action implements IObjectActionDelegate {
	protected ISelection selection = null;
	protected IWorkbenchPart targetPart = null;
	protected IAction action = null;
	
	public MyAbstractAction() {
		super();
		this.action = this;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public String getId() {
		String id = super.getId();
		if (id == null && action != this) {
			id = action.getId();
		}
		if (id == null) {
			id = this.getClass().getSimpleName();
		}
		return id;
	}
	
	public String getActionId() {
		String id = action.getId();
		return id;
	}

	@Override
	public void runWithEvent(Event event) {
		run(this);
	}

	@Override
	public void run(IAction action) {
		this.action = action;
		
		Set<MobileBuilder> mbSet = new HashSet<MobileBuilder>();
		try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
    			for (TreeObject ob: treeObjects) {
					MobileBuilder mb = MobileBuilder.getBuilderOf(((TreeObject)ob).getObject());
					if (mb != null) {
						mbSet.add(mb);
					}
    			}
    		}
			
			Engine.logStudio.info("---------------------- Action started: "+ action.getId() + "----------------------");
			for (MobileBuilder mb: mbSet) {
				if (mb != null) {
					mb.prepareBatchBuild();
				}
			}
			BatchOperationHelper.start();
			
			run();
			
			BatchOperationHelper.stop();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			BatchOperationHelper.cancel();
			mbSet.clear();
			Engine.logStudio.info("---------------------- Action ended:   "+ action.getId() + "----------------------");
		}
	}
	
	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
		MessageDialog.openInformation(
			shell,
			"Convertigo Plug-in",
			"The choosen operation is not yet implemented : '"+ action.getId() + "'.");
		
		shell.setCursor(null);
		waitCursor.dispose();
	}
	
	public Shell getParentShell() {
		return targetPart == null ? ConvertigoPlugin.getMainShell() : targetPart.getSite().getShell();
	}
	
	public IWorkbenchPage getActivePage() {
		return PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();
	}
	
	public IWorkbenchPart getActivePart() {
		if (targetPart == null)
			return getActivePage().getActivePart();
		else
			return targetPart;
	}
	
	public ProjectExplorerView getProjectExplorerView() {
		ProjectExplorerView projectExplorerView = null;
		
		if ((targetPart != null) && (targetPart instanceof ProjectExplorerView)) {
			projectExplorerView = (ProjectExplorerView)targetPart;
		}
		else {
			projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		}
		return projectExplorerView;
	}
	
	public IEditorPart getConnectorEditor(Connector connector) {
		IEditorPart editorPart = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			if (connector != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ConnectorEditorInput)) {
							if (((ConnectorEditorInput)editorInput).is(connector)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					}
					catch(PartInitException e) {
						//ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return editorPart;
	}

	public IEditorPart getJscriptTransactionEditor(Transaction transaction) {
		IEditorPart editorPart = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			if (transaction != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof JScriptEditorInput)) {
							if (transaction.equals(((JScriptEditorInput) editorInput).getDatabaseObject())) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					} catch(PartInitException e) {
						//ConvertigoPlugin.logException(e, "Error while retrieving the jscript transaction editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return editorPart;
	}
}
