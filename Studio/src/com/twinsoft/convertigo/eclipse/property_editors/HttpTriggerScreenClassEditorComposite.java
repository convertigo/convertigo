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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.screenclasses.HtmlScreenClass;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.parsers.triggers.ScreenClassTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;

public class HttpTriggerScreenClassEditorComposite extends AbstractHttpTriggerCustomEditorComposite {
	private Label scrClass_label = null;
	private List scrClass_list = null;

	public HttpTriggerScreenClassEditorComposite(HttpTriggerEditorComposite parent) {
		super(parent);
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		ScreenClassTrigger trigger = (parent.getTrigger() instanceof ScreenClassTrigger) ? (ScreenClassTrigger) parent
				.getTrigger() : null;

		scrClass_label = new Label(this, SWT.NONE);
		scrClass_label.setText("Screen classes");
		scrClass_list = new List(this, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);

		java.util.List<HtmlScreenClass> scList = getScreenClassList();

		java.util.List<String> items = new ArrayList<String>();

		for (ScreenClass sc : scList) {
			String scName = sc.getName();
			scrClass_list.add(scName);
			if (trigger != null) {
				if (trigger.getScreenClasses().contains(scName)) {
					items.add(scName);
				}
			}
		}

		scrClass_list.setSelection(items.toArray(new String[items.size()]));
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = GridData.FILL;
		gridData2.verticalAlignment = GridData.FILL;
		gridData2.grabExcessHorizontalSpace = true;
		scrClass_list.setLayoutData(gridData2);
		scrClass_list.setSize(400, 250);
	}

	public TriggerXMLizer getTriggerXMLizer() {
		String items[] = scrClass_list.getSelection();
		XMLVector<String> list = new XMLVector<String>();
		for (int i = 0; i < items.length; i++) {
			list.add(items[i]);
		}
		return new TriggerXMLizer(new ScreenClassTrigger(list, parent.getTimeout()));
	}

	public String getHelp() {
		return "This synchronizer waits for one of the selected ScreenClasses defined here to be detected.\n"
				+ "You can select multiple screen classes by holding the Ctrl key while selecting the screen "
				+ "class with the mouse.";
	}

	private java.util.List<HtmlScreenClass> getScreenClassList() {

		java.util.List<HtmlScreenClass> screenClasses = null;

		TreeObject treeConnector = parent.cellEditor.databaseObjectTreeObject
				.getParentDatabaseObjectTreeObject().getConnectorTreeObject();
		HtmlConnector connector = (HtmlConnector) treeConnector.getObject();
		screenClasses = connector.getAllScreenClasses();

		return screenClasses;
	}
}
