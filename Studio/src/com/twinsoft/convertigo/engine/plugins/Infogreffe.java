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

package com.twinsoft.convertigo.engine.plugins;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class Infogreffe {
	private static Properties cacheProperties = null;
	private Properties prop;
	protected List<String> errorCodes;
	protected List<String> errorsHandling;
	
	public Infogreffe() {
		prop 			= null;
		errorCodes 		= null;
		errorsHandling 	= null;
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
				Engine.logEngine.debug("[Infogreffe] Unable to retrieve \"cacheEntry.properties\" file!", null);
			}
		}
	}
	
	public static boolean includeVariableIntoRequestString(String projectName, TransactionWithVariables transaction, String variableName) {
		if (projectName.indexOf("InfoGreffe") == -1) {
			return true;
		}
		
		if (cacheProperties == null) initCacheProperties(transaction.context.getProjectDirectory());
		if (cacheProperties.getProperty("isValid").equals("false")) {
			Engine.logEngine.debug("[Infogreffe] Variable added to cache request string because of invalid properties file 'cacheEntry'.");
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

	public static void handleInfogreffeError(Context context) {
		
		if (cacheProperties == null) initCacheProperties(context.getProjectDirectory());
		if (cacheProperties.getProperty("isValid").equals("false")) {
			Engine.logEngine.debug("[Infogreffe] Transaction cached because of invalid properties file 'cacheEntry'.");
			return;
		}
		
		// extracting error codes
		String myXPath = "//return/text()";
		NodeList messageElements;
		try {
			messageElements = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), myXPath);
		} catch (TransformerException e) {
			Engine.logEngine.debug("[Infogreffe] Transaction cached because no text node was found under 'return' in the response dom.");
			return;
		}
		String messages = "";
		for (int i = 0 ; i < messageElements.getLength() ; i ++) {
			Text elem = (Text)messageElements.item(i);
			String value = elem.getNodeValue();
			if (!value.equalsIgnoreCase("\n")) {
				messages += value;
				Engine.logEngine.trace("[Infogreffe] Error found : '" + elem.getNodeValue() + "'.");
			}
		}
		// checking error codes : if one implies the transaction is not cachable
		// ==> set value context.isCacheEnabled to false and return
		StringTokenizer st 	= new StringTokenizer(messages, "-");
		
		if (st.countTokens() == 0) {
			Engine.logEngine.debug("[Infogreffe] Transaction cached because no error code was found under 'return' in the response dom.");
			return;
		}
		
		while(st.hasMoreTokens()) {
			String tmp = st.nextToken().trim();
			try {
				Integer.parseInt(tmp);
				// reach this point : the token treated is an error code
				// retrieving error code
				String errorCode = tmp;
				if (cacheProperties.getProperty(errorCode,"N").equals("O")) {
					Engine.logEngine.debug("[Infogreffe] Transaction cached because one error code implying cachable transaction was found under 'return' in the response dom : '" + errorCode + "'.");
					return;
				}
			} catch (NumberFormatException e) {
				// reach this point : the token treated is an error message
				// do nothing
			}
		}
		
		context.isCacheEnabled = false;
		Engine.logEngine.debug("[Infogreffe] Transaction not cached because all error codes found under 'return' in the response dom implyed not cachable transaction.");
	}
	
	public static void handleErrorMessages(Context context) throws TransformerException {
		NodeList messageElements = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), "//return/text()");
		String messages = "";
		Node elem;
		String value;
		for (int i = 0 ; i < messageElements.getLength() ; i ++) {
			elem = messageElements.item(i);
			value = elem.getNodeValue();
			if (value != "\n")
				messages += value;
		}
		StringTokenizer st = new StringTokenizer(messages, "-");
		String errorCode 	= "";
		String errorMessage = "";
		Element errorElem 	= null;

		Element errorRootElem = context.outputDocument.createElement("messages");
		
		while(st.hasMoreTokens()) {
			String tmp = st.nextToken().trim();
			try {
				Integer.parseInt(tmp);
				// reach this point : the token treated is an error code
				// retrieving error code
				errorCode = tmp;
				Engine.logEngine.debug("errorCode : '" + errorCode + "'");
				
				errorElem = context.outputDocument.createElement("error");
				errorElem.setAttribute("errorCode", errorCode);
			} catch (NumberFormatException e) {
				// reach this point : the token treated is an error message
				// retrieving error message
				errorMessage = tmp;
				Engine.logEngine.debug("errorMessage : '" + errorMessage + "'");
				
				String errormsgTmp = errorElem.getAttribute("errorMessage"); 
				if (errormsgTmp != null && !errormsgTmp.equals(""))
					errorMessage = errormsgTmp + "-" + errorMessage;
				errorElem.setAttribute("errorMessage", errorMessage);
				errorRootElem.appendChild(errorElem);
			}
		}
		
		findOrCreateInfo(context).appendChild(errorRootElem);
	}
	
	private static Element findOrCreateInfo(Context context) throws TransformerException {
		NodeList info = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), "/document/info");
		Element infoElem = null;
		if (info.getLength() == 0) {
			infoElem = context.outputDocument.createElement("info");
			context.outputDocument.getDocumentElement().appendChild(infoElem);
		} else {
			infoElem = (Element)info.item(0);
		}
		return infoElem;
	}
	
	public static Element addTextNodeUnderInfo(Context context, String tagName, String textValue) throws TransformerException {
		Element infoElem = findOrCreateInfo(context);
		Element newElement = context.outputDocument.createElement(tagName);
		if(textValue != null){
			Text textElement = context.outputDocument.createTextNode(textValue);
			newElement.appendChild(textElement);
		}
		infoElem.appendChild(newElement);
		return newElement;
	}
	
	public static String handleDate(Context context) throws TransformerException {
		// jour
		String jour = "";
		Calendar date = Calendar.getInstance();
		
		if (date.get(Calendar.DAY_OF_WEEK) == 1)
			jour = "Dimanche";
		else if (date.get(Calendar.DAY_OF_WEEK) == 2)
			jour = "Lundi";
		else if (date.get(Calendar.DAY_OF_WEEK) == 3)
			jour = "Mardi";
		else if (date.get(Calendar.DAY_OF_WEEK) == 4)
			jour = "Mercredi";
		else if (date.get(Calendar.DAY_OF_WEEK) == 5)
			jour = "Jeudi";
		else if (date.get(Calendar.DAY_OF_WEEK) == 6)
			jour = "Vendredi";
		else if (date.get(Calendar.DAY_OF_WEEK) == 7)
			jour = "Samedi";
		// date jour
		int dateJour = date.get(Calendar.DAY_OF_MONTH);
		// mois
		String mois = "";
		if (date.get(Calendar.MONTH) == 0)
			mois = "Janvier";
		else if (date.get(Calendar.MONTH) == 1)
			mois = "Février";
		else if (date.get(Calendar.MONTH) == 2)
			mois = "Mars";
		else if (date.get(Calendar.MONTH) == 3)
			mois = "Avril";
		else if (date.get(Calendar.MONTH) == 4)
			mois = "Mai";
		else if (date.get(Calendar.MONTH) == 5)
			mois = "Juin";
		else if (date.get(Calendar.MONTH) == 6)
			mois = "Juillet";
		else if (date.get(Calendar.MONTH) == 7)
			mois = "Aout";
		else if (date.get(Calendar.MONTH) == 8)
			mois = "Septembre";
		else if (date.get(Calendar.MONTH) == 9)
			mois = "Octobre";
		else if (date.get(Calendar.MONTH) == 10)
			mois = "Novembre";
		else if (date.get(Calendar.MONTH) == 11)
			mois = "Décembre"; 
		// annee
		int annee = date.get(Calendar.YEAR);
		// heure
		int heure = date.get(Calendar.HOUR_OF_DAY);
		String heureStr = "" + heure;
		if (heureStr.length() == 1)
			heureStr = "0" + heureStr;
		// minutes
		int minutes = date.get(Calendar.MINUTE);
		String minutesStr = "" + minutes;
		if (minutesStr.length() == 1)
			minutesStr = "0" + minutesStr;
		// seconde
		int secondes = date.get(Calendar.SECOND);
		String secondesStr = "" + secondes;
		if (secondesStr.length() == 1)
			secondesStr = "0" + secondesStr;
			
		String dateStr = "" + jour + " " + dateJour + " " + mois + " " + annee + " " + heureStr + ":" + minutesStr + ":" + secondesStr;
		
		addTextNodeUnderInfo(context, "dateGen"	, dateStr);
		
		return dateStr;
	}
	
	public void setErrors(Context context) {
		String myXPath = "//return/text()";
		NodeList messageElements;
		try {
			messageElements = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), myXPath);
		} catch (TransformerException e) {
			Engine.logEngine.debug("[Infogreffe] No text node was found under 'return' in the response dom.");
			errorCodes 		= null;
			errorsHandling 	= null;
			return;
		}
		String messages = "";
		for (int i = 0 ; i < messageElements.getLength() ; i ++) {
			Text elem = (Text)messageElements.item(i);
			String value = elem.getNodeValue();
			if (!value.equalsIgnoreCase("\n")) {
				messages += value;
				Engine.logEngine.trace("[Infogreffe] Error found : '" + elem.getNodeValue() + "'.");
			}
		}
		
		// fill the errorCodes vector with the error codes
		// and check the corresponding handling in the properties
		StringTokenizer st 	= new StringTokenizer(messages, "-");
		int lg 				= st.countTokens() / 2;
		errorCodes 			= new Vector<String>(lg);
		errorsHandling 		= new Vector<String>();

		while(st.hasMoreTokens()) {
			String tmp = st.nextToken().trim();
			try {
				Integer.parseInt(tmp);
				// reach this point : the token treated is an error code
				// retrieving error code and adding it to errorCodes
				String errorCode = tmp;
				errorCodes.add(errorCode);
				if (prop != null) {
					// retrieving error handling and adding it to errorsHandling if needed
					String handler = prop.getProperty(errorCode);
					if (handler != null && !handler.equalsIgnoreCase("O")) {
						errorsHandling.add(handler);
						Engine.logEngine.trace("[Infogreffe] Error handling added : '" + handler + "'.");
					}
				}
			} catch (NumberFormatException e) {
				// reach this point : the token treated is an error message
				// do nothing
			}
		}
	}
	
	public boolean isBillable(Context context) {
		// not billable if errors handling contains "N" or "n"
		if (errorsHandling.contains("N") || errorsHandling.contains("n")) {
			Engine.logEngine.debug("[Infogreffe] Billing aborted : one error at least corresponds to value 'N' in properties, which implies the transaction is not billable .");
			return false;
		}
		
		// not billable if no variables for the transaction
		TransactionWithVariables trans;
		try {
			trans = (TransactionWithVariables)context.requestedObject;
		} catch (ClassCastException e) {
			Engine.logEngine.debug("[Infogreffe] Billing aborted : no variables for the transaction '" + context.requestedObject.getName() + "'.");
			return false;
		}
		
		// not billable if need to substract every modes costs
		Object tmp = trans.getVariableValue("mode");
		if (tmp == null){
			Engine.logEngine.debug("[Infogreffe] Billing aborted : no mode requested.");
			return false;
		}
		if (tmp instanceof Vector) {
			// multivaluated
			Vector<String> modes = GenericUtils.cast(tmp);
			boolean oneCostStay = false;
			for (String mode : modes) {
				if (!errorsHandling.contains(mode))
					oneCostStay = true;
			}
			if (!oneCostStay) {
				Engine.logEngine.debug("[Infogreffe] Billing aborted : every modes costs had to be substracted because of error messages.");
				return false;
			}
		} else if (tmp instanceof String[]) {
			// multivaluated
			String[] modes = (String[]) tmp;
			boolean oneCostStay = false;
			for (String mode : modes) {
				if (!errorsHandling.contains(mode))
					oneCostStay = true;
			}
			if (!oneCostStay) {
				Engine.logEngine.debug("[Infogreffe] Billing aborted : every modes costs had to be substracted because of error messages.");
				return false;
			}
		} else if (tmp instanceof String) {
			// only one value
			String mode = (String) tmp; 
			if (errorsHandling.contains(mode)) {
				Engine.logEngine.debug("[Infogreffe] Billing aborted : every modes costs had to be substracted because of error messages.");
				return false;
			}
		} else {
			Engine.logEngine.debug("[Infogreffe] Billing aborted : mode variable value is of unknown type.");
			return false;
		}
	
		return true;
	}

	public boolean isResponseFromCache(Context context) {
		Document document 		= context.outputDocument;
		Element documentElement = document.getDocumentElement();
		String fromCache 		= documentElement.getAttribute("fromcache");
		if ("true".equalsIgnoreCase(fromCache)) {
			Engine.logEngine.debug("[Infogreffe] Billing aborted because the response was in cache.");
			return true;
		}
		return false;
	}
	
	public double getPrice(Transaction transaction) throws EngineException {
		return getPrice(transaction, null);
	}
	
	public double getPrice(Transaction transaction, Map<String, String> variables) throws EngineException {
		TransactionWithVariables trans;
		try {
			trans = (TransactionWithVariables)transaction;
		} catch (ClassCastException e) {
			throw new EngineException("[Infogreffe] Transaction with no variables are not accepted; the requested transaction is:" + transaction.getName());
		}
		
		if (transaction.getName().indexOf("Liste") != -1)
			return getListeTransactionPrice(trans, variables);
		else if (transaction.getName().equals("GetEtatEndettement"))
			return getTransactionPriceByInscription(trans, variables);
		else 
			return getStandardTransactionPrice(trans, variables);
	}
	
	private double getListeTransactionPrice(TransactionWithVariables transaction, Map<String, String> variables) {
		if (!transaction.getName().equals("GetListeBilanSaisi"))
			// other transactions are not billable ==> price 0.0
			return 0.0;
		
		if (transaction.context == null) {
			// no context  running
			if (variables != null) {
				// jsp calling before transaction execution
				return getStandardTransactionPrice(transaction, variables);
			} 
			//transaction not executed, no return XML to analyse ==> price 0.0
			return 0.0;
		}
		
		// context running
		Context context = transaction.context;
		NodeList listeBilanSaisiNodes = null;
		NodeList bilanSaisiNodes = null;
		try {
			listeBilanSaisiNodes = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), "//listeBilanSaisi");
			bilanSaisiNodes = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), "//bilanSaisi");
		} catch (TransformerException e) {
			return 0.0;
		}
		if(listeBilanSaisiNodes.getLength() == 0 && bilanSaisiNodes.getLength() != 0) {
			// return xml contains no list but bilanSaisi node
			return getStandardTransactionPrice(transaction, variables);
		} else {
			// list case
			return 0.0;
		}
	}
	
	private double getStandardTransactionPrice(TransactionWithVariables transaction, Map<String, String> variables) {
		double prixTTC = Double.parseDouble( prop.getProperty( transaction.getName()+".prix" ) );
		prixTTC = addModesPrices(transaction, variables, prixTTC);
		return prixTTC;
	}
	
	private double addModesPrices(TransactionWithVariables transaction, Map<String, String> variables, double prixTTC) {
		return addModesPrices(transaction, variables, prixTTC, false);
	}
	
	private double addModesPrices(TransactionWithVariables transaction, Map<String, String> variables, double prixTTC, boolean total) {
		Object tmp;
		if (variables != null)
			tmp = variables.get("mode");
		else
			tmp = transaction.getVariableValue("mode");
		
		if (tmp != null) {
			if (errorsHandling != null) {
				// modes prices with errors handling
				if (tmp instanceof Vector) {
					// multivaluated
					Vector<String> modes = GenericUtils.cast(tmp);
					for (String mode : modes) {
						if (!errorsHandling.contains(mode)) {
							if (mode.equals("C") && total) {
								prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+".total."+mode ) );
							} else {
								prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+"."+mode ) );
							}
						}
					}
				} else if (tmp instanceof String[]) {
					// multivaluated
					String [] modes = (String[]) tmp;
					for (String mode : modes) {
						if (!errorsHandling.contains(mode)) {
							if (mode.equals("C") && total) {
								prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+".total."+mode ) );
							} else {
								prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+"."+mode ) );
							}
						}
					}
				} else if (tmp instanceof String) {
					// only one value
					String mode = (String) tmp;
					if (!errorsHandling.contains(mode)) {
						if (mode.equals("C") && total) {
							prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+".total."+mode ) );
						} else {
							prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+"."+mode ) );
						}
					}
				} else {
					Engine.logBillers.error("Error when calculating price for transaction '" + transaction.getName() + "' : 'mode' variable value is of unknown type.");
				}
			} else {
				// modes prices without errors handling 
				if (tmp instanceof Vector) {
					// multivaluated
					Vector<String> modes = GenericUtils.cast(tmp);
					for (String mode : modes) {
						if (mode.equals("C") && total) {
							prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+".total."+mode ) );
						} else {
							prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+"."+mode ) );
						}
					}
				} else if (tmp instanceof String[]) {
					// multivaluated
					String [] modes = (String[]) tmp;
					for (String mode : modes) {
						if (mode.equals("C") && total) {
							prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+".total."+mode ) );
						} else {
							prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+"."+mode ) );
						}
					}
				} else if (tmp instanceof String) {
					// only one value
					String mode = (String) tmp; 
					if (mode.equals("C") && total) {
						prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+".total."+mode ) );
					} else {
						prixTTC += Double.parseDouble( prop.getProperty( transaction.getName()+"."+mode ) );
					}
				} else {
					Engine.logBillers.error("Error when calculating price for transaction '" + transaction.getName() + "' : 'mode' variable value is of unknown type.");
				}
			}
		}
		prixTTC = round(prixTTC, 2);
		return prixTTC;
	}
	
	private double getTransactionPriceByInscription(TransactionWithVariables transaction, Map<String, String> variables) {
		double prixTTC = Double.parseDouble( prop.getProperty( transaction.getName()+".prix" ) );
		
		int nbInscriptions = 0;
		final int NB_INSCRIPTIONS_MAX = 14;
		Object tmp;
		
		if (variables != null)
			tmp = variables.get("inscription");
		else
			tmp = transaction.getVariableValue("inscription");
		
		if (tmp != null)
			if (tmp instanceof Vector) {
				// multivaluated
				Vector<String> inscriptions = GenericUtils.cast(tmp);
				nbInscriptions 		= inscriptions.size();
			} else if (tmp instanceof String[]) {
				// multivaluated
				String[] inscriptions = (String[]) tmp;
				nbInscriptions 		= inscriptions.length;
			} else if (tmp instanceof String) {
				// only one value
				nbInscriptions 		= 1;
			} else {
				Engine.logBillers.error("Error when calculating price for transaction '" + transaction.getName() + "' : 'inscription' variable value is of unknown type.");
			}

		prixTTC = round(prixTTC * nbInscriptions, 2);
		prixTTC = addModesPrices(transaction, variables, prixTTC, nbInscriptions == NB_INSCRIPTIONS_MAX);
		
		return prixTTC;
	}
	
	private double round(double what, int howmuch) {
		return (double)( (int)(what * Math.pow(10,howmuch) + .5) ) / Math.pow(10,howmuch);
	}

	public Properties getProp() {
		return prop;
	}

	public void setProp(Properties prop) {
		this.prop = prop;
	}
}
