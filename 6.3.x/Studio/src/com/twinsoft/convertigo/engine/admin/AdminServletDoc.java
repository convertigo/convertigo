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

package com.twinsoft.convertigo.engine.admin;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * Servlet implementation class AdminServlet
 */
public class AdminServletDoc extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AdminServletDoc() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	public static List<Class<?>> getClassesForPackage(String pckgname)
			throws ClassNotFoundException {
		// This will hold a list of directories matching the pckgname.
		// There may be more than one if a package is split over multiple
		// jars/paths
		List<Class<?>> classes = new ArrayList<Class<?>>();
		ArrayList<File> directories = new ArrayList<File>();
		try {
			ClassLoader cld = Thread.currentThread().getContextClassLoader();
			if (cld == null) {
				throw new ClassNotFoundException("Can't get class loader.");
			}
			// Ask for all resources for the path
			Enumeration<URL> resources = cld.getResources(pckgname.replace('.',
					'/'));
			while (resources.hasMoreElements()) {
				URL res = resources.nextElement();
				if (res.getProtocol().equalsIgnoreCase("jar")) {
					JarURLConnection conn = (JarURLConnection) res
							.openConnection();
					JarFile jar = conn.getJarFile();
					for (JarEntry e : Collections.list(jar.entries())) {

						if (e.getName().startsWith(pckgname.replace('.', '/'))
								&& e.getName().endsWith(".class")
								&& !e.getName().contains("$")) {
							String className = e.getName().replace("/", ".")
									.substring(0, e.getName().length() - 6);
							System.out.println(className);
							classes.add((Class<?>) Class.forName(className));
						}
					}
				} else
					directories.add(new File(URLDecoder.decode(res.getPath(),
							"UTF-8")));
			}
		} catch (NullPointerException x) {
			throw new ClassNotFoundException(pckgname
					+ " does not appear to be "
					+ "a valid package (Null pointer exception)");
		} catch (UnsupportedEncodingException encex) {
			throw new ClassNotFoundException(pckgname
					+ " does not appear to be "
					+ "a valid package (Unsupported encoding)");
		} catch (IOException ioex) {
			throw new ClassNotFoundException(
					"IOException was thrown when trying "
							+ "to get all resources for " + pckgname);
		}

		// For every directory identified capture all the .class files
		for (File directory : directories) {
			if (directory.exists()) {
				// Get the list of the files contained in the package
				String[] files = directory.list();
				for (String file : files) {
					// we are only interested in .class files
					if (file.endsWith(".class")) {
						// removes the .class extension
						classes.add(Class.forName(pckgname + '.'
								+ file.substring(0, file.length() - 6)));
					}
				}
			} else {
				throw new ClassNotFoundException(pckgname + " ("
						+ directory.getPath()
						+ ") does not appear to be a valid package");
			}
		}
		return classes;
	}

	public static List<Class<?>> getClassessOfInterface(String thePackage, Class<?> theInterface) {
		List<Class<?>> classList = new ArrayList<Class<?>>();
		try {
			for (Class<?> discovered : getClassesForPackage(thePackage)) {
				if (Arrays.asList(discovered.getInterfaces()).contains(
						theInterface)) {
					classList.add(discovered);
				}
			}
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}

		return classList;
	}

	private void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			String serviceName = "?";
			try {
				response.addHeader("Expires", "-1");
    			response.addHeader("Pragma", "no-cache");
	    		
				String requestURL = request.getRequestURL().toString();
				int i = requestURL.lastIndexOf('/');
				serviceName = requestURL.substring(i + 1);
				
	    		response.setContentType("text/plain");
	    		
//		    		Class<? extends Service> serviceClass = Service.class;
	    		
	    		List<Class<?>> classes = getClassesForPackage("com.twinsoft.convertigo.engine.admin.services.");
	    		
				String buffer = "";
				for (Class<?> clazz : classes) {
					buffer += clazz.getName() + "\r\n";
				}
				
				Writer writer = response.getWriter();
				writer.write(buffer);
			}
			catch (Exception e) {
				Engine.logAdmin.error("Unable to execute the service '" + serviceName + "'", e);
				String result = XMLUtils.prettyPrintDOM(DOMUtils.handleError(e));
	
				Writer writer = response.getWriter();
				writer.write(result);
				response.flushBuffer();
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

}
