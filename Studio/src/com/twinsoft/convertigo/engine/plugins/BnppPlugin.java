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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.engine.plugins;

import java.sql.Timestamp;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

//import org.apache.log4j.DailyRollingFileAppender;
//import org.apache.log4j.FileAppender;
//import org.apache.log4j.Logger;
//import org.apache.log4j.PatternLayout;

import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.util.Log;

public class BnppPlugin extends Plugin {

//	static Logger customLog = Logger.getLogger(BnppPlugin.class);
	static Log logCustom;
	
	public BnppPlugin() {
		super();
		this.prop_prefix = "bnpp.";
	}

	@Override
	protected void init(Properties properties) {
		super.init(properties);
/*		
		PatternLayout layout = new PatternLayout("%m%n");
	    FileAppender appender = null;
		try {
			appender = new DailyRollingFileAppender(layout,Engine.LOG_PATH + "/custom.log","'.'yyyy-MM-dd");
			customLog.addAppender(appender);

			String headerLine = getProperty("custom.log.header","ClientIP,SessionID,Date,User,Request URI,Transaction,Parameters,Convertigo duration (ms),BDF duration (ms)");
			if (headerLine != null) {
			 	String s = ""; 
				String[] headers = headerLine.split(","); 
			 	for (int i=0;i<headers.length;i++) 
			 		s += (s.equals("")?"":"\t| ") + headers[i]; 
			 	customLog.info("\n"+s);
			 }
			
		} catch(Exception e) {
			e.printStackTrace();
		}
*/
		try {
			if ("true".equals(getProperty("custom.log", "false"))) {
				logCustom = new Log(Engine.LOG_PATH + "/custom.log");
				logCustom.logLevel = Log.LOGLEVEL_MESSAGE;
				logCustom.setLogStringFormat("%m" + Log.lineSeparator);
				logCustom.createDailyLogFiles(true);
				String headerLine = getProperty("custom.log.header","ClientIP,SessionID,Date,User,Request URI,Transaction,Parameters,Convertigo duration (ms),BDF duration (ms)");
				if (headerLine != null) {
				 	String s = ""; 
					String[] headers = headerLine.split(","); 
				 	for (int i=0;i<headers.length;i++) 
				 		s += (s.equals("")?"":"\t| ") + headers[i]; 
				 	logCustom.message("\n"+s);
				 }
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void destroy() {
		super.destroy();
	}
	
	@Override
	protected void httpConnectorGetDataStart(Context context) {
		if ("true".equals(getProperty("custom.log", "false"))) {
			try {
				context.httpServletRequest.setAttribute("request.hostDuration", new Long(0L));
			}
			catch (Exception e) {}
		}
	}

	@Override
	protected void httpConnectorGetDataEnd(Context context, Long t0, Long t1) {
		if ("true".equals(getProperty("custom.log", "false"))) {
			try {
				context.httpServletRequest.setAttribute("request.hostDuration", new Long(Math.abs(t1 - t0)));
			}
			catch (Exception e) {}
		}
	}

	@Override
	protected void httpServletRequestEnd(HttpServletRequest request, Long t0, Long t1) {
		if ("true".equals(getProperty("custom.log", "false"))) {
			try {
				if ("PobiCommon".equals(request.getAttribute("request.projectName")))
					return;
				
				String defaultLogLine = "request.clientIP,request.sessionID,datetime,request.userName,request.uri,request.transactionName,request.transactionVariable,duration,request.hostDuration";
				String logLine = getProperty("custom.log.line",defaultLogLine);
				if (logLine != null) {
					String[] keys = logLine.split(",");
                    Object value = null; 
                    String s = ""; 
                    for (String key : keys) { 
                    	if (key.startsWith("request.")) 
                    		value = request.getAttribute(key); 
                    	else if (key.equals("datetime")) 
                    		value = new Timestamp(t0).toString().substring(0, 19); 
                    	else if (key.equals("duration")) 
                    		value = new Long(Math.abs(t1 - t0)); 
                                             
                    	value = (value == null) ? "_":value.toString(); 
                    	s += (s.equals("")?"":"\t\t") + value; 
                    }
                    //customLog.info(s);
                    logCustom.message(s);
				}
			}
			catch (Exception e) {}
		}
	}

	@Override
	protected void requesterCoreProcessRequestStart(Context context, Object inputData) {
		if ("true".equals(getProperty("custom.log", "false"))) {
			try {
				HttpServletRequest request = (HttpServletRequest)inputData;
				
				request.setAttribute("request.hostDuration", new Long(0L));
			}
			catch (Exception e) {}
		}
	}

	@Override
	protected void requesterCoreProcessRequestEnd(Context context, Object inputData) {
		if ("true".equals(getProperty("custom.log", "false"))) {
			try {
				HttpServletRequest request = (HttpServletRequest)inputData;
				
				request.setAttribute("request.sessionID", context.httpSession.getId());
				request.setAttribute("request.clientIP", context.remoteAddr);
				request.setAttribute("request.uri", request.getRequestURI());
				request.setAttribute("request.userName", context.tasUserName);
				request.setAttribute("request.projectName", context.projectName);
				request.setAttribute("request.transactionName", context.transactionName);
				
				Object variableValue = getVariableValue(context);
				if (variableValue != null)
					request.setAttribute("request.transactionVariable", variableValue);
			}
			catch (Exception e) {}
		}
	}
	
	private Object getTransactionVariableValue(Context context, String variableName) {
		TransactionWithVariables transaction = (TransactionWithVariables)context.transaction;
		Object variableValue = transaction.getVariableValue(variableName);
		return variableValue;
	}
	
	private Object getVariableValue(Context context) {
		String projectName = context.projectName;
		String transactionName = context.transactionName;
		String s = null;
		
		if (projectName.equals("BnppInfoGreffe")) {
			if (transactionName.equals("GetKbisByNumGestion")) {
				String greffe =  (String)getTransactionVariableValue(context, "greffe");
				String millesime = (String)getTransactionVariableValue(context, "dossier_millesime");
				String statut = (String)getTransactionVariableValue(context, "dossier_statut");
				String chrono = (String)getTransactionVariableValue(context, "dossier_chrono");
				s = greffe + " " + millesime + " " + statut + " " + chrono;
			}
			else {
				s = (String)getTransactionVariableValue(context, "siren");
			}
		}
		else if (projectName.equals("BnppAltaresRisque")) {
			s = (String)getTransactionVariableValue(context, "siren");
		}
		else if (projectName.equals("BnppAltaresIdentite")) {
			if (transactionName.equals("doRechercheDirigeant")) {
				String nom = (String)getTransactionVariableValue(context, "nom");
				String prenom = (String)getTransactionVariableValue(context, "prenom");
				String annee = (String)getTransactionVariableValue(context, "annee");
				String lieu = (String)getTransactionVariableValue(context, "lieu");
    			s = nom + " " + prenom + " " + annee + " " + lieu;
			}
			else if (transactionName.equals("doRechercheEntreprise")) {
				String siren = (String)getTransactionVariableValue(context, "siren");
				String siret = (String)getTransactionVariableValue(context, "siret");
				s = siren + " " + siret;
			}
			else if (transactionName.equals("doRechercheSimple")) {
				String ou = (String)getTransactionVariableValue(context, "ou");
				String qui = (String)getTransactionVariableValue(context, "qui");
				s = ou + " " + qui;
			}
			else if (transactionName.equals("getBilan") ||
					transactionName.equals("getBilanComplet") ||
					transactionName.equals("getBilanRetraite") ||
					transactionName.equals("getCompteResultat") ||
					transactionName.equals("getRatioIndicateurCourtTerme") ||
					transactionName.equals("getRatioRentabilite") ||
					transactionName.equals("getRatioStructure") ||
					transactionName.equals("getSoldeIntermediaireGestion") ||
					transactionName.equals("getSyntheseFinanciere")) {
				String siren = (String)getTransactionVariableValue(context, "siren");
				String bilanId = (String)getTransactionVariableValue(context, "bilanId");
				s = siren + " " + bilanId;
				
			}
			else if (transactionName.equals("getIdentiteAltaN3Etablissement") ||
				transactionName.equals("getIdentiteAltaN4Etablissement")) {
				s = (String)getTransactionVariableValue(context, "sirenSiret");
			}
			else if (transactionName.equals("getHistoriqueIdentiteEtablissement")) {
				s = (String)getTransactionVariableValue(context, "siret");
			}
			else {
				s = (String)getTransactionVariableValue(context, "siren");
			}
		}
		else if (projectName.equals("PobiFiben")) {
			if (transactionName.startsWith("m")) {
				if (transactionName.equalsIgnoreCase("m07")) {
					s = (String)getTransactionVariableValue(context, "denom");
				}
				else {
					s = (String)getTransactionVariableValue(context, "id");
				}
			}
			else if (transactionName.equalsIgnoreCase("ls")) {
				String param1 = null;
				param1 = (((param1 == null)||(param1.equals(""))) ? (String)getTransactionVariableValue(context, "Email"):param1);
				param1 = (((param1 == null)||(param1.equals(""))) ? (String)getTransactionVariableValue(context, "Guichet"):param1);
				param1 = (((param1 == null)||(param1.equals(""))) ? (String)getTransactionVariableValue(context, "cdguichet"):param1);
				s = ((param1 == null) ? "":param1);
			}
			else {
				s = (String)getTransactionVariableValue(context, "id");
			}
		}
		else if (projectName.equals("PobiFicp")) {
			if (transactionName.equals("GlobalePersonnePhysique")) {
    			s = (String)getTransactionVariableValue(context, "cleBdf");
			}
		}
		else if (projectName.equals("PobiFcc")) {
			if ((transactionName.equals("DetailleePersonnePhysique")) ||
				(transactionName.equals("GlobalePersonnePhysique"))) {
    			s = (String)getTransactionVariableValue(context, "cleBdf");
			}
			else if ((transactionName.equals("DetailleePersonneMorale")) || 
					(transactionName.equals("GlobalePersonneMorale"))) {
				String natimmat = (String)getTransactionVariableValue(context, "NatImmat");
				String numimmat = (String)getTransactionVariableValue(context, "NumImmat");
    			s = natimmat + " " + numimmat;
			}
			else if (transactionName.equals("TitulaireCompte")) {
				String codEtc = (String)getTransactionVariableValue(context, "codEtc");
				String codGuich = (String)getTransactionVariableValue(context, "codGuich");
				String numCompte = (String)getTransactionVariableValue(context, "numCompte");
    			s = codEtc + " " + codGuich + " " + numCompte;
			}
			else if (transactionName.equals("PmncDemande")) {
				String denom = (String)getTransactionVariableValue(context, "denom");
				String lig2 = (String)getTransactionVariableValue(context, "lig2");
				String lig3 = (String)getTransactionVariableValue(context, "lig3");
    			s = denom + "/" + lig2+ "/" + lig3;
				
			}
			else if (transactionName.equals("PmncReponse")) {
				s = (String)getTransactionVariableValue(context, "numDPMN");
			}
		}
		else if (projectName.equals("PobiFnci")) {
			if (transactionName.equals("EtatRib")) {
    			String codebanque = (String)getTransactionVariableValue(context, "codebanque");
    			String codeguichet = (String)getTransactionVariableValue(context, "codeguichet");
    			String rib1 = (String)getTransactionVariableValue(context, "rib1");
    			String rib2 = (String)getTransactionVariableValue(context, "rib2");
    			s = codebanque + " " + codeguichet + " " + rib1 + " / " + rib2;
			}
		}
		
		return s;
	}

}
