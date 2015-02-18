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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.util.StringEx;

public class PobiProxyBiller extends PobiBiller {
	// FCC
	private String strFccMajPersonneMoraleNonImmatriculee =		"Votre déclaration a été remise à la Banque de France pour traitement.";

	private String strFccSuppressionChequesPayes = 				"Suppression de chèques payés émis en infraction";
	private String strFccSuppressionChequeBdFenInfraction =		"Votre suppression a été remise à la Banque de France";

	private String strFccDeclarationChequesPayes = 				"Déclaration de chèques payés émis en infraction";
	private String strFccDeclarationRemiseBDF =					"Votre déclaration a été remise à la Banque de France.";

	private String strFccConsultationFichierCentralCheques =	"Consultation du Fichier central des chèques";
	private String strFccFichierCentralCheques =				"Fichier central des chèques";

	//
	// SurvMP
	//
	private String strPreClotureDeclarationSurvMP = 	"SurvMP Référentiel chèque > Déclaration en cours > Pré-clôture > Impression";
	private String strClotureChequeSurvMP = 			"SurvMP Référentiel chèque > Déclaration en cours > Remise";
	private String strSaisieSurvMP = 					"SurvMP Référentiel chèque > Déclaration en cours > Saisie";
	private String strCartographieSurvMP =				"SurvMP Cartographie \"établissements\" > Déclaration en cours > Saisie";
	private String strCartographiePlus = 				"Contenu de la déclaration";
	private String strClotureCartographie =				"SurvMP Cartographie \"établissements\" > Déclaration en cours > Remise";
	private String strCartoPrivativeSurvMP =			"SurvMP Cartographie \"émetteurs de cartes privatives\" > Déclaration en cours > Saisie";
	private String strCartographiePrivativePlus = 		"Contenu de la déclaration";
	private String strClotureCartoPrivative =			"SurvMP Cartographie \"émetteurs de cartes privatives\" > Déclaration en cours > Remise";
	private String strContenuDeclarationSurvMP = 		"Contenu de la déclaration";
	private String strAccueilSurvMP = 					"Merci d'indiquer votre numéro d'accréditation";
	private String strSaisieDeclarationFraude =			"SurvMP Recensement de la fraude > Déclaration en cours > Saisie";
	private String strSaisieDeclarationFraudePlus =		"Contenu de la déclaration";	
	private String strClotureFraude =					"SurvMP Recensement de la fraude > Déclaration en cours > Remise";

	//
	// Eligibilité
	//
	private String strServiceEligibilite = 			"Informations sur l'éligibilité des créances";


	public PobiProxyBiller() throws IOException {
		super();
	}

	public double getCostImpl(Context context, Object data) throws Exception {
		String html = (String) data;

		// Mise à jour "Votre déclaration a été remise à la Banque de France"			
		if ((html.indexOf(strFccDeclarationRemiseBDF) != -1)
				&& (html.indexOf(strFccDeclarationChequesPayes) != -1))
			return getFccCostProxy(html);

		// Mise à jour "Votre suppression a été remise à la Banque de France"			
		if ((html.indexOf(strFccSuppressionChequeBdFenInfraction) != -1)
				&& (html.indexOf(strFccSuppressionChequesPayes) != -1))
			return getFccCostProxy(html);

		if (html.indexOf(strFccConsultationFichierCentralCheques) != -1)
			return getFccCostProxy(html);

		if (html.indexOf(strFccFichierCentralCheques) != -1)
			return getFccCostProxy(html);

		if (html.indexOf("FICP") != -1)
			return getFicpCostProxy(html);

		if (html.indexOf("FNCI") != -1)
			return getFnciCostProxy(html);

		if (html.indexOf("SurvMP") != -1)
			return getSurvMPCostProxy(html);

		if (html.indexOf("ELIGIBILITE") != -1)
			return getEligibiliteCostProxy(html);

		return -1;
	}

	private double getFccCostProxy(String html) throws SQLException {
		/*		String cleBDF, sousCle = "";
		int nbp;
		int index1, index2, index3;

		if ((index1 = html.indexOf("inexistant")) != -1) {
			index2 = html.indexOf('>', index1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);

			nbp = 1;
		}
		else if ((html.indexOf("Relevé global") != -1) || (html.indexOf("Relevé\160global") != -1)) {
			index1 = html.indexOf("Clé BDF");
			index2 = html.indexOf('>', index1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);

			index1 = html.indexOf("Sous-Clé");
			index2 = html.indexOf('>', index1);
			index3 = html.indexOf('<', index2);
			sousCle = html.substring(index2 + 1, index3);

			nbp = (isFirstHomonyme(cleBDF, sousCle, "fcc") ? 0 : 1);
		}
		else if ((index1 = html.indexOf("dossiers recensés")) != -1) {
			index2 = html.indexOf(':', index1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
			cleBDF = cleBDF.replace((char) 0xA0, ' ');
			cleBDF = cleBDF.trim();

			nbp = 1;
		}
		else if (html.indexOf("Titulaire(s)") != -1) {
			index1 = html.indexOf("Coordonnée bancaire");
			index2 = html.indexOf("<span", index1);
			index2 = html.indexOf('>', index2);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
			cleBDF = cleBDF.replace((char) 0xA0, ' ');
			cleBDF = cleBDF.replace('\n', ' ');
			cleBDF = cleBDF.replace('\r', ' ');
			cleBDF = cleBDF.replace('\t', ' ');
			cleBDF = cleBDF.trim();
			StringEx sx = new StringEx(cleBDF);
			sx.replaceAll(" ", "");
			cleBDF = sx.toString();
			nbp = 1;
		}*/

		// Mise à jour "Votre déclaration a été remise à la Banque de France"			
		if ((html.indexOf(strFccDeclarationRemiseBDF) != -1)
				&& (html.indexOf(strFccDeclarationChequesPayes) != -1))
			return 0;

		// Mise à jour "Votre suppression a été remise à la Banque de France"			
		if ((html.indexOf(strFccSuppressionChequeBdFenInfraction) != -1)
				&& (html.indexOf(strFccSuppressionChequesPayes) != -1))
			return 0;

		// Mise à jour "déclaration personnes non immatriculées" 
		if (html.indexOf(strFccMajPersonneMoraleNonImmatriculee) != -1)			                  
			return 0;

		// Mise à jour "déclaration"
		if (html.indexOf("Votre déclaration a été enregistrée.") != -1)
			return 0;

		// Mise à jour "suppresion"
		if (html.indexOf("Votre suppression a été enregistrée.") != -1)
			return 0;

		// Enquête etat civil : retour à la liste !!!
		if (html.indexOf("<title>Object moved</title>") != -1)
			return -1;

		// Enquête etat civil : modification
		if (html.indexOf("Votre réponse à l'enquête d'état civil a bien été prise en compte pour traitement à la Banque de France") != -1)
			return 0;
		
		return -1;

		//		return getApplicationCost("fcc", cleBDF, sousCle, nbp);
	}

	private double getEligibiliteCostProxy(String html) throws SQLException {
		// Mise à jour "éligibilité" 
		if (html.indexOf(strServiceEligibilite) != -1)			                  
			return 0;

		return -1;
	}

	private double getFicpCostProxy(String html) throws SQLException {
		// Mise à jour "déclaration"
		if (html.indexOf("La déclaration d'incident a bien été enregistrée") != -1)
			return 0;

		// Mise à jour "paiement intégral"
		if (html.indexOf("Le paiement intégral des sommes dues a bien été enregistré") != -1)
			return 0;

		// Mise à jour "annulation"
		if (html.indexOf("L'annulation d'incident a bien été enregistrée") != -1)
			return 0;

		// Enquête etat civil : retour à la liste !!!
		if (html.indexOf("<title>Object moved</title>") != -1)
			return -1;
		
		// Enquête etat civil : modification
		if (html.indexOf("Votre réponse à l'enquête d'état civil a bien été prise en compte pour traitement à la Banque de France") != -1)
			return 0;
		
		
		/*String cleBDF, sousCle = "";
		int nbp;
		int index1, index2, index3;

		if ((html.indexOf("<title>Consultation FICP - Formulaire de recherche</title>") != -1) &&
				(index1 = html.indexOf("Dossier non trouvé")) != -1) {
			index2 = html.indexOf('>', index1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
			nbp = 1;
		}
		else if ((html.indexOf("<title>Consultation FICP - Relevé global</title>") != -1) &&
				(html.indexOf("Coordonnées du dossier") != -1)) {
			index1 = html.indexOf("Clé BDF");
			index2 = html.indexOf('>', index1);
			index2 = html.indexOf('>', index2 + 1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);

			index1 = html.indexOf("Homonymes");
			if (index1 != -1) {
				index2 = html.indexOf('>', index1);
				index3 = html.indexOf('<', index2);
				sousCle = html.substring(index2 + 1, index3);
				if (sousCle.length() == 1) sousCle = "0" + sousCle;
			}

			nbp = (isFirstHomonyme(cleBDF, sousCle, "ficp") ? 0 : 1);
		}
		else if ((index1 = html.indexOf("dossiers trouvés")) != -1) {
			index2 = html.indexOf('>', index1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
			cleBDF = cleBDF.replace((char) 0xA0, ' ');
			cleBDF = cleBDF.trim();

			nbp = 1;
		}
		else if (html.indexOf("Titulaire(s)") != -1) {
			index1 = html.indexOf("Coordonnée bancaire");
			index2 = html.indexOf("<span", index1);
			index2 = html.indexOf('>', index2);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
			cleBDF = cleBDF.replace((char) 0xA0, ' ');
			cleBDF = cleBDF.replace('\n', ' ');
			cleBDF = cleBDF.replace('\r', ' ');
			cleBDF = cleBDF.replace('\t', ' ');
			cleBDF = cleBDF.trim();
			StringEx sx = new StringEx(cleBDF);
			sx.replaceAll(" ", "");
			cleBDF = sx.toString();
			nbp = 1;
		}
		else {
			return -1;
		}

		if (cleBDF != "")		
			return getApplicationCost("ficp", cleBDF, sousCle, nbp);*/

		return -1;
	}

	protected boolean isFirstHomonyme(String cleBDF, String sousCle, String application) throws SQLException {
		boolean bFirstHomonyme = false;
		Statement statement = null;

		long startCost = System.currentTimeMillis();

		String sSqlRequest = null;
		try {
			StringEx sqlRequest = new StringEx(sqlRequester.getProperty(PobiBiller.PROPERTIES_SQL_REQUEST_GET_REQUEST));

			sqlRequest.replace("{RefClient}", certificate);
			sqlRequest.replace("{CleBdf}", cleBDF);
			sqlRequest.replace("{SousCleCompOp}", "=");
			sqlRequest.replace("{SousCle}", "");
			sqlRequest.replace("{Application}", application);

			sSqlRequest = sqlRequest.toString();
			Engine.logBillers.debug("[PobiProxyBiller] SQL: " + sSqlRequest);

			statement = sqlRequester.connection.createStatement();
			ResultSet resultSet = statement.executeQuery(sSqlRequest);

			resultSet.next();

			if (resultSet.getInt("nbclebdf") == 1) {
				Engine.logBillers.debug("[PobiProxyBiller] Homonyme page yet requested");

				statement.close();

				sqlRequest = new StringEx(sqlRequester.getProperty(PobiBiller.PROPERTIES_SQL_REQUEST_GET_REQUEST));

				sqlRequest.replace("{RefClient}", certificate);
				sqlRequest.replace("{CleBdf}", cleBDF);
				sqlRequest.replace("{SousCleCompOp}", " like ");
				sqlRequest.replace("{SousCle}", "[0-9][0-9]");
				sqlRequest.replace("{Application}", application);

				sSqlRequest = sqlRequest.toString();
				Engine.logBillers.debug("[PobiProxyBiller] SQL: " + sSqlRequest);

				statement = sqlRequester.connection.createStatement();
				resultSet = statement.executeQuery(sSqlRequest);

				resultSet.next();
				bFirstHomonyme = (resultSet.getInt("nbclebdf") == 0);

				Engine.logBillers.debug("[PobiProxyBiller] First homonyme: " + bFirstHomonyme);
			}

			return bFirstHomonyme;
		}
		catch(SQLException e) {
			Engine.logBillers.warn("[PobiProxyBiller] Unable to find whether it is the first homonyme; ignoring and assuming false.\n" + e.getMessage() + " (error code: " + e.getErrorCode() + ")\nSQL: " + sSqlRequest);
			return false;
		}
		catch(Exception e) {
			Engine.logBillers.error("[PobiProxyBiller] Unable to find whether it is the first homonyme; ignoring and assuming false.", e);
			return false;
		}
		finally {
			if (statement != null) {
				statement.close();
			}

			Engine.logBillers.info("[PobiBiller] isFirstHomonyme, 1 request in " + (System.currentTimeMillis() - startCost) + " ms");
		}
	}

	private double getFnciCostProxy(String html) throws SQLException {
		if (html.indexOf("Votre saisie a été enregistrée") != -1) {
			return 0;
		}
		else {
			return -1;
		}
	}

	private double getSurvMPCostProxy(String html) throws SQLException {		
		if (html.indexOf(strAccueilSurvMP) != -1)				// SurvMP page d'accueil
			return 0;

		if ((html.indexOf(strClotureCartographie) != -1) || ((html.indexOf(strCartographieSurvMP) != -1) && (html.indexOf(strCartographiePlus) != -1)))		// SurvMP Cartographie
			return 0;		

		if ((html.indexOf(strClotureCartoPrivative) != -1) || ((html.indexOf(strCartoPrivativeSurvMP) != -1)  && (html.indexOf(strCartographiePrivativePlus) != -1)))	// SurvMP Cartographie privative
			return 0;		

		// IMPORTANT : must have these two strings to trigger
		if ((html.indexOf(strSaisieSurvMP) != -1) && (html.indexOf(strContenuDeclarationSurvMP) != -1))			// SurvMP Homologation Chéque
			return 0;

		if (html.indexOf(strClotureChequeSurvMP) != -1)			// SurvMP Cloture Cheque Declaration
			return 0;		

		if (html.indexOf(strPreClotureDeclarationSurvMP) != -1)	// SurvMP PreCloture Declaration
			return 0;		

		if ((html.indexOf(strSaisieDeclarationFraude) != -1) && (html.indexOf(strSaisieDeclarationFraudePlus) != -1))	// SurvMP Saisie Fraude Declaration
			return 0;

		if (html.indexOf(strClotureFraude) != -1)				// SurvMP Cloture Fraude
			return 0;		

		// default
		return -1;
	}

	protected String getService(Context context, Object data) {
		String html = (String) data;

		// FCC
		/*if (((html.indexOf(strFccSuppressionChequeBdFenInfraction) != -1)
				&& (html.indexOf(strFccSuppressionChequesPayes) != -1))
				|| ((html.indexOf(strFccDeclarationRemiseBDF) != -1)
						&& (html.indexOf(strFccDeclarationChequesPayes) != -1))
						|| (html.indexOf(strFccConsultationFichierCentralCheques) != -1)
						|| (html.indexOf(strFccFichierCentralCheques) != -1))
			return "FCC mise à jour";*/
		if (html.indexOf("FCC") != -1 || html.indexOf("chèques payés") != -1)
			return "FCC mise à jour";

		// FICP
		if (html.indexOf("FICP") != -1)
			return "FICP mise à jour";

		// FNCI
		if (html.indexOf("FNCI") != -1)
			return "FNCI mise à jour";

		// SURVMP
		if ((html.indexOf(strClotureChequeSurvMP) != -1)
				|| (html.indexOf(strPreClotureDeclarationSurvMP) != -1)
				|| (html.indexOf(strSaisieSurvMP) != -1))
			return "Chèque";

		if (html.indexOf(strSaisieSurvMP) != -1)
			return "Chèque";

		if (html.indexOf(strAccueilSurvMP) != -1)
			return "SurvMP";

		if (html.indexOf(strServiceEligibilite) != -1)
			return "Eligibilité";

		if (((html.indexOf(strSaisieDeclarationFraude) != -1) && (html.indexOf(strSaisieDeclarationFraudePlus) != -1))
				|| (html.indexOf(strClotureFraude) != -1))
			return "Fraude";		

		if ((html.indexOf(strCartographieSurvMP) != -1) || (html.indexOf(strClotureCartographie) != -1))
			return "Cartographie";		

		if ((html.indexOf(strClotureCartoPrivative) != -1) || (html.indexOf(strCartoPrivativeSurvMP) != -1))
			return "Cartographie";		

		// default
		return super.getService(context, data);
	}

	protected String getModule(Context context, Object data) {
		String html = (String) data;

		// FCC
		if (html.indexOf("<title>Déclaration FCC -") !=-1)
			return "Déclaration incident";
		
		if (html.indexOf("<title>Suppression FCC -") !=-1)
			return "Suppression incident";
		
		if (html.indexOf("<title>Déclaration de chèques payés -") !=-1)
			return "Déclaration de chèques payés";
		
		if (html.indexOf("<title>Suppression de chèques payés -") !=-1)
			return "Suppression de chèques payés";
		
		if ((html.indexOf(strFccSuppressionChequeBdFenInfraction) != -1)
				&& (html.indexOf(strFccSuppressionChequesPayes) != -1))
			return "Suppression chèques payés";

		if ((html.indexOf(strFccDeclarationRemiseBDF) != -1)
				&& (html.indexOf(strFccDeclarationChequesPayes) != -1))
			return "Déclaration chèques payés";

		if (html.indexOf("<title>Gestion Etat Civil FCC</title>") !=-1)
			return "Gestion état civil";
		
		
		// FICP
		if (html.indexOf("<title>Gestion Etat Civil du FICP</title>") !=-1)
			return "Gestion état civil";

		if (html.indexOf("<title>Déclaration au FICP -") !=-1)
			return "Déclaration incident";
		
		if (html.indexOf("<title>Mise à jour du FICP -") !=-1)
			return "Suppression incident";
		
		
		// FNCI
		if (html.indexOf("FNCI : Saisie enregistrée") !=-1)
			return "Déclaration faux chèque(s)";
		
		if (html.indexOf("FNCI : suppression confirmée") !=-1)
			return "Suppression faux chèque(s)";
		
		
		// SURVMP
		if (html.indexOf(strCartographieSurvMP) != -1)
			return "Saisie";

		if (html.indexOf(strClotureCartographie) != -1)
			return "Clôture";

		if (html.indexOf(strCartoPrivativeSurvMP) != -1)
			return "Saisie";

		if (html.indexOf(strClotureCartoPrivative) != -1)
			return "Clôture";

		if (html.indexOf(strFccMajPersonneMoraleNonImmatriculee) != -1)
			return "Déclaration";

		if (html.indexOf(strPreClotureDeclarationSurvMP) != -1)
			return "Pré-Clôture";

		if (((html.indexOf(strSaisieDeclarationFraude) != -1) && (html.indexOf(strSaisieDeclarationFraudePlus) != -1))
				|| (html.indexOf(strSaisieSurvMP) != -1)) 
			return "Saisie";

		if (html.indexOf(strAccueilSurvMP) != -1)
			return "Accueil";

		if (html.indexOf(strServiceEligibilite) != -1)
			return ""; // empty

		if ((html.indexOf(strClotureChequeSurvMP) != -1)
				|| (html.indexOf(strClotureFraude) != -1))
			return "Clôture";

		// default
		return "(proxy)";
	}

	protected String getDataKey(Context context, Object data) {
		String html = (String) data;

		if (((html.indexOf(strFccSuppressionChequeBdFenInfraction) != -1) && (html
				.indexOf(strFccSuppressionChequesPayes) != -1))
				|| ((html.indexOf(strFccDeclarationRemiseBDF) != -1) && (html
						.indexOf(strFccDeclarationChequesPayes) != -1))
						|| (html.indexOf(strFccConsultationFichierCentralCheques) != -1)
						|| (html.indexOf(strFccFichierCentralCheques) != -1))
			return getFccBdfKeyProxy(html);

		if (html.indexOf("FICP") != -1)
			return getFicpBdfKeyProxy(html);

		if (html.indexOf("FNCI") != -1)
			return getFnciBdfKeyProxy(html);

		// return empty string here to avoid falling into
		// Exception generation just after
		// changed by jmc 05/11/2007
		if (html.indexOf("SurvMP") != -1)
			return ""; 

		// si on n'a pas trouvé
		Engine.logBillers.error(
				"[PobiBiller] Unable to get the transaction BDF key; aborting billing.",
				new EngineException("Unknown html page type (accepted types are: FCC, FIBEN, FNCI, FICP); the analyzed page is:\n" + html));
		return "?";
	}

	private String getFccBdfKeyProxy(String html) {
		String cleBDF = "";
		int index1, index2, index3;

		if ((index1 = html.indexOf("Référence du RIB en cours")) != -1) {
			index3 = html.indexOf('>', index1);
			index2 = html.indexOf('>', index3 + 1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
		}
		else if ((index1 = html.indexOf("Numéro Enquête")) != -1) {
			index2 = html.indexOf("<span", index1);
			index2 = html.indexOf('>', index2);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
		}
		else {
			Engine.logBillers.error(
					"[PobiBiller] Unable to get the transaction BDF key; aborting billing.",
					new EngineException("Unknown html page ; the analyzed page is:\n" + html)
					);
			cleBDF = "?";
		}

		return (cleBDF);
	}

	private String getFicpBdfKeyProxy(String html) {
		String cleBDF = "";
		int index1, index2, index3;

		if ((index1 = html.indexOf("Dossier non trouvé")) != -1) {
			index2 = html.indexOf('>', index1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
		}
		else if ((index1 = html.indexOf("Clé BDF")) != -1) {
			index2 = html.indexOf('>', index1);
			index2 = html.indexOf('>', index2 + 1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);

		}
		else if ((index1 = html.indexOf("dossiers trouvés")) != -1) {
			index2 = html.indexOf('>', index1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
			cleBDF = cleBDF.replace((char) 0xA0, ' ');
			cleBDF = cleBDF.trim();
		}
		else if (html.indexOf("Titulaire(s)") != -1) {
			index1 = html.indexOf("Coordonnée bancaire");
			index2 = html.indexOf("<span", index1);
			index2 = html.indexOf('>', index2);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
			cleBDF = cleBDF.replace((char) 0xA0, ' ');
			cleBDF = cleBDF.replace('\n', ' ');
			cleBDF = cleBDF.replace('\r', ' ');
			cleBDF = cleBDF.replace('\t', ' ');
			cleBDF = cleBDF.trim();
			StringEx sx = new StringEx(cleBDF);
			sx.replaceAll(" ", "");
			cleBDF = sx.toString();
		}
		else if ((index1 = html.indexOf("Numéro Enquête")) != -1) {
			index2 = html.indexOf("<span", index1);
			index2 = html.indexOf("<span", index2+1);
			index2 = html.indexOf('>', index2);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);
		}
		else {
			Engine.logBillers.error(
					"[PobiBiller] Unable to get the transaction BDF key; aborting billing.",
					new EngineException("Unknown html page ; the analyzed page is:\n" + html)
					);
			cleBDF = "?";
		}

		return (cleBDF);
	}

	private String getFnciBdfKeyProxy(String html) {
		String cleBDF = "";
		int index1, index2, index3;

		if ((index1 = html.indexOf("Référence du RIB en cours")) != -1) {
			index3 = html.indexOf('>', index1);
			index2 = html.indexOf('>', index3 + 1);
			index3 = html.indexOf('<', index2);
			cleBDF = html.substring(index2 + 1, index3);

			//
			// Format the 3 values as : "##### ####### ################"
			// jmc 18/05/06 
			//
			try {
				String strResult = "";
				StringTokenizer strtok = new StringTokenizer(cleBDF, " \r\n\t");

				while(strtok.hasMoreTokens())
					strResult += strtok.nextToken().trim() + " ";

				cleBDF = strResult.trim();
			}
			catch(Exception e) {
				cleBDF = "?";
			}
		}
		else if (html.indexOf("Référence") != -1) {
			if ((index1 = html.indexOf("Zone interbancaire")) != -1) {
				index2 = html.indexOf("<span", index1);
				index2 = html.indexOf('>', index2);
				index3 = html.indexOf('<', index2);
				cleBDF = html.substring(index2 + 1, index3);
				if ((index1 = html.indexOf("Zone interne")) != -1) {
					index2 = html.indexOf("<span", index1);
					index2 = html.indexOf('>', index2);
					index3 = html.indexOf('<', index2);
					cleBDF += "/"+ html.substring(index2 + 1, index3);
				}
				StringEx sx = new StringEx(cleBDF);
				sx.replaceAll(" ", "");
				cleBDF = sx.toString();
			}
		}
		else {
			Engine.logBillers.error(
					"[PobiBiller] Unable to get the transaction BDF key; aborting billing.",
					new EngineException("Unknown html page ; the analyzed page is:\n" + html)
					);
			cleBDF = "?";
		}

		return (cleBDF);
	}
}
