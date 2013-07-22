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

package com.twinsoft.convertigo.eclipse.learnproxy.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;


public class ConfigManager {

    //private static final Log logger = LogFactory.getLog(ConfigManager.class);

    private static Properties props = null;
    private String fileName = null;
    public boolean hasLoaded = false;
    
    public ConfigManager(String fileName) {
    	this.fileName = fileName;
    	hasLoaded = init();
    }
    
    public boolean init() {
    	props = new Properties();
    	
    	if (fileName == null)
    		return false;
        
    	try {
            InputStream inStream = new FileInputStream(fileName);
            props.load(inStream);
            /*Iterator iter = props.keySet().iterator();
            logger.debug("Properties:");
            while (iter.hasNext()) {
                String key = (String)iter.next();
                logger.debug(key + " -> " + props.get(key));
            }*/
            return true;
        }
        catch (IOException e) {
			ConvertigoPlugin.logException(e, "Unexpected exception");
            throw new RuntimeException("Couldn't read properties");
        }
    }
    
    public String getNamedProperty(String key) {
        try {
            return props.getProperty(key);
        }
        catch (NullPointerException e) {
            throw new RuntimeException("Could not find property: " + key);
        }
    }
    
    public int getIntProperty(String key) {
        String str = getNamedProperty(key);
        try {
            return Integer.parseInt(str);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("Named property " + key + " is no valid int value");
        }            
    }
    
    
}
