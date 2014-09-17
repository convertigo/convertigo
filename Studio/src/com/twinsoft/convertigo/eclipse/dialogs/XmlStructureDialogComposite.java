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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

public class XmlStructureDialogComposite extends MyAbstractDialogComposite {

	public ProgressBar progressBar = null;
	public Label labelProgression = null;
	public Text xml = null;
	
	public XmlStructureDialogComposite(Composite parent, int style, Object parentObject) {
		super(parent, style);
		
		initialize();
	}
	
	public XmlStructureDialogComposite(Composite parent, int style, Object parentObject, String xmlContent) {
		super(parent, style);
		
		initialize();
		xml.setText(xmlContent);
	}

	protected void initialize() {
		Label label0 = new Label (this, SWT.NONE);
		label0.setText ("Please enter the XML structure to import into a sequence's step:");
		
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 200;
		xml = new Text(this, SWT.BORDER | SWT.V_SCROLL);
		xml.setLayoutData (data);

        GridData gridData2 = new GridData();
		gridData2.horizontalSpan = 2;
		gridData2.verticalAlignment = GridData.CENTER;
		gridData2.horizontalAlignment = GridData.FILL;
		labelProgression = new Label(this, SWT.NONE);
		labelProgression.setText("Progression");
		labelProgression.setLayoutData(gridData2);
		
        GridData gridData4 = new GridData();
		gridData4.horizontalSpan = 2;
		gridData4.verticalAlignment = GridData.CENTER;
		gridData4.horizontalAlignment = GridData.FILL;
        progressBar = new ProgressBar(this, SWT.NONE);
        //progressBar.setBounds(new Rectangle(16, 349, 571, 17));
        progressBar.setLayoutData(gridData4);
        
		GridLayout gridLayout = new GridLayout();
		setLayout(gridLayout);
		setSize(new Point(408, 251));
	}
	
	public Object getValue(String name) {
		return xml.getText();
	}
}
