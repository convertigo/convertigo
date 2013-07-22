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
	
	<!-- TEMPLATE TABLE -->
	<xsl:template match="*[@type='table']">
		<xsl:param name="offsety" />
		<div class="scrollGrid"
			 style="overflow:auto;
			 		position:absolute;
			 		left:{@column*$coefx+$offsetx}px;
			 		top:{(@line - @offset)*$coefy + $offsety}px;
			 		width:{@width * $coefx};
			 		height:{(@height+1) * $coefy}">
			 
		<table cellpadding="0" cellspacing="0"	border="0" width="100%">
			<tr>
				<td>
					<table border="0" cellspacing="0" cellpadding="0"
						class="data" width="100%" height="100%">
						<tr>
							<xsl:apply-templates select="row/Title" />
						</tr>
						<xsl:apply-templates
							select="row[position() > 1]" />
					</table>
				</td>
				<td>
					<table height="100%" width="16" border="0">
						<tr>
							<td height="16" width="16" valign="top">
								<img width="16" height="16"
									src="../../images/hide.gif"
									onclick="return doAction('KEY_ROLLUP')" />
							</td>
						</tr>
						<tr>
							<td height="98%" valign="middle">
								<img width="16" height="98%"
									src="../../images/barre.gif" />
							</td>
						</tr>
						<tr>
							<td height="16" width="16"
								valign="bottom">
								<img width="16" height="16"
									src="../../images/show.gif"
									onclick="return doAction('KEY_ROLLDOWN')" />
							</td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
		</div>
		<xsl:apply-templates select="actionsTable" />
	</xsl:template>

	<!-- TEMPLATE ROW -->
	<xsl:template match="row" priority="1">
		<tr style="line-height: {$coefy - 1}px">
			<xsl:if test="(@line mod 2) = 0">
				<xsl:attribute name="class">dataroweven</xsl:attribute>
			</xsl:if>
			<xsl:if test="(@line mod 2) != 0">
				<xsl:attribute name="class">datarowodd</xsl:attribute>
			</xsl:if>

			<xsl:for-each select="child::*">
				<td class="cell">
					<xsl:choose>
						<xsl:when test='(@type = "field")'>
							<xsl:attribute name="align">center</xsl:attribute>

							<xsl:if test="not(@autoenter)">
								<xsl:if
									test="(@columnSelection = 'true')">
									<input class="fixed"
										style="height:{$coefy - 1}px;width:{$coefx*2}px" type="text"
										name="{@name}" onfocus="currentFieldOnFocus=this.id"
										id="{@name}" value="{.}">
										<xsl:attribute name="onclick">show_mySelectMenu2(event, document.getElementById('<xsl:value-of select="@name"/>'), document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
										<xsl:attribute name="onblur">hide_mySelectMenu2(document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
									</input>
								</xsl:if>
								<xsl:if
									test="not(@columnSelection = 'true')">
									<input class="fixed"
										style="height:{$coefy - 1}px" name="{@name}"
										onkeyup="checkInputChars(event, {@size}, false, document.javelin_form.{@name})"
										onfocus="currentFieldOnFocus=this.id" id="{@name}"
										size="{@size}" maxlength="{@size}" value="{.}" />
								</xsl:if>
							</xsl:if>

							<xsl:if test="@autoenter">
								<xsl:if
									test="(@columnSelection = 'true')">
									<input class="fixed"
										style="height:{$coefy - 1}px;width:{$coefx*2}px" type="text"
										name="{@name}" onfocus="currentFieldOnFocus=this.id"
										id="{@name}" value="{.}">
										<xsl:attribute name="onclick">show_mySelectMenu2(event, document.getElementById('<xsl:value-of select="@name"/>'), document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
										<xsl:attribute name="onblur">hide_mySelectMenu2(document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
									</input>
								</xsl:if>
								<xsl:if
									test="not(@columnSelection = 'true')">
									<input class="fixed"
										style="height:{$coefy - 1}px" name="{@name}"
										onkeyup="checkInputChars(event, {@size}, true, document.javelin_form.{@name})"
										onfocus="currentFieldOnFocus=this.id" id="{@name}"
										size="{@size}" maxlength="{@size}" value="{.}" />
								</xsl:if>
							</xsl:if>

							<xsl:if test="(@hasFocus = 'true')">
								<script type="text/javascript"
									language="javascript">
									var elt=document.getElementsByTagName("INPUT");
									if(elt['<xsl:value-of select="@name" />'])
										focusOnField = elt['<xsl:value-of select="@name" />'];
								</script>
							</xsl:if>

						</xsl:when>
						<xsl:when test='(@type = "choice")'>
							<select class="fixed" name="{@name}"
								onfocus="currentFieldOnFocus=this.id" id="{@name}">
								<xsl:for-each select="child::*">
									<option value="{@action}">
										<xsl:if
											test="(@selected = 'true')">
											<xsl:attribute
												name="selected">
												selected
											</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="@value" />
									</option>
								</xsl:for-each>
							</select>
							<xsl:if test="(@hasFocus ='true')">
								<script type="text/javascript"
									language="javascript">
									var elt = document.getElementsByTagName("SELECT");
									if(elt['<xsl:value-of select="@name" />'])
										focusOnField = elt['<xsl:value-of select="@name" />'];
								</script>
							</xsl:if>
						</xsl:when>
						<xsl:otherwise>
							<xsl:if
								test="number(translate(.,',','.'))=number(translate(.,',','.'))">
								<xsl:attribute name="align">right</xsl:attribute>
							</xsl:if>
							<xsl:if test="string-length(.) = 0">&#160;</xsl:if>
							<xsl:if test="string-length(.) != 0"><xsl:value-of select="." /></xsl:if>&#160;
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>

	<!-- TEMPLATE TITLE -->
	<xsl:template match="Title" priority="1">
		<td class="datatitle">
			<xsl:if test="string-length(.) = 0"> </xsl:if>
			<xsl:if test="string-length(.) != 0">
				<xsl:value-of select="." />
			</xsl:if>
		</td>
	</xsl:template>

	<!-- TEMPLATE ACTIONSTABLE IN DIV -->
	<xsl:template match="actionsTable" priority="1">
		<script src="../../scripts/selectMenu.js"><!--script--></script >
		<div class="actDiv" id="act_c{../@column}_l{../@line}">
			<img src="../../images/croix.gif" alt="Fermer" width="8" height="8" onclick="hide_mySelectMenu2(document.getElementById('act_c{../@column}_l{../@line}'));"/>
			<table cellpadding="0" cellspacing="0">
				<xsl:for-each select="./action">
					<tr>
						<td class="menuitems" onmouseover="highlight(this);" onMouseout="lowlight(this);">
							<xsl:choose>
								<xsl:when test="@key = 'null'">
									<xsl:attribute name="onclick">updateDataInObjSelected2('<xsl:value-of select="@char"/>', document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'));</xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="onclick">updateDataInObjSelected2('<xsl:value-of select="@char"/>', document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')); return doAction('<xsl:value-of select="@key"/>');</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="@label"/>
						</td>
						<td>
							(<xsl:value-of select="@char"/>)&#160;
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>

</xsl:stylesheet>
