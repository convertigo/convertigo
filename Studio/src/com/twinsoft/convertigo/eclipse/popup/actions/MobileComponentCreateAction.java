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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/DatabaseObjectCreateAction.java $
 * $Author: fabienb $
 * $Revision: 33546 $
 * $Date: 2013-02-11 15:19:04 +0100 (lun., 11 févr. 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.wizards.new_mobile.ComponentObjectWizard;

public class MobileComponentCreateAction extends MyAbstractAction {
	protected String databaseObjectClassName = null;
	
	public MobileComponentCreateAction() {
		super();
	}
	
	public MobileComponentCreateAction(String databaseObjectClassName) {
		super();
		this.databaseObjectClassName = databaseObjectClassName;
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		TreeObject parentTreeObject = null;
    		DatabaseObject databaseObject = null;
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			parentTreeObject = explorerView.getFirstSelectedTreeObject();
    			
    			if (parentTreeObject instanceof ObjectsFolderTreeObject) {
    				parentTreeObject = ((ObjectsFolderTreeObject) parentTreeObject).getParent();
    				databaseObject  = (DatabaseObject) parentTreeObject.getObject();
    			}
    			else {
    				databaseObject = (DatabaseObject) parentTreeObject.getObject();
    			}
    			
    			ComponentObjectWizard newObjectWizard = new ComponentObjectWizard(databaseObject, databaseObjectClassName);
        		WizardDialog wzdlg = new WizardDialog(shell, newObjectWizard);
        		wzdlg.setPageSize(850, 650);
        		wzdlg.open();
        		int result = wzdlg.getReturnCode();
        		if ((result != Window.CANCEL) && (newObjectWizard.newBean != null)) {
        			postCreate(parentTreeObject, newObjectWizard.newBean);
        		}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to create a new database object '"+ databaseObjectClassName +"'!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

	public void postCreate(TreeObject parentTreeObject, DatabaseObject createdDatabaseObject) throws Exception {
		ProjectExplorerView explorerView = getProjectExplorerView();
		explorerView.reloadTreeObject(parentTreeObject);
		explorerView.objectSelected(new CompositeEvent(createdDatabaseObject));
		
		/* No more needed since #20 correction : see DatabaseObjectTreeObject:setParent(TreeParent parent)
		TreeObject selectedTreeObject = explorerView.getFirstSelectedTreeObject();
		if ((selectedTreeObject != null) && (selectedTreeObject.getObject().equals(createdDatabaseObject)))
			explorerView.fireTreeObjectAdded(new TreeObjectEvent(selectedTreeObject));*/
	}
	
}
