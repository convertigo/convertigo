/*
 * Copyright (c) 2001-2022 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class ReadCSVStep extends ReadFileStep {
	private static final long serialVersionUID = -6548050468297488381L;

	private String separator = ",";		
	private String tagLineName = "line";
	private String tagColName = "col";
	private boolean titleLine = false;
	private boolean verticalDirection = false;
	private String encoding="utf-8";
	
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
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {
		}
		
		return "ReadCSV :" + label;
	}
	
	@Override
	public String getStepNodeName() {
		if (replaceStepElement) {
			return "document";
		} else {
			return super.getStepNodeName();
		}
	}
	
	protected void processTitleLine(String[] row) {
		if (titleLine && row != null) {
			for (int i = 0; i < row.length; i++) {
				String cell = row[i];
				if (org.apache.commons.lang3.StringUtils.isBlank(cell)) {
					cell = "title" + i;
				}
			}
		}
	}
	
	protected CSVReader getCSVReader(Reader reader) {
		return new CSVReaderBuilder(reader).withCSVParser(new CSVParserBuilder().withSeparator(separator.charAt(0)).build()).build();
	}
	
	protected Document read(String filePath, boolean schema) throws EngineException {
		Document csvDoc = null;
		
		File csvFile = new File(getAbsoluteFilePath(filePath));
		if (!csvFile.exists()) {
			throw new EngineException("The CSV file \""+ filePath +"\" does not exist.");
		}
		
		if (this.encoding.isEmpty() || this.separator.isEmpty()) {
			BufferedReader br = null;
			String[] seps = new String[]{",", ";", "\t", " ", "|"};
			String separator = seps[0];
			String encoding = "UTF-8";
			if (!this.encoding.isEmpty()) {
				encoding = this.encoding;
			}
			int max = 0;
			try {
				br = new BufferedReader(new FileReader(csvFile, Charset.forName(encoding)));
				String l1 = "" + br.readLine();
				String l2 = "" + br.readLine();
				if (this.encoding.isEmpty() && (l1.indexOf("�") >= 0 || l2.indexOf("�") >= 0)) {
					br.close();
					br = new java.io.BufferedReader(new java.io.FileReader(csvFile, java.nio.charset.Charset.forName(encoding = "ISO-8859-1")));
					l1 = "" + br.readLine();
					l2 = ""  +br.readLine();
				}
				
				if (this.encoding.isEmpty()) {
					this.encoding = encoding;
				}
				
				if (this.separator.isEmpty()) {
					for (String s: seps) {
						int len = l1.split(s).length;
						if (len > max && len == l2.split(s).length) {
							max = len;
							separator = s;
						}
					}
					this.separator = separator;
				}
				
				br.close();
			} catch (Exception e) {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e1) {
					}
				}
			}
		}
		
		try (Reader reader = new InputStreamReader(FileUtils.newFileInputStreamSkipBOM(csvFile), encoding.isEmpty() ? "utf-8" : encoding)) {
			//construction of the DOM's root
			csvDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
			Element root = csvDoc.createElement("document");
			csvDoc.appendChild(root);
			
			
			List<String[]> vLines = new ArrayList<String[]>();
			
			// Reads file line by line
			getCSVReader(reader).forEach(row -> vLines.add(row));
			
			if (vLines.isEmpty()) {
				return csvDoc;
			}
			
			int lines = vLines.size(), cols = vLines.get(0).length;
			
			processTitleLine(vLines.get(0));
			
			// Generates dom
			Element line, col;
			int i = 0, j = 0;
			if (verticalDirection) {
				while (j < cols) {
					i = (titleLine ? 1 : 0);
					if (titleLine) {
						col = csvDoc.createElement(StringUtils.normalize(vLines.get(0)[j]));
						col.setAttribute("originalKeyName", vLines.get(0)[j]);
						col.setAttribute("type", "string");
					} else {
						col = csvDoc.createElement(getTagColName());
					}
					
					while (i < lines) {
						line = csvDoc.createElement(getTagLineName());
						if (!schema) {
							line.appendChild(csvDoc.createTextNode(vLines.get(i)[j]));
						}
						col.appendChild(line);
						i++;
						//if (schema)break; // comment/uncomment this line to see or not iterations
					}
					j++;
					root.appendChild(col);
					//if (schema && !titleLine)break; // comment/uncomment this line to see or not iterations
				}
			} else {
				i = (titleLine ? 1 : 0);
				while ((i < lines && !schema) || (i < 2 && schema)) {
					j = 0;
					line = csvDoc.createElement(getTagLineName());
					while (j < cols) {
						if (titleLine) {
							col = csvDoc.createElement(StringUtils.normalize(vLines.get(0)[j]));
							col.setAttribute("originalKeyName", vLines.get(0)[j]);
							col.setAttribute("type", "string");
						} else {
							col = csvDoc.createElement(getTagColName());
						}
						if (!schema) {
							col.appendChild(csvDoc.createTextNode(vLines.get(i)[j]));
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
		}
		
		return csvDoc;
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		XmlSchemaElement base = elt;
		if (!replaceStepElement) {
			XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
			base = element;
	
			XmlSchemaComplexType cType0 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
			element.setType(cType0);
	
			XmlSchemaSequence sequence0 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
			cType0.setParticle(sequence0);
	
			sequence0.getItems().add(elt);
		}
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
			File csvFile = getFile();
			String[] cols = null;
			if (csvFile != null && csvFile.exists()) {
				try (Reader reader = new InputStreamReader(new FileInputStream(csvFile), encoding.isEmpty() ? "iso-8859-1" : encoding)) {
					CSVReader csvReader = getCSVReader(reader);
					cols = csvReader.readNext();
					processTitleLine(cols);
				} catch (Exception e) {
					Engine.logBeans.warn("Failed to read the CVS file: " + csvFile, e);
				}
				
				if (cols != null) {
					if (verticalDirection) {
						for (String col : cols) {
							subElt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
							subElt.setName(StringUtils.normalize(col));
							sequence.getItems().add(subElt);
							
							cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
							subElt.setType(cType);
							
							XmlSchemaAttribute attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
							attribute.setName("originalKeyName");
							attribute.setSchemaTypeName(Constants.XSD_STRING);
							attribute.setDefaultValue(col);
							cType.getAttributes().add(attribute);
							
							attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
							attribute.setName("type");
							attribute.setSchemaTypeName(Constants.XSD_STRING);
							attribute.setDefaultValue("string");
							cType.getAttributes().add(attribute);
							
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
							subElt.setName(StringUtils.normalize(col));
							sequence.getItems().add(subElt);
							
							cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
							subElt.setType(cType);
							
							XmlSchemaAttribute attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
							attribute.setName("originalKeyName");
							attribute.setSchemaTypeName(Constants.XSD_STRING);
							attribute.setDefaultValue(col);
							cType.getAttributes().add(attribute);
							
							attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
							attribute.setName("type");
							attribute.setSchemaTypeName(Constants.XSD_STRING);
							attribute.setDefaultValue("string");
							cType.getAttributes().add(attribute);
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

		return base;
	}
	
	@Override
	protected String migrateSourceXpathFor620(String filePath, String xpath) throws Exception {
		if (xpath.startsWith("./")) {
			xpath = xpath.replaceFirst("./", "./document/");
		}
		return xpath;
	}
}