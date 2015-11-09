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

import java.io.File;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
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
import com.twinsoft.convertigo.eclipse.dialogs.SchemaObjectsDialog;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.SchemaUtils;

public class SequenceImportFromXsdAction extends MyAbstractAction {

	public SequenceImportFromXsdAction() {
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
    			
            	FileDialog fileDialog = new FileDialog(shell, SWT.PRIMARY_MODAL | SWT.SAVE);
            	fileDialog.setText("Import schema file");
            	fileDialog.setFilterExtensions(new String[]{"*.xsd"});
            	fileDialog.setFilterNames(new String[]{"Schema files"});
            	fileDialog.setFilterPath(Engine.PROJECTS_PATH);
            	
            	String filePath = fileDialog.open();
            	if (filePath != null) {
            		filePath = filePath.replaceAll("\\\\", "/");
            		
            		XmlSchemaCollection collection = new XmlSchemaCollection();
            		collection.setBaseUri(filePath);
            		XmlSchema xmlSchema = SchemaUtils.loadSchema(new File(filePath), collection);
            		SchemaMeta.setCollection(xmlSchema, collection);
            		
            		SchemaObjectsDialog dlg = new SchemaObjectsDialog(shell, sequence, xmlSchema);
					if (dlg.open() == Window.OK) {
						if (dlg.result instanceof Throwable) {
							throw (Throwable)dlg.result;
						}
						else {
							Step step = (Step)dlg.result;
							if (step != null) {
								if (databaseObject instanceof Sequence) {
									sequence.addStep(step);
									sequence.hasChanged = true;
								}
								else {
									StepWithExpressions swe = (StepWithExpressions)databaseObject;
									swe.addStep(step);
									swe.hasChanged = true;
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
        	ConvertigoPlugin.logException(e, "Unable to import step from xsd!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
