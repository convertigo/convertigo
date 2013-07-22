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
import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.xpath.XPathAPI;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class InseeBiller extends LCABiller {

	public InseeBiller() throws IOException {
		super();
	}
	
	protected boolean isBillable(Context context, String clef, Vector<String> fiches) {
		if (!super.isBillable(context))
			return false;
		
		if (!context.transactionName.equals("FicheDetaillee")) {
			Engine.logEngine.debug("[InseeBiller] Billing aborted : no billing behaviour described for this transaction.");
			return false;
		}
		
		Engine.logEngine.debug("[InseeBiller] Searching key '" + clef + "' in the context.");
		// if vector exists in context and if it contains the key siren/nic
		if (fiches != null && fiches.contains(clef)) {
			Engine.logEngine.debug("[InseeBiller] Billing aborted : key '" + clef + "' was found in the context.");
			return false;
		}
		
		return true;
	}
	
	public void insertBilling(Context context) throws EngineException {
		String clef 	= null;
		Vector<String> fiches 	= null;
		// retrieve clef value to search
		String myXPath = "//sql_output/row";
		NodeList rowElements;
		try {
			rowElements = XPathAPI.selectNodeList(context.outputDocument.getDocumentElement(), myXPath);
		} catch (TransformerException e) {
			Engine.logEngine.debug("[InseeBiller] Billing aborted : no 'row' node was found in the response dom.");
			return;
		}
		Element elem 	= (Element)rowElements.item(0);
		String siren 	= elem.getAttribute("siren");
		String nic 		= elem.getAttribute("nic");
		clef 			= siren + nic;
		// retrieve the vector containing fiches from the context
		fiches = GenericUtils.cast(context.get("fichesDetaillees"));
		
		if (!isBillable(context, clef, fiches))
			return;
		
		// vector doesn't exist in context ==> no fiche has been billed in this context
		// or it doesn't contain the key siren/nic
		Engine.logEngine.debug("[InseeBiller] Key '" + clef + "' not found in the context.");
		// do the billing
		doInsertBilling(context);
		
		// if vector doesn't exist in context, create one
		if (fiches == null)
			fiches = new Vector<String>();
		// add new key siren/nic to the vector
		fiches.add(clef);
		// save the vector in the context
		context.set("fichesDetaillees", fiches);
		Engine.logEngine.debug("[InseeBiller] Key '" + clef + "' added in the context.");
	}
}
