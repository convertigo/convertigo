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

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.wizards.new_ngx.SharedComponentWizard;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class NgxSharedComponentExtractAction extends MyAbstractAction {
	public NgxSharedComponentExtractAction() {
		super();
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		boolean enable = false;
		try {
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.size() > 0) {
				boolean doIt = true;
				TreeObject previous = null;
				@SuppressWarnings("unchecked")
				List<TreeObject> list = structuredSelection.toList();
				for (TreeObject to: list) {
					if (!isAllowed(to)) {
						doIt = false;
						break;
					}
					
					if (previous != null) {
						if (!to.getPreviousSibling().equals(previous)) {
							doIt = false;
							break;
						}
					}
					previous = to;
				}
				
				if (doIt) {
					enable = true;
				}
			}
		}
		catch (Exception e) {}
		action.setEnabled(enable);
	}
	
	private boolean isAllowed(TreeObject treeObject) {
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject) treeObject;
			if (doto.isEnabled() && !doto.hasAncestorDisabled()) {
				DatabaseObject dbo = doto.getObject();
				if (dbo instanceof UIElement) {
					UIElement uie = (UIElement)dbo;
					boolean isUIDynamicAction = uie instanceof UIDynamicAction;
					boolean isInForm = uie.getUIForm() != null && !uie.equals(uie.getUIForm());
					
					if (!isUIDynamicAction && !isInForm) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			DatabaseObjectTreeObject firstSelectedDoTo = GenericUtils.cast(explorerView.getFirstSelectedTreeObject());
    			DatabaseObjectTreeObject parentTreeObject = firstSelectedDoTo.getParentDatabaseObjectTreeObject();
   				
    			TreeObject appTo = getAppTreeObject(firstSelectedDoTo);
    			if (appTo == null) {
    				throw new Exception("Unable to retrieve target application");
    			}
    			
    			TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
    			List<DatabaseObject> objectList = GenericUtils.cast(Arrays.asList(explorerView.getSelectedDatabaseObjects()));
    			SharedComponentWizard newObjectWizard = new SharedComponentWizard(objectList);
        		WizardDialog wzdlg = new WizardDialog(shell, newObjectWizard);
        		wzdlg.setPageSize(850, 650);
        		wzdlg.open();
        		int result = wzdlg.getReturnCode();
        		if ((result != Window.CANCEL) && (newObjectWizard.newBean != null)) {
        			for (TreeObject to: treeObjects) {
        				if (((DatabaseObject)to.getObject()).getParent() == null) {
        					parentTreeObject.removeChild(firstSelectedDoTo);
        				}
        			}
        			explorerView.reloadTreeObject(appTo);
        			explorerView.objectSelected(new CompositeEvent(newObjectWizard.newBean));
        		}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to create a new shared component!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

	private TreeObject getAppTreeObject(TreeObject parentTreeObject) {
		while (parentTreeObject != null) {
			Class<?> c = parentTreeObject.getObject().getClass();
			if (c.equals(ApplicationComponent.class)) {
				return parentTreeObject;
			}
			parentTreeObject = parentTreeObject.getParent();
		}
		return null;
	}
}
