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

package com.twinsoft.convertigo.beans.transactions;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class SiteClipperTransaction extends TransactionWithVariables {
	private static final long serialVersionUID = -1726228364762127385L;
	
		
	private String targetURL = "http://www.convertigo.com/";

	public SiteClipperTransaction() {
		super();
	}

	@Override
	public String getRequestString(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	protected void insertObjectsInScope() throws EngineException {
		super.insertObjectsInScope();
	}

	@Override
	public void runCore() throws EngineException {
		String location = SiteClipperConnector.constructSiteLocation(context, getConnector(), targetURL);		
		Engine.logBeans.debug("(SiteClipperTransaction) Computed location for SiteClipper : " + location);
		context.outputDocument.getDocumentElement().setAttribute("redirect_location", location);
		
		context.setSharedScope(RhinoUtils.copyScope(((RequestableThread) Thread.currentThread()).javascriptContext, scope));
	}

	@Override
	public void setStatisticsOfRequestFromCache() {
		// TODO Auto-generated method stub

	}
	
	public String getTargetURL() {
		return targetURL;
	}

	public void setTargetURL(String targetURL) {
		this.targetURL = targetURL;
	}
}
