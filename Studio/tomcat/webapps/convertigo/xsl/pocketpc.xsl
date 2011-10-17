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

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output encoding="UTF-8" method="xml" media-type="text/html" indent="no"/>
	
	<xsl:variable name="font" select="'arial'" />
	<xsl:variable name="fontSize" select="1" />
	
	<xsl:template match="document">
		<html>
			<head>
				<title>Convertigo</title>
			</head>
			<body>
				<font face="Verdana" />		
				<script type="text/javascript" language="Javascript">
					function doAction(actionName) {
						document.javelin_form.__javelin_action.value = actionName;
						document.javelin_form.submit();
					 } 
				</script>
				
				<form onsubmit="return doAction('KEY_ENTER')" name="javelin_form" method="post">
					<input value="" id="__javelin_action" name="__javelin_action" type="hidden"/>
					<input value="" id="__sesskey" name="__sesskey" type="hidden"/>
					<input value="" id="__context" name="__context" type="hidden"/>
					<input value="" id="__javelin_current_field" name="__javelin_current_field" type="hidden"/>
					<input id="__transaction" name="__transaction" type="hidden"/>
					<hr/>
					<input onclick="doAction('KEY_ENTER')" type="button" value="Valider"/>
					<input onclick="doAction('convertigo_refresh')" type="button" value="R�actualiser"/>
					<xsl:for-each select="/document/blocks//block[@type='keyword']">
						<input href="#" onclick="doAction('{@action}')" type="button" value="{.}"/>
					</xsl:for-each>
					<hr/>
					<font  size="{$fontSize}" face="{$font}">
						<xsl:apply-templates/>
					</font>
					<xsl:call-template name="handleFocus"/>
				</form>
			</body>
		</html>
	</xsl:template>
	
	
	<xsl:template match="blocks">
			<xsl:call-template name="handleLineOfBlocks">
				<xsl:with-param name="i" select="0"/>
			</xsl:call-template>
	</xsl:template>
	
	<xsl:template name="handleBlock">
			<xsl:text>&#160;</xsl:text><xsl:apply-templates select="."/>
	</xsl:template >
	
	
	<xsl:template name="handleLineOfBlocks">
		<xsl:param name="i" select="0"/>
		
			<xsl:if test="$i &lt; 25">
				<xsl:for-each select="//block[@line=$i] | //menuitem[@line = $i]">
					<xsl:call-template name="handleBlock"/>
				</xsl:for-each>
				<br />
				
				<xsl:call-template name="handleLineOfBlocks">
					<xsl:with-param name="i" select="$i+1"/>
				</xsl:call-template>
				
			</xsl:if>
	</xsl:template>
	
	
	<xsl:template match="block[@type='static']">
		<xsl:if test="@underline">
			<u>
				<b>
					<xsl:value-of select="."/>
				</b>
			</u>
		</xsl:if>
		<xsl:if test="not(@underline)">
			<xsl:value-of select="."/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="*[@type='keyword']"/>


	<xsl:template match="menuitem">
		<!--
		<a href="#" onclick="doMenu({@id})">
			<xsl:value-of select="@id"/>-<xsl:value-of select="@literal"/>
		</a>
		-->
		<input type="button" onclick="doMenu({@id})" value="{@id}"/>-<xsl:value-of select="@literal"/>
	</xsl:template>
	
	
	<xsl:template match="*[@type='field']">
		<xsl:if test="@hidden = 'true'">
			<input type="password" id="{@name}" name="{@name}"  value="{.}" size="{@size}" maxlength="{@size}"/>
		</xsl:if>
		<xsl:if test="not(@hidden = 'true')">
			<input id="{@name}" name="{@name}" type="text" value="{.}" size="{@size}" maxlength="{@size}"/>
		</xsl:if>
	</xsl:template>
	
	
	<xsl:template match="*[@type='filler']">
		<xsl:value-of select="."/>
	</xsl:template>

	<xsl:template match="*[@type='panel']">
	</xsl:template>

	<xsl:template match="*[@type='snamenu']">
	</xsl:template>
	
	<xsl:template name="handleFocus">
		<xsl:for-each select="//*[@type='field']">
			<xsl:if test="(@hasFocus='true')">
				<script type="text/javascript" language="javascript">
					document.javelin_form.<xsl:value-of select="@name"/>.focus();
					
					function doMenu(id) {
						document.javelin_form.<xsl:value-of select="@name"/>.value=id;
						doAction("KEY_ENTER");
					}
					
				</script>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
	
</xsl:stylesheet>
