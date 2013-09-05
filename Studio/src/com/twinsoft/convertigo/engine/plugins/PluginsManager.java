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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.engine.plugins;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public class PluginsManager {
	public static final String PROPERTIES_FILE_NAME = "/plugins.properties";
	
	private Properties properties;
	
	private Map<String, Plugin> plugins = new HashMap<String, Plugin>();
	
    public void init() {
    	properties = new Properties();

		FileInputStream propsInputStream = null;
		String pluginsPropertiesFile = Engine.CONFIGURATION_PATH + PROPERTIES_FILE_NAME;
        try {
        	Engine.logEngine.info("Loading Convertigo plugins properties from " + pluginsPropertiesFile);

    		try {
        		propsInputStream = new FileInputStream(pluginsPropertiesFile);
    			properties.load(propsInputStream);
    			
    			for (Enumeration<Object> e = properties.keys(); e.hasMoreElements() ;) {
   		         	String key = (String) e.nextElement();
   		         	String value = properties.getProperty(key);
   		         
   		         	if (key.startsWith("plugins.classname.")) {
   		         		String pluginClassName = value;
   		         		Plugin plugin = null;
   		         		try {
   		         			plugin = (Plugin) Class.forName(pluginClassName).newInstance();
   		         			plugin.init(properties);
   		         			plugins.put(key, plugin);
   		         		}
   		         		catch (Throwable t) {
   		         			Engine.logEngine.warn("Unable to instantiate plugin from '" + pluginClassName + "'.");
   		         		}
   		         	}
    			}
    		}
            catch (FileNotFoundException e) {
                Engine.logEngine.warn("Unable to find the Convertigo plugins configuration file '" + pluginsPropertiesFile + "'.");
            }
        }
        catch(IOException e) {
        	properties = null;
        	Engine.logEngine.warn("Unable to load the Convertigo plugins configuration file '" + PROPERTIES_FILE_NAME + "'.", e);
        }
        finally {
        	if (propsInputStream != null) {
        		try {
					propsInputStream.close();
				} catch (IOException e) {
				}
        	}
        }
    }

    public void destroy() {
    	properties = null;
    	
    	if (plugins != null) {
    		for (Plugin plugin : plugins.values()) {
        		try {
        			plugin.destroy();
    	    	}
    	    	catch (Exception e) {}
    		}
    		plugins.clear();
    	}
    }
    
    public synchronized void fireHttpConnectorGetDataStart(Context context) {
    	for (Plugin plugin : plugins.values()) {
    		try {
    			plugin.httpConnectorGetDataStart(context);
	    	}
	    	catch (Exception e) {}
		 }
    }
    public synchronized void fireHttpConnectorGetDataEnd(Context context, Long t0, Long t1) {
  		 for (Plugin plugin : plugins.values()) {
  			 try {
				 plugin.httpConnectorGetDataEnd(context, t0, t1);
			 }
  			 catch (Exception e) {}
  		 }
    }
    
    public synchronized void fireRequesterCoreProcessRequestStart(Context context, Object inputData) {
    	try {
        	for (Plugin plugin : plugins.values()) {
        		try {
        			plugin.requesterCoreProcessRequestStart(context, inputData);
        		}
        		catch (Exception e) {}
    		}
    	}
    	catch (Exception e) {}
    }
    
    public synchronized void fireRequesterCoreProcessRequestEnd(Context context, Object inputData) {
    	try {
        	for (Plugin plugin : plugins.values()) {
        		try {
        			plugin.requesterCoreProcessRequestEnd(context, inputData);
        		}
        		catch (Exception e) {}
    		}
    	}
    	catch (Exception e) {}
    }
    
    public synchronized void fireHttpServletRequestEnd(HttpServletRequest request, Long t0, Long t1) {
    	for (Plugin plugin : plugins.values()) {
    		try {
    			plugin.httpServletRequestEnd(request, t0, t1);
    		}
    		catch (Exception e) {}
		}
    }
}
