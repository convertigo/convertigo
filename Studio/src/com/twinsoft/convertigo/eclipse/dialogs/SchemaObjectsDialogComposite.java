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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ProgressBar;

import com.twinsoft.convertigo.engine.util.GenericUtils;

public class SchemaObjectsDialogComposite extends MyAbstractDialogComposite {

	private XmlSchema xmlSchema = null;
	private java.util.List<QName> qnames = null;
	protected List list = null;
	public ProgressBar progressBar = null;
	public Label labelProgression = null;
	
	public SchemaObjectsDialogComposite(Composite parent, int style, Object parentObject, XmlSchema xmlSchema) {
		super(parent, style);
		this.xmlSchema = xmlSchema;
		
		initialize();

		fillList();
	}

	protected void initialize() {
		Label label0 = new Label (this, SWT.NONE);
		label0.setText ("Please choose an element to import into a sequence's step:");
		
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 200;
		list = new List(this, SWT.BORDER | SWT.V_SCROLL);
		list.setLayoutData (data);

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
	
	private void fillList() {
		qnames = new ArrayList<QName>();//xsd.getSchemaElementNames();
		
		Iterator<QName> it = GenericUtils.cast(xmlSchema.getElements().getNames());
		while (it.hasNext()) {
			qnames.add(it.next());
		}
			
		Collections.sort(qnames, new Comparator<QName>(){
			public int compare(QName o1, QName o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
		if (qnames != null) {
			for (QName qname: qnames) {
				list.add(qname.toString());
			}
		}
	}
	
	public Object getValue(String name) {
		if (qnames != null)
			return qnames.get(list.getSelectionIndices()[0]);
		return null;
	}
}
