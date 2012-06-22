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
import org.eclipse.swt.widgets.Spinner;

import com.twinsoft.convertigo.engine.parsers.triggers.DocumentCompletedTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;

public class HttpTriggerDocumentCompletedEditorComposite extends AbstractHttpTriggerCustomEditorComposite {
	protected Spinner nbdoc_spin;
	protected Button check;

	public HttpTriggerDocumentCompletedEditorComposite(HttpTriggerEditorComposite parent) {
		super(parent);
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		DocumentCompletedTrigger trigger = (parent.getTrigger() instanceof DocumentCompletedTrigger) ? (DocumentCompletedTrigger) parent.getTrigger() : null;
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
        Label nbdoc_label = new Label(this, SWT.NONE);
        nbdoc_label.setText("Number of document completed");
        
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = GridData.CENTER;
        
        nbdoc_spin = new Spinner(this, SWT.BORDER);
        nbdoc_spin.setLayoutData(gridData);
        nbdoc_spin.setMinimum(1);
        nbdoc_spin.setSelection(trigger != null ? trigger.getCount() : 1);
        
        nbdoc_label = new Label(this, SWT.NONE);
        nbdoc_label.setText("Stop on alert");
        
        check = new Button(this, SWT.CHECK);
        check.setSelection(trigger != null ? trigger.isStopOnAlert() : false);
	}

	public TriggerXMLizer getTriggerXMLizer(){
		return new TriggerXMLizer(new DocumentCompletedTrigger(nbdoc_spin.getSelection(), parent.getTimeout(), check.getSelection()));
	}

	public String getHelp() {
		return "This synchronizer waits for a number of document completed.";
	}
}
