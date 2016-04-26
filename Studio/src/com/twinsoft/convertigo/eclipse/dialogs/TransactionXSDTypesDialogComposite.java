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
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;

public class TransactionXSDTypesDialogComposite extends MyAbstractDialogComposite {

	private HtmlTransaction transaction = null;
	private String xsdTypes = null;
	protected List list = null;
	private Map<String, String> items = null;
	private ArrayList<String> currenTypes = null;
	
	public TransactionXSDTypesDialogComposite(Composite parent, int style, Object parentObject) {
		super(parent, style);
		this.transaction = (HtmlTransaction)parentObject;
		
		initialize();
		
		try {
			currenTypes = getTypesListFromXsd();
			xsdTypes = transaction.generateXsdTypes(null, false);
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
		if (xsdTypes != null) {
			String sTypes = this.xsdTypes;
			
			String prefix = transaction.getXsdTypePrefix();
			String responseSchema = "<xsd:complexType name=\""+ prefix + transaction.getName() + "ResponseData\">";
			int i = sTypes.indexOf(responseSchema);
			if (i != -1) {
				int j = sTypes.indexOf("<xsd:sequence>",i);
				if (j != -1) {
					int k = sTypes.indexOf("</xsd:sequence>", j);
					if (k != -1) {
						sTypes = sTypes.substring(j+"<xsd:sequence>".length(), k);
						String[] types = sTypes.split("\\n");
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
									if (currenTypes.contains(s))
										list.select(list.indexOf(s));
								}
							}
						}
					}
				}
			}
		}
	}
	
	private ArrayList<String> getTypesListFromXsd() {
		ArrayList<String> types = new ArrayList<String>();
		try {
			String tns = transaction.getProject().getTargetNamespace();
			String projectName = transaction.getProject().getName();
			XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(projectName);
			QName responseTypeQName = new QName(tns, transaction.getXsdResponseTypeName());
			XmlSchemaComplexType xmlSchemaType = (XmlSchemaComplexType) schema.getTypeByName(responseTypeQName);
			XmlSchemaParticle xmlSchemaParticle = xmlSchemaType.getParticle();
			if (xmlSchemaParticle != null && xmlSchemaParticle instanceof XmlSchemaSequence) {
				XmlSchemaObjectCollection xmlSchemaObjectCollection = ((XmlSchemaSequence)xmlSchemaParticle).getItems();
				for (int i=0;i<xmlSchemaObjectCollection.getCount();i++) {
					XmlSchemaObject xso = xmlSchemaObjectCollection.getItem(i);
					if (xso instanceof XmlSchemaElement) {
						XmlSchemaElement xmlSchemaElement = (XmlSchemaElement)xso;
						String elName = xmlSchemaElement.getName();
						QName elType = xmlSchemaElement.getSchemaTypeName();
						String value = "<xsd:element name=\""+elName+"\" type=\""+elType.getPrefix()+":"+elType.getLocalPart()+"\"/>";
						types.add(value);
					}
				}
			}
		}
		catch (Exception e) {
		}
		return types;
	}
	
	protected void generateXSDTypes() {
		String[] types = list.getSelection();
		
		String prefix = transaction.getXsdTypePrefix();
		
		String sTypes = this.xsdTypes;
		int i = sTypes.indexOf("<xsd:complexType name=\""+ prefix + transaction.getName() + "ResponseData\">");
		if (i != -1) {
			int j = sTypes.indexOf("<xsd:sequence>",i);
			if (j != -1) {
				sTypes = sTypes.substring(0, j);
				sTypes += "<xsd:sequence>\n";
				for (int k=0; k<types.length; k++)
					sTypes += (String)items.get((String)types[k]) + "\n";
				sTypes += "</xsd:sequence>\n";
				sTypes += "</xsd:complexType>\n";
			}
		}
		xsdTypes = sTypes;
	}
	
	public String getXsdTypes() {
		return xsdTypes;
	}
	
	public Object getValue(String name) {
		return getXsdTypes();
	}
}
