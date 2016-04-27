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

package com.twinsoft.convertigo.engine.util;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Engine;

public class Copybook {
	public Vector<String> lines = null;
	public Vector<COBOLVariable> variables = null;
	
	public Copybook() {
		lines = new Vector<String>();
		variables = new Vector<COBOLVariable>();
	}
	
	public void getLines(Reader reader) throws IOException {
		String line = null, entireLine = "";
		LineNumberReader input = null;
		boolean wasEmpty = true;
		int index = 0, level = 0;
		char c;
		
		input = new LineNumberReader(reader);
		while ((line = input.readLine()) != null) {
			String s = parse(line);
			if (s != null) {
				//System.out.println(s);
				wasEmpty = entireLine.equals("");
				entireLine += s;
				
				// line is terminated by a point
				index = entireLine.length() - 1;
				c = entireLine.charAt(index);
				if (c != '.') // treat special case where there's some datas after point.
					index = entireLine.lastIndexOf(". ");
				
				if (index != -1) {
					entireLine = entireLine.substring(0,index);
					//System.out.println(entireLine);
					level = getLevel(entireLine);
					if ((level < 88) && (level != 0))
						lines.addElement(entireLine);
							
					entireLine = "";
				}
				else {
					if (wasEmpty) {
						level = getLevel(entireLine);
						if ((level < 88) && (level != 0))
							entireLine += " ";
						else
							entireLine = "";
					}
					else
						entireLine += " ";
				}
			}
		}
	}
	
	protected int getLevel(String line) {
		int level = 0, index;

		if (line.length()>=2) {
			index = line.indexOf(" ");
			if (index != -1) {
				try {
					level = Integer.parseInt(line.substring(0,index));
				}
				catch (NumberFormatException e) {}
			}
		}
		return level;
	}
	
	public String parse(String line) {
		
		// replace tabulation with spaces
		Pattern p = Pattern.compile("\\t");
		Matcher m = p.matcher(line);
		line = m.replaceAll("        ");
		
		// start at col=7
		if (line.length()>7) {
			
			line = line.substring(6);

			// look for first caracter different from space
			int i = 0;
			while (line.charAt(i) == ' ') {
				if (++i >= line.length())
					return null;
			}
			line = line.substring(i);
			
			char indicator = line.charAt(0);
			if (indicator == '*')
				return null;
			if (indicator == '/')
				return null;
			if (indicator == '$')
				return null;

			return line;
		}
		return null;
	}
	
	public void parseLine(String line) {
		String value = "", nvalue = "", s = "%20";
		Pattern ps = null;
		Matcher m = null;
		int index, i, j;
		
		// preserve spaces for VALUE keyword
		if ((index = line.indexOf("VALUE")) != -1) {
			if ((i = line.indexOf("'",index)) != -1) {
				if ((j = line.indexOf("'",i+1)) != -1) {
					value = line.substring(i,j+1);
					if (value.indexOf(" ") != -1) {
						ps = Pattern.compile("\\s");
						m = ps.matcher(value);
						nvalue = m.replaceAll(s);
						line = line.replaceAll(value,nvalue);
					}
				}
			}
		}
		
		// replace spaces by one space
		ps = Pattern.compile("\\s++");
		m = ps.matcher(line);
		line = m.replaceAll(" ");

		// split line on space
		Pattern p = Pattern.compile("\\s");
		String[] words = p.split(line);
		int len = words.length;
		
		if (len <= 0)
			return;
		
		String word = "";
		COBOLVariable cblvar = new COBOLVariable(line);
		for (i=0;i<len;i++) {
			word = words[i];
			//System.out.println(i+ ":" + words[i]);
			if (!word.equals("")) {
				if (i == 0)
					cblvar.level = word;
				if (i == 1)
					cblvar.name = word;
				if ((word.equalsIgnoreCase("PIC")) || (word.equalsIgnoreCase("PICTURE")))
					cblvar.picture = words[i+1];
				if (word.startsWith("COMP") || word.equalsIgnoreCase("POINTER"))
					cblvar.format = word;
				if (word.equalsIgnoreCase("OCCURS")) {
					try {
						if (words[i+2].equalsIgnoreCase("TO")) {
							cblvar.occurs[0] = Integer.parseInt(words[i+1]); 
							cblvar.occurs[1] = Integer.parseInt(words[i+3]); 
						}
						//if (words[i+2].equalsIgnoreCase("TIMES"))
						else
							cblvar.occurs[0] = Integer.parseInt(words[i+1]);
					}
					catch (ArrayIndexOutOfBoundsException e) {
						cblvar.occurs[0] = Integer.parseInt(words[i+1]);
					}
				}
				if (word.equalsIgnoreCase("DEPENDING"))
					cblvar.depends = words[i+1];
				if (word.equalsIgnoreCase("VALUE")) {
					value = words[i+1];
					// restore spaces for VALUE keyword
					if (value.indexOf(s) != -1) {
						ps = Pattern.compile(s);
						m = ps.matcher(value);
						value = m.replaceAll(" ");
					}
					// remove '' caracters
					if (value.startsWith("'")) {
						if (value.endsWith("'"))
							value = value.substring(1,value.length()-1);
					}
					cblvar.defvalue = value;
				}
			}
		}
		
		variables.addElement(cblvar);
	}
	
	public XMLVector<XMLVector<Object>> importFromFile(Reader reader) throws IOException {
		String line = null;
		XMLVector<XMLVector<Object>> v = new XMLVector<XMLVector<Object>>();
		
		// retrieves usefull lines
		getLines(reader);
				
		// parse lines into COBOLVariables
		if (lines.size()>0) {
			Enumeration<String> enumeration = lines.elements();
			while (enumeration.hasMoreElements()) {
				line = enumeration.nextElement();
				//System.out.println(line);
				parseLine(line);
			}
		}
				
		// retrieve datas under a vector
		if (variables.size()>0) {
			int offset = 0;
			Enumeration<COBOLVariable> enumeration = variables.elements();
			while (enumeration.hasMoreElements()) {
				COBOLVariable cblvar = enumeration.nextElement();
				cblvar.offset = offset;
				cblvar.bytes = COBOLUtils.getSizeInBytes(cblvar.picture, cblvar.format);
				offset += cblvar.bytes;
				
				v.add(cblvar.toXMLVector());
				
				//System.out.println("\n"+ cblvar.line);
				//System.out.println(cblvar.toString());
			}
		}
		return v;
	}
	
	public XMLVector<XMLVector<Object>> importFromFile2(Reader reader) throws IOException {
		String line = null;
		XMLVector<XMLVector<Object>> v = new XMLVector<XMLVector<Object>>();
		
		// retrieves usefull lines
		getLines(reader);
				
		// parse lines into COBOLVariables
		if (lines.size()>0) {
			Enumeration<String> enumeration = lines.elements();
			while (enumeration.hasMoreElements()) {
				line = enumeration.nextElement();
				//System.out.println(line);
				parseLine(line);
			}
		}

		// retrieve datas under a vector
		if (variables.size()>0) {
			int offset = 0;
			Enumeration<COBOLVariable> enumeration = variables.elements();
			while (enumeration.hasMoreElements()) {
				COBOLVariable cblvar = enumeration.nextElement();
				cblvar.offset = offset;
				cblvar.bytes = COBOLUtils.getSizeInBytes(cblvar.picture, cblvar.format);
				offset += cblvar.bytes;
				
				v.add(cblvar.toXMLVector2());
				
				//System.out.println("\n"+ cblvar.line);
				//System.out.println(cblvar.toString());
			}
		}
		return v;
	}

	public Element addLeveledXMLVector(Document doc, Element root, XMLVector<XMLVector<Object>> v) {
		Element parent = null, node = null, last = null;
				
		if (doc == null) {
			try {
				doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
			}
			catch (Exception e) {
				Engine.logEngine.error("Unexpected exception", e);
			}
		}
		
		if (doc != null) {
			int curlevel = 0;
			if (root == null) {
				root = doc.getDocumentElement();
				if (root == null) {
					root = doc.createElement("cics");
					doc.appendChild(root);
				}
			}
			if (v != null) {
				Enumeration<XMLVector<Object>> rows = v.elements();
				while (rows.hasMoreElements()) {
					XMLVector<Object> row = rows.nextElement();
				
					String sLevel = (String)row.get(0);
					String sName = (String)row.get(1);
					String sOccurs = (String)row.get(2);
					String sBytes = ((Integer)row.get(3)).toString();
					String sFormat = (String)row.get(4);
					String sPicture = (String)row.get(5);
					String sValue = (String)row.get(6);
				
					int level = Integer.parseInt(sLevel,10);
					if (level > 0) {
						node = doc.createElement(sName);
						node.setAttribute("level",sLevel);
						if (!sOccurs.equalsIgnoreCase(""))
							node.setAttribute("occurs",sOccurs);
						if (!sPicture.equalsIgnoreCase(""))
							node.setAttribute("picture",sPicture);
						if (!sBytes.equalsIgnoreCase(""))
							node.setAttribute("bytes",sBytes);
						if (!sFormat.equalsIgnoreCase(""))
							node.setAttribute("format",sFormat);
						node.appendChild(doc.createTextNode(sValue));
					
						if (curlevel == 0)
							parent = root;
						else if (level < curlevel) {
							curlevel = level;
							do {
								parent = (Element)last.getParentNode();
								try {
									level = Integer.parseInt((parent.getAttribute("level")),10);
								}
								catch (NumberFormatException e) {
									level = 0;
								}
								last = parent;
							}
							while (level >= curlevel);
								level = curlevel;
						}
						else if (level == curlevel) {
							parent = (Element)last.getParentNode();
						}
						else  {
							parent = last;
						}
					
						if (parent != null) {
							parent.appendChild(node);
							last = node;
							curlevel = level;
						}
					}
				}
			}
		}
		root = extendXMLMap(root);		
		return root;
	}

	public Element extendXMLMap(Element elt) {
		
		if (elt != null) {
			NodeList childs = elt.getChildNodes();
			int len = childs.getLength();
			if (len > 0) {
				for (int i=0; i<len; i++) {
					Node children = childs.item(i);
					if (children.getNodeType() == Node.ELEMENT_NODE)
						extendXMLMap((Element)children);
				}
				
			}
			String sOccurs = elt.getAttribute("occurs");
			if ((sOccurs != null) && (!sOccurs.equalsIgnoreCase(""))) {
				String aOccurs[] = sOccurs.split(":");
				int j = Integer.parseInt(aOccurs[0],10);
				if (aOccurs.length>1)
					j = Integer.parseInt(aOccurs[1],10);
				int count = 1;
				elt.setAttribute("occurence",""+count);
				while (count++ < j) {
					Element clone = (Element)elt.cloneNode(true);
					clone.setAttribute("occurence",""+count);
					elt.getParentNode().appendChild(clone);
				}
			}
		}
		
		return elt;	
	}
	
	public class COBOLVariable {
		public String line		= null;
		public String level		= null;
		public String name		= null;
		public String picture	= "";
		public String format	= "";
		public String defvalue  = "";
		public String value 	= "";
		public String depends	= "";
		public int offset 		= 0;
		public int bytes 		= 0;
		public int size 		= 0;
		public int[] occurs	= new int[]{0,0};
		
		public COBOLVariable(String varLine) {
			if ((varLine == null) || (varLine.equals("")))
				return;
			line = varLine;
		}
		
		public XMLVector<Object> toXMLVector() {
			XMLVector<Object> v = new XMLVector<Object>();
			
			v.add(name);
			v.add(new Integer(offset));
			v.add(new Integer(bytes));
			v.add(format);
			v.add(picture);
			
			return v;
		}
		
		public XMLVector<Object> toXMLVector2() {
			XMLVector<Object> v = new XMLVector<Object>();
			
			v.add(level);
			name = (Character.isDigit(name.charAt(0)) ? "_":"") + name;
			v.add(name.replaceAll("-","_"));
			if (occurs[1]>0) {
				if (!depends.equalsIgnoreCase(""))
					v.add(""+ occurs[0] + ":" + occurs[1] + ":" + depends.replaceAll("-","_"));
				else
					v.add(""+ occurs[0] + ":" + occurs[1]);
			}
			else if (occurs[0]>0)
				v.add(""+ occurs[0]);
			else
				v.add("");
			//v.addElement(new Integer(offset));
			v.add(new Integer(bytes));
			v.add(format);
			v.add(picture);
			v.add(defvalue);
			
			return v;
		}

		public String toString() {
			String s  = "";
			s += "level=" + level;
			s += " name=" + name;
			s += " occurs=["+ occurs[0] + ":" + occurs[1] + "]";
			//s += " offset=" + offset;
			s += " bytes=" + bytes;
			s += " format=" + format;
			s += " picture=" + picture;
			s += " value=" + defvalue;
			return s;
		}
	}

	public static void main(String[] args) {
		try {
			String strFilePath = null;
			
			if (args != null) {
				if (args.length > 0) {
					strFilePath = args[0];
				}
			}
		
			//strFilePath = "C:/Projects/dev/convertigo/tomcat/webapps/convertigo/copybook/tbd/pg01.cbl";
			//strFilePath = "C:/Projects/dev/convertigo/tomcat/webapps/convertigo/copybook/tbd/cicseci.cbl";
			//strFilePath = "C:/Projects/dev/convertigo/tomcat/webapps/convertigo/copybook/samcomm.cob";
			//strFilePath = "C:/Projects/Canon/CO242502.txt";
			//strFilePath = "C:/Projects/Canon/CO242702.txt";
			if (strFilePath != null) {
				//System.out.println("Parsing copybook '"+ strFilePath +"'");
				FileReader reader = new FileReader(strFilePath);
				Copybook cpbk = new Copybook();
				
				//XMLVector v = cpbk.importFromFile(reader);
				
				XMLVector<XMLVector<Object>> v2 = cpbk.importFromFile2(reader);
				
				Element elt = cpbk.addLeveledXMLVector(null, null, v2);
				if (elt != null) {
					Document doc = elt.getOwnerDocument();
					String s = XMLUtils.prettyPrintDOM(doc);
					System.out.println(s);				
				}
			}
		}
		catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
		finally {
			System.exit(0);
		}
	}
}
