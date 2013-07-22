<!--
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
-->

<%
//response.addHeader("P3P","CP=\"CAO PSA OUR\""); 
%>
<POLICY-REFERENCES>
  <POLICY-REF about="/w3c/policy.xml">
    <COOKIE-INCLUDE name="*" value="*" domain="*" path="*"/>  
  </POLICY-REF>
</POLICY-REFERENCES>
<%@ page 
	contentType="text/html; charset=iso-8859-1" 
	language="java" 
	session="true"
	import="itlm.directkit.*,
			java.util.*,
			com.twinsoft.convertigo.engine.*,
			java.io.*,
			java.net.URI
			"
%>
	
<%	// handle page to redirect after wait page
	if (session.getAttribute("webPage") == null)
		session.setAttribute("webPage", "callDF.jsp");


	//response.addHeader("P3P","CP=\"IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT\"");
	response.addHeader("P3P","CP=\"NON DSP COR CURa PSA PSD OUR BUS NAV STA\"");

	// récupération du storeId dans les properties
	URI uri 		= new URI(this.getClass().getClassLoader().getResource("/directkit.properties").toString());
	File fichier 	= new File(uri);
	Properties prop = new Properties();
	prop.load(new FileInputStream(fichier));
	String storeId 	= prop.getProperty("storeId");
		
	// préparation de l'authentification LCA
	String sessionTrackingId 	= session.getId();
	String requestUrl 			= request.getRequestURL().toString();
	//String tmpUrl 			= Engine.getProperty(Engine.ConfigurationProperties.APPLICATION_SERVER_DREAMFACE_URL);
	//String pageName 			= "df/boutiquePage.jsp?application=OrangeBusinessServices";
	String tmpUrl 				= Engine.getProperty(Engine.ConfigurationProperties.APPLICATION_SERVER_CONVERTIGO_URL);
	String pageName 			= "directKitAccessWait.jsp?";
	
	String boutique = request.getParameter("boutique");
	if (boutique != null && !boutique.equals(""))
		pageName += "&boutique=" + boutique;
	
	String backUrl 		= tmpUrl.charAt(tmpUrl.length()-1)== '/' ? tmpUrl + pageName : tmpUrl + "/" + pageName;
	String urlRedirect 	= null;
	
	// mise en session de variables utiles pour la suite
	session.setAttribute("storeId", storeId);
	session.setAttribute("sessionTrackingId", sessionTrackingId);
	
	// authentification LCA
	try {
		urlRedirect	= DirectKit.getAuthenticationUrl(storeId, sessionTrackingId, backUrl);
		if (urlRedirect == null) {
			// erreur
			String errorUrl = requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1) + "directKitError.jsp?"
							+ "errorMessage=URL%20de%20redirection%20invalide%20pour%20authentification%20LCA.";
			response.sendRedirect(errorUrl);
		} else {
			// redirection vers urlRedirect
			response.sendRedirect(urlRedirect);
		}
	} catch(ServiceNotAvailableException e) {
		// erreur dans getAuthenticationUrl
		String errorUrl = requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1) + "directKitError.jsp?"
						+ "errorMessage=Service%20indisponible.<br/>" + e.getMessage();
		response.sendRedirect(errorUrl);
	} catch(NullParameterException e) {
		// erreur dans getAuthenticationUrl
		String errorUrl = requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1) + "directKitError.jsp?"
						+ "errorMessage=Parametre(s)%20incorrect(s)%20:%20null%20ou%20vide.<br/>" + e.getMessage();
		response.sendRedirect(errorUrl);
	}
%>
