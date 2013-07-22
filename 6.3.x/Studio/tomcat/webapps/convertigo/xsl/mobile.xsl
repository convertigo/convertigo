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
	<xsl:variable name="fontSize" select="3" />
	<xsl:variable name="width" select="80" />

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
					<table width="100%" border="0" cellpadding="0" cellspacing="2">
						<tr>
							<td align="center">
								<input name="__validate" id="__validate" onclick="doAction('KEY_ENTER')" type="button" value="Valider"/>
							</td>
							<xsl:for-each select="/document/blocks/block[@type='keyword']">
								<td align="center">
									<input href="javascript:return(doAction('{@action}');" name="{@name}" id="{@name}" onclick="doAction('{@action}')" type="button" value="{.}"/>
								</td>
							</xsl:for-each>
						</tr>
					</table>
					<xsl:apply-templates/>
					<xsl:call-template name="handleFocus"/>
				</form>
			</body>
		</html>
	</xsl:template>
	
	
	<xsl:template match="blocks">
		<table border="0">
				<xsl:call-template name="handleLineOfBlocks">
					<xsl:with-param name="i" select="0"/>
				</xsl:call-template>
		</table>
	</xsl:template>
	
	<xsl:template name="handleBlock">
		<xsl:param name="lastCol" select="0"/>
		
		<xsl:if test="$lastCol">
			<xsl:variable name="prevColspan" select="@column -$lastCol" />
			<xsl:if test="@type != 'field'">
				<td colspan="{$prevColspan}">&#160;</td>
				<td colspan="{string-length()}"><font  size="{$fontSize}" face="{$font}"><xsl:apply-templates select="."/></font></td>
			</xsl:if>
			<xsl:if test="@type = 'field'">
				<td colspan="{$prevColspan}">&#160;</td>
				<td colspan="{@size}"><font size="{$fontSize}" face="{$font}"><xsl:apply-templates select="."/></font></td>
			</xsl:if>
		</xsl:if>
		
		<xsl:if test="not($lastCol)">
			<xsl:variable name="prevColspan" select="@column" />
			<xsl:if test="@type != 'field'">
				<td colspan="{$prevColspan}">&#160;</td>
				<td colspan="{string-length()}"><font size="{$fontSize}" face="{$font}"><xsl:apply-templates select="."/></font></td>
			</xsl:if>
			<xsl:if test="@type = 'field'">
				<td colspan="{$prevColspan}">&#160;</td>
				<td colspan="{@size}"><font size="{$fontSize}" face="{$font}"><xsl:apply-templates select="."/></font></td>
			</xsl:if>
		</xsl:if>
		
		
	</xsl:template >
	
	
	<xsl:template name="handleLineOfBlocks">
		<xsl:param name="i" select="0"/>
		
			<xsl:if test="$i &lt; 25">
				<tr>
					<xsl:for-each select="block[@line=$i and @column &lt; $width]">
					
						<xsl:variable name="previousBlockOnLine" select="preceding-sibling::block[1][@line = $i]" />
						<xsl:variable name="lastCol" select="$previousBlockOnLine/@column + string-length($previousBlockOnLine)" />
						
						<xsl:call-template name="handleBlock">
							<xsl:with-param name="lastCol" select="$lastCol"/>
						</xsl:call-template>
						
					</xsl:for-each>
				</tr>		
				<tr>
					<xsl:for-each select="block[@line=$i and @column &gt; $width]">
					
						<xsl:variable name="previousBlockOnLine" select="preceding-sibling::block[1][@line = $i]" />
						<xsl:variable name="lastCol" select="$previousBlockOnLine/@column + string-length($previousBlockOnLine)" />
						
						<xsl:call-template name="handleBlock">
							<xsl:with-param name="lastCol" select="$lastCol"/>
						</xsl:call-template>
						
					</xsl:for-each>
				</tr>		
				
				<xsl:call-template name="handleLineOfBlocks">
					<xsl:with-param name="i" select="$i+1"/>
				</xsl:call-template>
				
			</xsl:if>
	</xsl:template>
	
	
	<xsl:template match="*[@type='static']">
		<xsl:if test="@underline">
			<font color="red">
				<b>
					<xsl:value-of select="."/>
				</b>
			</font>
		</xsl:if>
		<xsl:if test="not(@underline)">
			<xsl:if test="@background != 'black'">
				<xsl:value-of select="."/>
			</xsl:if>
			<xsl:if test="@background = 'black'">
				<xsl:value-of select="."/>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="*[@type='keyword']"/>
	
	
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
	
	<xsl:template name="handleFocus">
		<xsl:for-each select="//*[@type='field']">
			<xsl:if test="(@hasFocus='true')">
				<script type="text/javascript" language="javascript">
					document.javelin_form.<xsl:value-of select="@name"/>.focus();
				</script>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
	
</xsl:stylesheet>
