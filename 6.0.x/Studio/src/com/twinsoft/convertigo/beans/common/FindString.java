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

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

/**
 * The FindString criteria is useful for searching in the host
 * screen a specific string at a given position with eventually
 * a given attribute.
 */
public class FindString extends LegacyScreenCriteria {

	private static final long serialVersionUID = -5863908759643169363L;

	/**
     * The string to search.
     */
    protected String string = "";
    
    /**
     * Retrieves the string to search.
     *
     * @return the string to search.
     */
    public String getString() {
        return string;
    }
    
    /**
     * Sets the string to search.
     *
     * @param string the string to search.
     */
    public void setString(String string) {
        this.string = string;
    }
    
    /**
     * Constructs a new empty Criteria object.
     */
    public FindString() {
        super();
    }
    
    protected boolean isMatching0(Connector connector) {
    	iJavelin javelin = ((JavelinConnector) connector).javelin;

        Engine.logBeans.trace("FindString");
        Engine.logBeans.trace("  string: " + string);
        Engine.logBeans.trace("  attribute: " + attribute);
		Engine.logBeans.trace("  position: (" + x + ", " + y + ")");
		
		int stringLen = string.length();
		int screenWidth = javelin.getScreenWidth();

		if (x + stringLen > screenWidth) {
			Engine.logBeans.warn("The criteria \"" + getName() + "\" exceeds the screen width; please correct it!");
		}
		else {
			// First, verify attribute
			if (attribute != -1) {
				for (int i = 0 ; i < stringLen ; i++) {
					if (!isSameAttribute(javelin.getCharAttribute(x + i, y))) {
						Engine.logBeans.trace("  Attribute Not matching : criteriaFailed");
						return false;
					}
				}
			}
			
			// Then, verify text
			String stringAtGivenPosition = javelin.getString(x, y, string.length());
			Engine.logBeans.trace("  String found on screen :  [" + stringAtGivenPosition + "]");
			if (stringAtGivenPosition.equals(string)) return true;
		}
		
		return false;
    }
    
	@Override
	public String toString() {
        Object[] args = new Object[] { getString() };
        return processToString(MessageFormat.format("Search ''{0}''", args));
	}

	@Override
	protected int getCharAttribute(iJavelin javelin, int bx, int by) {
		return javelin.getCharAttribute(bx, by) & 0x1B3F;
	}
    
}