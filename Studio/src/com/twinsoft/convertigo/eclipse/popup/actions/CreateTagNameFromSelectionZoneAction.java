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

import java.awt.Rectangle;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

import com.twinsoft.convertigo.beans.common.TagName;
import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorPart;
import com.twinsoft.convertigo.eclipse.editors.connector.JavelinConnectorComposite;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ScreenClassTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.twinj.Javelin;

public class CreateTagNameFromSelectionZoneAction extends MyAbstractAction {

	public CreateTagNameFromSelectionZoneAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
        
		try {
			final ProjectExplorerView explorerView = getProjectExplorerView();
			IWorkbenchPart wpart = getActivePart();
			if ((explorerView != null) && (wpart != null) && (wpart instanceof ConnectorEditor)) {
				ConnectorEditor connectorEditor = (ConnectorEditor)wpart;
				ConnectorEditorPart connectorEditorPart = connectorEditor.getConnectorEditorPart();
				AbstractConnectorComposite connectorComposite = connectorEditorPart.getConnectorComposite();
				if ((connectorComposite != null) && (connectorComposite instanceof JavelinConnectorComposite)) {
					final Javelin javelin = ((JavelinConnectorComposite)connectorComposite).getJavelin();
					ScreenClass currentScreenClass = ((JavelinConnector) connectorEditorPart.getConnector()).getCurrentScreenClass();
					Engine.theApp.fireObjectDetected(new EngineEvent(currentScreenClass));
					
					final ScreenClassTreeObject lastDetectedScreenClassTreeObject = explorerView.getLastDetectedScreenClassTreeObject();
					if (lastDetectedScreenClassTreeObject != null) {
						final ScreenClass lastDetectedScreenClass = (ScreenClass)lastDetectedScreenClassTreeObject.getObject();
						final TagName tagName = new TagName();
						
						final InputDialog dlg = new InputDialog(shell,"New TagName", "Please enter a tag name :", "_configure_a_tag_name_", null);
				        if (dlg.open() == Window.OK) {
				        	display.asyncExec(new Runnable() {
				    			public void run() {
				    				try {
							        	String name = dlg.getValue();
							        	
							        	Rectangle zone = javelin.getSelectionZone();
							        	
						    			tagName.setTagName(StringUtils.normalize(name));
							            tagName.setSelectionScreenZone(new XMLRectangle(zone.x, zone.y, zone.width, zone.height));
							            tagName.setSelectionAttribute(javelin.getCharAttribute(zone.x, zone.y));
							            tagName.setSelectionType("");
							            tagName.hasChanged = true;
							            tagName.bNew = true;
							            
						    			lastDetectedScreenClass.addExtractionRule(tagName);
							            
							            explorerView.reloadTreeObject(lastDetectedScreenClassTreeObject);
							            
				    				} catch (Exception e) {
				    					ConvertigoPlugin.logException(e, "Unable to create screen class from selection zone!");
				    				}
				    				
						            javelin.setSelectionZone(new Rectangle(0, 0, 0, 0));
				    			}
				        	});
				        }
				        else {
				        	javelin.setSelectionZone(new Rectangle(0, 0, 0, 0));
				        }
					}
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to create screen class from selection zone!");
		}
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
