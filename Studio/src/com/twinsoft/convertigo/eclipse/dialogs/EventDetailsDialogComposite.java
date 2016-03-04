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
 * $URL: http://sourceus/svn/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/dialogs/ProjectDeployDialogComposite.java $
 * $Author: jibrilk $
 * $Revision: 29132 $
 * $Date: 2011-11-30 11:00:16 +0100 (mer., 30 nov. 2011) $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class EventDetailsDialogComposite extends MyAbstractDialogComposite {

	public Label labelTime = null;
	public Label labelLevel = null;
	public Label labelCategory = null;
	public Label labelThread = null;
	
	public Label logTime = null;
	public Label logLevel = null;
	public Label logCategory = null;
	public Label logThread = null;
	
	public Text textMessage = null;
	//public Text textExtra = null;
	public Text textClientIp, textConnector, textContextId, textProject, textTransaction, textUID, textUser, textSequence, textClientHostName = null, textUUID;
	
	public EventDetailsDialogComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	/**
	 * This method initializes this
	 * 
	 */
	protected void initialize() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		
        labelTime = new Label(this, SWT.NONE);
        labelTime.setText("Time:");
        logTime = new Label(this, SWT.NONE);
        
        labelLevel = new Label(this, SWT.NONE);
        labelLevel.setText("Level:");
        logLevel = new Label(this, SWT.NONE);
        
        labelCategory = new Label(this, SWT.NONE);
        labelCategory.setText("Category:");
        logCategory = new Label(this, SWT.NONE);
        
        CTabFolder tabFolder = new CTabFolder(this, SWT.BORDER);
        
        CTabItem item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("Message");
		textMessage = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textMessage);
		
		//item = new CTabItem (tabFolder, SWT.NONE);
		//item.setText ("Extra");
		//textExtra = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		//item.setControl(textExtra);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("ClientIP");
		textClientIp = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textClientIp);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("Connector");
		textConnector = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textConnector);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("ContextID");
		textContextId = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textContextId);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("Project");
		textProject = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textProject);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("Transaction");
		textTransaction = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textTransaction);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("UID");
		textUID = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textUID);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("User");
		textUser = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textUser);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("Sequence");
		textSequence = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textSequence);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("ClientHostName");
		textClientHostName = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textClientHostName);
		
		item = new CTabItem (tabFolder, SWT.NONE);
		item.setText ("UUID");
		textUUID = new Text(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		item.setControl(textUUID);
		
		labelThread = new Label(this, SWT.NONE);
        labelThread.setText("Thread:");
        logThread = new Label(this, SWT.NONE);
        
        GridData gridData = new GridData();
        gridData.verticalIndent = 5;
        labelThread.setLayoutData(gridData);
        
        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalIndent = 5;
        logThread.setLayoutData(gridData);
        
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = GridData.FILL;
        gridData.verticalIndent = 5;
        tabFolder.setLayoutData(gridData);
	}

	public Object getValue(String name) {
		// TODO Auto-generated method stub
		return null;
	}	
}  //  @jve:decl-index=0:visual-constraint="10,10"
