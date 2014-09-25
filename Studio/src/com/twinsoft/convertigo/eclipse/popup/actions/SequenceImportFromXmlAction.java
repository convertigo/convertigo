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

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.XmlStructureDialog;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SequenceImportFromXmlAction extends MyAbstractAction {

	public SequenceImportFromXmlAction() {
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
    			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)explorerView.getFirstSelectedTreeObject();
    			DatabaseObject databaseObject = databaseObjectTreeObject.getObject();
    			SequenceTreeObject sequenceTreeObject = (SequenceTreeObject) ((databaseObject instanceof Sequence) ? databaseObjectTreeObject:databaseObjectTreeObject.getParentDatabaseObjectTreeObject());
    			Sequence sequence = (databaseObject instanceof Sequence) ? (Sequence)databaseObject:((StepWithExpressions)databaseObject).getSequence();
    			
    			// Open a file dialog to search a XML file
    			FileDialog fileDialog = new FileDialog(shell, SWT.PRIMARY_MODAL | SWT.SAVE);
            	fileDialog.setText("Import XML file");
            	fileDialog.setFilterExtensions(new String[]{"*.xml"});
            	fileDialog.setFilterNames(new String[]{"XML"});
            	fileDialog.setFilterPath(Engine.PROJECTS_PATH);
    			
            	String filePath = fileDialog.open();

            	if (filePath != null) {
            		// Get XML content from the file
            		File xmlFile = new File(filePath);
            		Charset charset = XMLUtils.getEncoding(xmlFile);
            		String xmlContent = FileUtils.readFileToString(xmlFile, charset.name());
            		
            		// Open and add XML content to the dialog area
            		XmlStructureDialog dlg = new XmlStructureDialog(shell, sequence, xmlContent);

    				if (dlg.open() == Window.OK) {
    					if (dlg.result instanceof Throwable) {
    						throw (Throwable)dlg.result;
    					}
    					else {
    						Step step = (Step)dlg.result;
    						if (step != null) {
    							if (databaseObject instanceof Sequence) {
    								sequence.addStep(step);
    							}
    							else {
    								StepWithExpressions swe = (StepWithExpressions)databaseObject;
    								swe.addStep(step);
    							}
    							
    							sequence.hasChanged = true;
    							
    							// Reload sequence in tree without updating its schema for faster reload
    							ConvertigoPlugin.logDebug("Reload sequence: start");
    							explorerView.reloadTreeObject(sequenceTreeObject);
    							ConvertigoPlugin.logDebug("Reload sequence: end");
    							
    							// Select target dbo in tree
    							explorerView.objectSelected(new CompositeEvent(databaseObject));
    						}
    					}
    	        	}
            	}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to import step from xml!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
