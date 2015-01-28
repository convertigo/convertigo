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

import java.io.IOException;
import com.twinsoft.convertigo.engine.Context;

public class AltaresBiller extends FrontalBiller {

	Altares altares;
	
	/**
	 * @throws IOException
	 */
	public AltaresBiller() throws IOException {
		super();
		altares = new Altares();
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.engine.plugins.FrontalBiller#getCostImpl(com.twinsoft.convertigo.engine.Context, java.lang.Object)
	 */
	public double getCostImpl(Context context, Object data) throws Exception {
		// Set the properties
		altares.setProp(loadCosts(context));
		
		// Do not bill responses from cache
		if (altares.isResponseFromCache(context))
			return -2;
		
		// Do not bill responses in error
		if (!altares.isResponseCorrect(context))
			return 0;
		
		return altares.getCost(context);
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.engine.plugins.Biller#getDataKey(com.twinsoft.convertigo.engine.Context, java.lang.Object)
	 */
	protected String getDataKey(Context context, Object data) {
		String transaction = context.transactionName;
		String dataKey = "";
		
		if (transaction.equals("doRechercheDirigeant")) {
			dataKey = altares.getResponseParameterValue(context, "nom");
			dataKey += "/" + altares.getResponseParameterValue(context, "prenom");
			dataKey += "/" + altares.getResponseParameterValue(context, "annee");
			dataKey += "/" + altares.getResponseParameterValue(context, "lieu");
		}
		else if (transaction.equals("doRechercheEntreprise")) {
			dataKey = altares.getResponseParameterValue(context, "siren");
			dataKey += "/" + altares.getResponseParameterValue(context, "siret");
		}
		else if (transaction.equals("doRechercheSimple")) {
			dataKey = altares.getResponseParameterValue(context, "ou");
			dataKey += "/" + altares.getResponseParameterValue(context, "qui");
		}
		else if (transaction.equals("getBilan") ||
				transaction.equals("getBilanComplet") ||
				transaction.equals("getBilanRetraite") ||
				transaction.equals("getCompteResultat") ||
				transaction.equals("getRatioIndicateurCourtTerme") ||
				transaction.equals("getRatioRentabilite") ||
				transaction.equals("getRatioStructure") ||
				transaction.equals("getSoldeIntermediaireGestion") ||
				transaction.equals("getSyntheseFinanciere")) {
			dataKey = altares.getResponseParameterValue(context, "siren");
			dataKey += "/" + altares.getResponseParameterValue(context, "bilanId");
		}
		else if (transaction.equals("getIdentiteAltaN3Etablissement") ||
				transaction.equals("getIdentiteAltaN4Etablissement")) {
			dataKey = altares.getResponseParameterValue(context, "sirenSiret");
		}
		else if (transaction.equals("getHistoriqueIdentiteEtablissement")) {
			dataKey = altares.getResponseParameterValue(context, "siret");
		}
		else {
			dataKey = altares.getResponseParameterValue(context, "siren");
		}
		
		return dataKey;
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.engine.plugins.Biller#getModule(com.twinsoft.convertigo.engine.Context, java.lang.Object)
	 */
	protected String getModule(Context context, Object data) {
		return context.transactionName;
	}
}
