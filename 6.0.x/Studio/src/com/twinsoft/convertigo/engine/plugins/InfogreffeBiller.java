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
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class InfogreffeBiller extends LCABiller {
	
	Infogreffe inf;
	
	public InfogreffeBiller() throws IOException {
		super();
		inf = new Infogreffe();
	}

	
	/* BILLING METHODS */
	
	public void insertBilling(Context context) throws EngineException {
		// set the properties
		inf.setProp(context);
		// fill the errors from the context 
		// and the handling from the properties
		inf.setErrors(context);
		
		if (isBillable(context))
			// do the billing
			doInsertBilling(context);
	}
	
	protected boolean isBillable(Context context) {
		if (!super.isBillable(context))
			return false;
		
		if (!inf.isBillable(context))
			return false;
		
		return true;
	}
	
	
	/* PRICES METHODS */
	
	public double[] getPrices(Properties prop, Transaction transaction) throws EngineException {
		if (inf.getProp() == null)
			inf.setProp(prop);
		
		double prixTTC = inf.getPrice(transaction);
		double tauxTVA = Double.parseDouble(prop.getProperty(transaction.getName()	+ ".tauxTVA"));
		
		return createPricesTable(prixTTC, tauxTVA);
	}
	
	public double[] getPrices(Properties prop, Transaction transaction, Map<String, String> variables) throws EngineException {
		if (inf.getProp() == null)
			inf.setProp(prop);
		
		double prixTTC = inf.getPrice(transaction, variables);
		double tauxTVA = Double.parseDouble(prop.getProperty(transaction.getName()	+ ".tauxTVA"));
		
		return createPricesTable(prixTTC, tauxTVA);
	}
	
	private double getPrixHT(double prixTTC, double tauxTVA) {
		double x1 			= (prixTTC * 100 / tauxTVA);
		double x2 			= 1 + (100 / tauxTVA);
		double prixTotHT	= x1 / x2;
		return round(prixTotHT, 2);
	}
	
	private double[] createPricesTable (double prixTTC, double tauxTVA) {
		if (prixTTC == 0.0)
			return null;
		
		double prixHT 		= getPrixHT(prixTTC, tauxTVA);
		double montantTVA 	= round(prixTTC - prixHT, 2);
		
		double[] result 	= {prixHT, tauxTVA, montantTVA, prixTTC};
		return result;
	}
	
	
	/* LABELS METHODS */
	
	public String getLabel(Properties prop, Transaction transaction) throws EngineException {
		TransactionWithVariables trans;
		try {
			trans = (TransactionWithVariables)transaction;
		} catch (ClassCastException e) {
			throw new EngineException("Transaction with no variables are not accepted; the requested transaction is:" + transaction.getName());
		}
		
		if (transaction.getName().equals("GetEtatEndettement"))
			return getLabelByInscription(prop, trans);
		else 
			return getStandardLabel(prop, trans);
	}
	
	private String getStandardLabel(Properties prop, TransactionWithVariables transaction) {
		String label = prop.getProperty(transaction.getName() + ".label");
		
		return addModesLabels(prop, transaction, label);
	}
	
	private String addModesLabels(Properties prop, TransactionWithVariables transaction, String label) {
		Object tmp = transaction.getVariableValue("mode");
		
		// tmp might be null since 5.0.3 beans version
		if (tmp != null) {
			if (tmp instanceof Vector) {
				// multivaluated
				Vector<String> modes = GenericUtils.cast(tmp);
				if (modes.contains("XL"))
					label += " visu";
				if (modes.contains("C"))
					label += " courrier";
				if (modes.contains("M"))
					label += " email";
				if (modes.contains("T"))
					label += " pdf";
			} else if (tmp instanceof String[]) {
				// multivaluated
				String[] modes = (String[]) tmp;
				for (int i = 0 ; i < modes.length ; i++) {
					String mode = modes[i];
					if (mode.equals("XL"))
						label += " visu";
					if (mode.equals("C"))
						label += " courrier";
					if (mode.equals("M"))
						label += " email";
					if (mode.equals("T"))
						label += " pdf";
				}
			} else {
				// only one value
				String mode = (String) tmp;
				if (mode.equals("XL"))
					label += " visu";
				if (mode.equals("C"))
					label += " courrier";
				if (mode.equals("M"))
					label += " email";
				if (mode.equals("T"))
					label += " pdf";
			}
		}
				
		return label;
	}
	
	private String getLabelByInscription(Properties prop, TransactionWithVariables transaction) {
		String label = "";
		
		Object tmp = transaction.getVariableValue("inscription");
		
		// tmp might be null since 5.0.3 beans version
		if (tmp != null) {
			if (tmp instanceof Vector) {
				// multivaluated
				Vector<String> inscriptions = GenericUtils.cast(tmp);
				if (inscriptions.contains("01"))
					label += " Fonds de commerce";
				if (inscriptions.contains("02"))
					label += " Vendeur";
				if (inscriptions.contains("03"))
					label += " Privilège sécu";
				if (inscriptions.contains("04"))
					label += " Privilège trésor";
				if (inscriptions.contains("05"))
					label += " Crédit bail";
				if (inscriptions.contains("06"))
					label += " Protêts";
				if (inscriptions.contains("07"))
					label += " Privilège matériel";
				if (inscriptions.contains("08"))
					label += " Warrants";
				if (inscriptions.contains("17"))
					label += " Contrat de location";
				if (inscriptions.contains("18"))
					label += " Réserve de propriété";
				if (inscriptions.contains("20"))
					label += " Déclaration de créance";
				if (inscriptions.contains("21"))
					label += " Nantissement";
				if (inscriptions.contains("23"))
					label += " Biens inaliénables";
				if (inscriptions.contains("25"))
					label += " Nantissement artisanal";
			} else if (tmp instanceof String[]) {
				// multivaluated
				String[] inscriptions = (String[]) tmp;
				for (int i = 0 ; i < inscriptions.length ; i++) {
					String inscription = inscriptions[i];
					if (inscription.equals("01"))
						label += " Fonds de commerce";
					if (inscription.equals("02"))
						label += " Vendeur";
					if (inscription.equals("03"))
						label += " Privilège sécu";
					if (inscription.equals("04"))
						label += " Privilège trésor";
					if (inscription.equals("05"))
						label += " Crédit bail";
					if (inscription.equals("06"))
						label += " Protêts";
					if (inscription.equals("07"))
						label += " Privilège matériel";
					if (inscription.equals("08"))
						label += " Warrants";
					if (inscription.equals("17"))
						label += " Contrat de location";
					if (inscription.equals("18"))
						label += " Réserve de propriété";
					if (inscription.equals("20"))
						label += " Déclaration de créance";
					if (inscription.equals("21"))
						label += " Nantissement";
					if (inscription.equals("23"))
						label += " Biens inaliénables";
					if (inscription.equals("25"))
						label += " Nantissement artisanal";
				}
			} else {
				// only one value
				String inscriptions = (String) tmp; 
				if (inscriptions.equals("01"))
					label += " Fonds de commerce";
				if (inscriptions.equals("02"))
					label += " Vendeur";
				if (inscriptions.equals("03"))
					label += " Privilège sécu";
				if (inscriptions.equals("04"))
					label += " Privilège trésor";
				if (inscriptions.equals("05"))
					label += " Crédit bail";
				if (inscriptions.equals("06"))
					label += " Protêts";
				if (inscriptions.equals("07"))
					label += " Privilège matériel";
				if (inscriptions.equals("08"))
					label += " Warrants";
				if (inscriptions.equals("17"))
					label += " Contrat de location";
				if (inscriptions.equals("18"))
					label += " Réserve de propriété";
				if (inscriptions.equals("20"))
					label += " Déclaration de créance";
				if (inscriptions.equals("21"))
					label += " Nantissement";
				if (inscriptions.equals("23"))
					label += " Biens inaliénables";
				if (inscriptions.equals("25"))
					label += " Nantissement artisanal";
			}
		}
		
		return addModesLabels(prop, transaction, label);
	}
	
}