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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;


public class EngineServlet extends HttpServlet {

	private static final long serialVersionUID = -8111472586297582952L;

	/** Creates new EngineServlet */
    public EngineServlet() {
    }

    /** Returns a short description of the servlet.
    */
    public String getServletInfo() {
        return "Convertigo Enterprise Mashup Server Engine Startup Servlet";
    }
    
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		try {
			System.out.println("C-EMS Engine Startup servlet");
			
			ServletContext servletContext = servletConfig.getServletContext();
		
			String webAppPath = servletContext.getRealPath("").replace('\\', '/');
			System.out.println("C-EMS web application home: " + webAppPath);
			Engine.initPaths(webAppPath);
			Engine.start();
		}
//		catch(IOException e) {
//			System.out.println("Unable to configure the Convertigo engine.", e);
//		}
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
}
