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

import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.engine.util.JavelinUtils;
import com.twinsoft.twinj.iJavelin;

/**
 * The FindString criteria is useful for searching in the host
 * screen a specific string at a given position with eventually
 * a given attribute.
 */
public abstract class LegacyScreenCriteria extends Criteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -811091253273139569L;
	
	/**
     * The horizontal coordinate at which the string search
     * begins.
     */
    protected int x = 0;
    
    /**
     * Retrieves the horizontal coordinate at which the string
     * search begins.
     *
     * @return the horizontal coordinate.
     */
    public int getX() {
        return x;
    }
    
    /**
     * Sets the horizontal coordinate at which the string
     * search begins.
     *
     * @param x the horizontal coordinate.
     */
    public void setX(int x) {
        this.x = x;
    }
    
    /**
     * The vertical coordinate at which the string search begins.
     */
    protected int y = 0;
    
    /**
     * Retrieves the vertical coordinate at which the string
     * search begins.
     *
     * @return the vertical coordinate.
     */
    public int getY() {
        return y;
    }
    
    /**
     * Sets the vertical coordinate at which the string
     * search begins.
     *
     * @param y the vertical coordinate.
     */
    public void setY(int y) {
        this.y = y;
    }
    
    /**
     * The attribute to search (-1 means no attribute check).
     */
    protected int attribute = -1;
    
    /**
     * Retrieves the searched attribute (-1 means that there
     * is no attribute check).
     *
     * @return the searched attribute.
     */
    public int getAttribute() {
        return attribute;
    }
    
    /**
     * Sets the searched attribute (-1 means that there is no
     * attribute check).
     *
     * @param attribute the attribute to search.
     */
    public void setAttribute(int attribute) {
        this.attribute = attribute;
    }
    
    /**
     * Constructs a new empty Criteria object.
     */
    public LegacyScreenCriteria() {
        super();
    }
    
    /**
     * Retrieves the longest substring that matches with the criteria's
     * attribute.
     *
     * @param javelin the Javelin object to examine.
     * @param bx the horizontal beginning coordinate.
     * @param by the vertical beginning coordinate.
     *
     * @return the substring.
     */
    protected String getSubStringOfSameAttribute(iJavelin javelin, int bx, int by) {
        StringBuffer substring = new StringBuffer("");
        
        int screenWidth = javelin.getScreenWidth();
        int screenHeight = javelin.getScreenHeight();
        
        // Case of attribute check: we search the longest string
        // of the same attribute.
        if (attribute != -1) {
            int cx = bx; // current column
            int cy = by; // current line
            int ba = getCharAttribute(javelin, bx, by);
            int len = 0;
            
            if (isSameAttribute(ba)) {
                while (isSameAttribute(javelin.getCharAttribute(cx, cy))) {
                    cx++;
                    len++;
                    if (cx >= screenWidth) {
                        cx = (cy == by ? (x == -1 ? bx : x) : 0);
                        substring.append(javelin.getString(cx, cy, len));
                        cx = 0;
                        len = 0;
                        cy++;
                        if (cy >= screenHeight) {
                            break;
                        }
                    }
                }
                
                // We must add the current line if it has not be terminated.
                if (cy < screenHeight) {
                    cx = (cy == by ? bx : 0);
                    substring.append(javelin.getString(cx, cy, len));
                }
            }
        }
        // Case of no attribute check: we extract the string
        // from (x, y) to the end of the screen.
        else {
            int cx;
            for (int cy = by ; cy < screenHeight ; cy++) {
                cx = (cy == by ? bx : 0);
                substring.append(javelin.getString(cx, cy, screenWidth - cx));
            }
        }
        
        return substring.toString();
    }
    
    protected abstract int getCharAttribute (iJavelin javelin, int bx, int by);


    protected boolean isSameAttribute(int attributeToCompare) {
    	return JavelinUtils.isSameAttribute(attribute, attributeToCompare);
    }
}