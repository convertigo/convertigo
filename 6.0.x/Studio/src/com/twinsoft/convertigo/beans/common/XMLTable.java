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

package com.twinsoft.convertigo.beans.common;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.core.ITablesProperty;
import com.twinsoft.convertigo.engine.Engine;

public class XMLTable extends AbstractXMLReferer implements ITablesProperty {

	private static final long serialVersionUID = 8979406027000414349L;
	
	private XMLVector<XMLVector<Object>> description = null;
	
	private String tagName = null;
	
	private boolean accumulateDataInSameTable = false;

	private boolean flipTable = false;

	public XMLTable() {
		super();
		tagName = "XMLTable";
		description = new XMLVector<XMLVector<Object>>();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		XMLTable xmlTable = (XMLTable)super.clone();
		return xmlTable;
	}
	
	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
	public XMLVector<XMLVector<Object>> getDescription() {
		return description;
	}

	public void setDescription(XMLVector<XMLVector<Object>> description) {
		this.description = description;
	}
	
	public XMLTableRow getRow(int index) {
		return new XMLTableRow(description.get(index));
	}
	
	@Override
	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		int length;
		boolean appendTable;
		
		Element doc = outputDom.getDocumentElement();
		
		length = nodeList.getLength();
		for (int i=0; i< length && isRequestedObjectRunning(); i++) {			
			Node tableNode = nodeList.item(i);
			String tableTagName = (tagName.equals("") ? name:tagName);
			Element table;
			
			if (accumulateDataInSameTable // if we want to accumulate data in same table
				&& outputDom.getElementsByTagName(tableTagName).getLength() > 0) { // and if an element with the same tag name exists in the output dom
				// set the table element to this element from the dom
				table = (Element) outputDom.getElementsByTagName(tableTagName).item(0);
				appendTable = false;
			} else {
				// if we don't want to accumulate or didn't find an element with the same tag name in the output dom 
				// create the table element
				table = outputDom.createElement(tableTagName);
				appendTable = true;
			}
				
	        for (int j =0; j < description.size() && isRequestedObjectRunning(); j++) {
	        	NodeList rowList = null;
	    		XMLTableRow xrow = getRow(j);
	        	String rowXPath = xrow.getXpath();
	        	try {
	        		rowList = xpathApi.selectNodeList(tableNode, rowXPath);
				} catch (TransformerException e) {
	        		Engine.logBeans.error("Error with :" + rowXPath, e);
	        	}
	        	if (rowList != null) {
	        		for (int k = 0; k < rowList.getLength() && isRequestedObjectRunning(); k++) {
	        			Node row = outputDom.createElement(xrow.getName());
	        			Node rowNode = rowList.item(k);
	        			int len = xrow.getColumns().size();
	    	        	for (int z = 0; z < len && isRequestedObjectRunning(); z++) {
	    	        		String colXPath = xrow.getColumnXPath(z);
    		        		NodeList columList = null;
	    		        	try {
	    						columList = xpathApi.selectNodeList(rowNode, colXPath);
	    					} catch (TransformerException e) {
	    		        		Engine.logBeans.error("Error with :" + colXPath, e);
	    		        	}
	    		        	
	    		        	if (columList != null) {
	    			        	for (int w = 0; w < columList.getLength() && isRequestedObjectRunning(); w++) {
	    	    	        		Node col = outputDom.createElement(xrow.getColumnName(z));
	    			        		Node colNode = columList.item(w);
	    			            	Text text = outputDom.createTextNode(getStringValue(colNode, xrow.hasToExtract(z)));
	    			            	col.appendChild(text);
	    			            	row.appendChild(col);
	    			        	}
	    		        	}
	    	        	}
	    	        	table.appendChild(row);
	        		}
	        	}
	        }
	        if (appendTable) {
	        	if (isDisplayReferer()) {
					addReferer(table);
	        	}
	        	if (isFlipTable()) {
	        		table  = flipTable(table);
	        	}
	        	doc.appendChild(table);
	        }        	
			Engine.logBeans.trace("XMLTable '" + tableNode.getNodeName() + "' added to result document");
		}
	}

	private Element  flipTable(Element table) {
		// create lines for each column found in the first source line
		NodeList srcLines   = table.getChildNodes();
		NodeList srcCols    = srcLines.item(0).getChildNodes();
		int		 nbSrcLines = srcLines.getLength();
		int		 nbSrcCols  = srcCols.getLength();
		
		// for each column create a line from the source column names.
		for (int i = 0; i < nbSrcCols; i++) {
			Element destLine = table.getOwnerDocument().createElement(srcCols.item(i).getNodeName());
			// in this line create Columns from the source line names.
			for (int j = 0; j < nbSrcLines; j++) {
				Element 	destCol 	= table.getOwnerDocument().createElement(srcLines.item(j).getNodeName());
				Node 		textNode 	= srcLines.item(j).getChildNodes().item(i).getFirstChild().cloneNode(true);
				destCol.appendChild(textNode);
				destLine.appendChild(destCol);
				
			}
			table.appendChild(destLine);
		}
		
		// clean source table;
		for (int j = 0; j < nbSrcLines; j++) {
			table.removeChild(srcLines.item(0));
		}

		return table;
	}
	
	public String getSchema(String tns) {
		return getTableSchema(tns);
	}
	
	private String getTableSchema(String tns) {
		String tableName, tableTypeName, tableType;
		String tableSchema, tableTypeSchema, schema = "";
		Set<String> schemas = new HashSet<String>();
		
		tableName = getSchemaElementName();
		tableTypeName = getSchemaElementType();
		tableType = tns+":"+ tableTypeName;
		
		tableTypeSchema = "<xsd:complexType name=\""+ tableTypeName +"\">\n";
		tableTypeSchema += "<xsd:sequence>\n";

		List<String> colsNames = new LinkedList<String>();
		
		if (!isFlipTable()) {
			for (int i = 0; i < description.size(); i++) {
				colsNames.clear();
				// get row
				XMLTableRow xrow = getRow(i);
				// get columns names
				for (int j = 0 ; j < xrow.getColumns().size() ; j++)
					colsNames.add(xrow.getColumnName(j));
				// call making schema method
				schema = getRowType(tns, tableName, xrow.getName(), colsNames);

				// do not append doublet
				if (schemas.add(schema)) tableTypeSchema += schema;
			}
		} else {
			for (int i = 0; i <description.size(); i++) {
				// get row
				XMLTableRow xrow = getRow(i);
				// get columns
				for(List<Object> col : xrow.getColumns()) {
					colsNames.clear();
					// get columns names
					for (int k = 0 ; k < description.size() ; k++) {
						colsNames.add(getRow(k).getName());
					}
					// call making schema method
					schema = getRowType(tns, tableName, (String)col.get(0), colsNames);
					
					// do not append doublet
					if (schemas.add(schema)) {
						tableTypeSchema += schema;
					}
				}
			}
		}
		
		tableTypeSchema += "</xsd:sequence>\n";
		tableTypeSchema += "<xsd:attribute name=\"referer\" type=\"xsd:string\" use=\"optional\" />\n";
		tableTypeSchema += "</xsd:complexType>\n";
		addWsType(tableTypeName, tableTypeSchema);
		
		tableSchema = "<xsd:element minOccurs=\"0\" maxOccurs=\"unbounded\" name=\""+ tableName + "\" type=\""+ tableType +"\" />\n";
		
		return tableSchema;
	}
	
	
	private String getRowType(String tns, String tableName, String rowName, List<String> columnsNames) {
		String rowTypeName, rowType;
		String rowSchema, rowTypeSchema, schema;
		Set<String> schemas = new HashSet<String>();
		String maxOccurs = columnsNames.size()>1 ? "":"maxOccurs=\"unbounded\"";
		
		rowTypeName = rowName + "_" + tableName + "RowType";
		rowType = tns+":"+ rowTypeName;
		
		rowTypeSchema = "<xsd:complexType name=\""+ rowTypeName +"\">\n";
		rowTypeSchema += "<xsd:sequence>\n";
		for (String columnName : columnsNames) {
			schema = "<xsd:element minOccurs=\"0\" "+maxOccurs+" name=\""+ columnName + "\" type=\"xsd:string\" />\n";
			// do not append doublet
			if (schemas.add(schema)) {
				rowTypeSchema += schema;
			}
		}
		rowTypeSchema += "</xsd:sequence>\n";
		rowTypeSchema += "</xsd:complexType>\n";
		addWsType(rowTypeName, rowTypeSchema);

		rowSchema = "<xsd:element minOccurs=\"0\" maxOccurs=\"unbounded\" name=\""+ rowName + "\" type=\""+ rowType +"\" />\n";
		
		return rowSchema;
	}
	
	public String getSchemaElementName() {
		return (tagName.equals("") ? name : tagName);
	}

	public String getSchemaElementType() {
		return getSchemaElementName() + "TableType";
	}
	
	public String getSchemaElementNSType(String tns) {
		return tns + ":" + getSchemaElementType();
	}
	
	public boolean isAccumulateDataInSameTable() {
		return accumulateDataInSameTable;
	}

	public void setAccumulateDataInSameTable(boolean accumulateDataInSameTable) {
		this.accumulateDataInSameTable = accumulateDataInSameTable;
	}

	public boolean isFlipTable() {
		return flipTable;
	}

	public void setFlipTable(boolean flipTable) {
		this.flipTable = flipTable;
	}

	public String[] getTablePropertyNames() {
		return new String[] {"description"};
	}

	public XMLVector<XMLVector<Object>> getTableData(String propertyName) {
		if (propertyName.equals("description")) {
			return getDescription();
		}
		return null;
	}
	
	public String getTableRenderer(String propertyName) {
		if (propertyName.equals("description")) {
			return "XMLTableDescriptionTreeObject";
		}
		return null;
	}
}
