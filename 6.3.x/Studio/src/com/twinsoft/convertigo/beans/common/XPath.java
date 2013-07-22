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

package com.twinsoft.convertigo.beans.common;

import java.text.MessageFormat;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

public class XPath extends Criteria implements IXPathable {

	private static final long serialVersionUID = -210452202846966788L;
	
	private String xpath= "";
	
	public XPath() {
		super();
	}

	protected boolean isMatching0(Connector connector) {
		NodeList nodeList = null;
		int length = 0;
		
		HtmlConnector htmlConnector = (HtmlConnector)connector;
		Document xmlDocument = htmlConnector.getCurrentXmlDocument();
		TwsCachedXPathAPI xpathApi = htmlConnector.context.getXpathApi();
		
		if ((xmlDocument == null) || (xpathApi == null)) {
			Engine.logBeans.warn((xmlDocument == null) ? "(XPath) Current DOM of HtmlConnector is Null!":"TwsCachedXPathAPI of HtmlConnector is Null!");
			return false;
		}
		try {
			nodeList = xpathApi.selectNodeList(xmlDocument, xpath);
		} catch (TransformerException e) {
			return false;
		}
		length = nodeList.getLength();
		return (length > 0) ? true:false;
	}

	/**
	 * @return Returns the xpath.
	 */
	public String getXpath() {
		return xpath;
	}

	/**
	 * @param xpath The xpath to set.
	 */
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#toString()
	 */
	@Override
	public String toString() {
        Object[] args = new Object[] { getXpath() };
        return processToString(MessageFormat.format("Exists node at ''{0}''", args));
	}
}