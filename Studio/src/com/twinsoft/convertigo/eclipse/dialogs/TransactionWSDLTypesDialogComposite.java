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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class TransactionWSDLTypesDialogComposite extends MyAbstractDialogComposite {

	private HtmlTransaction transaction = null;
	private String wsdlType = null;
	protected List list = null;
	private Map<String, String> items = null;
	
	public TransactionWSDLTypesDialogComposite(Composite parent, int style, Object parentObject) {
		super(parent, style);
		this.transaction = (HtmlTransaction)parentObject;
		
		initialize();
		
		try {
			wsdlType = transaction.generateWsdlType(null);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unexpected exception");
		}
		
		fillList();
	}

	protected void initialize() {
		Label label0 = new Label (this, SWT.NONE);
		label0.setText ("Please choose type(s) which are returned by transaction response:");
		
		list = new List(this, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 200;
		list.setLayoutData (data);

		GridLayout gridLayout = new GridLayout();
		setLayout(gridLayout);
		setSize(new Point(408, 251));
	}
	
	private void fillList() {
		if (wsdlType != null) {
			String wsdlType = this.wsdlType;
			String prefix = transaction.getXsdTypePrefix();
			String transactionName = StringUtils.normalize(prefix + transaction.getName(), true);
			
			String responseSchema = "<xsd:complexType name=\""+ transactionName + "Response\">";
			int i = wsdlType.indexOf(responseSchema);
			if (i != -1) {
				int j = wsdlType.indexOf("<xsd:sequence>",i);
				if (j != -1) {
					int k = wsdlType.indexOf("</xsd:sequence>", j);
					if (k != -1) {
						wsdlType = wsdlType.substring(j+"<xsd:sequence>".length(), k);
						String[] types = wsdlType.split("\\n");
						int len = types.length;
						if (len > 0) {
							items = new HashMap<String, String>();
							for (int z = 0; z<len; z++) {
								String type = types[z];
								String s = type.replaceAll("\\r", "");
								s = s.replaceAll("minOccurs=\\\"\\w+\\\"\\s", "");
								s = s.replaceAll("maxOccurs=\\\"\\w+\\\"\\s", "");
								if (!s.equals("") && !s.trim().equals("")) {
									list.add(s);
									// TODO: case of doublet -> alert
									items.put(s, type);
								}
							}
						}
					}
				}
			}
		}
	}
	
	protected void generateWSDLType() {
		String[] types = list.getSelection();
		
		String 	schema = null;
		String prefix = transaction.getXsdTypePrefix();
		String transactionName = StringUtils.normalize(prefix + transaction.getName(), true);
		
		schema = "<xsd:complexType name=\""+ transactionName + "Response\">\n";
		schema += "<xsd:sequence>\n";
		for (int i=0; i<types.length; i++)
			schema += items.get(types[i]) + "\n";
		schema += "</xsd:sequence>\n";
		schema += "</xsd:complexType>\n";
		
		if (!transaction.isDefault)
			wsdlType = schema;
	}
	
	public String getWsdlType() {
		return wsdlType;
	}
	
	public Object getValue(String name) {
		return getWsdlType();
	}
}
