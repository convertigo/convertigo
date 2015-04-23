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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.CouchVariablesDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;

public class CouchAddVariables extends MyAbstractAction {
	protected String databaseObjectClassName = null;
	
	public CouchAddVariables() {
		super();
	}
	
	public CouchAddVariables(String databaseObjectClassName) {
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
    		AbstractCouchDbTransaction databaseObject = null;
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			parentTreeObject = explorerView.getFirstSelectedTreeObject();
    			
    			if (parentTreeObject.getObject() instanceof AbstractCouchDbTransaction) {
    				databaseObject = (AbstractCouchDbTransaction) parentTreeObject.getObject();
    				
    				PropertyDescriptor props[] = CachedIntrospector.getBeanInfo(databaseObject).getPropertyDescriptors();
    				props = cleanProps(databaseObject, props);
    				
    				if (props.length > 0) {
        				CouchVariablesDialog couchVariablesDialog = new CouchVariablesDialog(shell, databaseObject, props);
    					couchVariablesDialog.open();
    	                explorerView.reloadTreeObject(parentTreeObject);
    				} else {
    					MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK);
    			        messageBox.setMessage("No parameters are available for this transaction.");
    			        messageBox.setText("No availables parameters");
    			        messageBox.open();
    				} 
    				
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

	private PropertyDescriptor[] cleanProps(AbstractCouchDbTransaction couchDbTransaction, PropertyDescriptor[] props) {
		List<PropertyDescriptor> cleanProps = new ArrayList<PropertyDescriptor>();
		
		List<RequestableVariable> vars = couchDbTransaction.getVariablesList();
		for (PropertyDescriptor prop: props) {
			String propName = prop.getName();
			boolean find = false; int i = 0;
			if (propName.startsWith("p_") || propName.startsWith("q_")) {
				while ( !find && i < vars.size()) {
					String varName = vars.get(i).getName();
					if (varName.startsWith(CouchParam.prefix)) {
						if (varName.equals(CouchParam.prefix + propName.substring(2))) {
							find = true;
						}
					}
					++i;
				}
				if (!find) {
					cleanProps.add(prop);
				}
			}
		}
	
		PropertyDescriptor[] array = new PropertyDescriptor[cleanProps.size()];
		cleanProps.toArray(array);
		
		return array;
	}	
}
