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

package com.twinsoft.convertigo.eclipse.wizards.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.eclipse.wizards.enums.MobileFeature;
import com.twinsoft.convertigo.eclipse.wizards.enums.MobileLook;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class MobileUtils {
	private static String pathDom;
	private static String path;
	private static Document document;
	
	public MobileUtils(String path, Boolean isStructured) throws SAXException, IOException {
		if (isStructured == true) {
			MobileUtils.pathDom = path;
			document = XMLUtils.documentBuilderDefault.parse(new InputSource(path));
		}
		else {
			MobileUtils.path = path;
		}
	}
	
	public void removeSrc(MobileFeature feature) throws IOException, SAXException, ParserConfigurationException {
		NodeList scripts = document.getElementsByTagName("script");
		for(int i=0; i < scripts.getLength(); i++){
			Element e = (Element)scripts.item(i);
			if(e.getAttribute("src").equalsIgnoreCase("sources/" + feature.fileName())) {
				e.getParentNode().removeChild(e);	
			}
		}	
	}
	
	public void removeCss(String css) {
		NodeList links = document.getElementsByTagName("link");
		for(int i=0; i < links.getLength(); i++){
			Element e = (Element)links.item(i);
			if(e.getAttribute("href").equalsIgnoreCase("css/" + css)) {
				e.getParentNode().removeChild(e);	
			}
		}	
	}
	
	public int getLook() {
		boolean hasAndroid = false;
		boolean hasBb = false;
		boolean hasIos = false;
		boolean hasSencha = false;
		int look = -1;
		
		NodeList links = document.getElementsByTagName("link");
		for(int i=0; i < links.getLength(); i++){
			Element e = (Element)links.item(i);
			if(e.getAttribute("href").equalsIgnoreCase("css/" + MobileLook.ANDROID.fileName())) {
				hasAndroid = true;
				look = MobileLook.ANDROID.index();
			}
			if(e.getAttribute("href").equalsIgnoreCase("css/" + MobileLook.BB.fileName())) {
				hasBb = true;
				look = MobileLook.BB.index();
			}
			if(e.getAttribute("href").equalsIgnoreCase("css/" + MobileLook.IOS.fileName())) {
				hasIos = true;
				look = MobileLook.IOS.index();
			}
			if(e.getAttribute("href").equalsIgnoreCase("css/" + MobileLook.SENCHA.fileName())) {
				hasSencha = true;
				look = MobileLook.SENCHA.index();
			}
		}
		if (hasAndroid && hasBb && hasIos && hasSencha)
			look = MobileLook.AUTO.index();
		
		return look;
	}

	public void printDOM() {
        try {
        	document.normalize();
            Source source = new DOMSource(document);
            Result result = new StreamResult(pathDom);
            
            TransformerFactory fabrique = TransformerFactory.newInstance();
            Transformer transformer = fabrique.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            
            transformer.transform(source, result);
        }catch(Exception e){
        	e.printStackTrace();	
        }
    }
	
	public void print(String string) throws IOException {
		FileWriter writer = null;
		String txt = string;
		try{
		     writer = new FileWriter(path);
		     writer.write(txt);
		}catch(IOException ex){
		    ex.printStackTrace();
		}finally{
		  if(writer != null){
		     writer.close();
		  }	
		}
	}
	
	
	public String getContent() {	
	    StringBuilder contents = new StringBuilder();   
	    try {
	      BufferedReader input =  new BufferedReader(new FileReader(path));
	      try {
	        String line = null; 

	        while (( line = input.readLine()) != null){
	          contents.append(line);
	          contents.append(System.getProperty("line.separator"));
	        }
	      }
	      finally {
	        input.close();
	      }
	    }
	    catch (IOException ex){
	      ex.printStackTrace();
	    }
	    
	    return contents.toString();
	  }
		
	public static void unzipSencha(String path, String dest, List<String> files) throws FileNotFoundException, IOException{
	    ZipInputStream zis = new ZipInputStream(
	            new BufferedInputStream(new FileInputStream(path)));
	    		ZipEntry ze = null;
	  
	    String fileName = null;
	    try {
	        while(((ze = zis.getNextEntry()) != null)){
	        	fileName = ze.getName().replaceAll(".*/","");
	        	
	        	//Renaming in order to handle BlackBerry.
	        	fileName = fileName.replaceAll("-", "");
	        	
	            File f = new File(dest, fileName);
	
	            if (ze.isDirectory()) {
	                continue;
	            }
	            
	            for (String s : files) {
	            	if (s.equalsIgnoreCase(fileName)) {
	            		f.getParentFile().mkdirs();
		                OutputStream fos = new BufferedOutputStream(
		                        new FileOutputStream(f));
		                try {
		                    try {
		                        final byte[] buf = new byte[8192];
		                        int bytesRead;
		                        while (-1 != (bytesRead = zis.read(buf)))
		                            fos.write(buf, 0, bytesRead);
		                    }
		                    finally {
		                        fos.close();
		                    }
		                }
		                catch (final IOException ioe) {
		                    f.delete();
		                    throw ioe;
		                }
	            	} 	
	            }
	        }
	    }
	    finally {
	        zis.close();
	    }
	}
}
