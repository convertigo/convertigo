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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class DatabaseObjectFindDialog extends MyAbstractDialog {

	private String objectTextSubstring = null;
	private boolean bMatchCase = false;
	private int objectType;
	private List<DatabaseObjectTreeObject> vDatabaseObjects = new ArrayList<DatabaseObjectTreeObject>(64);
	
	private DatabaseObjectFindDialogComposite databaseObjectFindDialogComposite = null;
	
	public DatabaseObjectFindDialog(Shell parentShell) {
		this(parentShell, DatabaseObjectFindDialogComposite.class, "Find an Object");
	}
	
	public DatabaseObjectFindDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle) {
		super(parentShell, dialogAreaClass, dialogTitle);
		getDatabaseObjects(null);
	}

	@Override
	protected void okPressed() {
		if (dialogComposite instanceof DatabaseObjectFindDialogComposite) {
			databaseObjectFindDialogComposite = (DatabaseObjectFindDialogComposite)dialogComposite;
	        objectTextSubstring = (String)databaseObjectFindDialogComposite.getValue("Substring");
	        bMatchCase = ((Boolean)databaseObjectFindDialogComposite.getValue("matchCase")).equals(Boolean.TRUE);
	        objectType = Integer.parseInt((String)databaseObjectFindDialogComposite.getValue("ObjectType"));
	        findDatabaseObject();
		}
	}
	
	private void getDatabaseObjects(TreeParent treeObject) {
		ProjectTreeObject projectTreeObject = ConvertigoPlugin.projectManager.currentProjectTreeObject;
		DatabaseObjectTreeObject databaseObjectTreeObject = null;
		TreeParent treeParent = treeObject;
		if (treeObject == null)
			treeParent = projectTreeObject;
		
		for(TreeObject child : treeParent.getChildren()) {
			if (child instanceof DatabaseObjectTreeObject) {
				databaseObjectTreeObject = (DatabaseObjectTreeObject)child;
				if (!databaseObjectTreeObject.isInherited) {
					vDatabaseObjects.add(databaseObjectTreeObject);
					if (child instanceof TreeParent)
						getDatabaseObjects((TreeParent)child);
				}
			} else if (child instanceof TreeParent) {
				getDatabaseObjects((TreeParent)child);
			}
		}
	}

	private Enumeration<DatabaseObjectTreeObject> enumDatabaseObjects;

	protected void findDatabaseObject() {
    	enumDatabaseObjects = Collections.enumeration(vDatabaseObjects);
        while (true) {
            while (enumDatabaseObjects.hasMoreElements()) {
            	DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) enumDatabaseObjects.nextElement();
                DatabaseObject databaseObject = databaseObjectTreeObject.getObject(); 
                boolean bContinue = false;

                switch(objectType) {
                    case 0: // *
                        bContinue = true;
                        break;
                    case 1: // Screen class
                        bContinue = databaseObject.getDatabaseType().equals("ScreenClass");
                        break;
                    case 2: // Criteria
                        bContinue = databaseObject.getDatabaseType().equals("Criteria");
                        break;
                    case 3: // Extraction rule
                        bContinue = databaseObject.getDatabaseType().equals("ExtractionRule");
                        break;
                    case 4: // Sheet
                        bContinue = databaseObject.getDatabaseType().equals("Sheet");
                        break;
                    case 5: // Transaction
                        bContinue = databaseObject.getDatabaseType().equals("Transaction");
                        break;
                    case 6: // Statement
                        bContinue = databaseObject.getDatabaseType().equals("Statement");
                        break;
                    case 7: // Sequence
                        bContinue = databaseObject.getDatabaseType().equals("Sequence");
                        break;
                    case 8: // Step
                        bContinue = databaseObject.getDatabaseType().equals("Step");
                        break;
                }

                if (bContinue) {
                    String text = databaseObjectTreeObject.toString();
                    if (!bMatchCase) {
                        objectTextSubstring = objectTextSubstring.toLowerCase();
                        text = text.toLowerCase();
                    }

                    if (text.indexOf(objectTextSubstring) != -1) { // Object found !!!
                    	ConvertigoPlugin.getDefault().getProjectExplorerView().objectSelected(new CompositeEvent(databaseObject));
                    	vDatabaseObjects.remove(databaseObjectTreeObject);
                    	return;
                    }
                }
            }

        	MessageBox messageBox = new MessageBox(getShell(),SWT.YES | SWT.NO | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
			String message = "The end of the document has been reached. Do you want to retry the search from the beginning of the document?";
        	messageBox.setMessage(message);
        	int ret = messageBox.open();
        	if (ret == SWT.YES) {
        		getDatabaseObjects(null);
        	}
        	else {
        		return;
        	}
        }
    }
	
}
