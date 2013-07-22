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

import java.io.IOException;
import java.sql.SQLException;

import org.w3c.dom.*;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

// TODO: gérer les réponses d'erreur POBI de sorte à ce qu'il n'y ait pas de trace dans le log

public class PobiXmlBiller extends PobiBiller {
	public PobiXmlBiller() throws IOException {
		super();
	}
	
	public double getCostImpl(Context context, Object data) throws Exception {
		Document document = context.outputDocument;
		NodeList nodeList;
		
		Element documentElement = document.getDocumentElement();
		
		String fromCache = documentElement.getAttribute("fromcache");
		if ("true".equalsIgnoreCase(fromCache)) return -2;
		
		nodeList = documentElement.getElementsByTagName("FIBEN");
		if (nodeList.getLength() > 0) {
			return getFibenCost(document);
		}

		nodeList = documentElement.getElementsByTagName("FCC");
		if (nodeList.getLength() > 0) {
			return getFccCost(document);
		}

		nodeList = documentElement.getElementsByTagName("FNCI");
		if (nodeList.getLength() > 0) {
			return getFnciCost(document);
		}

		nodeList = documentElement.getElementsByTagName("FICP");
		if (nodeList.getLength() > 0) {
			return getFicpCost(document);
		}
		
		// Altares : Dun and BradStreet
		nodeList = documentElement.getElementsByTagName("CDX");
		if (nodeList.getLength() > 0) {
			return getDunCost(document);
		}
		
		String xml = XMLUtils.prettyPrintDOM(document);
		throw new EngineException("Unknown document type (accepted types are: FCC, FIBEN, FNCI, FICP, CDX); the analyzed document is:\n" + xml);
	}
	
	private double getDunCost(Document document) {
		try {
			Element documentElement = document.getDocumentElement();

			// looks for ALTARES errors
			NodeList nodeList = document.getElementsByTagName("ERROR");
			if (nodeList.getLength() != 0) {
				return 0;
			}
			
			Element operation = (Element) documentElement.getElementsByTagName("OPERATION").item(0);
			
			Element module = XMLUtils.findSingleElement(operation, "AVAILABLEPRODUCTS");
			if (module != null) {
				Engine.logBillers.debug("[PobiXmlBiller] DUN: no billable response");
				return 0;
			}
			else if ((module = XMLUtils.findSingleElement(operation, "PRODUCT")) != null) {
				Element code = XMLUtils.findSingleElement(module, "CODE");
				String moduleCode = code.getChildNodes().item(0).getNodeValue();
				String siren = getDunSiren(document);
				return getDunCost(siren, moduleCode);
			}
			else {
				return 0;
			}
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the CDX tag or module code.\n" + XMLUtils.prettyPrintDOM(document));
			return 0;
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the CDX tag or module code.\n" + XMLUtils.prettyPrintDOM(document), e);
			return 0;
		}
	}
	
	private double getFibenCost(Document document) {
		try {
			Element documentElement = document.getDocumentElement();
			Element element = (Element) documentElement.getElementsByTagName("FIBEN").item(0);
			String module = element.getAttribute("MODDEM");

			NodeList nodeList;
			if (module.equals("37")) {
				nodeList = documentElement.getElementsByTagName("MODULE37");
				if (nodeList.getLength() == 0) {
					Engine.logBillers.debug("[PobiXmlBiller] FIBEN: no billable response");
					return 0;
				}
			}
			else {
				nodeList = documentElement.getElementsByTagName("DIFF" + module);
				if (nodeList.getLength() == 0) {
					Engine.logBillers.debug("[PobiXmlBiller] FIBEN: no billable response");
					return 0;
				}
			}
			
			return getFibenCost(module);
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the FIBEN tag or module number.\n" + XMLUtils.prettyPrintDOM(document));
			return 0;
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the FIBEN tag or module number.\n" + XMLUtils.prettyPrintDOM(document), e);
			return 0;
		}
	}
	
	private double getFccCost(Document document) throws SQLException {
		String cleBDF = "", sousCle = "";
		Element element;

		// Relevé Titulaire d'un Compte
		NodeList nodeList = document.getDocumentElement().getElementsByTagName("FCC");
		if (nodeList.getLength() > 0) {
			element = (Element) nodeList.item(0);
			String typeRel = element.getAttribute("TYPREL");
			if (typeRel != null) {
				if (typeRel.equalsIgnoreCase("RTIT")) {
					return 0;
				}
			}
		}
		
		try {
			// Demande ou Relevé pmnc
			nodeList = document.getDocumentElement().getElementsByTagName("NATJURLIB");
			if (nodeList.getLength() > 0) {
				nodeList = document.getDocumentElement().getElementsByTagName("DENOM");
				element = (Element) nodeList.item(0);
				cleBDF = element.getChildNodes().item(0).getNodeValue();
				nodeList = document.getDocumentElement().getElementsByTagName("LIG1");
				element = (Element) nodeList.item(0);
				cleBDF += "/"+ element.getChildNodes().item(0).getNodeValue();
				nodeList = document.getDocumentElement().getElementsByTagName("LIG2");
				element = (Element) nodeList.item(0);
				cleBDF += "/"+ element.getChildNodes().item(0).getNodeValue();
			}
			else {
				// Relevé personne physique
				nodeList = document.getDocumentElement().getElementsByTagName("CLEBDF");
				if (nodeList.getLength() > 0) {
					element = (Element) nodeList.item(0);
					Node nodeCleBdf = element.getChildNodes().item(0);
					cleBDF = nodeCleBdf.getNodeValue();
				}
				// Relevé personne morale
				else {
					nodeList = document.getDocumentElement().getElementsByTagName("NATIMMAT");
					element = (Element) nodeList.item(0);
					cleBDF = element.getChildNodes().item(0).getNodeValue();
					nodeList = document.getDocumentElement().getElementsByTagName("NUMIMMAT");
					element = (Element) nodeList.item(0);
					cleBDF += element.getChildNodes().item(0).getNodeValue();
				}
			}
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the CLEBDF/NATIMMAT/NUMIMMAT/DENOM/LIG1/LIG2 parameter for calculating the FCC cost.\n" + XMLUtils.prettyPrintDOM(document));
			return 0;
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the CLEBDF/NATIMMAT/NUMIMMAT/DENOM/LIG1/LIG2 parameter for calculating the FCC cost.\n" + XMLUtils.prettyPrintDOM(document), e);
			return 0;
		}
		
		String numRep;
		int nbp;
		try {
			element = (Element) document.getDocumentElement().getElementsByTagName("REPONSE").item(0);
			// Demande pmnc
			numRep = (String) element.getAttribute("NUMDPMN");
			if ((numRep == null) || (numRep.equals(""))) {
				// Relevé pp/pm
				numRep = (String) element.getAttribute("NBP");
				if ((numRep == null) || (numRep.equals(""))) {
					// Relevé pmnc
					numRep = (String) element.getAttribute("TRP");
					nbp = Integer.parseInt(numRep);
					if (nbp >= 0) nbp = 1;
				}
				else {
					nbp = Integer.parseInt(numRep);
					if (nbp == 0) nbp = 1;
				}
			}
			else {
				return 0;
			}
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the FCC number of responses.\n" + XMLUtils.prettyPrintDOM(document));
			return 0;
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the FCC number of responses.\n" + XMLUtils.prettyPrintDOM(document), e);
			return 0;
		}
		
		return getApplicationCost("fcc", cleBDF, sousCle, nbp);
	}
	
	private double getFicpCost(Document document) throws SQLException {
		String cleBDF, sousCle = "";
		Element element;

		try {
			NodeList nodeList = document.getDocumentElement().getElementsByTagName("CLEBDF");
			if (nodeList.getLength() > 0) {
				element = (Element) nodeList.item(0);
				cleBDF = element.getChildNodes().item(0).getNodeValue();
			}
			else {
				nodeList = document.getDocumentElement().getElementsByTagName("NATIMMAT");
				element = (Element) nodeList.item(0);
				cleBDF = element.getChildNodes().item(0).getNodeValue();
				nodeList = document.getDocumentElement().getElementsByTagName("NUMIMMAT");
				element = (Element) nodeList.item(0);
				cleBDF += element.getChildNodes().item(0).getNodeValue();
			}
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the CLEBDF/NATIMMAT/NUMIMMAT parameter for calculating the FICP cost.\n" + XMLUtils.prettyPrintDOM(document));
			return 0;
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the CLEBDF/NATIMMAT/NUMIMMAT parameter for calculating the FICP cost.\n" + XMLUtils.prettyPrintDOM(document), e);
			return 0;
		}
		
		int nbp;
		try {
			element = (Element) document.getDocumentElement().getElementsByTagName("REPONSE").item(0);
			nbp = Integer.parseInt((String) element.getAttribute("NBP"));
			if (nbp == 0) nbp = 1;
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the FICP number of responses.\n" + XMLUtils.prettyPrintDOM(document));
			return 0;
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the FICP number of responses.\n" + XMLUtils.prettyPrintDOM(document), e);
			return 0;
		}
		
		return getApplicationCost("ficp", cleBDF, sousCle, nbp);
	}

	private double getFnciCost(Document document) throws SQLException {
		return 0;
	}

	protected String getModule(Context context, Object data) {
		return context.transactionName;
	}

	protected String getDataKey(Context context, Object data) {
		Document document = context.outputDocument;
		NodeList nodeList;
		
		Element documentElement = document.getDocumentElement();
		
		
		nodeList = documentElement.getElementsByTagName("FIBEN");
		if (nodeList.getLength() > 0) {
			return getFibenBdfKey(document);
		}

		nodeList = documentElement.getElementsByTagName("FCC");
		if (nodeList.getLength() > 0) {
			return getFccBdfKey(document);
		}

		nodeList = documentElement.getElementsByTagName("FNCI");
		if (nodeList.getLength() > 0) {
			return getFnciBdfKey(document);
		}

		nodeList = documentElement.getElementsByTagName("FICP");
		if (nodeList.getLength() > 0) {
			return getFicpBdfKey(document);
		}
		
		nodeList = documentElement.getElementsByTagName("CDX");
		if (nodeList.getLength() > 0) {
			return getDunSiren(document);
		}

		String xml = XMLUtils.prettyPrintDOM(document);
		Engine.logBillers.error(
			"[PobiBiller] Unable to get the transaction BDF key; aborting billing.",
			new EngineException("Unknown document type (accepted types are: FCC, FIBEN, FNCI, FICP, CDX); the analyzed document is:\n" + xml)
		);
		return "?";
	}
	
	private String getDunSiren(Document document) {
		String siren = "";
		try {
			Element documentElement = document.getDocumentElement();
			Element operation = (Element) documentElement.getElementsByTagName("OPERATION").item(0);
			Element sirenElement = (Element) operation.getElementsByTagName("SIREN").item(0);
			siren = sirenElement.getChildNodes().item(0).getNodeValue();
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the RESPONSE/OPERATION/SIREN parameter.\n" + XMLUtils.prettyPrintDOM(document));
			return "";
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the RESPONSE/OPERATION/SIREN parameter.\n" + XMLUtils.prettyPrintDOM(document), e);
			return "";
		}
		return siren;
	}
	
	private String getFccBdfKey(Document document) {
		String cleBDF = "";
		Element element;
		
		try {
			// Demande ou Relevé pmnc
			NodeList nodeList = document.getDocumentElement().getElementsByTagName("NATJURLIB");
			if (nodeList.getLength() > 0) {
				// Relevé pmnc
				nodeList = document.getDocumentElement().getElementsByTagName("NUMDPMN");
				if (nodeList.getLength() > 0) {
					element = (Element) nodeList.item(0);
					Node nodeCleBdf = element.getChildNodes().item(0);
					cleBDF = nodeCleBdf.getNodeValue();
				}
				// Demande pmnc
				else {
					nodeList = document.getDocumentElement().getElementsByTagName("REPONSE");
					if (nodeList.getLength() > 0) {
						element = (Element) nodeList.item(0);
						cleBDF = (String) element.getAttribute("NUMDPMN");
					}
				}
			}
			else {
				// Relevé personne physique
				nodeList = document.getDocumentElement().getElementsByTagName("CLEBDF");
				if (nodeList.getLength() > 0) {
					element = (Element) nodeList.item(0);
					Node nodeCleBdf = element.getChildNodes().item(0);
					cleBDF = nodeCleBdf.getNodeValue();
				}
				// Relevé personne morale
				else {
					nodeList = document.getDocumentElement().getElementsByTagName("NATIMMAT");
					if (nodeList.getLength() > 0) {
					element = (Element) nodeList.item(0);
					cleBDF = element.getChildNodes().item(0).getNodeValue();
					nodeList = document.getDocumentElement().getElementsByTagName("NUMIMMAT");
					element = (Element) nodeList.item(0);
					cleBDF += element.getChildNodes().item(0).getNodeValue();
					}
					// Titulaire d'un compte
					else {
						nodeList = document.getDocumentElement().getElementsByTagName("CODETC");
						element = (Element) nodeList.item(0);
						cleBDF = element.getChildNodes().item(0).getNodeValue();
						nodeList = document.getDocumentElement().getElementsByTagName("CODGUICH");
						element = (Element) nodeList.item(0);
						cleBDF += " "+element.getChildNodes().item(0).getNodeValue();
						nodeList = document.getDocumentElement().getElementsByTagName("NUMCOMPTE");
						element = (Element) nodeList.item(0);
						cleBDF += " "+element.getChildNodes().item(0).getNodeValue();
					}
				}
			}
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the CLEBDF/NATIMMAT/CODETC/NUMDPMN parameter.\n" + XMLUtils.prettyPrintDOM(document));
			return "";
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the CLEBDF/NATIMMAT/CODETC/NUMDPMN parameter.\n" + XMLUtils.prettyPrintDOM(document), e);
			return "";
		}
		
		return (cleBDF);
	}
	
	private String getFicpBdfKey(Document document) {
		String cleBDF = "";
		Element element;

		try {
			NodeList nodeList = document.getDocumentElement().getElementsByTagName("CLEBDF");
			if (nodeList.getLength() > 0) {
				element = (Element) nodeList.item(0);
				cleBDF = element.getChildNodes().item(0).getNodeValue();
			}
			else {
				nodeList = document.getDocumentElement().getElementsByTagName("NATIMMAT");
				element = (Element) nodeList.item(0);
				cleBDF = element.getChildNodes().item(0).getNodeValue();
				nodeList = document.getDocumentElement().getElementsByTagName("NUMIMMAT");
				element = (Element) nodeList.item(0);
				cleBDF += element.getChildNodes().item(0).getNodeValue();
			}
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the CLEBDF/NATIMMAT/NUMIMMAT parameter.\n" + XMLUtils.prettyPrintDOM(document));
			return "";
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the CLEBDF/NATIMMAT/NUMIMMAT parameter.\n" + XMLUtils.prettyPrintDOM(document), e);
			return "";
		}
		
		return (cleBDF);
	}
	
	private String getFnciBdfKey(Document document) {
		String cleBDF = "";
		Element element;

		try {
			NodeList nodeList = document.getDocumentElement().getElementsByTagName("RIBBQE");
			if (nodeList.getLength() > 0) {
				element = (Element) nodeList.item(0);
				cleBDF = element.getChildNodes().item(0).getNodeValue();
				nodeList = document.getDocumentElement().getElementsByTagName("RIBGUI");
				element = (Element) nodeList.item(0);
				cleBDF += " "+element.getChildNodes().item(0).getNodeValue();
				nodeList = document.getDocumentElement().getElementsByTagName("RIBCPT");
				element = (Element) nodeList.item(0);
				cleBDF += " "+element.getChildNodes().item(0).getNodeValue();
			}
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the RIB parameter.\n" + XMLUtils.prettyPrintDOM(document));
			return "";
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the RIB parameter.\n" + XMLUtils.prettyPrintDOM(document), e);
			return "";
		}
		
		return (cleBDF);
	}
	
	private String getFibenBdfKey(Document document) {
		String cleBDF = "";
		Element element;

		try {
			NodeList nodeList = document.getDocumentElement().getElementsByTagName("IDENTS");
			if (nodeList.getLength() > 0) {
				element = (Element) nodeList.item(0);
				cleBDF = element.getChildNodes().item(0).getNodeValue();
				nodeList = document.getDocumentElement().getElementsByTagName("IDENTC");
				if (nodeList.getLength() > 0) {
					element = (Element) nodeList.item(0);
					cleBDF += "/"+element.getChildNodes().item(0).getNodeValue();
				}				
			}
			else {
				nodeList = document.getDocumentElement().getElementsByTagName("IDENTC");
				if (nodeList.getLength() > 0) {
					element = (Element) nodeList.item(0);
					cleBDF = element.getChildNodes().item(0).getNodeValue();
				} else {	// added by jmc on Do's request 23/08/2006
					nodeList = document.getDocumentElement().getElementsByTagName("IDENT");
					if (nodeList.getLength() > 0) {
						element = (Element) nodeList.item(0);
						cleBDF = element.getChildNodes().item(0).getNodeValue();
					}
				}
			}
		}
		catch(NullPointerException e) {
			Engine.logBillers.warn("[PobiXmlBiller] Unable to get the IDENTS/IDENTC parameter.\n" + XMLUtils.prettyPrintDOM(document));
			return "";
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiXmlBiller] Unable to get the IDENTS/IDENTC parameter.\n" + XMLUtils.prettyPrintDOM(document), e);
			return "";
		}
		
		return (cleBDF);
	}
}
