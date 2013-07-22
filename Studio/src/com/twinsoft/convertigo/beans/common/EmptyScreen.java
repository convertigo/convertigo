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

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.twinj.iJavelin;

/**
 * The EmptyScreen criteria is useful for detect empty screen.
 */
public class EmptyScreen extends Criteria {

	private static final long serialVersionUID = -13052242781461710L;

	/**
     * Constructs a new empty Criteria object.
     */
    public EmptyScreen() {
        super();
    }
    
    protected boolean isMatching0(Connector connector) {
    	iJavelin javelin = ((JavelinConnector) connector).javelin;
        String line;
        int screenWidth = javelin.getScreenWidth();
        int screenHeight = javelin.getScreenHeight();
        for (int i = 0 ; i < screenHeight ; i++) {
        	line = javelin.getString(0, i, screenWidth).trim();
        	if (line.length() > 0) return false;
        }
		
		return true;
    }
    
	@Override
	public String toString() {
		return processToString("Search empty screen");
	}
}