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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.IOException;
import java.util.Hashtable;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.MashupDataViewConfiguration;
import com.twinsoft.convertigo.eclipse.MashupInformation;
import com.twinsoft.convertigo.eclipse.dialogs.MashupDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

public class DreamFaceAddDataViewAction extends DreamFaceAbstractAction {

	protected static String ERROR_DATAVIEW_EXIST = "dataview can not be added while it already exist!";
	
	public DreamFaceAddDataViewAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			RequestableObject requestable = (RequestableObject)explorerView.getFirstSelectedDatabaseObject();
    			
    			Connector connector = requestable.getConnector();
    			
    			String connectorName = (connector != null) ? connector.getName():"";
    			String projectName = requestable.getProject().getName();
    			String requestableName = requestable.getName();
    			
    			String key = projectName + "." + connectorName + "." + requestableName;
    			MashupInformation mashupInformation = getMashupInformation(projectName);
    			MashupDataViewConfiguration mdc = mashupInformation.getDataViewConfiguration(key);
    			if (mdc == null) {
    				mdc = new MashupDataViewConfiguration();
    			}
    			
	        	MashupDialog mashupDialog = new MashupDialog(shell, mdc);
	        	mashupDialog.open();
	    		if (mashupDialog.getReturnCode() != Window.CANCEL) {
	    			
	    			String dataview = mashupDialog.dataview;
	    			if ((dataview != null) && (!dataview.equals(""))) {
	    				if (securedLogin()) {
			    			try {
			    				// Add dataviews to Mashup Composer
				    			String dfRequest = addRequestDataView(requestable, dataview);
				    			if (!dfRequest.equals("")) {
				    	        	MessageBox messageBox = new MessageBox(shell, SWT.YES | SWT.APPLICATION_MODAL);
				    	        	messageBox.setMessage("For requestable request: " + dfRequest);
				    	        	messageBox.open();
				    			}

				    			String dfResponse = addResponseDataView(requestable, dataview);
				    			if (!dfResponse.equals("")) {
				    	        	MessageBox messageBox = new MessageBox(shell, SWT.YES | SWT.APPLICATION_MODAL);
				    	        	messageBox.setMessage("For requestable response: " + dfResponse);
				    	        	messageBox.open();
				    			}
				    			
				    			if (!dfResponse.equals(ERROR_DATAVIEW_EXIST)) {
					    			// Store dataview names
					    			if ((existClass(dataview + requestSufix)) || (existClass(dataview)))
					    				mdc.addDataView(dataview);
				    			}
				    			
			    		        mashupInformation.setDataViewConfiguration(key, mdc);
			    		        storeMashupInformation(mashupInformation, projectName);
		    				}
		    				catch (DreamFaceInvalidTemplateException ite) {
			    				mdc.removeDataView(dataview);
			    		        mashupInformation.setDataViewConfiguration(key, mdc);
			    		        storeMashupInformation(mashupInformation, projectName);
			    		        
			    		        deleteResponseTemplate(dataview);
			    		        throw ite;
		    				}
		    				finally {
		    					securedLogout();
		    				}
	    				}
	    			}
	    		}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to add dataview(s) to DreamFace!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
	private String addRequestDataView(RequestableObject requestable, String dataview) throws HttpException, IOException {
		if (!(requestable instanceof IVariableContainer))
			return "";
		
		if (((IVariableContainer)requestable).numberOfVariables() == 0)
			return "";
		
		String dataviewName = dataview + requestSufix;
        
		Hashtable<String, String> fields = getRequestFields(requestable, dataviewName);
		
		return addDataView(dataviewName,fields);
	}
	
	
	private String addResponseDataView(RequestableObject requestable, String dataview) throws HttpException, IOException, ParserConfigurationException, SAXException, DreamFaceInvalidTemplateException {
		Hashtable<String, String> fields = getResponseFields(ALL_DATAVIEW_TYPE, requestable, dataview);
        return addDataView(dataview,fields);
        
	}
	
	private String addDataView(String dataview, Hashtable<String, String> fields) throws HttpException, IOException {
        if (existClass(dataview))
        	return ERROR_DATAVIEW_EXIST;
        else
        	return addClass(fields);		
	}
}
