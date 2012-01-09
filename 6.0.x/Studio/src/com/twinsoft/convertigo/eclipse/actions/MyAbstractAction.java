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

package com.twinsoft.convertigo.eclipse.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

public abstract class MyAbstractAction extends Action {
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

	public void run(IAction action) {
		this.action = action;
		run();
	}
	
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
		if (targetPart == null) {
			Shell shell = Display.getDefault().getActiveShell();
			shell = ((shell == null) ? new Shell():shell);
			return shell;
		}
		else
			return targetPart.getSite().getShell();
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
}
