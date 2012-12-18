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

package com.twinsoft.convertigo.beans.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.util.StringEx;

public class ReadCSVStep extends ReadFileStep {
	private static final long serialVersionUID = -6548050468297488381L;

	private String separator = ";";		
	private String tagLineName = "line";
	private String tagColName = "col";
	private boolean titleLine = false;
	private boolean verticalDirection = false;
	private String encoding="iso-8859-1";
	
	public ReadCSVStep() {
		super();
	}

	@Override
    public ReadCSVStep clone() throws CloneNotSupportedException {
    	ReadCSVStep clonedObject = (ReadCSVStep) super.clone();
        return clonedObject;
    }
	
	@Override
    public ReadCSVStep copy() throws CloneNotSupportedException {
    	ReadCSVStep copiedObject = (ReadCSVStep) super.copy();
        return copiedObject;
    }

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
	public String getTagLineName() {
		return tagLineName;
	}

	public void setTagLineName(String tagLineName) {
		this.tagLineName = tagLineName;
	}

	public String getTagColName() {
		return tagColName;
	}

	public void setTagColName(String tagColName) {
		this.tagColName = tagColName;
	}

	public boolean isTitleLine() {
		return titleLine;
	}

	public void setTitleLine(boolean titleLine) {
		this.titleLine = titleLine;
	}

	public boolean isVerticalDirection() {
		return verticalDirection;
	}

	public void setVerticalDirection(boolean verticalDirection) {
		this.verticalDirection = verticalDirection;
	}
	
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {
		}
		
		return "ReadCSV :" + label + (!text.equals("") ? " // "+text:"");
	}

	protected Document read(String filePath, boolean schema) throws EngineException {
		if (separator.equals(""))
			throw new EngineException("The separator is empty");
		
		Document csvDoc = null;
		BufferedReader fichier = null;
		try {
			File csvFile = new File(getAbsoluteFilePath(filePath));
			if (!csvFile.exists()) {
				throw new EngineException("The CSV file \""+ filePath +"\" does not exist.");
			}
			
			//construction of the DOM's root
			csvDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();				
			Element root = csvDoc.createElement("document");
			csvDoc.appendChild(root);
			
			
			List<List<String>> vLines = new ArrayList<List<String>>();
			int lines = 0, cols = 0, tokens = 0;
			String str, data, value = "";
			boolean start = false;
			StringTokenizer st;
			
			// Reads file line by line
			fichier = new BufferedReader(new InputStreamReader(
                    new FileInputStream(getAbsoluteFilePath(filePath)), (encoding.length() > 0)? encoding : "iso-8859-1"));
			while ((str = fichier.readLine()) != null) {
				if (str.startsWith(separator)) {
					str = "_empty_"+str;
				}
				if (str.endsWith(separator)) {
					str = str + "_empty_";
				}
				
				StringEx tmp = new StringEx(str);
				while(tmp.toString().contains(separator+separator)){
					tmp.replaceAll(separator+separator, separator+ "_empty_" + separator);
				}
				str = tmp.toString();
				
				st = new StringTokenizer(str, separator, true);
				tokens = st.countTokens();
				if (tokens > 0) {
					List<String> vCol = new ArrayList<String>();
					while (st.hasMoreTokens()) {
						data = st.nextToken();
						if (!start && data.equals(separator)) {
							tokens--;
							continue;
						}
						
						value += data;
						if (!start && (data.startsWith("\"")) && !(data.endsWith("\""))) {
								start = true;
							tokens--;
							continue;
						}
						if (start) {
							if (data.equals(separator)) {
								tokens--;
								continue;
							}
							else if (data.endsWith("\"")) {
								start = false;
							}
							else {
								throw new EngineException("File '"+ filePath +"': corrupted at line="+ lines);
							}
						}
						vCol.add(value);
						value = "";
					}
					vLines.add(vCol);
					cols = (tokens>cols)? tokens:cols;
					lines++;
				}
			}
			
			// Constructs array
			String[][] table = new String[lines][cols];
			for (int i = 0; i < lines; i++) {
				List<String> vCol  = vLines.get(i);
				for (int j = 0; j < cols; j++) {
					try {
						data = vCol.get(j);
						if (data.equals("_empty_")) {
							data = "";
						}
					}
					catch (ArrayIndexOutOfBoundsException e) {
						data = "";
					}
					
					if (titleLine && (i == 0)) {
						// Title tag name must not be empty!
						if (data.trim().equals("")) {
							data = "titre"+j;
						}
						// Normalize tag name
						data = StringUtils.normalize(data);
					}
					table[i][j] = data;
				}
			}
			
			// Generates dom
			Element line, col;
			int i=0, j=0;
			if (verticalDirection) {
				while (j < cols) {
					i = (titleLine ? 1 : 0);
					col = csvDoc.createElement(titleLine ? table[0][j]:getTagColName());
					while (i < lines) {
						line = csvDoc.createElement(getTagLineName());
						if (!schema) {
							line.appendChild(csvDoc.createTextNode(table[i][j]));
						}
						col.appendChild(line);
						i++;
						//if (schema)break; // comment/uncomment this line to see or not iterations
					}
					j++;
					root.appendChild(col);
					//if (schema && !titleLine)break; // comment/uncomment this line to see or not iterations
				}
			}
			else {
				i = (titleLine ? 1 : 0);
				while ((i < lines && !schema) || (i < 2 && schema)) {
					j=0;
					line = csvDoc.createElement(getTagLineName());
					while (j < cols) {
						col = csvDoc.createElement(titleLine ? table[0][j]:getTagColName());
						if (!schema) {
							col.appendChild(csvDoc.createTextNode(table[i][j]));
						}
						line.appendChild(col);
						j++;
						//if (schema && !titleLine)break; // comment/uncomment this line to see or not iterations
					}
					root.appendChild(line);
					i++;
					//if (schema)break; // comment/uncomment this line to see or not iterations
				}
			}
		} catch (Exception e) {
			throw new EngineException("An error occured while creating dom of csv file",e);
		} finally {
			if (fichier != null) {
				try {
					fichier.close();
				} catch (IOException e) {
					throw new EngineException("An error occured while creating dom of csv file",e);
				}
			}
		}
		
		return csvDoc;
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);

		XmlSchemaComplexType cType0 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType0);

		XmlSchemaSequence sequence0 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType0.setParticle(sequence0);

		XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence0.getItems().add(elt);
		elt.setName("document");
		
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		elt.setType(cType);
		
		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		
		XmlSchemaElement subElt = null;
		if (!titleLine || !verticalDirection) {
			subElt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
			subElt.setMinOccurs(0);
			subElt.setMaxOccurs(Long.MAX_VALUE);
			sequence.getItems().add(subElt);
			
			cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
			subElt.setType(cType);
			
			sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
			cType.setParticle(sequence);
		}

		if (titleLine) {
			File file = getFile();
			String[] cols = null;
			if (file != null && file.exists()) {
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding.length() > 0 ? encoding : "iso-8859-1"));
					String line = reader.readLine();
					if (line != null) {
						cols = line.split(Pattern.quote(separator));
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e) {
						}
					}
				}
				
				if (cols != null) {
					if (verticalDirection) {
						for (String col : cols) {
							subElt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
							subElt.setName(col);
							sequence.getItems().add(subElt);
							
							cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
							subElt.setType(cType);
							
							XmlSchemaSequence subSequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
							cType.setParticle(subSequence);
							
							subElt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
							subElt.setName(tagLineName);
							subElt.setSchemaTypeName(Constants.XSD_STRING);
							subElt.setMinOccurs(0);
							subElt.setMaxOccurs(Long.MAX_VALUE);
							subSequence.getItems().add(subElt);							
						}
					} else {
						subElt.setName(tagLineName);
						for (String col : cols) {
							subElt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
							subElt.setName(col);
							subElt.setSchemaTypeName(Constants.XSD_STRING);
							sequence.getItems().add(subElt);							
						}
					}
				}
			}
			
		} else {
			subElt.setName(verticalDirection ? tagColName : tagLineName);

			subElt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
			subElt.setName(verticalDirection ? tagLineName : tagColName);
			subElt.setSchemaTypeName(Constants.XSD_STRING);
			subElt.setMinOccurs(0);
			subElt.setMaxOccurs(Long.MAX_VALUE);
			sequence.getItems().add(subElt);
		}

		return element;
	}
}