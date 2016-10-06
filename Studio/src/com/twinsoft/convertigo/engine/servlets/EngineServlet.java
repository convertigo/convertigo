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

package com.twinsoft.convertigo.engine.servlets;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;


public class EngineServlet extends HttpServlet {

	private static final long serialVersionUID = -8111472586297582952L;
	private static Map<String, DataSource> dataSources = new HashMap<String, DataSource>(); 

	/** Creates new EngineServlet */
    public EngineServlet() {
    }

    /** Returns a short description of the servlet.
    */
    public String getServletInfo() {
        return "Convertigo Enterprise Mobility Server Engine Startup Servlet";
    }

    private void walkDataSources(Context context, String path) {
    	try {
    		NamingEnumeration<NameClassPair> list = context.list(path);
    		while (list.hasMore()) {
    			try {
    				String subpath = path;
    				if (!path.endsWith(":")) {
    					subpath += "/";
    				}
    				subpath += list.next().getName();
    				System.out.println(" * " + subpath);
    				Object object = context.lookup(subpath);

    				if (object instanceof DataSource) {
    					System.out.println(" X " + subpath + " is a DataSource");
    					dataSources.put(subpath, (DataSource) object);
    				} else if (object instanceof DirContext) {
    					//skip
    				} else if (object instanceof Context) {
    					walkDataSources(context, subpath);
    				}
    			} catch (NamingException e) {
    				// skip it
    			}
    		}
    	} catch (NamingException e) {
    		// skip it
    	}
    }
    
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		try {
			System.out.println("C-EMS Engine Startup servlet");
			System.out.println("C-EMS Engine search for JNDI datasources:");
			walkDataSources(new InitialContext(), "java:");
			System.out.println("C-EMS Engine found " + dataSources.size() + " DataSource(s).");
			
			ServletContext servletContext = servletConfig.getServletContext();
		
			String webAppPath = servletContext.getRealPath("").replace('\\', '/');
			System.out.println("C-EMS web application home: " + webAppPath);
			Engine.initPaths(webAppPath);
			Engine.start();
		}
		catch(EngineException e) {
			System.out.println("Unable to start the Convertigo engine.");
			e.printStackTrace();
		}
		catch(Throwable e) {
			System.out.println("Unexpected exception while startup of the Convertigo engine.");
			e.printStackTrace();
		}
	}

	public void destroy() {
		try {
			Engine.logEngine.debug("Convertigo Engine servlet is being destroyed...");
			Engine.stop();
			Engine.logEngine.debug("Convertigo Engine Servlet has been successfully destroyed.");
		}
		catch(Throwable e) {
			Engine.logEngine.info("Unexpected exception while executing the stop request of the Convertigo engine.", e);
		}
	}
	
	public static DataSource getDataSource(String path) {
		return dataSources.get(path);
	}
}
