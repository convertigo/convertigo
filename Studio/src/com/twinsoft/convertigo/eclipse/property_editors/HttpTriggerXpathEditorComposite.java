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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.parsers.triggers.XpathTrigger;

public class HttpTriggerXpathEditorComposite extends AbstractHttpTriggerCustomEditorComposite{
	private Label xpath_label = null;
	private Text xpath_txt = null;
	
	public HttpTriggerXpathEditorComposite(HttpTriggerEditorComposite parent) {
		super(parent);
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		XpathTrigger trigger = (parent.getTrigger() instanceof XpathTrigger)?(XpathTrigger)parent.getTrigger():null;
        GridData gridData2 = new GridData();
        gridData2.horizontalAlignment = GridData.FILL;
        gridData2.grabExcessHorizontalSpace = true;
        gridData2.verticalAlignment = GridData.CENTER;
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
        xpath_label = new Label(this, SWT.NONE);
        xpath_label.setText("Waiting for Xpath");
        xpath_txt = new Text(this, SWT.BORDER);
        xpath_txt.setText( (trigger!=null)? trigger.getXPath():"");
        xpath_txt.setLayoutData(gridData2);
        xpath_txt.setSize(600, 50);
	}	
	
	public TriggerXMLizer getTriggerXMLizer(){
		return new TriggerXMLizer(new XpathTrigger(xpath_txt.getText(),parent.getTimeout()));
	}

	public String getHelp() {
		return "This synchronizer waits while the xpath is not found in the document dom.";
	}

}
