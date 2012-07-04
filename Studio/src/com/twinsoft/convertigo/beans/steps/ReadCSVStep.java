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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public class ReadCSVStep extends ReadFileStep {
	private static final long serialVersionUID = -6548050468297488381L;

	protected String separator = ";";		
	protected String tagLineName = "line";
	protected String tagColName = "col";
	protected boolean titleLine = false;
	protected boolean verticalDirection = false;
	protected String encoding="iso-8859-1";
	
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
		BufferedReader fichier;
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
		}
		
		return csvDoc;
	}
}