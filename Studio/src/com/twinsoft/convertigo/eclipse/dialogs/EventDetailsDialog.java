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
 * $URL: http://sourceus/svn/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/dialogs/ProjectDeployDialog.java $
 * $Author: jibrilk $
 * $Revision: 29098 $
 * $Date: 2011-11-29 09:09:45 +0100 (mar., 29 nov. 2011) $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.apache.log4j.Level;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.views.loggers.LogLine;

public class EventDetailsDialog extends MyAbstractDialog {
	
	private LogLine logLine = null;
	private Label logTime = null;
	private Label logLevel = null;
	private Label logCategory = null;
	private Label logThread = null;
	private Text textMessage = null;
	//private Text textExtra = null;
	private Text textClientIp, textConnector, textContextId, textProject, textTransaction, textUID, textUser, textSequence, textClientHostName, textUUID;
	
	public EventDetailsDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, LogLine logLine) {
		super(parentShell, dialogAreaClass, dialogTitle, 800, 450);
		this.logLine = logLine;
	}
	
	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar =  super.createButtonBar(parent);
		return buttonBar;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		dialogComposite = (EventDetailsDialogComposite)dialogComposite;
		logTime = ((EventDetailsDialogComposite)dialogComposite).logTime;
		logLevel = ((EventDetailsDialogComposite)dialogComposite).logLevel;
		logCategory = ((EventDetailsDialogComposite)dialogComposite).logCategory;
		logThread = ((EventDetailsDialogComposite)dialogComposite).logThread;
		textMessage = ((EventDetailsDialogComposite)dialogComposite).textMessage;
		//textExtra = ((EventDetailsDialogComposite)dialogComposite).textExtra;
		textClientIp = ((EventDetailsDialogComposite)dialogComposite).textClientIp;
		textConnector = ((EventDetailsDialogComposite)dialogComposite).textConnector;
		textContextId = ((EventDetailsDialogComposite)dialogComposite).textContextId;
		textProject = ((EventDetailsDialogComposite)dialogComposite).textProject;
		textTransaction = ((EventDetailsDialogComposite)dialogComposite).textTransaction;
		textUID = ((EventDetailsDialogComposite)dialogComposite).textUID;
		textUser = ((EventDetailsDialogComposite)dialogComposite).textUser;
		textSequence = ((EventDetailsDialogComposite)dialogComposite).textSequence;
		textClientHostName = ((EventDetailsDialogComposite)dialogComposite).textClientHostName;
		textUUID = ((EventDetailsDialogComposite)dialogComposite).textUUID;
		
		
		String level = logLine.getLevel();
		Color color = null;
		if (level.equals(Level.ERROR.toString())) {
			color = new Color(Display.getCurrent(), 255, 158, 147);
		} else if (level.equals(Level.INFO.toString())) {
			color = new Color(Display.getCurrent(), 225, 242, 228);
		} else if (level.equals(Level.DEBUG.toString())) {
			color = new Color(Display.getCurrent(), 249, 249, 177);
		} else if (level.equals(Level.WARN.toString())) {
			color = new Color(Display.getCurrent(), 242, 196, 208);
		}
		
		logTime.setText(logLine.getTime());
		logLevel.setText(logLine.getLevel());
		logLevel.setBackground(color);
		logCategory.setText(logLine.getCategory());
		logThread.setText(logLine.getThread());
		textMessage.setText(logLine.getFullMessage());
		String txt = logLine.getClientIP(); 
		if(txt != null) {
			textClientIp.setText(txt);
		}
		txt = logLine.getConnector(); 
		if(txt != null) {
			textConnector.setText(txt);
		}
		txt = logLine.getContextID(); 
		if(txt != null) {
			textContextId.setText(txt);
		}
		txt = logLine.getProject(); 
		if(txt != null) {
			textProject.setText(txt);
		}
		txt = logLine.getTransaction(); 
		if(txt != null) {
			textTransaction.setText(txt);
		}
		txt = logLine.getUID(); 
		if(txt != null) {
			textUID.setText(txt);
		}
		txt = logLine.getUser(); 
		if(txt != null) {
			textUser.setText(txt);
		}
		txt = logLine.getSequence(); 
		if(txt != null) {
			textSequence.setText(txt);
		}
		txt = logLine.getClientHostName(); 
		if(txt != null) {
			textClientHostName.setText(txt);
		}
		txt = logLine.getUUID(); 
		if(txt != null) {
			textUUID.setText(txt);
		}
		
		//textExtra.setText(logLine.getFullExtra());

		return composite;
	}
	
	@Override
	protected void okPressed() {
		super.okPressed();
	}
}
