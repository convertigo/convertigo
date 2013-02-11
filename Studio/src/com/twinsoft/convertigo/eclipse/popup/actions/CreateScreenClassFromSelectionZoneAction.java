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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

import com.twinsoft.convertigo.beans.common.FindString;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.AbstractConnectorComposite;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorPart;
import com.twinsoft.convertigo.eclipse.editors.connector.JavelinConnectorComposite;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ScreenClassTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.util.JavelinUtils;
import com.twinsoft.twinj.Javelin;

public class CreateScreenClassFromSelectionZoneAction extends MyAbstractAction {

	public CreateScreenClassFromSelectionZoneAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
        
		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			IWorkbenchPart wpart = getActivePart();
			if ((explorerView != null) && (wpart != null) && (wpart instanceof ConnectorEditor)) {
				ConnectorEditor connectorEditor = (ConnectorEditor)wpart;
				ConnectorEditorPart connectorEditorPart = connectorEditor.getConnectorEditorPart();
				AbstractConnectorComposite connectorComposite = connectorEditorPart.getConnectorComposite();
				if ((connectorComposite != null) && (connectorComposite instanceof JavelinConnectorComposite)) {
					Javelin javelin = ((JavelinConnectorComposite)connectorComposite).getJavelin();
					ScreenClass currentScreenClass = ((JavelinConnector) connectorEditorPart.getConnector()).getCurrentScreenClass();
					Engine.theApp.fireObjectDetected(new EngineEvent(currentScreenClass));
					
					ScreenClassTreeObject lastDetectedScreenClassTreeObject = explorerView.getLastDetectedScreenClassTreeObject();
					if (lastDetectedScreenClassTreeObject != null) {
			            Rectangle zone = javelin.getSelectionZone();
			            
			            String strZone = javelin.getString(zone.x, zone.y, zone.width);
			            
			            ScreenClass lastDetectedScreenClass = (ScreenClass)lastDetectedScreenClassTreeObject.getObject();
			            JavelinScreenClass screenClass = new JavelinScreenClass();
			            screenClass.priority = lastDetectedScreenClass.priority + 1;
			            screenClass.hasChanged = true;
			            screenClass.bNew = true;
			            
			            lastDetectedScreenClass.add(screenClass);
			            
			            FindString fs = new FindString();
			            fs.setString(strZone);
			            fs.setX(zone.x);
			            fs.setY(zone.y);
						fs.hasChanged = true;
						fs.bNew = true;
			            
			            // Determine whether there is the same attribute for each character
			            boolean isSameAttribute = true;
			            int attribute = javelin.getCharAttribute(zone.x, zone.y);
			            for (int i = 1 ; (i < zone.width) && isSameAttribute ; i++) {
			            	isSameAttribute = JavelinUtils.isSameAttribute(attribute, javelin.getCharAttribute(zone.x + i, zone.y));
			            }
			            fs.setAttribute(isSameAttribute ? attribute : -1);
			            
			            screenClass.addCriteria(fs);
			            
			            explorerView.reloadTreeObject(lastDetectedScreenClassTreeObject);
			            Engine.theApp.fireObjectDetected(new EngineEvent(screenClass));
			            
			            javelin.setSelectionZone(new Rectangle(0, 0, 0, 0));
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
