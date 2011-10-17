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

package com.twinsoft.convertigo.engine.admin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



public class ManipuleFicAndDOM {
	

public static String insertSubstring(String stringBase,String substring, int NumberOfChar){
	String result=stringBase.substring(0,NumberOfChar );
	result+=substring;
	result+=stringBase.substring(NumberOfChar );
	return result;
}

public static ArrayList<String> regexResult(String theString, String regex){		
	Pattern p = Pattern.compile(regex);	
	Matcher m = p.matcher(theString);	
	ArrayList<String> result=new ArrayList<String>();
	while(m.find()){
		result.add(m.group(0));
	}
	return result;
	
}
	
	
public static Document getFileAsDom(String source) throws Exception{		
		
		File xmlFile = new File(source);		
		if (!xmlFile.exists()) {
			throw new Exception("le fichier source n'existe pas!");
		}		
		return parseDOM(new FileInputStream(xmlFile));		
	}

public static String getFileAsString(String source) throws Exception{		
	File file = new File(source);		
	if (!file.exists()) {
		throw new Exception("le fichier source n'existe pas!");
	}	
	String str,rep="";
	BufferedReader fichier;	
	fichier = new BufferedReader(new FileReader(file));
	while ((str = fichier.readLine()) != null) {
		rep+=str+"\n";
	}
	return rep;
}

public static String getFileAsString2(String  source) throws Exception{	
	
	String str,rep="";
	InputStream is=getInputStream(source);
	InputStreamReader isr=new InputStreamReader(is);
	BufferedReader fichier= new BufferedReader(isr);
	
	while ((str = fichier.readLine()) != null) {
		rep+=str+"\n";
	}
	return rep;
}

protected static InputStream getInputStream(String source) throws Exception{

	InputStream is=ManipuleFicAndDOM.class.getResourceAsStream(source);
	if(is==null){
		throw new Exception("fichier introuvable : "+source);
	}
	return is;
}

public static void setDomAsFile(Document doc,String destination) throws Exception{
	setDomAsFile(doc, destination,"UTF-8");
}

public static void setDomAsFile(Document doc,String destination,String encoding) throws Exception{		
	OutputStreamWriter sortie = new OutputStreamWriter(new FileOutputStream(destination) ,encoding); 
	sortie.write(prettyPrintDOMWithEncoding(doc, encoding));			
	sortie.close();	
}

public static void setStringAsFile(String doc,String destination,String encoding) throws Exception{		
	OutputStreamWriter sortie = new OutputStreamWriter(new FileOutputStream(destination) ,encoding); 
	sortie.write(doc);			
	sortie.close();	
}

public static void setStringAsFile(String doc,String destination) throws Exception{		
	setStringAsFile(doc,destination,"UTF-8");
}

public static boolean createOrReplaceFile(String source) throws Exception{
	boolean rep=false;
	File file = new File(source);
	file.createNewFile();
	if (file.exists()) {
		setStringAsFile("", source);
		rep=true;
	}	
	return rep;
}

public static boolean testfile(String source){
	boolean rep=false;
	File file = new File(source);
	if (file.exists()) {		
		rep=true;
	}	
	return rep;
}


public static Document getStringAsDom(String entry) throws SAXException, IOException, ParserConfigurationException{	
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		return documentBuilder.parse(new InputSource(new StringReader(entry)));
	
}

public static Document getStringWithEnglobingRootAsDom(String entry,String root) throws SAXException, IOException, ParserConfigurationException{			
		
	return getStringAsDom("<"+root+">"+entry+"</"+root+">");	
}

//transforme un XML avec un tableau de remplacement de valeur (taille [n] [2]) 
public static String transformXML(String[][] tab,String xml){
	return transformXML(tab,xml,false);
}

//transforme un XML avec un tableau de remplacement de valeur (taille [n] [2]) 
public static String transformXML(String[][] tab,String xml,boolean first){
	//System.out.println(xml);
	for(int i=0;i<tab.length;i++){
		if(first){
			xml=xml.replaceFirst(tab[i][0], tab[i][1]);
			
		}
		else
			xml=xml.replaceAll(tab[i][0], tab[i][1]);
	}
	//System.out.println(xml);
	return xml;
}

public static String prettyPrintDOMWithEncoding(Document doc) {
	return prettyPrintDOMWithEncoding(doc,"UTF-8");
}
public static String prettyPrintDOMWithEncodingTrim(Document doc) {
	return prettyPrintDOMWithEncodingTrim(doc,"UTF-8");
}

public static String prettyPrintDOMWithEncodingTrim(Document doc, String encoding) {
	// TODO Auto-generated method stub
	return prettyPrintDOMWithEncoding(doc,"UTF-8").replaceAll("[\\f\\n\\r\\t\\v]", "");
}

//################################################################
//########       FONCTIONS TIREES DE XMLUTILS              #######
//################################################################

////////tiré de XMLUtils de convertigo
static public Document parseDOM(InputStream is) throws SAXException, IOException, ParserConfigurationException{
	/*
	 * DocumentBuilder has better performance than DOMParser and Transformer
	 * and define features save a lot of time
	 */
	DocumentBuilder bd;

	try{
		DocumentBuilderFactory bdf =  DocumentBuilderFactory.newInstance();
		bdf.setFeature("http://xml.org/sax/features/external-general-entities", false);
		bdf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		bdf.setFeature("http://xml.org/sax/features/validation", false);
		bdf.setFeature("http://xml.org/sax/features/namespaces", false);
		bdf.setFeature("http://apache.org/xml/features/validation/schema", false);
		bdf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		bdf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",false);
		bdf.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
		bd = bdf.newDocumentBuilder();
	}catch (ParserConfigurationException e) {
		
			bd = DocumentBuilderFactory.newInstance().newDocumentBuilder();				
		
	}
	Document doc = bd.parse(is);
	//doc.normalizeDocument();
	return doc;
}




////////tiré de XMLUtils de convertigo
 public static String prettyPrintDOMWithEncoding(Document doc, String defaultEncoding) {
		Node firstChild = doc.getFirstChild();
		boolean omitXMLDeclaration = false;
		String	encoding = defaultEncoding; // default Encoding char set if non is found in the PI
		
		if ( (firstChild.getNodeType() == Document.PROCESSING_INSTRUCTION_NODE)
		  && (firstChild.getNodeName().equals("xml"))) {
			omitXMLDeclaration = true;
			String piValue = firstChild.getNodeValue();
			// extract from PI the encoding Char Set
			int	encodingOffset = piValue.indexOf("encoding=\"");
			if (encodingOffset != -1) {
				encoding = piValue.substring(encodingOffset+10);
				// remove the last "
				encoding = encoding.substring(0, encoding.length()-1);
			}
		}
		StringWriter strWtr = new StringWriter();
		TransformerFactory tfac = TransformerFactory.newInstance();
		try {
			Transformer t = tfac.newTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, encoding);
			t.setOutputProperty(OutputKeys.INDENT, "no");
			t.setOutputProperty(OutputKeys.METHOD, "xml"); //xml, html, text
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration? "yes":"no");
			t.transform(new DOMSource(doc), new StreamResult(strWtr));
			
			return strWtr.getBuffer().toString();
		} catch (Exception e) {
			System.err.println("XML.toString(Document): " + e);
			return e.getMessage();
		}
		
    }
 
 public static String parseSring(String entry){
	 return entry==null?null:entry.replaceAll("'", "\\\\'");
 }

 
 //for tests
 public static void main(String[] args) throws Exception{
	/* String xml="<class>WebNote</class><class>WebNote</class>";
		String[][] tab2=new String[1][2];
		tab2[0][0]="<class>WebNote</class>";
		tab2[0][1]="<class>WebNote"+"aa"+"</class>";
	 xml=transformXML(tab2, xml,true);
	 System.out.println(xml);*/
	 
	 /*String histoire="c'est l'histoire d'un navet qui s'appelait scrountch ";
	 histoire=insertSubstring(histoire,"\"toto\", il traverse la route et ... ", histoire.lastIndexOf("ait")+4);
	 System.out.println(histoire);*/
	 if(createOrReplaceFile("C:\\toto2.txt")){
		 System.out.println("file exist");
	 }
	 else
		 System.out.println("file not exist");
	 
 }
	
	


}
