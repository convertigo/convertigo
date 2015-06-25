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

package com.twinsoft.convertigo.eclipse;

import java.io.File;
import java.io.IOException;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.util.Log;

public class EmbeddedTomcat implements Runnable {

	private Tomcat embedded;
	public String tomcatHome;
	private int httpPort = 18080;
	
	/**
	 * Default Constructor
	 * @throws IOException 
	 */
	public EmbeddedTomcat(String tomcatHome) throws IOException {
		this.tomcatHome = tomcatHome;
		System.out.println("Tomcat Home: " + tomcatHome);
		
		// This call is needed for initializing engine paths
		com.twinsoft.convertigo.engine.Engine.initPaths(tomcatHome + "/webapps/convertigo");
	}

	/**
	 * Starts the Tomcat server.
	 */
	public void start() {
		try {
			System.out.println("(EmbeddedTomcat) Creating the embedded Tomcat servlet container");

			System.out.println("(EmbeddedTomcat) Catalina home: " + tomcatHome);
			System.setProperty("catalina.home", tomcatHome);
			System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
	        
			// Create an embedded server
			System.out.println("(EmbeddedTomcat) Creating a new instance of EmbeddedTomcat");
			embedded = new Tomcat();
			embedded.enableNaming();
			
			// Assemble and install a default HTTP connector
			int httpConnectorPort = 8080;

			// We must load the engine properties first 
			EnginePropertiesManager.loadProperties();

			String convertigoServer = com.twinsoft.convertigo.engine.EnginePropertiesManager.getProperty(
					com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
			System.out.println("(EmbeddedTomcat) Convertigo server property: " + convertigoServer);
			
			int i = convertigoServer.indexOf(':', 6);
			if (i != -1) {
				int j = convertigoServer.indexOf("/convertigo");
				httpConnectorPort = Integer.parseInt(convertigoServer.substring(i+1, j));
			}

			embedded.setPort(httpPort = httpConnectorPort);
			
			int httpsConnectorPort = httpConnectorPort + 1;
			System.out.println("(EmbeddedTomcat) Installing the embedded HTTPS connector listening on port " + httpsConnectorPort);
			
			Connector connector = new Connector();
			connector.setPort(httpsConnectorPort);
			connector.setSecure(true);
			connector.setScheme("https");
			connector.setAttribute("keystorePass", "password"); 
			connector.setAttribute("keystoreFile", tomcatHome + "/conf/.keystore"); 
			connector.setAttribute("clientAuth", false);
			connector.setAttribute("sslProtocol", "TLS");
			connector.setAttribute("SSLEnabled", true);
			embedded.getService().addConnector(connector);
			
			Context context = embedded.addWebapp("", tomcatHome + "webapps/ROOT");
			context.setParentClassLoader(this.getClass().getClassLoader());
			
			context = embedded.addWebapp("/convertigo", com.twinsoft.convertigo.engine.Engine.WEBAPP_PATH);
			context.setParentClassLoader(this.getClass().getClassLoader());
			
			File configFile = new File(com.twinsoft.convertigo.engine.Engine.USER_WORKSPACE_PATH + "/studio/context.xml");
			if (configFile.exists()) {
				System.out.println("(EmbeddedTomcat) Set convertigo webapp config file to " + configFile.getAbsolutePath());
				context.setConfigFile(configFile.toURI().toURL());
			}
		
			// Start the embedded server
			System.out.println("(EmbeddedTomcat) Starting the server");
			embedded.start();

			System.out.println("(EmbeddedTomcat) Server successfully started!");
		}
		catch(Throwable e) {
			String stackTrace = Log.getStackTrace(e);
			System.out.println("(EmbeddedTomcat) Unexpected exception while launching Tomcat:\n" + stackTrace);
			Engine.isStartFailed = true;			
		}
	}
	
	/**
	 * Stops the Tomcat server.
	 */
	public void stop() {
		try {
			// Stop the embedded server
			if (embedded != null) {
				embedded.stop();
			}
		}
		catch(Throwable e) {
			String stackTrace = Log.getStackTrace(e);
			System.out.println("(EmbeddedTomcat) Unexpected exception while stopping Tomcat:\n" + stackTrace);
		}
	}

	public void run() {
		start();
	}

	public int getHttpPort() {
		return httpPort;
	}
}