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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.parsers.triggers.WaitTimeTrigger;

public class HttpTriggerWaitTimeEditorComposite extends AbstractHttpTriggerCustomEditorComposite {
	private Button check = null; 
	
	public HttpTriggerWaitTimeEditorComposite(HttpTriggerEditorComposite parent) {
		super(parent);
		initialize();
	}
	
	private void initialize() {
		WaitTimeTrigger trigger = (parent.getTrigger() instanceof WaitTimeTrigger) ? (WaitTimeTrigger) parent.getTrigger() : null;
        GridData gridData2 = new GridData();
        gridData2.horizontalAlignment = GridData.FILL;
        gridData2.grabExcessHorizontalSpace = true;
        gridData2.verticalAlignment = GridData.CENTER;
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
        Label label = new Label(this, SWT.CHECK);
        label.setText("Check this to force detection of any DOM changes\nwhile waiting. (Can cause heavy CPU load)");
        check = new Button(this, SWT.CHECK);
        check.setSelection((trigger != null) ? trigger.isDoDirty() : false);
	}	

	public TriggerXMLizer getTriggerXMLizer(){
		return new TriggerXMLizer(new WaitTimeTrigger(parent.getTimeout(), isDoDirty()));
	}

	public String getHelp() {
		return "This synchronizer only waits the timeout.";
	}
	
	protected boolean isDoDirty() {
		return check.getSelection();
	}
}