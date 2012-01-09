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

import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.beans.core.Statement;

abstract public class XpathableStatement extends Statement implements IXPathable{
	private static final long serialVersionUID = -1382597462556957815L;
	
	protected String xpath = "";
	
	public XpathableStatement() {
		super();
	}
	
	public XpathableStatement(String xpath) {
		this.xpath = xpath;
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
	
	public void setPureXpath(String xpath){
		xpath = "'"+xpath.replace("\\", "\\\\").replace("'", "\\'")+"'";
		setXpath(xpath);
	}
}