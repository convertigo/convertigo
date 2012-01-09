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

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE CHOICE -->
	<xsl:template match="*[@type='choice']" priority="1">
		<xsl:param name="fieldSize">
			<xsl:value-of select="@size" />
		</xsl:param>
		<xsl:param name="offsety" />
		<xsl:if test="@radio = 'true'">
			<input class="fixed" name="{@name}"
				onfocus="currentFieldOnFocus=this.id" id="{@name}"
				style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px"
				size="{@size}" maxlength="{@size}" value="{.}" />
			<span class="panel"
				style="position:absolute; left:{(@column+@size+1)*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
				<xsl:for-each select="child::*">
					<xsl:if test="@selected = 'true'">
						<input type="radio" checked="true"
							name="{../@name}_r" id="{../@name}_r"
							onclick="document.javelin_form.{../@name}.value = '{@value}'"
							value="{@value}" />
						<xsl:value-of select="@value" />
						 
					</xsl:if>
					<xsl:if test="string-length(@selected) = 0">
						<input type="radio" name="{../@name}_r"
							id="{../@name}_r"
							onclick="document.javelin_form.{../@name}.value = '{@value}'"
							value="{@value}" />
						<xsl:value-of select="@value" />
						 
					</xsl:if>
				</xsl:for-each>
			</span>
			<xsl:if test="(@hasFocus ='true')">
				<script type="text/javascript" language="javascript">
					var elt= document.getElementsByTagName("INPUT");
					if(elt['<xsl:value-of select="@name" />']) focusOnField = elt['<xsl:value-of select="@name" />'];
				</script>
			</xsl:if>
		</xsl:if>
		<xsl:if test="@radio = 'false'">
			<select class="fixed" name="{@name}"
				onfocus="currentFieldOnFocus=this.id" id="{@name}"
				style="position:absolute; left:{@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
				<xsl:for-each select="child::*">
					<option value="{@action}">
						<xsl:if test="(@selected = 'true')">
							<xsl:attribute name="selected">selected</xsl:attribute>
						</xsl:if>
						<xsl:value-of select="@value" />
					</option>
				</xsl:for-each>
			</select>
			<xsl:if test="(@hasFocus ='true')">
				<script type="text/javascript" language="javascript">
					var elt= document.getElementsByTagName("SELECT");
					if(elt['<xsl:value-of select="@name" />']) focusOnField = elt['<xsl:value-of select="@name" />'];
				</script>
			</xsl:if>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
