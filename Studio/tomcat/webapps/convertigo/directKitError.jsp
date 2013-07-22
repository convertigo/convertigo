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
	import="com.twinsoft.convertigo.engine.plugins.LCABiller"
%>
<%	String errorCode = (request.getParameter("errorCode") == null || request.getParameter("errorCode").equals(""))
						? "-1" : request.getParameter("errorCode");
	String errorMessage = (request.getParameter("errorMessage") == null || request.getParameter("errorMessage").equals(""))
						? "" : request.getParameter("errorMessage");
	if (!errorCode.equals("-1") && errorMessage.equals(""))
		errorMessage = LCABiller.getErrorMessage(errorCode);
%>
<html>
	<head>
		<title>Erreur Kit Marchand LCA</title>
		<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
		<link href="css/infogreffe.css" rel="stylesheet" type="text/css"/>
	</head>
	<body>
		<span class="normal">
			<img src="images/goRight.gif" width="13" height="13"/>
			<%=errorMessage %>
		</span>
	</body>
</html>