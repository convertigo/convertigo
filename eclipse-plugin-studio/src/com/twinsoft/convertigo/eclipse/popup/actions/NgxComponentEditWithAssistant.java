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
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.assistant.AssistantView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class NgxComponentEditWithAssistant extends MyAbstractAction {

	public NgxComponentEditWithAssistant() {
		super();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		try {
			boolean enable = false;
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject dbo = (DatabaseObject) treeObject.getObject();
				if (dbo instanceof UISharedRegularComponent) {
					enable = !dbo.getProject().getName().equals("ConvertigoAssistant");
				}
			}
			action.setEnabled(enable);
		}
		catch (Exception e) {}
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
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				Object databaseObject = treeObject.getObject();
				if ((databaseObject != null) && (databaseObject instanceof UISharedRegularComponent)) {
					UISharedRegularComponent uisc = (UISharedRegularComponent)databaseObject;
					String threadId = null;
					String qname = null;
					try {
						int idx = uisc.getComment().indexOf("thread");
						threadId = uisc.getComment().substring(idx);
						qname = uisc.getQName();
					} catch (Exception e) {}
					if (threadId != null && qname != null) {
						IWorkbenchPage activePage = getActivePage();
						if (activePage != null) {
							IViewPart viewPart =  activePage.findView(AssistantView.ID);
							if (viewPart == null) {
								viewPart = activePage.showView(AssistantView.ID);
							}
							if (viewPart != null) {
								AssistantView assistantView = (AssistantView)viewPart;
								assistantView.changeThread(qname, threadId);
							}
						}
					}
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to edit component with AI Asssistant!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

}
