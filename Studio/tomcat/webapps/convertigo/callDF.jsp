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
	
<%	// récupération des variables de sessions utiles
	String storeId 				= (String)session.getAttribute("storeId");
	String sessionTrackingId 	= (String)session.getAttribute("sessionTrackingId");
	
	// vérification de l'authentification
	String requestUrl 			= request.getRequestURL().toString();
	try {
		DirectKit.waitForAuthenticationResponse(storeId, sessionTrackingId);
		if (!DirectKit.isIdentified(storeId, sessionTrackingId)) {
			// user non authentifié
			// affichage message d'erreur
			String errorUrl 	= requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1) + "directKitError.jsp?"
								+ "errorCode=" + DirectKit.getAuthenticationError(storeId, sessionTrackingId);
			response.sendRedirect(errorUrl);
		} else {
			// user identifié : récupération des infos concernant le user
			String userId = DirectKit.getUserId(storeId, sessionTrackingId);
			// vérifie si le userId correspond à un groupe ou à un login
			/*boolean group = true;
			if (userId.length() <= 14)
				group = false;
			else
				try {
					String tmp = userId.substring(0, 14);
					Long.parseLong(tmp);
				} catch (NumberFormatException e) {
					group = false;
				}
			
			String userDF = "";
			if (group)
				userDF = userId.substring(14);
			else*/
			String userDF = userId;
			String tmpUrl 		= Engine.getProperty(Engine.ConfigurationProperties.APPLICATION_SERVER_DREAMFACE_URL);
			String pageName 	= "df/boutiquePage.jsp?userDF="+userDF;
	
			String boutique 	= request.getParameter("boutique");
			if (boutique != null && !boutique.equals(""))
				pageName += "&boutique=" + boutique;
				
			String urlRedirect 	= tmpUrl.charAt(tmpUrl.length()-1)== '/' ? tmpUrl + pageName : tmpUrl + "/" + pageName;
			response.sendRedirect(urlRedirect);
		}
	} catch (InterruptedException e) {
		// erreur dans waitForAuthenticationResponse
		String errorUrl = requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1) + "directKitError.jsp?"
						+ "errorMessage=Thread%20interrompu.<br/>" 
						+ e.getMessage();
		response.sendRedirect(errorUrl);
	} catch (NullParameterException e) {
		// erreur dans waitForAuthenticationResponse
		String errorUrl = requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1) + "directKitError.jsp?"
						+ "errorMessage=Parametre(s)%20incorrect(s)%20:%20null%20ou%20vide.<br/>" 
						+ e.getMessage();
		response.sendRedirect(errorUrl);
	} catch (InvalidStoreIdException e) {
		// erreur dans isIdentified
		String errorUrl = requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1) + "directKitError.jsp?"
						+ "errorMessage=StoreId%20invalide.<br/>" 
						+ e.getMessage();
		response.sendRedirect(errorUrl);
	} catch (InvalidSessionTrackingIdException e) {
		// erreur dans isIdentified
		String errorUrl = requestUrl.substring(0, requestUrl.lastIndexOf('/') + 1) + "directKitError.jsp?"
						+ "errorMessage=SessionTrackingId%20invalide." 
						+ e.getMessage();
		response.sendRedirect(errorUrl);
	} 
%>