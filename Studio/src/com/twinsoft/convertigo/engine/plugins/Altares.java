/*
 * Copyright (c) 2001-2011 Convertigo SA. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of Convertigo SA.
 * The program(s) may  be used  and/or copied  only with the written
 * permission  of  Convertigo SA or in accordance with the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * Convertigo  makes  no  representations  or  warranties  about the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness for a particular purpose, or non-infringement. Convertigo
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */

/*
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.engine.plugins;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public class Altares {
	
	private static Properties cacheProperties = null;
	private Properties prop;
	
	public Altares() {
		prop 			= null;
	}
	
	public Properties getProp() {
		return prop;
	}

	public void setProp(Properties prop) {
		this.prop = prop;
	}
	
	public void setProp(Context context) {
		String propertiesFileName 	= context.getProjectDirectory() + "/biller.properties";
		Properties property			= new Properties();
		try {
			property.load(new FileInputStream(propertiesFileName));
		} catch (Exception e) {
			Engine.logEngine.debug("[Altares] Billing aborted because an exception occured while loading properties : " + e.getMessage() + ".");
			property = null;
		}
		setProp(property);
	}
	
	public boolean isResponseFromCache(Context context) {
		Document document 		= context.outputDocument;
		Element documentElement = document.getDocumentElement();
		String fromCache 		= documentElement.getAttribute("fromcache");
		if ("true".equalsIgnoreCase(fromCache)) {
			Engine.logEngine.debug("[Altares] Billing aborted because the response was in cache.");
			return true;
		}
		return false;
	}
	
	public boolean isResponseCorrect(Context context) {
		// Retrieve 'correct' node value
		Element root = context.outputDocument.getDocumentElement();
		NodeList list = root.getElementsByTagNameNS("http://response.callisto.newsys.altares.fr/xsd", "correct");
		if (list.getLength()>0)
			return ((Element)list.item(0)).getTextContent().equalsIgnoreCase("true");
		return false;
	}
	
	public String getResponseParameterValue(Context context, String paramName) {
		// Retrieve 'parameters' node
		Element root = context.outputDocument.getDocumentElement();
		NodeList list = root.getElementsByTagNameNS("http://response.callisto.newsys.altares.fr/xsd", "parametres");
		if (list.getLength()>0) {
			Element parameters = (Element)list.item(0);
			// Retrieve 'parameter' nodes
			list = parameters.getElementsByTagNameNS("http://vo.callisto.newsys.altares.fr/xsd", "parametre");
			for (int i=0; i<list.getLength(); i++) {
				Element parameter = (Element)list.item(i);
				try {
					// Retrieve 'nom' nodes
					String name = ((Element)parameter.getElementsByTagNameNS("http://vo.callisto.newsys.altares.fr/xsd", "nom").item(0)).getTextContent();
					if (paramName.equalsIgnoreCase(name))
						// Return 'value' nodes
						return ((Element)parameter.getElementsByTagNameNS("http://vo.callisto.newsys.altares.fr/xsd", "valeur").item(0)).getTextContent();
				}
				catch (Exception e) {}
			}
		}
		return "";
	}
	
	public double getCost(Context context) {
		if (prop == null) setProp(context);
		
		String pkey = context.projectName.indexOf("AltaresIdentite")!=-1 ? "altares.identite":"altares.risque";
		double moduleCost = Double.parseDouble(prop.getProperty("cost."+ pkey + "."+ context.transactionName,"0.0"));
		double pointCost = Double.parseDouble(prop.getProperty("cost."+ pkey+ ".point","1.0"));
		double cost = (moduleCost * pointCost);
		
		BigDecimal bd = new BigDecimal(cost);
		bd = bd.setScale(2,BigDecimal.ROUND_HALF_UP);

		double responseCost = bd.doubleValue();
		return responseCost;
	}
	
	protected static void initCacheProperties(String projectDirectory) {
		if (cacheProperties == null) {
			String propertiesFileName = projectDirectory + "/cacheEntry.properties";
			cacheProperties = new Properties();
			try {
				cacheProperties.load(new FileInputStream(propertiesFileName));
				cacheProperties.setProperty("isValid", "true");
			} catch (Exception e) {
				cacheProperties.setProperty("isValid", "false");
				Engine.logEngine.debug("[Altares] Unable to retrieve \"cacheEntry.properties\" file!");
			}
		}
	}
	
	public static boolean includeVariableIntoRequestString(String projectName, TransactionWithVariables transaction, String variableName) {
		if (projectName.indexOf("Altares") == -1) {
			return true;
		}
		
		if (cacheProperties == null) initCacheProperties(transaction.context.getProjectDirectory());
		if (cacheProperties.getProperty("isValid").equals("false")) {
			Engine.logEngine.debug("[Altares] Variable added to cache request string because of invalid properties file 'cacheEntry'.");
			return true;
		}
		
		String vars = (String)cacheProperties.getProperty(transaction.getName(),"");
		StringTokenizer st = new StringTokenizer(vars, ",");
		while(st.hasMoreTokens()) {
			if (st.nextToken().equals(variableName))
				return true;
		}
		return false;
	}

	public static void handleIntuizError(Context context) {
		
		if (cacheProperties == null) initCacheProperties(context.getProjectDirectory());
		if (cacheProperties.getProperty("isValid").equals("false")) {
			Engine.logEngine.debug("[Altares] Transaction cached because of invalid properties file 'cacheEntry'.");
			return;
		}
		
		// Retrieve root element
		Element root = context.outputDocument.getDocumentElement();
		
		// Retrieve 'correct' node
		NodeList list = root.getElementsByTagNameNS("http://response.callisto.newsys.altares.fr/xsd", "correct");
		if (list.getLength()>0) {
			Element correct = (Element)list.item(0);
			boolean isResponseCorrect = correct.getTextContent().equalsIgnoreCase("true");
			
			// Response is correct
			if (isResponseCorrect) {
				Engine.logEngine.debug("[Altares] Transaction cached because no error was found.");
				return;
			}
			// An error occured
			else {
				// Retrieve 'exception' node
				list = root.getElementsByTagNameNS("http://response.callisto.newsys.altares.fr/xsd", "exception");
				if (list.getLength()>0) {
					Element exception = (Element)list.item(0);
					Element code = (Element)exception.getElementsByTagNameNS("http://vo.callisto.newsys.altares.fr/xsd", "code").item(0);
					Element erreur = (Element)exception.getElementsByTagNameNS("http://vo.callisto.newsys.altares.fr/xsd", "erreur").item(0);
					Element description = (Element)exception.getElementsByTagNameNS("http://vo.callisto.newsys.altares.fr/xsd", "description").item(0);
					String errMsg = "[code: "+code.getTextContent()+", erreur: "+erreur.getTextContent()+", description: "+description.getTextContent()+"]";
					Engine.logEngine.debug("[Altares] Transaction not cached because following error has been found: "+ errMsg);
				}
				else
					Engine.logEngine.debug("[Altares] Transaction not cached because 'exception' node wasn't found.");
			}
		}
		else {
			Engine.logEngine.debug("[Altares] Transaction not cached because 'correct' node wasn't found.");
		}
		
		context.isCacheEnabled = false;
	}
	
	public static void handleErrorMessages(Context context) throws TransformerException {
		
	}
}
