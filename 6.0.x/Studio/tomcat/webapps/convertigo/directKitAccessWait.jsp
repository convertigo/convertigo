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
	String webPage = "";
	if (session.getAttribute("webPage") == null) {
		webPage = "directKitAccess.jsp";
		session.setAttribute("webPage", "callDF.jsp");
	} else {
		webPage = (String)session.getAttribute("webPage");
		session.removeAttribute("webPage");
	}
%>


<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<title>Recherche</title>
	</head>
	<body onload="document.getElementById('accessForm').submit();">
		<div style="position: absolute; top: 40%; left: 0; width: 100%; z-index: 99999; text-align: center;">
			<center>
				<div style="height:40; font-family: Arial, Helvetica, sans-serif; font-size: 11px; font-weight: bold; color: #247DB9; text-decoration: none;">
			         Connexion en cours, merci de patienter.
			    </div>
				<img src="images/loading.gif"/>
			</center>
		</div>
		<div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; background-color: #B5D3E7; filter: alpha(opacity=20);-moz-opacity: 0.2; opacity: 0.2; z-index: 99998;"/>
		<form id="accessForm" style="display:none;" action="<%=webPage %>" method="post">
<%	// handle parameters
	Enumeration en = request.getParameterNames();
	while(en.hasMoreElements()) {
		String paramName = (String)en.nextElement();
		String values[] = request.getParameterValues(paramName);
		for (int i = 0 ; i < values.length ; i++) {
%>			<input type="hidden" name="<%=paramName %>" value="<%=values[i] %>" />
<%		}
	}
%>
		</form>
	</body>
</html>