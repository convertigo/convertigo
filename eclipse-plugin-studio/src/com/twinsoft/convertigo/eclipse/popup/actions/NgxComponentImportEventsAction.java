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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class NgxComponentImportEventsAction extends MyAbstractAction {

	public NgxComponentImportEventsAction() {
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
				if (dbo instanceof UIUseShared) {
					enable = true;
					action.setText("Import events from the targeted shared component");
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
				if (databaseObject != null) {
					if (databaseObject instanceof UIUseShared) {
						UIUseShared useShared = (UIUseShared)databaseObject;
						UISharedComponent sharedComp = useShared.getTargetSharedComponent();
						if (sharedComp != null) {
							for (UICompEvent event: sharedComp.getUICompEventList()) {
								String eventName = event.getName();
								if (useShared.getEvent(eventName) == null) {
									if (!StringUtils.isNormalized(eventName))
										throw new EngineException("event name is not normalized : \""+eventName+"\".");

									UIControlEvent uiEvent = new UIControlEvent();
									uiEvent.setEventName(eventName);
									uiEvent.setComment(event.getComment());

									useShared.addUIComponent(uiEvent);

									uiEvent.bNew = true;
									uiEvent.hasChanged = true;
									useShared.hasChanged = true;
								}
							}

							if (useShared.hasChanged) {
								explorerView.reloadTreeObject(treeObject);
								StructuredSelection structuredSelection = new StructuredSelection(treeObject);
								ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)explorerView, structuredSelection);
							}
						}
					}
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to add event to action !");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	@Override
	protected boolean canImpactMobileBuilder(TreeObject ob) {
		return true;
	}
}
