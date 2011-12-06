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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.apache.log4j.Logger;
import org.dom4j.io.DocumentSource;
import org.w3c.dom.Document;

import com.twinsoft.util.Log;

public class SOAPUtils {

	/**
	 * SOAP to DOM transformer.
	 */
	public static Object getDOM(SOAPPart soapPart) throws TransformerException, SOAPException
	{
		Object ob = null;
		
		Source sc = soapPart.getContent();
		
		if (sc instanceof DocumentSource) {
			ob = ((DocumentSource) sc).getDocument();
		}
		else {
			// Create a transformer
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();

			// Create an instance of Result for a DOM tree
			DOMResult domResult = new DOMResult();

			// Transform !!
			transformer.transform(sc, domResult);
			ob = (Document)domResult.getNode();
		}

		return ob;
	}
	
	public static String toString(SOAPMessage soapMessage, String encoding) throws TransformerConfigurationException, TransformerException, SOAPException, IOException {		
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		soapMessage.writeTo(out);
        
        String s = new String(out.toByteArray(), "UTF-8"); 
        
		return s;
    }
    
    public static String writeSoapFault(Throwable e, Logger log) throws IOException {
		String faultString;
		if (e instanceof SOAPException) {
			Throwable cause = ((SOAPException) e).getCause();
			log.error("A SOAP error has occured while processing the web service request.", e);
			log.error("Cause:", cause);
			faultString = "A SOAP error has occured while processing the web service request: " + cause.getMessage();
		}
		else {
			log.error("Unable to analyze or execute the web service.", e);
			faultString = "Unable to analyze or execute the web service.";
		}
		
		try {
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage faultMessage = messageFactory.createMessage();
		
			SOAPPart sp = faultMessage.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			SOAPBody sb = se.getBody();
			SOAPFault fault = sb.addFault();
	
			fault.setFaultString(faultString);
			if (System.getProperty("java.specification.version").compareTo("1.6") < 0) {
				fault.setFaultCode("server");
			}
			else {
				Name qname = se.createName("Server", null,javax.xml.soap.SOAPConstants.URI_NS_SOAP_ENVELOPE);			
	            Method m;
				try {
					m = fault.getClass().getMethod("setFaultCode", new Class[] {Name.class});
		            m.invoke(fault, new Object[]{qname});
				} catch (Exception ex) {}
			}
			
			Detail detail = fault.addDetail();
			SOAPFactory soapFactory = SOAPFactory.newInstance();
	
			Name name;
			DetailEntry detailEntry;
			
			String faultDetail = e.getMessage();
			if (faultDetail == null) faultDetail = "";
	
			name = soapFactory.createName(e.getClass().getName());
			detailEntry = detail.addDetailEntry(name);
			detailEntry.addTextNode(faultDetail == null ? "(no more information)" : faultDetail);
			
			if (System.getProperty("java.specification.version").compareTo("1.4") >= 0) {
				Throwable eCause = e;
				while ((eCause = eCause.getCause()) != null)  {
					faultDetail = eCause.getMessage();
					name = soapFactory.createName(eCause.getClass().getName());
					detailEntry = detail.addDetailEntry(name);
					detailEntry.addTextNode(faultDetail == null ? "(no more information)" : faultDetail);
				}
			}
	
			name = soapFactory.createName("moreinfo");
			detailEntry = detail.addDetailEntry(name);
			detailEntry.addTextNode("See the Convertigo engine log files for more details...");
			
			String sResponseMessage = SOAPUtils.toString(faultMessage,"UTF-8");
			
			if (log.isDebugEnabled()) {
				log.debug("SOAP response:\n" + sResponseMessage);
			}
			
			return sResponseMessage;

		}
		catch(Throwable ee) {
			log.error("Unable to send the SOAP FAULT message.", ee);
			String response = "";
			response += Log.getStackTrace(ee);
			response += "SOAP error: " + faultString + "\n";
			response += "Initial exception:\n";
			response += Log.getStackTrace(e);
			if (e instanceof SOAPException) {
				Throwable cause = ((SOAPException) e).getCause();
				response += "SOAP Cause:\n";
				response += Log.getStackTrace(cause);
			}
			return response;
		}
	}
	
}

///**
//* DOM to String transformer.
//* @throws SOAPException 
//* @throws IOException 
//*/
//public static String toString(SOAPPart soapPart, String encoding) throws TransformerConfigurationException, TransformerException, SOAPException, IOException {
//	String s = null;
//	
//	Object ob = SOAPUtils.getDOM(soapPart);
//	
//	if (ob instanceof Document) {
//		Document doc = (Document)ob;
//		
//	    TransformerFactory tfactory = TransformerFactory.newInstance();
//	    StringWriter writer = null;
//	
//	    Transformer serializer = tfactory.newTransformer();
//	    Properties oprops = new Properties();
//	    oprops.put("method", "xml");
//	    oprops.put("indent", "yes");
//	    serializer.setOutputProperties(oprops);
//	    writer = new StringWriter();
//	    serializer.transform(new DOMSource(doc), new StreamResult(writer));
//	
//	    s = writer.toString();
//	}
//	else {
//		OutputFormat format = new OutputFormat();
//		format.setIndent(true);
//		format.setIndentSize(4);
//		format.setNewlines(true);
//		format.setTrimText(true);
//		format.setEncoding(encoding);
//		
//		StringWriter sw = new StringWriter();
//		XMLWriter writer = new XMLWriter(sw, format);
//		writer.write(ob);
//		writer.close();
//		s = sw.toString();
//	}
//	
//	return s;
//}