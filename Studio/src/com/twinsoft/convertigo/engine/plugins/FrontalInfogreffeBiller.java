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

import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.xpath.XPathAPI;

import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class FrontalInfogreffeBiller extends FrontalBiller {

	Infogreffe inf;
	
	public FrontalInfogreffeBiller() throws IOException {
		super();
		inf = new Infogreffe();
	}

	public double getCostImpl(Context context, Object data) throws Exception {
		// set the properties
		inf.setProp(context);
		// fill the errors from the context 
		// and the handling from the properties
		inf.setErrors(context);
		
		double usualPrice = -3, realPrice = -3, returnPrice = -3;
		
		if (inf.isResponseFromCache(context)) {
			realPrice = 0.0;
			returnPrice = -2;
		} else if (!inf.isBillable(context)) {
			realPrice = 0.0;
			returnPrice = 0.0;
		}
		
		try {
			usualPrice = inf.getPrice((Transaction)context.requestedObject);
			if (realPrice == -3)
				realPrice = usualPrice;
			if (returnPrice == -3)
				returnPrice = usualPrice;
		} catch (EngineException e) {
			usualPrice = -1;
			Engine.logEngine.debug("Exception while getting price from Infogreffe class : " + e.getMessage());
		}
		
		NodeList liste = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), "//coutReel");
		if (liste.getLength() != 0) {
			Element elem = (Element)liste.item(0);
			elem.getParentNode().removeChild(elem);
		}
		Infogreffe.addTextNodeUnderInfo(context, "coutReel", "" + realPrice);
		
		NodeList liste2 = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), "//coutUsuel");
		if (liste2.getLength() != 0) {
			Element elem2 = (Element)liste2.item(0);
			elem2.getParentNode().removeChild(elem2);
		}
		Infogreffe.addTextNodeUnderInfo(context, "coutUsuel", "" + usualPrice);
		
		return returnPrice;
	}

	protected String getDataKey(Context context, Object data) {
/*
		// siren
		NodeList elems = context.outputDocument.getElementsByTagName("return");
		if (elems.getLength() != 1) {
			Engine.logBillers.debug("[FrontalInfogreffeBiller] No one 'return' element in context output document.");
			return null;
		}
		Element returnElem = (Element) elems.item(0);
		NodeList sirens = null;
		try {
			sirens = XPathAPI.selectNodeList(returnElem, "//@siren");
		} catch (TransformerException e) {
			Engine.logBillers.debug("[FrontalInfogreffeBiller] No attribute 'siren' in context output document.");
			return null;
		}

		Node no = null;
		String siren = null;
		if (sirens.getLength() != 0) {
			no = sirens.item(0);
			siren = no.getNodeValue();
			Engine.logEngine.debug("Siren found : '" + siren + "'", context.log);
		}
*/
		String siren = context.outputDocument.getDocumentElement().getAttribute("siren");
		Engine.logEngine.debug("Siren found : '" + siren + "'");
		return siren;
	}

	protected String getModule(Context context, Object data) {
		// module
		if (context.transactionName.equals("GetKbis") || context.transactionName.equals("GetKbisByNumGestion"))
			return "KB";
		if (context.transactionName.equals("GetEtatEndettement"))
			return "PN";
		if (context.transactionName.equals("GetDernierStatutAJour")) 
			return "ST";
		if (context.transactionName.equals("GetListeActe"))
			return "LAC";
		if (context.transactionName.equals("GetActe"))
			return "AC";
		if (context.transactionName.equals("GetListeBilanComplet"))
			return "LBI";
		if (context.transactionName.equals("GetBilanComplet"))
			return "BI";
		if (context.transactionName.equals("GetListeBilanSaisi")) {
			NodeList listeBilanSaisiNodes = null;
			NodeList bilanSaisiNodes = null;
			try {
				listeBilanSaisiNodes = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), "//listeBilanSaisi");
				bilanSaisiNodes = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), "//bilanSaisi");
			} catch (TransformerException e) {
				return "LBS";
			}
			if(listeBilanSaisiNodes.getLength() == 0 && bilanSaisiNodes.getLength() != 0)
				// no list and bilansSaisi node
				return "BS";
			else
				return "LBS";
		}
		if (context.transactionName.equals("GetBilanSaisi"))
			return "BS";
		if (context.transactionName.equals("GetListeEtablissement"))
			return "LE";
		return null;
	}

}
