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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.YamlConverter;


public class DatabaseObjectFindDialog extends MyAbstractDialog {

	private String objectTextSubstring = null;
	private boolean bMatchCase = false;
	private boolean bRegExp = false;
	private int objectType;
	private List<DatabaseObjectTreeObject> vDatabaseObjects = new ArrayList<DatabaseObjectTreeObject>(64);
	private TreeObject firstSelected = null;
	
	private DatabaseObjectFindDialogComposite databaseObjectFindDialogComposite = null;
	
	public DatabaseObjectFindDialog(Shell parentShell) {
		this(parentShell, DatabaseObjectFindDialogComposite.class, "Find an Object");
	}
	
	public DatabaseObjectFindDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle) {
		super(parentShell, dialogAreaClass, dialogTitle);
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(false);
		getDatabaseObjects(null);
	}

	@Override
	public boolean close() {
		vDatabaseObjects.clear();
		return super.close();
	}

	@Override
	protected int getShellStyle() {
		return SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE;
	}

	@Override
	protected void okPressed() {
		if (dialogComposite instanceof DatabaseObjectFindDialogComposite) {
			databaseObjectFindDialogComposite = (DatabaseObjectFindDialogComposite)dialogComposite;
	        objectTextSubstring = (String)databaseObjectFindDialogComposite.getValue("Substring");
	        bMatchCase = ((Boolean)databaseObjectFindDialogComposite.getValue("matchCase")).equals(Boolean.TRUE);
	        bRegExp = ((Boolean)databaseObjectFindDialogComposite.getValue("isRegExp")).equals(Boolean.TRUE);
	        objectType = Integer.parseInt((String)databaseObjectFindDialogComposite.getValue("ObjectType"));
	        findDatabaseObject();
		}
	}
	
	private void getDatabaseObjects(TreeParent treeObject) {
		ProjectTreeObject projectTreeObject = ConvertigoPlugin.projectManager.currentProjectTreeObject;
		
		TreeParent treeParent = treeObject;
		if (treeObject == null) {
			treeParent = projectTreeObject;
			
			TreeObject treeSelected = ConvertigoPlugin.getDefault().getProjectExplorerView().getFirstSelectedTreeObject();
    		while (treeSelected != null && !(treeSelected instanceof TreeParent)) {
    			treeSelected = treeSelected.getParent();
    		}
    		if (treeSelected != null) {
    			treeParent = (TreeParent)treeSelected;
    		}
			if (firstSelected == null) {
				firstSelected = treeSelected;
			}
		}
		
		List<? extends TreeObject> children = treeParent.getChildren();
		children.sort(ConvertigoPlugin.getDefault().getProjectExplorerView().getViewerComparator());
		for (TreeObject child : children) {
			if (child instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)child;
				if (!databaseObjectTreeObject.isInherited) {
					vDatabaseObjects.add(databaseObjectTreeObject);
					getDatabaseObjects(databaseObjectTreeObject);
				}
			} else if (child instanceof TreeParent) {
				getDatabaseObjects((TreeParent)child);
			}
		}
	}

	protected void findDatabaseObject() {
		Pattern pattern = null;
		String substring = "";
		
		try {
			pattern = bRegExp ? (bMatchCase ? Pattern.compile(objectTextSubstring) : 
												Pattern.compile(objectTextSubstring, Pattern.CASE_INSENSITIVE)) : null;
		} catch (Exception pex) {
			ConvertigoPlugin.errorMessageBox(pex.getClass().getName()+ ":\n"+ pex.getMessage());
			return;
		}
		
		substring = bMatchCase ? objectTextSubstring : objectTextSubstring.toLowerCase();
		
        while (true) {
    		Enumeration<DatabaseObjectTreeObject> enumDatabaseObjects = Collections.enumeration(vDatabaseObjects);
            while (enumDatabaseObjects.hasMoreElements()) {
            	DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) enumDatabaseObjects.nextElement();
                DatabaseObject databaseObject = databaseObjectTreeObject.getObject(); 
                boolean bContinue = false;

                switch(objectType) {
                    case 0: // *
                        bContinue = true;
                        break;
                    case 1: // Mobile Component
                    	bContinue = databaseObject.getDatabaseType().equals("MobileComponent");
                    	break;
                    case 2: // Screen class
                        bContinue = databaseObject.getDatabaseType().equals("ScreenClass");
                        break;
                    case 3: // Criteria
                        bContinue = databaseObject.getDatabaseType().equals("Criteria");
                        break;
                    case 4: // Extraction rule
                        bContinue = databaseObject.getDatabaseType().equals("ExtractionRule");
                        break;
                    case 5: // Sheet
                        bContinue = databaseObject.getDatabaseType().equals("Sheet");
                        break;
                    case 6: // Transaction
                        bContinue = databaseObject.getDatabaseType().equals("Transaction");
                        break;
                    case 7: // Statement
                        bContinue = databaseObject.getDatabaseType().equals("Statement");
                        break;
                    case 8: // Sequence
                        bContinue = databaseObject.getDatabaseType().equals("Sequence");
                        break;
                    case 9: // Step
                        bContinue = databaseObject.getDatabaseType().equals("Step");
                        break;
                }

                if (bContinue) {
                    String text = databaseObjectTreeObject.toString();
                	try {
                		text = YamlConverter.toYaml(databaseObject.toXml(XMLUtils.createDom()));
					} catch (Exception e) {
						e.printStackTrace();
					}
                	
                	boolean bFound = false;
                	if (bRegExp) {
                		Matcher matcher = pattern.matcher(text);
                		bFound = matcher.find();
                	} else {
                    	text = bMatchCase ? text : text.toLowerCase();
                		bFound = text.indexOf(substring) != -1;
                	}
                	
                    if (bFound) { // Object found !!!
                    	//System.out.println(text);
                    	ConvertigoPlugin.getDefault().getProjectExplorerView().objectSelected(new CompositeEvent(databaseObject));
                    	vDatabaseObjects.remove(databaseObjectTreeObject);
                    	return;
                    }
                }
            }

    		TreeObject treeSelected = firstSelected == null ? ConvertigoPlugin.projectManager.currentProjectTreeObject : firstSelected;
        	MessageBox messageBox = new MessageBox(getShell(),SWT.YES | SWT.NO | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
			String message = "End of the search for "+ treeSelected.toString() +" object.\nDo you want to retry the search from the beginning?";
        	messageBox.setMessage(message);
        	int ret = messageBox.open();
        	if (ret == SWT.YES) {
        		ConvertigoPlugin.getDefault().getProjectExplorerView().setSelectedTreeObject(treeSelected);
        		vDatabaseObjects.clear();
        		getDatabaseObjects(null);
        	}
        	else {
        		return;
        	}
        }
    }
	
}
