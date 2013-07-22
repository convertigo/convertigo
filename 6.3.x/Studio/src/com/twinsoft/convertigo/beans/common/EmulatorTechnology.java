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
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

/**
 * The EmulatorTechnology criteria is useful for detecting which
 * emulator technology the current Javelin object is using.
 */
public class EmulatorTechnology extends Criteria {

	private static final long serialVersionUID = 6298613096493213182L;

    public static final String BULLDKU = "Bull DKU 7107";
    public static final String IBM3270 = "IBM 3270";
    public static final String IBM5250 = "IBM 5250 (AS/400)";
    public static final String VDX = "Videotex (Minitel)";
    public static final String VT = "Unix (VT220)";
	
	/**
     * Constructs a new empty EmulatorTechnology object.
     */
    public EmulatorTechnology() {
        super();
    }
    
    protected boolean isMatching0(Connector connector) {
        try {
			JavelinConnector javelinConnector = (JavelinConnector) connector;
	    	iJavelin javelin = ((JavelinConnector) connector).javelin;
            String projectConnectorTechnology = javelinConnector.getEmulatorTechnology();
            String javelinTerminalClass = javelin.getTerminalClass();
            return (javelinTerminalClass.equals(projectConnectorTechnology));
        }
        catch(Exception e) {
            Engine.logBeans.error("Unable to match the emulator technology", e);
            return false;
        }
    }

	@Override
	public String toString() {
		String technology = null;
		try {
			String techClassName = ((JavelinConnector)getConnector()).getEmulatorTechnology();
			if (techClassName.equals(com.twinsoft.api.Session.DKU)) technology = BULLDKU;
			else if (techClassName.equals(com.twinsoft.api.Session.SNA)) technology = IBM3270;
			else if (techClassName.equals(com.twinsoft.api.Session.AS400)) technology = IBM5250;
			else if (techClassName.equals(com.twinsoft.api.Session.VDX)) technology = VDX;
			else if (techClassName.equals(com.twinsoft.api.Session.VT)) technology = VT;
			
		}
		catch (Exception e) {}
		return processToString(technology == null ? getName():technology);
	}
}