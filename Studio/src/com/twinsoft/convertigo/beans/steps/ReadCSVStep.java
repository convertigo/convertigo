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
//import java.io.FileReader;
import java.io.InputStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.util.StringEx;

//import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;
//import java.util.regex.* ;

import javax.xml.parsers.DocumentBuilderFactory;

public class ReadCSVStep extends ReadFileStep {
	
	private static final long serialVersionUID = -6548050468297488381L;

	//private static final char CHAR_NEW_LINE ='\n';
	//private static final char CHAR_CARRIAGE_RETURN ='\r';
	
	protected String separator = ";";		
	protected String tagLineName = "line";
	protected String tagColName = "col";
	protected boolean titleLine = false;
	protected boolean verticalDirection = false;
	protected String encoding="iso-8859-1";
	
	//private transient ArrayList listeTitle = null;

	public ReadCSVStep() {
		super();
	}

    public Object clone() throws CloneNotSupportedException {
    	ReadCSVStep clonedObject = (ReadCSVStep) super.clone();
    	//clonedObject.listeTitle = null;
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
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
		return readCSVbyNat(filePath, schema);
		//return readCSVbyMartin(schema);
	}
	
	protected Document readCSVbyNat(String filePath, boolean schema) throws EngineException {
		if (separator.equals(""))
			throw new EngineException("The separator is empty");
		
		Document csvDoc = null;
		BufferedReader fichier;
		try {
			File csvFile = new File(getAbsoluteFilePath(filePath));
			if (!csvFile.exists())
				throw new EngineException("The CSV file \""+ filePath +"\" does not exist.");
			
			//construction of the DOM's root
			csvDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();				
			Element root = csvDoc.createElement("document");
			csvDoc.appendChild(root);
			
			Vector vCol, vLines = new Vector();
			int lines = 0, cols = 0, tokens = 0;
			String str, data, value = "";
			boolean start = false;
			StringTokenizer st;
			
			// Reads file line by line
			fichier = new BufferedReader(new InputStreamReader(
                    new FileInputStream(getAbsoluteFilePath(filePath)),(encoding.length()>0)?encoding:"iso-8859-1"));
			//fichier = new BufferedReader(new FileReader(csvFile));
			while ((str = fichier.readLine()) != null) {
				if (str.startsWith(separator)) str = "_empty_"+str;
				if (str.endsWith(separator)) str = str + "_empty_";
				
				StringEx tmp = new StringEx(str);
				while(tmp.toString().contains(separator+separator)){
					tmp.replaceAll(separator+separator, separator+ "_empty_" + separator);
				}
				str = tmp.toString();
				
				st = new StringTokenizer(str, separator, true);
				tokens = st.countTokens();
				if (tokens > 0) {
					vCol = new Vector();
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
							else
								throw new EngineException("File '"+ filePath +"': corrupted at line="+ lines);
						}
						vCol.addElement(value);
						value = "";
					}
					vLines.addElement(vCol);
					cols = (tokens>cols)? tokens:cols;
					lines++;
				}
			}
				
			// Constructs array
			String[][] table = new String[lines][cols];
			for (int i=0; i<lines; i++) {
				vCol = (Vector)vLines.elementAt(i);
				for (int j=0; j<cols; j++) {
					try {
						data = (String)vCol.elementAt(j);
						if (data.equals("_empty_")) data = "";
					}
					catch (ArrayIndexOutOfBoundsException e) {
						data = "";
					}
					
					if (titleLine && (i==0)) {
						// Title tag name must not be empty!
						if (data.trim().equals(""))
							data = "titre"+j;
						// Normalize tag name
						data = StringUtils.normalize(data);
					}
					table[i][j] = data;
					//System.out.println("table["+i+"]["+j+"] = "+data);
				}
			}
			
			// Generates dom
			Element line, col;
			int i=0, j=0;
			if (verticalDirection) {
				while (j<cols) {
					i = (titleLine ? 1:0);
					col = csvDoc.createElement(titleLine ? table[0][j]:getTagColName());
					while (i<lines) {
						line = csvDoc.createElement(getTagLineName());
						if (!schema) line.appendChild(csvDoc.createTextNode(table[i][j]));
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
				i = (titleLine ? 1:0);
				while ((i<lines && !schema)||(i<2 && schema)) {
					j=0;
					line = csvDoc.createElement(getTagLineName());
					while (j<cols) {
						col = csvDoc.createElement(titleLine ? table[0][j]:getTagColName());
						if (!schema) col.appendChild(csvDoc.createTextNode(table[i][j]));
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
	
	
	
	//all code after that is for readCSVbyMartin
	/*
	protected Document readCSVbyMartin(boolean schema) throws EngineException {
		if (separator.equals(""))
			throw new EngineException("The separator is empty");
		
		Document csvDoc = null;
		BufferedReader fichier;
		try {
			File csvFile = new File(getAbsoluteFilePath(dataFile));
			if (!csvFile.exists())
				throw new EngineException("The CSV file \""+ dataFile +"\" does not exist.");
			
			//construction of the DOM's root
			csvDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();				
			Element root = csvDoc.createElement("document");
			csvDoc.appendChild(root);
			

			String str,chaine="",chaineOld="";				
			StringTokenizer tokenizer; 
			int cptL=1,cptC=1;			
			
			fichier = new BufferedReader(new FileReader(csvFile));
			str = fichier.readLine( );
			if(!verticalDirection){
				while ((str != null && schema==false)
						||(str != null && schema==true && titleLine==true && cptL<=2)
						||(str != null && schema==true && titleLine==false && cptL<=1)
						){//for each line						
					str=encode(str);					
					if(titleLine==true && cptL==1){//construction of an array of the title
						String title=str;
						String stringTitle;
						String StringTitleOld="";
						java.util.StringTokenizer tokenizer2 = new java.util.StringTokenizer(title, separator,true);
						listeTitle = new ArrayList();
						while ( tokenizer2.hasMoreTokens() ) {//loop which count the number of column 
							stringTitle=tokenizer2.nextToken();
						    if(isData(stringTitle,StringTitleOld))
						    	listeTitle.add(stringTitle.equals(separator)?"":stringTitle);
						    StringTitleOld=stringTitle;
						} 
						//for(int i=0;i<listeTitle.size();i++)
							//Engine.logBeans.warn("^^^^^^"+(String)listeTitle.get(i));
						
					}
					else{
					//construction of a new line 
					Element ligne= csvDoc.createElement(getTagLineName());					
					root.appendChild(ligne);											
					
					//creation of a tokenizer 
					tokenizer = new java.util.StringTokenizer(str, separator,true);

					while ( tokenizer.hasMoreTokens() ) {
					    chaine=tokenizer.nextToken();
					    
					   
					    if(isData(chaine,chaineOld)){
					    	chaine=TransformString(chaine,chaineOld);						    	
					    	//construction of a new column 						    	
					    	createCol(csvDoc,ligne,decode(chaine),decode(getTitle(cptC)));												    	
							cptC++;
					    }					    
					    chaineOld=chaine;
					}
					if(chaine.equals(separator)){
						//if the line ends with a separator we must add an ampty line to the DOM							
						createCol(csvDoc,ligne,"",decode(getTitle(cptC)));
					}				
					
					
					cptC=1;
					}					
					str = fichier.readLine( );						
					cptL++;
	             }
			}
			else{//vertical direction
				
					cptC=0;					
				tokenizer = new java.util.StringTokenizer(str, separator,true);
				
				while ( tokenizer.hasMoreTokens() ) {//loop which count the number of column 
				    chaine=tokenizer.nextToken();
				    if(isData(chaine,chaineOld))
				    	cptC++; 
				    chaineOld=chaine;
				}  
				ArrayList liste = new ArrayList();
				while (str != null){//recuperation of each line in a arrayList
					liste.add(encode(str));
					str=fichier.readLine();
				}
					
				str=(String)liste.get(0);
				int j=0;
				Element col=null;					
				for(int i=0;i<cptC;i++){//for each column	
					
					for (cptL=0;cptL<liste.size();cptL++){
						tokenizer = new java.util.StringTokenizer(str, separator,true);	
						chaineOld="";
						
						while(j<=i){//recuperation of the good element(the good column) 
							chaine=tokenizer.nextToken();
							if(isData(chaine,chaineOld)){
								chaine=TransformString(chaine,chaineOld);
								j++;									
							}
							chaineOld=chaine;
							//if the line ends with a separator we must add an ampty line to the DOM
							if(!tokenizer.hasMoreTokens() && chaine.equals(separator)){
								chaine="";
								j++;
							}	
						}
						j=0;
												
						
						if(cptL==0){//if its the first line
							if(titleLine)
								col= csvDoc.createElement((chaine.length()>1)? decode(chaine):getTagLineName());	
							else									
								col= csvDoc.createElement(getTagLineName());					
												
							root.appendChild(col);								
						   
							if(!titleLine){
								Element lig= csvDoc.createElement(getTagColName()+((titleLine)?cptL:(cptL+1)));	
								col.appendChild(lig);
								lig.appendChild(csvDoc.createTextNode(decode(chaine)));
							}
						   
						}else{
							//if the curseur is not in the first line the line is a data line
							Element lig= csvDoc.createElement(getTagColName()+((titleLine)?cptL:(cptL+1)));	
							col.appendChild(lig);
							lig.appendChild(csvDoc.createTextNode(decode(chaine)));
						}
						
						str=(String)liste.get((cptL+1)%liste.size());														
						
					}							
				}
			}			
			fichier.close();				
			//Engine.logBeans.warn(XMLUtils.prettyPrintDOM(csvDoc));
		
		} catch (Exception e) {
			throw new EngineException("An error occured while creating dom of csv file",e);
		}
		
		return csvDoc;
	}
	

	private void createCol(Document doc, Element ligne,String chaine,String nameCol){
		Element col= doc.createElement(nameCol);	
		ligne.appendChild(col);
		col.appendChild(doc.createTextNode(chaine));
	}
	
	private String getTitle(int cptC){
		if(!titleLine){				
    		return getTagColName()+cptC;
		}
    	else{	
    		return(((listeTitle.size()>cptC-1)&& ((String)listeTitle.get(cptC-1)).length()>0))?makeTagNamePossible(((String)listeTitle.get(cptC-1))):getTagColName()+cptC;
    	}
	}
	
	private String makeTagNamePossible(String entry){
		entry=entry.replaceAll("\\s", "_");			
		entry=entry.replaceFirst("^\\d", "_$0");
		return entry;
		
	}
	
	private boolean isData(String chaine,String chaineOld){
		return !chaine.equals(separator) || (chaine.equals(separator)&& chaineOld.equals(separator)) ||(chaine.equals(separator)&&chaineOld.equals(""));
	}
	
	private String TransformString(String chaine,String chaineOld){
		if((chaine.equals(separator)&& chaineOld.equals(separator)) ||(chaine.equals(separator)&&chaineOld.equals(""))){								
			chaine="";
		}	
		return chaine;
	}
				

	private String encode(String entry){				
		char[] entryEnChar;
		//Engine.logBeans.warn("############INITIAL_ENTRY="+entry);
		entry=separator+entry;	
		entryEnChar=entry.toCharArray();
		Pattern p = Pattern.compile(separator+"\"([^\"]*)\"");  
		Pattern p2 = Pattern.compile(separator); 
		Matcher m2,m = p.matcher(entry);			
		int debut=0,debut2;
		while(m.find(debut)){	
			//Engine.logBeans.warn("############TOKEN="+m.group());					
			debut2=1;
			m2=p2.matcher(m.group());
			while(m2.find(debut2)){
				entryEnChar[m.start()+m2.start()]=CHAR_NEW_LINE;
				debut2++;
			}				
			entryEnChar[m.start()+1]=CHAR_CARRIAGE_RETURN;
			entryEnChar[m.end()-1]=CHAR_CARRIAGE_RETURN;						
			debut=m.end()-2;
			m=p.matcher(entry);
		}			
		entry="";
		for (int i=1;i<entryEnChar.length;i++){
			//Engine.logBeans.warn("############indice="+i+": "+entryEnChar[i]);				
			entry+=entryEnChar[i];
		}
		entry=entry.replaceAll(""+CHAR_CARRIAGE_RETURN,"");
		//Engine.logBeans.warn("############FINAL_REP="+entry);
		return entry;		
	}
	
	private String decode(String entry){
		return entry.replaceAll(""+CHAR_NEW_LINE, separator);
	}*/
}
