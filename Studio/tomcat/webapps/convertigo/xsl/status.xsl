<?xml version="1.0" encoding="UTF-8"?>

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

<!-- TWinSoft Convertigo generic status style sheet -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:template match="status">
		<html>
			<head>
				<meta name="GENERATOR" content="TWinSoft Convertigo"/>
				<meta http-equiv="expires" content="0"/>
				<meta http-equiv="refresh" content="2; url={@refresh-url}"/>
				<style> p {font-family:verdana;} </style>
			</head>
			<body bgcolor="#FFFFFF" background="../../images/marbre.gif">
				<p>
					<img src="../../images/twinsoft.gif"/>
				</p>
				<table border="0" cellpadding="8" 
					style="border: 2px solid #FA8072;">
					<tr>
						<td width="100%">
							<xsl:apply-templates/>
						</td>
					</tr>
				</table>
				<p>
					<a href="http://www.convertigo.com">
						<img border="0" src="../../images/convertigo.gif"/>
					</a>
					<br/>
					<font size="-1">Copyright © 2001-2011 Convertigo SA. All rights reserved.</font>
				</p>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="job">
		<p>
			<b>La transaction (job #<xsl:value-of select="@id"/>) est en cours 
				d'éxécution ; veuillez patienter...</b>
		</p>
		<p>
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	<xsl:template match="step">
		<br>
			<xsl:apply-templates/>
		</br>
	</xsl:template>
</xsl:stylesheet>