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
import java.io.OutputStream;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.engine.print.PrintStatus;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;


public abstract class ConvertigoPrint {

	protected String outputMime;
	protected String projectName;
	protected String extension;
	protected String localizedDir;
	protected String outputFileName;
	protected OutputStream out = null;
	protected File outputFile;
	protected String outputFolder;
	protected PrintStatus status=null;
	private String product="";


	public ConvertigoPrint(String projectName) {
		this.projectName = projectName;
	}

	public ConvertigoPrint(String projectName, PrintStatus status) {
		this(projectName);
		this.status = status;
	}


	protected void updateStatus(String message, int value) {
		if (status != null) {
			status.set(value, message);
		}
	}

	protected void updateStatus(int value) {
		if (status!=null) {
			status.setStatus(value);
		}
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String print(String location) throws IOException, EngineException, SAXException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException {
		try {	
			//create the result repository
			outputFolder = location + "/ConvertigoPrint";
			File repository = new File(outputFolder);
			if (!repository.exists()) {
				repository.mkdir();
			}
			
			outputFileName = outputFolder + "\\" + projectName + extension;
			outputFile = new File(outputFileName);
			updateStatus("Setting up printing",20);
			
			// Construct/Configure a FopFactory
			FopFactory fopFactory = FopFactory.newInstance();
			fopFactory.setBaseURL(Engine.TEMPLATES_PATH + "/doc/");
			fopFactory.setStrictValidation(false);
			
			// Configure foUserAgent as desired
			FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
			foUserAgent.setBaseURL(Engine.TEMPLATES_PATH + "/doc/");
			foUserAgent.setAuthor("Convertigo EMS");
			
			// Setup output
			out = new FileOutputStream(outputFile);
			out = new java.io.BufferedOutputStream(out);
			
			// Construct fop with desired output format
			Fop fop = fopFactory.newFop(outputMime, foUserAgent, out);
			
			updateStatus("Analysing project",40);
			
			//Transform the project into a source
			Document document = XMLUtils.createDom("java");
			Element convertigoElement = document.createElement("convertigo");
			convertigoElement.setAttribute("exported", new Date().toString());
			convertigoElement.setAttribute("studio", product);
			convertigoElement.setAttribute("engine", com.twinsoft.convertigo.engine.Version.version);
			convertigoElement.setAttribute("beans", com.twinsoft.convertigo.beans.Version.version);
			//work around to let the external-graphics tag to read relative path
			//TODO configure fopFactory or/and foUserAgent
			convertigoElement.setAttribute("path", Engine.TEMPLATES_PATH+"/doc/");
			document.appendChild(convertigoElement);
			ProjectUtils.getFullProjectDOM(document, projectName, ExportOption.bIncludeDisplayName, ExportOption.bIncludeCompiledValue);
			Source src=new DOMSource(document);
			
			updateStatus("Printing in progress",60);
			
			// Setup XSLT
			String templateFileName = Engine.TEMPLATES_PATH + "/doc/doc.fo.xml";
			File foFile = new File(templateFileName);
			localizedDir = Engine.TEMPLATES_PATH+ "/doc/";
			Source xsltSrc = new StreamSource(new FileInputStream(foFile), localizedDir);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer(xsltSrc);
			
			updateStatus(70);
			
			// Resulting SAX events (the generated FO) must be piped through to FOP
			DefaultHandler fopHandler=fop.getDefaultHandler();
			Result res = new SAXResult(fopHandler);
			
			// Start XSLT transformation and FOP processing
			transformer.transform(src,res);
			
			return outputFileName;		
		} finally {							
			if (out != null) {
				out.close();
			}
		}		
	}
}