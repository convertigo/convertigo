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

import itlm.directkit.DirectKit;
import itlm.directkit.InvalidInvoiceException;
import itlm.directkit.InvalidParameterException;
import itlm.directkit.InvalidStoreIdException;
import itlm.directkit.NoSuchAuthorisationRequestException;
import itlm.directkit.NoSuchDebitRequestReferenceException;
import itlm.directkit.NullParameterException;
import itlm.directkit.ServiceNotAvailableException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class LCABiller extends AbstractBiller {

	public static Map<String, String> errorCodes;
	
	static {
		errorCodes = new Hashtable<String, String>();
		errorCodes.put("-1", "Interface : erreur base de données.");
		errorCodes.put("0"	 , "OK.");
		errorCodes.put("21", "Interface : moyen de paiement indisponible.");
		errorCodes.put("22", "Interface : trop de messages simultanés.");
		errorCodes.put("23", "Interface : nombre de paramètre incorrect.");
		errorCodes.put("68", "Interface : time-out.");
		errorCodes.put("90", "Interface : trop de messages.");
		errorCodes.put("91", "Interface : trop de messages.");
		errorCodes.put("96", "Interface : erreur d'inscription en base de donnees.");
		errorCodes.put("100", "Type d'utilisateur inconnu.");
		errorCodes.put("101", "Utilisateur inconnu.");
		errorCodes.put("102", "Mot de passe incorrect.");
		errorCodes.put("103", "Type de profil incorrect.");
		errorCodes.put("104", "Demande de profil non autorisée pour les marchands.");
		errorCodes.put("105", "Compte détruit.");
		errorCodes.put("201", "Compte inexistant.");
		errorCodes.put("202", "Magasin inexistant.");
		errorCodes.put("203", "Devise incorrecte.");
		errorCodes.put("204", "Achat de biens logiques non autorisé. Contactez votre administrateur.");
		errorCodes.put("205", "Achat de biens physiques non autorisé. Contactez votre administrateur.");
		errorCodes.put("206", "Achat refusé: compte utilisateur détruit.");
		errorCodes.put("207", "Achat refusé: compte utilisateur suspendu (demander la réactivation de votre compte à votre administrateur).");
		errorCodes.put("208", "Achat refusé, ce compte entreprise n'est plus valide. Contactez votre administrateur.");
		errorCodes.put("209", "Achat refusé: compte entreprise détruit.");
		errorCodes.put("210", "Achat refusé: compte entreprise suspendu. Contactez votre administrateur.");
		errorCodes.put("211", "Achat refusé: l'achat dans ce magasin ne vous est pas permis. Contactez votre administrateur.");
		errorCodes.put("212", "Achat refusé: magasin indisponible.");
		errorCodes.put("213", "Achat refusé: magasin indisponible.");
		errorCodes.put("214", "Achat refusé: le montant de cette commande dépasse votre plafond autorise par achat. Contactez votre administrateur.");
		errorCodes.put("215", "Achat refusé: votre plafond de consommation est dépassé. Contactez votre administrateur.");
		errorCodes.put("216", "Achat refusé : le plafond de consommation de votre entreprise est dépassé. Contactez votre administrateur.");
		errorCodes.put("217", "Achat refusé: le plafond de consommation de votre entreprise est dépassé. Contactez votre administrateur.");
		errorCodes.put("218", "Demande de débit refusée: la référence de DA est incorrecte.");
		errorCodes.put("219", "Demande de débit refusée : cette DA a été refusée.");
		errorCodes.put("220", "Demande de débit refusée: cette DA a expiré.");
		errorCodes.put("221", "Demande de débit refusée: le montant dépassé celui de la DA");
		errorCodes.put("222", "Achat refusé : magasin supprimé.");
		errorCodes.put("223", "Achat refusé : votre entreprise n'a pas le droit d'acheter dans ce magasin. Contactez votre administrateur.");
		errorCodes.put("224", "Achat refusé : votre plafond de consommation dans ce magasin est dépassé. Contactez votre administrateur.");
		errorCodes.put("225", "Achat refusé : le plafond de consommation de votre entreprise dans ce magasin est dépassé. Contactez votre administrateur.");
		errorCodes.put("226", "Achat refusé : votre entreprise n'a pas le droit d'acheter dans ce magasin (GFF incompatibles).");
		errorCodes.put("227", "Annulation d'achat refusée : cette DA ne peut être annulée.");
		errorCodes.put("228", "Annulation d'achat refusée : la référence de DA est incorrecte.");
		errorCodes.put("229", "Achat refuse : votre entreprise n'a pas le droit d'acheter dans ce magasin suite a une interdiction par l'administrateur Intelmatique.");
		errorCodes.put("1010", "Service indisponible.");
		errorCodes.put("1020", "Parametres incorrects : null ou vide.");
		errorCodes.put("1030", "Erreur interne.");
		errorCodes.put("1040", "Nom de methode inconnue, verifier le protocole.");
		errorCodes.put("1050", "Aucune demande d'autorisation correspondante.");
		errorCodes.put("1060", "StoreId invalide.");
		errorCodes.put("1070", "SessionTrackingId invalide.");
		errorCodes.put("1080", "Aucune demande de debit correspondante.");
		errorCodes.put("1090", "Montants de la facture incorrects.");
		errorCodes.put("1100", "Erreur.");
		errorCodes.put("1110", "Erreur indefinie.");
		errorCodes.put("1120", "Pas de profil utilisateur, il faut s'authentifier au prealable.");
		errorCodes.put("1130", "Sauvegarde du fichier de configuration impossible.");
		errorCodes.put("1140", "Mise en attente interrompue.");
		errorCodes.put("1150", "Paramètre invalide.");
		errorCodes.put("1160", "Pas de réponse du serveur concurent.");
		errorCodes.put("1190", "Aucune DARA correspondante.");
		errorCodes.put("2001", "Achat refusé : authentification utilisateur incorrecte.");
		errorCodes.put("2002", "Achat refusé : erreur interne.");
		errorCodes.put("2003", "Demande de débit refusée : problème lors du renouvellement de la DA.");
		errorCodes.put("2004", "Achat refusé : authentification utilisateur incorrecte");
		errorCodes.put("2005", "Achat annulé par l'utilisateur.");
		errorCodes.put("2006", "Achat refusé : magasin incorrect.");
		errorCodes.put("2007", "Achat refuse : cet utilisateur n'a pas le droit de modifier son adresse de livraison.");
		errorCodes.put("2008", "Annulation d'achat refusee : commande partiellement livree.");
		errorCodes.put("2009", "Annulation d'achat refusee : la reference de DA est incorrecte.");
		errorCodes.put("2011", "Demande de débit refusée : la référence de DA est incorrecte.");
		errorCodes.put("2021", "Demande de profil étendu : non autorisée pour les marchands.");
		errorCodes.put("2022", "Demande de profil étendu : contexte client invalide.");
		errorCodes.put("2023", "Demande de profil étendu non autorisée.");
		errorCodes.put("2024", "Demande de profil étendu : magasin incorrect.");
		errorCodes.put("2025", "Demande de profil etendu : authentification utilisateur incorrecte.");
		errorCodes.put("2031", "Demande de debit refusee : Impossible de trouver l'autorisation initiale.");
		errorCodes.put("2032", "Demande de débit refusée : demande en cours pour cette DA.");
		errorCodes.put("2033", "Demande de débit refusée : magasin incorrect.");
		errorCodes.put("2034", "Demande de debit refusee : la modification de l'adresse de livraison n'est pas autorisee.");
		errorCodes.put("2041", "Authentification annulée par l'utilisateur.");
		errorCodes.put("2042", "Authentification en erreur : magasin incorrect.");
		errorCodes.put("2043", "Authentification par portail non autorisée.");
		errorCodes.put("2044", "Impossible d'obtenir le profil : client non authentifie.");
		errorCodes.put("2051", "Echec lors du renouvellement de la clé secrète.");
		errorCodes.put("2061", "Time-out sur réponse synchrone.");
		errorCodes.put("2071", "L'interface d'authentification est inaccessible.");
		errorCodes.put("2072", "L'interface de paiement est inaccessible.");
		errorCodes.put("2111", "Service incorrect.");
		errorCodes.put("2112", "Compte utilisateur ou client suspendu ou détruit.");
		errorCodes.put("2113", "Identifiant dabonnement incorrect.");
		errorCodes.put("2114", "Identifiant dabonnement supprimé");
		errorCodes.put("2115", "Lutilisateur a refusé de souscrire");
		errorCodes.put("3000", "Demande de débit refusée: problème lors du renouvellement de la DA.");
		errorCodes.put("3201", "Demande de débit refusée: problème lors du renouvellement de la DA (Compte inexistant)");
		errorCodes.put("3202", "Demande de débit refusée: problème lors du renouvellement de la DA (Magasin inexistant).");
		errorCodes.put("3203", "Demande de débit refusée: problème lors du renouvellement de la DA (Devise incorrecte).");
		errorCodes.put("3204", "Demande de débit refusée: problème lors du renouvellement de la DA (Achat de biens logiques non autorise).");
		errorCodes.put("3205", "Demande de débit refusée: problème lors du renouvellement de la DA (Achat de biens physiques non autorise).");
		errorCodes.put("3206", "Demande de débit refusée: problème lors du renouvellement de la DA (Compte utilisateur détruit).");
		errorCodes.put("3207", "Demande de débit refusée: problème lors du renouvellement de la DA (Compte	utilisateur suspendu).");
		errorCodes.put("3208", "Demande de débit refusée: problème lors du renouvellement de la DA (Le compte ce cette entreprise n'est plus valide).");
		errorCodes.put("3209", "Demande de débit refusée: problème lors du renouvellement de la DA (Compte entreprise détruit).");
		errorCodes.put("3210", "Demande de débit refusée: problème lors du renouvellement de la DA (Compte	entreprise suspendu).");
		errorCodes.put("3211", "Demande de débit refusée: problème lors du renouvellement de la DA (Achat dans	ce magasin non autorise).");
		errorCodes.put("3212", "Demande de débit refusée: problème lors du renouvellement de la DA (Magasin indisponible).");
		errorCodes.put("3213", "Demande de débit refusée: problème lors du renouvellement de la DA (Magasin indisponible).");
		errorCodes.put("3214", "Demande de débit refusée: problème lors du renouvellement de la DA (Dépassement du plafond autorise par achat).");
		errorCodes.put("3215", "Demande de débit refusée: problème lors du renouvellement de la DA	(Dépassement du plafond de consommation utilisateur).");
		errorCodes.put("3216", "Demande de débit refusée: problème lors du renouvellement de la DA	(Dépassement du plafond de consommation entreprise).");
		errorCodes.put("3217", "Demande de débit refusée: problème lors du renouvellement de la DA	(Dépassement du plafond de consommation entreprise).");
		errorCodes.put("3218", "Demande de débit refusée: problème lors du renouvellement de la DA (Référence de DA incorrecte).");
		errorCodes.put("3219", "Demande de débit refusée: problème lors du renouvellement de la DA (Cette DA a été refusée).");
		errorCodes.put("3220", "Demande de débit refusée: problème lors du renouvellement de la DA (Cette DA a expire).");
		errorCodes.put("3221", "Demande de débit refusée: problème lors du renouvellement de la DA (Le	montant dépasse celui de la DA).");
		errorCodes.put("3222", "Demande de débit refusée: problème lors du renouvellement de la DA (Magasin supprime).");
		errorCodes.put("3223", "Demande de débit refusée: problème lors du renouvellement de la DA (Cette entreprise n'a pas le droit d'acheter dans ce magasin).");
		errorCodes.put("3224", "Demande de débit refusée: problème lors du renouvellement de la DA	(Dépassement du plafond de consommation utilisateur dans ce magasin).");
		errorCodes.put("3225", "Demande de débit refusée: problème lors du renouvellement de la DA (Dépassement du plafond de consommation entreprise dans ce magasin).");
		errorCodes.put("3226", "Demande de débit refusée: problème lors du renouvellement de la DA (Cette entreprise n'a pas le droit d'acheter dans ce magasin (GFF incompatibles)).");
		errorCodes.put("3227", "Demande de débit refusée: problème lors du renouvellement de la DA (Cette DA ne peut être annulee).");
		errorCodes.put("3228", "Demande de debit refusee: probleme lors du renouvellement de la DA (Annulation impossible la reference de DA est incorrecte).");
		errorCodes.put("3229", "Demande de debit refusee: probleme lors du renouvellement de la DA (Cette entreprise n'a pas le droit d'acheter dans ce magasin suite a une interdiction par l'administrateur Intelmatique).");
	}
	
	public LCABiller() throws IOException {
		super();
	}

	static public String getErrorMessage(String errorCode) {
		return (String)errorCodes.get(errorCode);
	}
	
	public void insertBilling(Context context) throws EngineException {
		if (isBillable(context))
			doInsertBilling(context);
	}
	
	/**
	 * Récupération des attributs de session.
	 * 
	 * @param context the transaction Context
	 * @param attr the searched attribute String
	 * 
	 * @return the object value of attribute in context's session
	 */
	protected Object getSessionAttribute(Context context, String attr) {
		String contextName = null;
		Object value = null;
		
		// Current context name
		contextName = context.name.equals("default") || context.name.startsWith("studio_") ? "" : context.name;
		
		// Retrieve attribute's value from context's HTTP session
		value = context.httpSession.getAttribute(attr);
		
		// Transaction may have been called by a mother sequence
		if (value == null) {
			// Try to get mother sequence's context
			Context motherContext = context.parentContext;
			if (motherContext!= null) {
				// Replace current context name with mother's one in searched attribute
				String attrib = attr.replaceAll("_"+contextName+"_", "_"+motherContext.name+"_");
				// Retrieve attribute's value from mother context's HTTP session
				value = motherContext.httpSession.getAttribute(attrib);
			}
		}
		
		return value;
	}
	
	protected void doInsertBilling(Context context) throws EngineException {
		Engine.logEngine.debug("[LCABiller] Do the billing.");
		
		String refDD 				= null;
		String contextName 			= context.name.equals("default") || context.name.startsWith("studio_") ? "" : context.name;
		String refDA 				= (String)getSessionAttribute(context, "refDA_" + contextName + "_" + context.transactionName);
		String authorisationUrl		= (String)getSessionAttribute(context, "authorisationUrl_" + contextName + "_" + context.transactionName);
		try {
			Properties prop 	= loadFromFile(context);
			String storeId 		= prop.getProperty("storeId");
			double[] prix 		= getPrices(prop, (Transaction)context.requestedObject);
			if (prix == null) {
				Engine.logEngine.debug("[LCABiller] Billing aborted because 'getPrices' method returned null : not billable.");
				return;
			}
			double prixHT 		= prix[0];
			double tauxTVA 		= prix[1];
			double montantTVA 	= prix[2];
			double prixTTC 		= prix[3];
			String libelle 		= getLabel(prop, (Transaction)context.requestedObject);
			refDD = DirectKit.newInvoice(storeId, refDA);
				
			if (refDD == null)
				throw new EngineException("Référence de demande de débit 'null'.");
			
			DirectKit.addInvoiceItem(storeId, refDD, 0, libelle, prixHT, 1, prixHT, "digitalItem", tauxTVA, montantTVA);
			
			int retourInvoice = DirectKit.settleInvoice(storeId, refDD, prixHT, montantTVA, prixTTC, 0, 0, 0, null);
			
			if (retourInvoice == 0) {
				String invoiceNumber = DirectKit.getInvoiceNumber(storeId, refDD);
				String orderNumber = DirectKit.getOrderNumber(storeId, authorisationUrl);
				Engine.logEngine.debug("[LCABiller] Billing is completed, invoice number : '" + invoiceNumber + "', order number : '" + orderNumber + "'");
				context.addTextNodeUnderRoot("orderNumber", orderNumber);
				context.addTextNodeUnderRoot("invoiceNumber", invoiceNumber);
			} else {
				throw new EngineException(getErrorMessage(String.valueOf(retourInvoice)));
			}
		} catch (NoSuchDebitRequestReferenceException e) {
			throw new EngineException(e.getMessage());
		} catch (InvalidStoreIdException e) {
			throw new EngineException(e.getMessage());
		} catch (ServiceNotAvailableException e) {
			throw new EngineException(e.getMessage());
		} catch (InvalidParameterException e) {
			throw new EngineException(e.getMessage());
		} catch (InvalidInvoiceException e) {
			throw new EngineException(e.getMessage());
		} catch (NullParameterException e) {
			throw new EngineException(e.getMessage());
		} catch (NoSuchAuthorisationRequestException e) {
			throw new EngineException(e.getMessage());
		} catch (FileNotFoundException e) {
			throw new EngineException(e.getMessage());
		} catch (IOException e) {
			throw new EngineException(e.getMessage());
		}
	}
	
	protected boolean isBillable(Context context) {
		Document document 		= context.outputDocument;
		Element documentElement = document.getDocumentElement();
		String fromCache 		= documentElement.getAttribute("fromcache");
		if ("true".equalsIgnoreCase(fromCache)) {
			Engine.logEngine.debug("[LCABiller] Billing aborted because the response was in cache.");
			return false;
		}
		return true;
	}
	
	public double[] getPrices(Properties prop, Transaction transaction) throws EngineException {
		double prixHT 		= Double.parseDouble(prop.getProperty(transaction.getName()	+ ".prix"));
		double tauxTVA 		= Double.parseDouble(prop.getProperty(transaction.getName()	+ ".tauxTVA"));
		double montantTVA 	= round(prixHT * tauxTVA / 100, 2);
		double prixTTC 		= round(prixHT + montantTVA, 2);
		double[] result = {prixHT, tauxTVA, montantTVA, prixTTC};
		return result;
	}
	
	protected double round(double what, int howmuch) {
		return (double)( (int)(what * Math.pow(10,howmuch) + .5) ) / Math.pow(10,howmuch);
	}
	
	public String getLabel(Properties prop, Transaction transaction) throws EngineException {
		return prop.getProperty(transaction.getName() + ".label");
	}
}
