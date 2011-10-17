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

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt">
	<xsl:output encoding="UTF-8" media-type="text/html" method="html"/>

	<xsl:template match="document">
		<xsl:apply-templates select="HEAD/LINK"/>
		<xsl:apply-templates select="HEAD/STYLE"/>
		<xsl:apply-templates select="HEAD/SCRIPT"/>
		<xsl:apply-templates select="HEAD/following-sibling::*" />
		<div id="menu_div" onmouseover="this.style.top=this.style.left='0px'" onmouseout="this.style.left='-25px';this.style.top='-28px'">
		 	<img src="../../images/e_refresh.gif" alt="Refresh" title="Refresh" onclick="C8O.call()"></img>
		 	<img src="../../images/d_back.gif" alt="Restart" title="Restart" onclick="C8O.doReconnect()"></img><br/>
		 	<img src="../../images/e_back.gif" alt="Backward" title="Backward" onclick="C8O.doNavigationBarEvent('backward')"></img>
		 	<img src="../../images/e_forward.gif" alt="Forward" title="Forward" onclick="C8O.doNavigationBarEvent('forward')"></img>
		</div>
	</xsl:template>
	
	
	<xsl:template match="ALERT">
		<table id="alertTable" onclick="this.parentNode.removeChild(this)">
			<tr id="alertTableTitle">
				<td><b><xsl:value-of select="TYPE"/></b></td>
				<td><xsl:value-of select="TITLE"/></td>
				<td><b>X</b></td>
			</tr>
			<tr>
				<td colspan="3"><xsl:value-of select="MESSAGE"/></td>
			</tr>
		</table>
	</xsl:template>

	<xsl:template match="@*|node()">
	  <xsl:copy>
	    <xsl:apply-templates select="@*|node()"/>
	  </xsl:copy>
	</xsl:template>
	
	<xsl:template match="//A[contains(@original_url,'mailto:')]" >
		<a href="{@original_url}"><xsl:value-of select="." /></a>                       
	</xsl:template>
	
</xsl:stylesheet>