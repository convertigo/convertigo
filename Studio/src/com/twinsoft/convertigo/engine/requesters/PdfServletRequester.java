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

package com.twinsoft.convertigo.engine.requesters;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;

public class PdfServletRequester extends ServletRequester {

	private FopFactory fopFactory;
	
	public void preGetDocument() {
		super.preGetDocument();
		context.isXsltRequest = true;
	}

	public PdfServletRequester(FopFactory fopFactory) {
		this.fopFactory = fopFactory;
    }

    public String getName() {
        return "PdfServletRequester";
    }

    public void setStyleSheet(Document document) {
        // Nothing to do
    }

	protected Object performXSLT(Document document) throws Exception {
		String t1 = context.statistics.start(EngineStatistics.XSLT);

        try {
            Engine.logEngine.debug("XSL/FO process is beginning");
            
        	Object result = null;
        	
            Engine.logEngine.debug("Sheet absolute URL: " + context.absoluteSheetUrl);
    		if (context.absoluteSheetUrl == null) throw new EngineException("You have required an XSL/FO process, but Convertigo has been unable to find a stylesheet for your request. Verify your project's settings.");

    		// XSL/FO engine
    		StreamSource streamSource = null;
    		try {
	        	// Configure foUserAgent as desired
	        	FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
	        	foUserAgent.setBaseURL(new File(Engine.PROJECTS_PATH + "/" + context.projectName).toURI().toASCIIString());
	        	foUserAgent.setAuthor("Convertigo EMS");
	        	
	        	// Setup output
	        	org.apache.commons.io.output.ByteArrayOutputStream out = new org.apache.commons.io.output.ByteArrayOutputStream();
	        	
				// Construct fop with desired output format
				Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);
				
				// Setup XSLT
				streamSource = new StreamSource(new File(context.absoluteSheetUrl).toURI().toASCIIString());
				TransformerFactory factory = TransformerFactory.newInstance();
				Transformer transformer = factory.newTransformer(streamSource);
				
				// Setup input for XSLT transformation
				Element element = document.getDocumentElement();
				DOMSource src = new DOMSource(element);
				
				// Resulting SAX events (the generated FO) must be piped through to FOP
				Result res = new SAXResult(fop.getDefaultHandler());
				
				// Start XSLT transformation and FOP processing
				transformer.transform(src, res);
				
				result = out.toByteArray();
            }
    		finally {
    			if (streamSource != null) {
    				InputStream inputStream = streamSource.getInputStream();
    				if (inputStream != null) inputStream.close();
    			}
    		}
    		
            Engine.logEngine.trace("XSLT result:\n" + result);
            
            return result;
        }
        finally {
            Engine.logEngine.debug("XSLT process has finished");
			context.statistics.stop(t1);
        }
	}
	
	protected Object addStatisticsAsData(Object result) { 
		return result; 
	} 
	
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{ 
		return result; 
	} 
}
