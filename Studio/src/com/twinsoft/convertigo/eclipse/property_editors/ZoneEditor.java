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

import java.lang.reflect.Method;

import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class ZoneEditor extends AbstractDialogCellEditor {
	
	public ZoneEditor(Composite parent) {
        super(parent);

        dialogTitle = "Screen zone";
        dialogCompositeClass = ZoneEditorComposite.class;
    }
	
	/**
     * Sets the property according to the current selected zone.
     * @param databaseObject
     * @param connector
     * @param setter
     */
    public static void setPropertyValueFromSelectionZone(DatabaseObject databaseObject, Connector connector, Method propertySetter) {
    	if (connector == null) {
            throw new IllegalArgumentException("The connector object is null");
        }

    	JavelinConnector jTmp = null;
    	try {
    		jTmp = (JavelinConnector) connector;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a iJavelin");
    	}
    	
        XMLRectangle zone = jTmp.getSelectionZone();
        
        if ((zone.width < 1) || (zone.height < 1)) 
        	return;
        
        try {
        	propertySetter.invoke(databaseObject, new Object[] { zone });
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
    public static Object getSelectionZoneValue(DatabaseObject databaseObject, Connector connector, Method propertySetter) {
    	if (connector == null) {
            throw new IllegalArgumentException("The connector object is null");
        }

    	JavelinConnector jTmp = null;
    	try {
    		jTmp = (JavelinConnector) connector;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a iJavelin");
    	}
    	
        XMLRectangle zone = jTmp.getSelectionZone();
        
        if ((zone.width < 1) || (zone.height < 1)) 
        	return null;
        
        return zone;
    }
    
    /**
     * Displays the value of the selected property if it is a screen zone.
     * @param databaseObject
     * @param connector
     * @param getter
     */
    public static void displayPropertyValueFromSelectionZone(DatabaseObject databaseObject, Connector connector, Method propertyGetter) {
    	if (connector == null) {
            throw new IllegalArgumentException("The connector object is null");
        }

    	XMLRectangle zone = null;
        
        try {
        	zone = (XMLRectangle) propertyGetter.invoke(databaseObject, (Object[])null);
		} catch (Throwable e) {
			String message = "Error : "+e.getMessage(); 
            ConvertigoPlugin.logException(e, message);
		}
		
		JavelinConnector jTmp = null;
    	try {
    		jTmp = (JavelinConnector) connector;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a iJavelin");
    	}
		
    	jTmp.javelin.setSelectionZone(zone);
    }
}
