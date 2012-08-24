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

package com.twinsoft.convertigo.beans.statements;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class ContinueWithSiteClipperStatement extends Statement implements ITagsProperty {
	private static final long serialVersionUID = 1473841735963382690L;

	private String siteClipperConnectorName = "";

	public ContinueWithSiteClipperStatement() {
		super();
	}

	@Override
	public String toJsString() {
		return "";
	}

	public String getSiteClipperConnectorName() {
		return siteClipperConnectorName;
	}

	public void setSiteClipperConnectorName(String siteClipperConnectorName) {
		this.siteClipperConnectorName = siteClipperConnectorName;
	}

	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("siteClipperConnectorName")) {
			return getSiteClippersConnectorNames(getProject());
		}
		return new String[0];
	}

	public static String[] getSiteClippersConnectorNames(Project projectLoaded) {
		List<String> connectorList = new ArrayList<String>();
		connectorList.add("");
		for (Connector connector : projectLoaded.getConnectorsList()) {
			if (connector instanceof SiteClipperConnector) {
				connectorList.add(connector.getName());
			}
		}
		return connectorList.toArray(new String[connectorList.size()]);
	}

	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (siteClipperConnectorName.equals("")) {
				throw new EngineException("You didn't choose a Site Clipper connector for redirection.");
			}
			Connector siteClipperConnector = getProject().getConnectorByName(siteClipperConnectorName);
			com.twinsoft.convertigo.engine.Context context = getParentTransaction().context;
			
			String targetURL = getConnector().getHtmlParser().getReferer(context);
			String location = SiteClipperConnector.constructSiteLocation(context, siteClipperConnector, targetURL);
			getConnector().context.outputDocument.getDocumentElement().setAttribute("redirect_location", location);
			ReturnStatement.returnLoop(this.parent, "skip");

			context.setSharedScope(RhinoUtils.copyScope(javascriptContext, scope));
			
			context.stopXulRecording();
		}

		return isEnable();
	}
	
	@Override
	public String toString() {
		return (siteClipperConnectorName.equals("") ? "! no connector selected ! " : "") + super.toString();
	}
}