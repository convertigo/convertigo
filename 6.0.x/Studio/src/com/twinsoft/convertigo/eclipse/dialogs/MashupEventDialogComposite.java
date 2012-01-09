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

import java.io.File;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import com.twinsoft.convertigo.eclipse.MashupDataViewConfiguration;
import com.twinsoft.convertigo.eclipse.MashupEventConfiguration;
import com.twinsoft.convertigo.eclipse.popup.actions.DreamFaceAbstractAction;
import com.twinsoft.convertigo.engine.Engine;

public class MashupEventDialogComposite extends MyAbstractDialogComposite {

	private Label description = null;
	private Label label = null;
	public List list = null;
	
	public MashupEventDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	public Object getValue(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void initialize() {
        GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		this.setLayout(gridLayout);
		
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.BEGINNING;
		data.grabExcessHorizontalSpace = true;
		description = new Label(this, SWT.NONE);
		description.setText ("This will add an Event to the selected Dataview. 'Events' are sent by\nDataviews and wired to 'Actions' using Mashup Composer\n\n");
		description.setLayoutData(data);
		
        label = new Label(this, SWT.NONE);
        label.setText("Allowed dataviews :");

        createList();
	}

	private void createList() {
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = GridData.FILL;
		gridData1.verticalAlignment = GridData.FILL;
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.grabExcessVerticalSpace = true;
		list = new List(this, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		list.setLayoutData(gridData1);
	}
	
	public void fillList(int type, MashupDataViewConfiguration mdc, MashupEventConfiguration mec) {
		if (mec != null) {
			File dir = new File(Engine.USER_WORKSPACE_PATH + DreamFaceAbstractAction.MASHUP_TEMPLATE_SUBPATH);
		    String[] children = dir.list();
		    for (int i=0; i<children.length; i++) {
		    	String item = children[i];
		    	if (item.endsWith(".xml")) {
		    		String dataview = item.substring(0, item.length()-4);
		    		if (!mdc.getDataViews().contains(dataview)) {
		    			if (mec.getDataViews().contains(dataview)) {
		    				if ((type == MashupEventDialog.TYPE_UPDATE) || (type == MashupEventDialog.TYPE_DELETE))
		    					list.add(dataview);
		    			}
		    			else {
		    				if (type == MashupEventDialog.TYPE_ADD)
		    					list.add(dataview);
		    			}
		    		}
		    	}
		    }
		    if (children.length > 0)
		    	list.select(0);
		    
		    if (type == MashupEventDialog.TYPE_DELETE) {
				description.setText ("This will remove the Event from the selected Dataview.\n\n");
		    }
		}
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
