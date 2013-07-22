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
import java.util.Hashtable;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XSDUtils;
import com.twinsoft.convertigo.engine.util.XSDUtils.XSD;
import com.twinsoft.convertigo.engine.util.XSDUtils.XmlGenerationDescription;

public class TransactionXSDTypesDialogComposite extends MyAbstractDialogComposite {

	private HtmlTransaction transaction = null;
	private String xsdTypes = null;
	protected List list = null;
	private Hashtable<String, String> items = null;
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
							items = new Hashtable<String, String>();
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
		String projectName = transaction.getProject().getName();
		String xsdURI = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.xsd";
		try {
			String prefix = transaction.getXsdTypePrefix();
			String transactionName = StringUtils.normalize(prefix + transaction.getName(), true);

			XSD xsd = XSDUtils.getXSD(xsdURI);
			XmlGenerationDescription xmlDescription = xsd.getXmlGenerationDescription();
			xmlDescription.setOutputOccurences(false);
			xmlDescription.setOutputSchemaTypeCData(true);
			xmlDescription.setOutputOccursAttribute(false);
			xmlDescription.setOutputElementWithNS(false);

			Document xsdDom = xsd.generateTypeXmlStructure(projectName+"_ns", transactionName +"ResponseData");
			NodeList children = xsdDom.getDocumentElement().getChildNodes();
			Element element;
			Node node;
			for (int i=0;i<children.getLength();i++) {
				node = children.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					element = findChildElementByTagName((Element)node,"schema-type");
					if (element != null) {
						Node cdata = XMLUtils.findChildNode(element, Node.CDATA_SECTION_NODE);
						if (cdata != null) {
							String value = cdata.getNodeValue().trim();
							value = value.replaceAll("minOccurs=\\\"\\w+\\\"\\s", "");
							value = value.replaceAll("maxOccurs=\\\"\\w+\\\"\\s", "");
							value = value.replaceAll(" />", "/>");
							value = value.trim();
							types.add(value);
						}
					}
				}
			}
		}
		catch (Exception e) {
		}
		return types;
	}
	
	private Element findChildElementByTagName(Element element, String tagName) {
		Element elt = null;
		Node node;
		NodeList children = element.getChildNodes();
		for (int i=0;i<children.getLength();i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				elt = (Element)node;
				if (elt.getTagName().equals(tagName))
					break;
			}
		}
		return elt;
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
