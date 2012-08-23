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

import com.twinsoft.twinj.iJavelin;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.*;
import com.twinsoft.convertigo.engine.*;
import org.apache.regexp.*;

/**
 * The RegularExpression class is useful for searching in the host
 * screen a specific string eventually at a given position.
 */
public class RegularExpression extends LegacyScreenCriteria {

	private static final long serialVersionUID = -6264619154116378252L;

	/**
     * The regular expression object.
     */
    transient protected RE regexp;
    
    /**
     * The regular expression to search.
     */
    private String regularExpression = "";
    
    /**
     * Retrieves the regular expression associated with the criteria.
     *
     * @return the criteria regular expression.
     */
    public String getRegularExpression() {
        return regularExpression;
    }
    
    /**
     * Sets the regular expression associated with the criteria.
     *
     * @param regularExpression the criteria regular expression.
     */
    public void setRegularExpression(String regularExpression) throws RESyntaxException {
        regexp = new RE(regularExpression);
        this.regularExpression = regularExpression;
    }
    
    /**
     * Constructs a new empty Criteria object.
     */
    public RegularExpression() {
        super();
        try {
            setRegularExpression("");
        }
        catch (RESyntaxException e) {
        }
    }
    
    protected boolean isMatching0(Connector connector) {
    	iJavelin javelin = ((JavelinConnector) connector).javelin;
        String substring;
        
    	int attribute = getAttribute();
    	int x = getX();
    	int y = getY();
    	
    	Engine.logBeans.trace("RegularExpression");
        Engine.logBeans.trace("  regularExpression: " + regularExpression);
        Engine.logBeans.trace("  attribute: " + attribute);
        
        // If the object has just been deserialized, we must
        // recreate the regular expression object.
        if (regexp == null) {
            try {
                setRegularExpression(regularExpression);
            }
            catch (RESyntaxException e) {
                Engine.logBeans.error("Unable to create the regular expression object", e);
            }
        }
        
        // Case (x, y)
        if ((x != -1) && (y != -1)) {
			Engine.logBeans.trace("  => case (x, y)");
			
            substring = getSubStringOfSameAttribute(javelin, x, y);
        
			// If the substring is empty, it doesn't match.
			if (substring.length() == 0) return false;
        
			Engine.logBeans.trace("  substring: " + substring);
        
			// We check the regular expression.
			boolean isMatching = regexp.match(substring);
        
			return isMatching;
        }
        // Case (*, y)
        else if ((x == -1) && (y != -1)) {
			Engine.logBeans.trace("  => case (*, y)");
            int screenWidth = javelin.getScreenWidth();
            for (int cx = 0 ; cx < screenWidth ; cx++) {
                substring = getSubStringOfSameAttribute(javelin, cx, y);
                
                int substringLength = substring.length();
                
                // If the substring is empty, it doesn't match.
                if (substringLength == 0) continue;
                
                if (regexp.match(substring)) return true;
                cx += substringLength - 1;
            }
            return false;
        }
        // Case (x, *)
        else if ((x != -1) && (y == -1)) {
			Engine.logBeans.trace("  => case (x, *)");
            int screenHeight = javelin.getScreenHeight();
            for (int cy = 0 ; cy < screenHeight ; cy++) {
                substring = getSubStringOfSameAttribute(javelin, x, cy);
                
                // If the substring is empty, it doesn't match.
                if (substring.length() == 0) continue;
                
                if (regexp.match(substring)) return true;
            }
            return false;
        }
        // Case (*, *)
        else {
			Engine.logBeans.trace("  => case (*, *)");
			int screenWidth = javelin.getScreenWidth();
			int screenHeight = javelin.getScreenHeight();
			for (int cx = 0 ; cx < screenWidth ; cx++) {
				for (int cy = 0 ; cy < screenHeight ; cy++) {
		            substring = getSubStringOfSameAttribute(javelin, cx, cy);
					Engine.logBeans.trace("  substring: " + substring);
        
					// If the substring is empty, it doesn't match.
					if (substring.length() == 0) continue;
        
					// We check the regular expression.
					boolean isMatching = regexp.match(substring);
        
					if (isMatching) return true;
				}
			}
        }
        
        return false;
    }
    
	@Override
	public String toString() {
        Object[] args = new Object[] { getRegularExpression() };
        return processToString(MessageFormat.format("Search ''{0}''", args));
	}

	@Override
	protected int getCharAttribute(iJavelin javelin, int bx, int by) {
		return javelin.getCharAttribute(bx, by);
	}
}