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

package com.twinsoft.convertigo.engine.print;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.MimeConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.engine.print.ConvertigoPrint;
import com.twinsoft.convertigo.engine.print.PrintStatus;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class PrintHTML extends ConvertigoPrint{
	
	public PrintHTML(String projectName){
		super(projectName);	
		init();
	}
	
	public PrintHTML(String projectName,PrintStatus status){
		super(projectName,status);	
		init();
	}

	private void init(){
		outputMime=MimeConstants.MIME_FOP_AREA_TREE;
		extension=".tmp.xml";
	}
	
	@Override
	public String print(String location) throws IOException, EngineException, SAXException, TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException{
		super.print(location);
		out.close();
		
		updateStatus("Create Ressources Directory",70);
		
		//get the dom
		Document fopResult=XMLUtils.parseDOM(outputFile);						
		Element root = fopResult.getDocumentElement();
		String templateFileName = Engine.TEMPLATES_PATH + "/doc/doc.html.xsl";
		File htmlFile = new File(templateFileName);						
		Source xsltSrc = new StreamSource(new FileInputStream(htmlFile), localizedDir);
		
		//create the ressources repository					
		String ressourcesFolder=outputFolder+"/ressources";
		File repository=new File(ressourcesFolder);
		if(!repository.exists()){
			repository.mkdir();
		}	
							
		//export images
		NodeList images=fopResult.getElementsByTagName("image");
		Node image;	
		String attrImg,attrImgName;
		InputStream imagesIn;
		OutputStream imagesOut;		
		for(int  i=0;i<images.getLength();i++){
			image=images.item(i);					
			attrImg=image.getAttributes().getNamedItem("url").getTextContent();							
			attrImgName=attrImg.replaceAll("(.*)/", "");						
			image.getAttributes().getNamedItem("url").setTextContent(attrImgName);		
			imagesIn = new FileInputStream(attrImg);
			imagesOut = new FileOutputStream(ressourcesFolder+"/"+attrImgName);		 
			org.apache.commons.io.IOUtils.copy(imagesIn,imagesOut);						
			imagesIn.close();
			imagesOut.close();
		}	
		
		//export css
		FileInputStream cssIn = new FileInputStream(Engine.TEMPLATES_PATH + "/doc/style.css");
		FileOutputStream cssOut = new FileOutputStream(ressourcesFolder+"/style.css");		 
		org.apache.commons.io.IOUtils.copy(cssIn,cssOut);		
		cssIn.close();
		cssOut.close();
		
		updateStatus("HTML Transformation",85);
		
		// transformation of the dom
		Transformer xslt = TransformerFactory.newInstance().newTransformer(xsltSrc);				
		Element xsl = fopResult.createElement("xsl");
		xslt.transform(new DOMSource(fopResult), new DOMResult(xsl));			
		fopResult.removeChild(root);
		fopResult.appendChild(xsl.getFirstChild());					
							
		//write the dom
		String newOutputFileName = outputFolder
		+"/"+ projectName + ".html";			
		outputFile = new File(newOutputFileName);
		out = new FileOutputStream(outputFile);
		out = new java.io.BufferedOutputStream(out);					
		OutputStreamWriter output = new OutputStreamWriter(out); 
		output.write(XMLUtils.prettyPrintDOM(fopResult));                       
		output.close();  
		
		//remove the temp file
		new File(outputFileName).delete();
		
		updateStatus("Printing finished",100);
		
		return newOutputFileName;	
	}
}
