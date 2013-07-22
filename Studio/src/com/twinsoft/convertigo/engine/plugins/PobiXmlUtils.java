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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class PobiXmlUtils {
	
	public static void handleDunError(Context context) {
		Document document = context.outputDocument;
		NodeList nodeList = document.getElementsByTagName("ERRORS");
		if (nodeList.getLength() != 0) {
			Element element = (Element)nodeList.item(0);
			int count = Integer.parseInt(element.getAttribute("COUNT"), 10);
			if (count > 0) {
				// Prevent caching for this error document
				context.isCacheEnabled = false;
				
				// Inserting the error tag
				String message = null;
				
				try {
					nodeList = document.getElementsByTagName("ERROR");
					if (nodeList.getLength() != 0) {
						// look at first error only
						Node node = nodeList.item(0);
						
						Node nodetmp = XMLUtils.findSingleElement((Element) node, "CODE");
						nodetmp = XMLUtils.findChildNode(nodetmp, Node.TEXT_NODE);
						message = "(" + nodetmp.getNodeValue() + ") ";

						nodetmp = XMLUtils.findSingleElement((Element) node, "MSG");
						nodetmp = XMLUtils.findChildNode(nodetmp, Node.TEXT_NODE);
						message += nodetmp.getNodeValue();
						
						/*nodetmp = XMLUtils.findSingleElement((Element) node, "stackTrace");
						nodetmp = XMLUtils.findChildNode(nodetmp, Node.TEXT_NODE);
						if (nodetmp != null) message += nodetmp.getNodeValue();*/
					}
				}
				catch(Exception e) {
					message = "Impossible de récupérer l'erreur DUN : " + e.getMessage();
					String documentText = XMLUtils.prettyPrintDOM(document);
					Engine.logEngine.error("Impossible de récupérer l'erreur DUN :\n" + documentText, e);
				}
				
				if (message == null) {
					String documentText = XMLUtils.prettyPrintDOM(document);
					message = "Erreur inconnue :\n" + documentText;
				}
	
				Element erreurElement = document.createElement("MessageErreurPobi");
				Text erreurMessage = document.createTextNode(message);
				erreurElement.appendChild(erreurMessage);
				document.getDocumentElement().appendChild(erreurElement);
			}
		}
	}
	
	public static void handleFibenError(Context context) {
		Document document = context.outputDocument;
		NodeList nodeList = document.getElementsByTagName("ERREUR");
		if (nodeList.getLength() != 0) {
			// Prevent caching for this error document
			context.isCacheEnabled = false;
			
			// Inserting the error tag
			String message = null;

			try {
				nodeList = document.getElementsByTagName("UT");
				if (nodeList.getLength() != 0) {
					message = "(UT) La demande ne peut pas être satisfaite.";
				}

				nodeList = document.getElementsByTagName("IET01");
				if (nodeList.getLength() != 0) {
					message = "(IET01) Aucun établissement n'est enregistré pour cette entreprise.";
				}

				nodeList = document.getElementsByTagName("UCR");
				if (nodeList.getLength() != 0) {
					Node node = nodeList.item(0);

					nodeList = document.getElementsByTagName("U01");
					if (nodeList.getLength() != 0) message = "(UCR U01) Identifiant erroné";
					nodeList = document.getElementsByTagName("U011");
					if (nodeList.getLength() != 0) message = "(UCR U011) Clé incomplète";
					nodeList = document.getElementsByTagName("U101");
					if (nodeList.getLength() != 0) message = "(UCR U101) Module non disponible sur ce secteur d'activité";
					nodeList = document.getElementsByTagName("U107");
					if (nodeList.getLength() != 0) message = "(UCR U107) Dossier en cours d'instruction";
					nodeList = document.getElementsByTagName("U102");
					if (nodeList.getLength() != 0) message = "(UCR U102) Dossier non enregistré dans la base FIBEN";
					nodeList = document.getElementsByTagName("U103");
					if (nodeList.getLength() != 0) message = "(UCR U103) Module non disponilbe sur identifiant résidant à l'étranger ou dnas un TOM hors Mayotte et Saint-Pierre-et-Miquelon";
					nodeList = document.getElementsByTagName("U02");
					if (nodeList.getLength() != 0) message = "(UCR U02) Module inexistant";
					nodeList = document.getElementsByTagName("U03");
					if (nodeList.getLength() != 0) message = "(UCR U03) Module temporairement indisponible";
					nodeList = document.getElementsByTagName("U04A");
					if (nodeList.getLength() != 0) message = "(UCR U04A) Vous n'êtes pas abonné à ce service";
					nodeList = document.getElementsByTagName("U04B");
					if (nodeList.getLength() != 0) message = "(UCR U04B) Votre abonnement pour ce service est suspendu";
					nodeList = document.getElementsByTagName("U04C");
					if (nodeList.getLength() != 0) message = "(UCR U04C) Votre abonnement pour ce service est résilié";
					nodeList = document.getElementsByTagName("U04D");
					if (nodeList.getLength() != 0) message = "(UCR U04D) Votre équipement n'est pas reconnu comme acrédité";
					nodeList = document.getElementsByTagName("U04F");
					if (nodeList.getLength() != 0) message = "(UCR U04F) Aucun abonnement n'est associé à votre numéro de client";
					nodeList = document.getElementsByTagName("U04G");
					if (nodeList.getLength() != 0) message = "(UCR U04G) Incident sur les abonnements";
					nodeList = document.getElementsByTagName("U04I");
					if (nodeList.getLength() != 0) message = "(UCR U04I) Incident sur le numéro de client";
					nodeList = document.getElementsByTagName("U04J");
					if (nodeList.getLength() != 0) message = "(UCR U04J) Client sans concentrateur et guichet servi";
					nodeList = document.getElementsByTagName("U04K");
					if (nodeList.getLength() != 0) message = "(UCR U04K) Code guichet non saisi ; problème lié à l'utilisation d'un concentrateur";
					nodeList = document.getElementsByTagName("U04L");
					if (nodeList.getLength() != 0) message = "(UCR U04L) Code guichet saisi incorrect ; problème lié à l'utilisation d'un concentrateur";
					nodeList = document.getElementsByTagName("U04M");
					if (nodeList.getLength() != 0) message = "(UCR U04M) Code guichet saisi inconnu ; problème lié à l'utilisation d'un concentrateur";
					nodeList = document.getElementsByTagName("U04S");
					if (nodeList.getLength() != 0) message = "(UCR U04S) Code banque incorrect ; problème lié à l'utilisation d'un concentrateur";
					nodeList = document.getElementsByTagName("U09");
					if (nodeList.getLength() != 0) message = "(UCR U09) Application indisponible pour cause de fermeture des bases FIBEN";
					nodeList = document.getElementsByTagName("IST06");
					if (nodeList.getLength() != 0) message = "(UCR IST06) Module non disponible pour les personnes physiques";
					nodeList = document.getElementsByTagName("IST09");
					if (nodeList.getLength() != 0) message = "(UCR IST09) Module non disponible pour les entreprises individuelles";
					nodeList = document.getElementsByTagName("ST30");
					if (nodeList.getLength() != 0) message = "(UCR ST30) Demande de suivi impossible pour les personnes physiques";
					nodeList = document.getElementsByTagName("U500");
					if (nodeList.getLength() != 0) message = "(UCR U500) Un incident technique s'est produit";
					
					if (message == null) {
						Node nodeErrorCode = XMLUtils.findChildNode(node, Node.ELEMENT_NODE);
						message = "(UCR " + nodeErrorCode.getNodeName() + ") Erreur non répertoriée";
					}
				}

				nodeList = document.getElementsByTagName("PBTECH");
				if (nodeList.getLength() != 0) {
					Node nodeErrorMessage = XMLUtils.findChildNode(nodeList.item(0), Node.TEXT_NODE);
					String textPbTech = nodeErrorMessage.getNodeValue();
					message = "(PBTECH) Problème technique : " + textPbTech;
				}
			}
			catch(Exception e) {
				message = "Impossible de récupérer l'erreur POBI : " + e.getMessage();
				String documentText = XMLUtils.prettyPrintDOM(document);
				Engine.logEngine.error("Impossible de récupérer l'erreur POBI :\n" + documentText, e);
			}
			
			if (message == null) {
				String documentText = XMLUtils.prettyPrintDOM(document);
				message = "Erreur inconnue :\n" + documentText;
			}

			Element erreurElement = document.createElement("MessageErreurPobi");
			Text erreurMessage = document.createTextNode(message);
			erreurElement.appendChild(erreurMessage);
			document.getDocumentElement().appendChild(erreurElement);
		}
		else
			handleCommonError(context, "FIBEN");
	}
	
	public static void handleFccError(Context context) {
		handleCommonError(context, "FCC");
	}
	
	public static void handleFnciError(Context context) {
		handleCommonError(context, "FNCI");
	}
	
	public static void handleFicpError(Context context) {
		handleCommonError(context, "FICP");
	}
	
	public static void handleCommonError(Context context, String application) {
		Document document = context.outputDocument;
		Node node = XMLUtils.findSingleElement(document.getDocumentElement(), application + "/ERREUR");
		if (node != null) {
			// Prevent caching for this error document
			context.isCacheEnabled = false;
			
			// Inserting the error tag
			String message = null;

			try {
				Node nodetmp = XMLUtils.findSingleElement((Element) node, "ERRCOD");
				if (nodetmp == null) nodetmp = XMLUtils.findSingleElement((Element) node, "CODE");
				nodetmp = XMLUtils.findChildNode(nodetmp, Node.TEXT_NODE);
				message = "(" + nodetmp.getNodeValue() + ") ";

				nodetmp = XMLUtils.findSingleElement((Element) node, "ERRLIB");
				if (nodetmp == null) nodetmp = XMLUtils.findSingleElement((Element) node, "MSG");
				nodetmp = XMLUtils.findChildNode(nodetmp, Node.TEXT_NODE);
				if (nodetmp != null) message += nodetmp.getNodeValue();
				else message += "ERREUR";
			}
			catch(Exception e) {
				message = "Impossible de récupérer l'erreur POBI : " + e.getMessage();
				String documentText = XMLUtils.prettyPrintDOM(document);
				Engine.logEngine.error("Impossible de récupérer l'erreur POBI :\n" + documentText, e);
			}
			
			Element erreurElement = document.createElement("MessageErreurPobi");
			Text erreurMessage = document.createTextNode(message);
			erreurElement.appendChild(erreurMessage);
			document.getDocumentElement().appendChild(erreurElement);
		}
		else {
			// Looks for error message in HTML response
			node = XMLUtils.findSingleElement(document.getDocumentElement(), "html");
			if (node != null) {
				try {
					String title = null;
					
					Node nodetmp = XMLUtils.findSingleElement((Element) node, "head/title");
					nodetmp = XMLUtils.findChildNode(nodetmp, Node.TEXT_NODE);
					if (nodetmp != null) title = nodetmp.getNodeValue();
					
					if ((title != null) && (title.equalsIgnoreCase("error"))) {
						
						// Prevent caching for this error document
						context.isCacheEnabled = false;
						
						// Inserting the error tag
						String message = null;
						
						nodetmp = XMLUtils.findSingleElement((Element) node, "body");
						nodetmp = XMLUtils.findChildNode(nodetmp, Node.TEXT_NODE);
						if (nodetmp != null) message = nodetmp.getNodeValue();
						
						if (message == null) {
							String documentText = XMLUtils.prettyPrintDOM(document);
							message = "Erreur inconnue :\n" + documentText;
						}

						Element erreurElement = document.createElement("MessageErreurPobi");
						Text erreurMessage = document.createTextNode(message);
						erreurElement.appendChild(erreurMessage);
						document.getDocumentElement().appendChild(erreurElement);
					}
				}
				catch(Exception e) {
				}
			}
			// HTML response is not well formatted
			else {
				// Looks for H1 tag in HTML response
				node = XMLUtils.findSingleElement(document.getDocumentElement(), "h1");
				if (node != null) {
					try {
						// Prevent caching for this error document
						context.isCacheEnabled = false;
						
						// Inserting the error tag
						String message = null;
						
						Node nodetmp = XMLUtils.findChildNode(node, Node.TEXT_NODE);
						if (nodetmp != null) message = nodetmp.getNodeValue();
						
						if (message == null) {
							String documentText = XMLUtils.prettyPrintDOM(document);
							message = "Erreur inconnue :\n" + documentText;
						}

						Element erreurElement = document.createElement("MessageErreurPobi");
						Text erreurMessage = document.createTextNode(message);
						erreurElement.appendChild(erreurMessage);
						document.getDocumentElement().appendChild(erreurElement);
					}
					catch(Exception e) {
					}
				}
			}
		}
	}
}
