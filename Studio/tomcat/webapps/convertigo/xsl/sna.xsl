<?xml version="1.0" encoding="ISO-8859-1"?>

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


<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt">
	<xsl:output method="html" indent="no" media-type="text/html" encoding="ISO-8859-1"/>
	
	<xsl:include href="status.xsl"/>
	<xsl:include href="variables.xsl"/>
	<xsl:include href="rules.xsl"/>
	<xsl:include href="headerfooter.xsl"/>
	
	<!-- TEMPLATE DOCUMENT -->
	<xsl:template match="document">
		<html>
			<xsl:call-template name="header">
				<xsl:with-param name="css">../../css/sna.css</xsl:with-param>
			</xsl:call-template>

			<!-- HTML page -->
			<body onResize="resize();" topmargin="5" leftmargin="5">
				<xsl:choose>
					<xsl:when test="/document/@show_kb='false'">
						<xsl:attribute name="onload">MM_preloadImages('../../images/down_selected.gif','../../images/up_selected.gif', '../../images/rtop_keyboard.gif');if(eval(parent.frames['check'])) parent.frames['check'].checkOnLoad();initElements();</xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="onload">MM_preloadImages('../../images/down_selected.gif','../../images/up_selected.gif', '../../images/rtop_keyboard.gif');keyboard_initVar();if(eval(parent.frames['check'])) parent.frames['check'].checkOnLoad();initElements();</xsl:attribute>
						<xsl:attribute name="onunload">keyboard_setCookie();</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
				
				<!-- Scripts -->
				<xsl:call-template name="loadscripts"/>
				
				<div id="contents" style="visibility: hidden">
					<xsl:copy-of select="document('template.html')/html/body/*"/>
				</div>
		
				<xsl:call-template name="generate-page"/>
			</body>
		</html>
		<xsl:if test="@back-attempt='true'">
			<script language="javascript">
				alert("L'utilisation des fonctions \"Page pr�c�dente\" et\n\"Page suivante\" de votre navigateur n'est pas permise.");
			</script>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="generate-page">
		<div id="generated_page">
			<xsl:call-template name="sizescript"/>
			<form name="javelin_form" method="post">
				<xsl:for-each select="blocks">
					<xsl:apply-templates>
						<xsl:with-param name="offsety" select="$offsety + @page-number * $coefy * 24"/>
					</xsl:apply-templates>
				</xsl:for-each>
				<input type="hidden" name="__javelin_current_field" id="__javelin_current_field"/>
				<input type="hidden" name="__javelin_action" id="__javelin_action"/>
				<input type="hidden" name="__transaction" id="__transaction"/>
				<input type="hidden" name="__sesskey" id="__sesskey"/>
				<input type="hidden" name="__context" id="__context" value="{/document/@context}"/>
				<input type="hidden" name="__signature" id="__signature" value="{/document/@signature}"/>

			</form>
			<xsl:call-template name="footer"/>
		</div>
	</xsl:template>

</xsl:stylesheet>
