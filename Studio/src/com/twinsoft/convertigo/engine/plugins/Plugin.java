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

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.Context;

public abstract class Plugin {

	protected String prop_prefix = ".";
	
	protected Properties properties = new Properties();
	
	protected void init(Properties properties) {
		for (Enumeration<Object> e = properties.keys(); e.hasMoreElements() ;) {
	         String key = (String) e.nextElement();
	         String value = properties.getProperty(key);
	         if (key.startsWith(prop_prefix)) {
	        	 this.properties.put(key, value);
	         }
		}
	}
	
	protected void destroy() {
		properties.clear();
	}
	
	protected String getProperty(String key, String defaultValue) {
		return properties.getProperty(prop_prefix+key, defaultValue);
	}
	
	abstract protected void httpConnectorGetDataStart(Context context);
	abstract protected void httpConnectorGetDataEnd(Context context, Long t0, Long t1);
	abstract protected void httpServletRequestEnd(HttpServletRequest request, Long t0, Long t1);
	abstract protected void requesterCoreProcessRequestStart(Context context, Object inputData);
	abstract protected void requesterCoreProcessRequestEnd(Context context, Object inputData);
}
