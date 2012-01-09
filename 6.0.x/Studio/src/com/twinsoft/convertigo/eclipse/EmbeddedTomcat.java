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
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Embedded;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.util.Log;

public class EmbeddedTomcat implements Runnable {

	private Embedded embedded;
	public String tomcatHome;
	
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
	        
			// Create an embedded server
			System.out.println("(EmbeddedTomcat) Creating a new instance of EmbeddedTomcat");
			embedded = new Embedded();
			
			embedded.setName("Catalina");
		
			// Setup log statements
//			embedded.setLogger(new LoggerBase() {
//				protected static final String info = "com.twinsoft.convertigo.studio.EmbeddedTomcatLogger/1.0";
//				public void log(String msg) {
//					ConvertigoPlugin.getDefault().tomcatConsoleStream.println(msg);
//				}
//			});
	
			// Create an engine
			System.out.println("(EmbeddedTomcat) Creating the engine for 'localhost'");

			Engine engine = embedded.createEngine();
			engine.setName("catalina");

			engine.setDefaultHost("localhost");
	
			// Create a default virtual host
			System.out.println("(EmbeddedTomcat) Creating the virtual host on 'localhost, " + tomcatHome + "/webapps'");

			Host host = embedded.createHost("localhost", tomcatHome + "/webapps");

			engine.addChild(host);

			// Create the default context
			System.out.println("(EmbeddedTomcat) Creating the default context");

			Context context = embedded.createContext("", tomcatHome + "/webapps/ROOT");
			context.setParentClassLoader(this.getClass().getClassLoader());
			host.addChild(context);
	
			// Create all contexts into the webapps directory
			System.out.println("(EmbeddedTomcat) Creating the webapp contexts");
			File dir = new File(tomcatHome + "/webapps");
			String[] directories = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (new File(dir, name)).isDirectory();
				}
			});
			
			String contextPath, docBase, directory;
			for (int i = 0 ; i < directories.length ; i++) {
				directory = directories[i];
				
				// Skip ROOT webapps
				if (directory.equals("ROOT")) continue;
				
				contextPath = "/" + directory;
				docBase = tomcatHome + "/webapps/" + directory;
				System.out.println("(EmbeddedTomcat) Adding context '" + contextPath + ", " + docBase);

				context = embedded.createContext(contextPath, docBase);
				context.setParentClassLoader(this.getClass().getClassLoader());
				host.addChild(context);
			}
	
			// Install the assembled container hierarchy
			System.out.println("(EmbeddedTomcat) Installing the assembled container hierarchy");

			embedded.addEngine(engine);

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
			System.out.println("(EmbeddedTomcat) Installing the embedded HTTP connector listening on port " + httpConnectorPort);

			Connector connector = embedded.createConnector((java.net.InetAddress) null, httpConnectorPort, false);

			embedded.addConnector(connector);
		
			// Start the embedded server
			System.out.println("(EmbeddedTomcat) Starting the server");

			embedded.start();

			System.out.println("(EmbeddedTomcat) Server successfully started!");
		}
		catch(Throwable e) {
			String stackTrace = Log.getStackTrace(e);
			System.out.println("(EmbeddedTomcat) Unexpected exception while launching Tomcat:\n" + stackTrace);
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

}