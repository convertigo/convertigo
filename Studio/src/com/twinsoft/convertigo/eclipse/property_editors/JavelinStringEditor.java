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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.awt.*;

import java.lang.reflect.Method;

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class JavelinStringEditor extends JavelinPropertyEditorSupport {

    public JavelinStringEditor() {
    }

    protected String string;
    
    public void setValue(Object o) {
        string = (String) o;
        firePropertyChange();
    }
    
    public Object getValue() {
        return string;
    }
    
    public String getAsText() {
        return string;
    }
    
    public void setAsText(String text) {
        setValue(text);
    }
    
    public boolean isPaintable() {
        return false;
    }

    public void paintValue(Graphics gfx, Rectangle box) {
    }
    
    public boolean supportsCustomEditor() {
        return false;
    }
    
    public Component getCustomEditor() {
        return null;
    }
    
    /**
     * Sets the property according to the current Javelin selected zone.
     */
    public void setPropertyValueFromJavelinZone() {
        if (javelin == null) {
            throw new IllegalArgumentException("The Javelin object is null");
        }

        Rectangle zone = javelin.getSelectionZone();
        if (zone.width < 1) return;
        
        string = javelin.getString(zone.x, zone.y, zone.width);
        if (string == null) string = "";
        firePropertyChange();
    }
    
    /**
     * Sets the Javelin zone according to the property.
     */
    public void setJavelinZoneFromProperty() {
        if (javelin == null) {
            throw new IllegalArgumentException("The Javelin object is null");
        }
        // Do nothing because there is nothing relevant to do.
    }
    
    /**
     * Sets the property according to the current selected zone.
     * @param databaseObject
     * @param connector
     * @param setter
     */
    public static void setPropertyValueFromSelectionZone(DatabaseObject databaseObject, Connector connector, Method setter) {
    	if (connector == null) {
            throw new IllegalArgumentException("The connector object is null");
        }

    	JavelinConnector jTmp = null;
    	try {
    		jTmp = (JavelinConnector) connector;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a iJavelin");
    	}
    	
        Rectangle zone = jTmp.getSelectionZone();
        
        if (zone.width < 1) 
        	return;
        
        String sTmp = jTmp.javelin.getString(zone.x, zone.y, zone.width);
        if (sTmp == null) 
        	sTmp = "";
        
        try {
			setter.invoke(databaseObject, new Object[] { sTmp });
		} catch (Throwable e) {
			String message = "Error : "+e.getMessage(); 
            ConvertigoPlugin.logException(e, message);
		}
    }
    /**
     * Gets the value to put in the property according to the current selected zone.
     * @param databaseObject
     * @param connector
     * @param setter
     */
    public static Object getSelectionZoneValue(DatabaseObject databaseObject, Connector connector, Method setter) {
    	if (connector == null) {
            throw new IllegalArgumentException("The connector object is null");
        }

    	JavelinConnector jTmp = null;
    	try {
    		jTmp = (JavelinConnector) connector;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a iJavelin");
    	}
    	
        Rectangle zone = jTmp.getSelectionZone();
        
        if (zone.width < 1) 
        	return null;
        
        String sTmp = jTmp.javelin.getString(zone.x, zone.y, zone.width);
        if (sTmp == null) 
        	sTmp = "";
        
        return sTmp;
    }
}
