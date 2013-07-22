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

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">

	<!-- TEMPLATE STATIC -->
	<xsl:template match="*[@type='static']" priority="1">
		<xsl:param name="offsety"/>
		
		<span class="color{@foreground}" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
			<xsl:if test="@blink">
				<span class="blink">
					<xsl:if test="@underline">
						<span class="underline">
							<span class="bgcolor{@background}">
								<xsl:value-of select="."/>
							</span>
						</span>
					</xsl:if>
					<xsl:if test="not(@underline)">
						<span class="bgcolor{@background}">
							<xsl:value-of select="."/>
						</span>
					</xsl:if>
				</span>
			</xsl:if>
			<xsl:if test="not(@blink)">
				<xsl:if test="@underline">
					<span class="underline">
						<span class="bgcolor{@background}">
							<xsl:value-of select="."/>
						</span>
					</span>
				</xsl:if>
				<xsl:if test="not(@underline)">
					<span class="bgcolor{@background}">
						<xsl:value-of select="."/>
					</span>
				</xsl:if>
			</xsl:if>
		</span>
	</xsl:template>
	
	<!-- TEMPLATE FIELD -->
	<xsl:template match="*[@type='field']" priority="1">
		<xsl:param name="fieldSize">
			<xsl:if test="(@column + @size) &lt; 80">
				<xsl:value-of select="@size"/>
			</xsl:if>
			<xsl:if test="(@column + @size) &gt;= 80">
				<xsl:value-of select="80 - @column"/>
			</xsl:if>
		</xsl:param>
		<xsl:param name="offsety"/>
		<xsl:param name="checkAutoEnter">
			<xsl:if test='(@autoenter)'>true</xsl:if>
			<xsl:if test='(not(@autoenter))'>false</xsl:if>
		</xsl:param>

		
		<xsl:if test="not(@history = 'true' and ./history/value)">
				<xsl:if test='not(@hidden = "true")'>
					<input class="fixed" name="{@name}" id="{@name}" onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name})" onfocus="currentFieldOnFocus=this.id" ondblclick="doAction('KEY_ENTER',  {@name})" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" size="{$fieldSize}" maxlength="{@size}" value="{.}" type="text"/>
				</xsl:if>
				<xsl:if test='(@hidden = "true")'>
					<input class="fixed" name="{@name}" id="{@name}" onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name})" onfocus="currentFieldOnFocus=this.id" ondblclick="doAction('KEY_ENTER',  {@name})" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" size="{$fieldSize}" maxlength="{@size}" value="{.}" type="password"/>
				</xsl:if>
		</xsl:if>
		
		<xsl:if test="@history='true' and ./history/value">
				<xsl:if test='not(@hidden = "true")'>
					<input class="fixed" id="{@name}" name="{@name}" autocomplete="off" LANGUAGE="javascript" onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name}); autoInput_onkeyup(event,this.form.name,this.name)" onkeypress="autoInput_onkeypress(event,this.form.name,this.name)" onkeydown="autoInput_onkeydown(event,this.form.name,this.name)" onblur="return autoInput_onblur(event,this.form.name,this.name)" onfocus="currentFieldOnFocus=this.id" ondblclick="autoInput_ondblclick(event,this.form.name,this.name)" style="position:absolute; left:{./@column*$coefx+ $offsetx}px; top:{./@line*$coefy + $offsety}px" size="{$fieldSize}" maxlength="{@size}" value="{text()}" type="text"/>
				</xsl:if>
				<xsl:if test='(@hidden = "true")'>
					<input class="fixed" id="{@name}" name="{@name}" autocomplete="off" LANGUAGE="javascript" onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name}); autoInput_onkeyup(event,this.form.name,this.name)" onkeypress="autoInput_onkeypress(event,this.form.name,this.name)" onkeydown="autoInput_onkeydown(event,this.form.name,this.name)" onblur="return autoInput_onblur(event,this.form.name,this.name)" onfocus="currentFieldOnFocus=this.id" ondblclick="autoInput_ondblclick(event,this.form.name,this.name)" style="position:absolute; left:{./@column*$coefx+ $offsetx}px; top:{./@line*$coefy + $offsety}px" size="{$fieldSize}" maxlength="{@size}" value="{text()}" type="password"/>
				</xsl:if>
				
				<div id="div_List{@name}" name="div_List{@name}" class="autoList" style="position:absolute; left:{./@column*$coefx+ $offsetx}px; top:{./@line*$coefy + $offsety + 14}px ; z-index:10" >
					<select id="List{@name}" name="List{@name}" size="5" LANGUAGE="javascript" onChange="autoList_onclick('javelin_form','{@name}');" style="font-size:8pt">
						<xsl:apply-templates mode="history" select="./history/value"/>
					</select>
				</div>
		</xsl:if>

		<xsl:if test="(@hasFocus ='true')">
			<script type="text/javascript" language="javascript">
				var elt= document.getElementsByTagName("INPUT");
				if(elt['<xsl:value-of select="@name"/>'])
					focusOnField = elt['<xsl:value-of select="@name"/>'];
	          	</script>
		</xsl:if>
	</xsl:template>

	<!-- TEMPLATE VALUE FOR HISTORY -->
	<xsl:template match="value" mode="history" priority="1">
		<option value='{.}'><xsl:value-of select="."/></option>
	</xsl:template>

	<!-- TEMPLATE VALUE -->
	<xsl:template match="value" priority="1"/>

	<!-- TEMPLATE DATE -->
	<xsl:template match="*[@type='date']" priority="1">
		<xsl:param name="offsety"/>
		<script src="../../scripts/calendar.js"><!--script--></script >
		<input class="fixed" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('KEY_ENTER')" onclick="window.dateField= {@name};show_myCalendar(event, '{@pattern}');" type="text" size="{@size}" maxlength="{@size}" value="{.}"/>
	</xsl:template>
	
	<!-- TEMPLATE KEYWORD -->
	<xsl:template match="*[@type='keyword']" priority="1">
		<xsl:param name="offsety"/>
		<xsl:if test="/document/blocks/*/@type ='panel'">
			<xsl:if test="../@type = 'panel'">
				<span style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:if test=". = '(F4)'">
						<input class="keywordbutton" name="{@name}" id="{@name}" type="button" style="font-size:6pt" value="(F4)">
							<xsl:if test="preceding-sibling::*[2]/@type='field' and preceding-sibling::*[2]/@line=./@line">
								<xsl:attribute name="onclick">document.javelin_form.<xsl:value-of select="preceding-sibling::*[2]/@name"/>.focus();currentFieldOnFocus='<xsl:value-of select="preceding-sibling::*[2]/@name"/>';doAction('<xsl:value-of select="@action"/>')</xsl:attribute>
							</xsl:if>
							<xsl:if test="not(preceding-sibling::*[2]/@type='field' and preceding-sibling::*[2]/@line=./@line)">
								<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action"/>')</xsl:attribute>
							</xsl:if>
						</input>
					</xsl:if>
					<xsl:if test="not(. = '(F4)')">
						<xsl:if test="$DisplayActionKey='true'">
							<input class="keywordbutton" name="{@name}" id="{@name}" onclick="" type="button" style="font-size:6pt" value="{substring(@action,6)}={.}">
								<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action"/>')</xsl:attribute>
							</input>
						</xsl:if>
						<xsl:if test="not($DisplayActionKey='true')">
							<input class="keywordbutton" name="{@name}" id="{@name}" onclick="" type="button" style="font-size:6pt" value="{.}">
								<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action"/>')</xsl:attribute>
							</input>
						</xsl:if>
					</xsl:if>
				</span>
			</xsl:if>
		</xsl:if>
		<xsl:if test="not(/document/blocks/*/@type ='panel')">
			<span style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:if test=". = '(F4)'">
						<input class="keywordbutton" name="{@name}" id="{@name}" type="button" style="font-size:6pt" value="(F4)">
							<xsl:if test="preceding-sibling::*[2]/@type='field' and preceding-sibling::*[2]/@line=./@line">
								<xsl:attribute name="onclick">document.javelin_form.<xsl:value-of select="preceding-sibling::*[2]/@name"/>.focus();currentFieldOnFocus='<xsl:value-of select="preceding-sibling::*[2]/@name"/>';doAction('<xsl:value-of select="@action"/>')</xsl:attribute>
							</xsl:if>
							<xsl:if test="not(preceding-sibling::*[2]/@type='field' and preceding-sibling::*[2]/@line=./@line)">
								<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action"/>')</xsl:attribute>
							</xsl:if>
						</input>
					</xsl:if>
					<xsl:if test="not(. = '(F4)')">
						<xsl:if test="$DisplayActionKey='true'">
							<input class="keywordbutton" name="{@name}" id="{@name}" onclick="" type="button" style="font-size:6pt" value="{substring(@action,6)}={.}">
								<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action"/>')</xsl:attribute>
							</input>
						</xsl:if>
						<xsl:if test="not($DisplayActionKey='true')">
							<input class="keywordbutton" name="{@name}" id="{@name}" onclick="" type="button" style="font-size:6pt" value="{.}">
								<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action"/>')</xsl:attribute>
							</input>
						</xsl:if>
					</xsl:if>
				</span>

		</xsl:if>

<!-- VERSION AVEC BOUTONS SOPHISTIQUES MAIS NON RESIZABLE
		<table border="0" cellpadding="0" cellspacing="0" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" onclick="doAction('{@action}')">
			<tr>
				<td vAlign="top">
					<img alt="" name="{@name}_left_img" id= "{@name}_left_img" src="../../images/kbutton_left.gif" border="0"/>
				</td>
				<xsl:if test="$DisplayActionKey='true'">
					<td vAlign="middle" align="center" class="kbutton" name="{@name}_txt" id="{@name}_txt">
						<span>
							<xsl:value-of select="substring(@action,6)"/>=<xsl:value-of select="."/>
						</span>
					</td>
				</xsl:if>
				<xsl:if test="not($DisplayActionKey='true')">
					<td vAlign="middle" align="center" class="kbutton" name="{@name}_txt" id="{@name}_txt">
						<span>
							<xsl:value-of select="."/>
						</span>
					</td>
				</xsl:if>
				<td vAlign="top">
					<img alt="" name="{@name}_right_img" id= "{@name}_right_img" src="../../images/kbutton_right.gif" border="0"/>
				</td>
			</tr>
		</table>
-->

	</xsl:template>
	
	<!-- TEMPLATE SNAMENU -->
	<xsl:template match="*[@type='snamenu']" priority="1">
		<xsl:for-each select="menu/menuitem">
				<span style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<a style="line-height: {$coefy - 1}px" class="menuItem" href="javascript:doMenu('KEY_ENTER','{@id}')">
						<xsl:value-of select="@id"/>. <xsl:value-of select="@literal"/> - <xsl:value-of select="@command"/>
					</a>
				</span>
		</xsl:for-each>
	</xsl:template>
	
	
	<!-- TEMPLATE PANEL -->
	<xsl:template match="*[@type='panel']" priority="1">
		<xsl:param name="offsety"/>
		<span class="panelOmbre" style="width:{@width*$coefx+$offsetw}px; height:{@height*$coefy+$offseth}px; solid; position:absolute; left:{./@column*$coefx+$offsetx + 4}px; top:{./@line*$coefy + $offsety+4}px"/>
		<span class="panel" style="width:{@width*$coefx+$offsetw}px; height:{@height*$coefy+$offseth}px; position:absolute; left:{./@column*$coefx + $offsetx}px; top:{./@line*$coefy + $offsety}px"/>
		<xsl:apply-templates>
			<xsl:with-param name="offsety" select="$offsety"/>
		</xsl:apply-templates>
		<!--xsl:for-each select=".//*[@type='keyword']">
			<input style="position:absolute; left:{@column*$coefx+$offsetx - 10}px; top:{@line*$coefy + $offsety}px; font-size:{$coefx  - 1}pt" name="{@name}" id="{@name}" onclick="doAction('{@action}')" type="button" value="{.}"/>
		</xsl:for-each-->
	</xsl:template>
	
	<!-- TEMPLATE TABLE -->
	<xsl:template match="*[@type='table']" priority="1">
		<xsl:param name="offsety"/>
		<table cellpadding="0" cellspacing="0" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{(./@line - @offset)*$coefy + $offsety}px ;  height:{@rows * $coefy}px; width:{$coefx*@width}px" border="0">
			<tr>
				<td>
					<table border="0" cellspacing="0" cellpadding="0" class="data"  width="100%" height="100%">
						<tr>
							<xsl:apply-templates select="row/Title"/>
						</tr>
						<xsl:apply-templates select="row[position() > 1]"/>
					</table>
				</td>
				<td>
					<table height="100%" width="16" border="0">
						<tr> 
							<td height="16" width="16" valign="top"><img width="16" height="16" src="../../images/hide.gif" onclick="return doAction('KEY_ROLLUP')"/></td>
						</tr>
						<tr>
							<td height="98%" valign="middle"><img width="16" height="98%" src="../../images/barre.gif"/></td>
						</tr>
						<tr> 
							<td height="16" width="16" valign="bottom"><img width="16" height="16" src="../../images/show.gif" onclick="return doAction('KEY_ROLLDOWN')"/></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
		<xsl:apply-templates select="actionsTable"/>
	</xsl:template>
	
	<!-- OLD TEMPLATE ACTIONSTABLE
	<xsl:template match="actionsTable">
		<xsl:comment>ajout de la table des actions</xsl:comment>
		<script src="../../scripts/selectMenu.js"><OpenCommentTag[script]CloseCommentTag></script >
		<script type="text/javascript" language="javascript">
			BeginSelect('table_c<xsl:value-of select="../@column"/>_l<xsl:value-of select="../@line"/>');
			<xsl:for-each select="child::*">
				<xsl:if test='contains(@label, "&#39;")'>
					AddSelect('<xsl:value-of select="@char"/>', '<xsl:value-of select="@key"/>', "<xsl:value-of select="@label"/>");
				</xsl:if>
				<xsl:if test='not(contains(@label, "&#39;" ))'>
					AddSelect('<xsl:value-of select="@char"/>', '<xsl:value-of select="@key"/>', '<xsl:value-of select="@label"/>');
				</xsl:if>
			</xsl:for-each>
			EndSelect();
          	</script>
	</xsl:template>
	-->
	<!-- TEMPLATE ACTIONSTABLE IN DIV -->
	<xsl:template match="actionsTable" priority="1">
		<script src="../../scripts/selectMenu.js"><!--script--></script >
		<div class="actDiv" id="act_c{../@column}_l{../@line}">
			<img src="../../images/croix.gif" alt="Fermer" width="8" height="8" onclick="hide_mySelectMenu(document.getElementById('act_c{../@column}_l{../@line}'));"/>
			<table cellpadding="0" cellspacing="0">
				<xsl:for-each select="./action">
					<tr>
						<td class="menuitems" onmouseover="highlight(this);" onMouseout="lowlight(this);">
							<xsl:choose>
								<xsl:when test="@key = 'null'">
									<xsl:attribute name="onclick">updateDataInObjSelected('<xsl:value-of select="@char"/>', document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'));</xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="onclick">updateDataInObjSelected('<xsl:value-of select="@char"/>', document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')); return doAction('<xsl:value-of select="@key"/>');</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="@label"/>
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>

	<!-- TEMPLATE ROW -->
	<xsl:template match="row" priority="2">
		<tr style="line-height: {$coefy - 1}px">
			<xsl:if test="(@line mod 2) = 0">
				<xsl:attribute name="class">dataroweven</xsl:attribute>
			</xsl:if>
			<xsl:if test="(@line mod 2) != 0">
				<xsl:attribute name="class">datarowodd</xsl:attribute>
			</xsl:if>

			<xsl:for-each select="child::*">
				<td>
					<xsl:choose>
						<xsl:when test='(@type = "field")'>
							<xsl:attribute name="align">center</xsl:attribute>
							
								<xsl:if test="not(@autoenter)">
									<xsl:if test="(@columnSelection = 'true')">
										<input class="fixed" style="height:{$coefy - 1}px;width:{$coefx*2}px" type="text"  name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" value="{.}">
											<!--<xsl:attribute name="onclick">show_mySelectMenu(event, this, 'table_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')</xsl:attribute>-->
											<!--<xsl:attribute name="onblur">blur_mySelectMenu('table_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')</xsl:attribute>-->
											<xsl:attribute name="onclick">show_mySelectMenu(event, document.getElementById('<xsl:value-of select="@name"/>'), document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
											<xsl:attribute name="onblur">hide_mySelectMenu(document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
										</input>
									</xsl:if>	
									<xsl:if test="not(@columnSelection = 'true')">
										<input class="fixed" style="height:{$coefy - 1}px" name="{@name}" onkeyup="checkInputChars(event, {@size}, false, document.javelin_form.{@name})"  onfocus="currentFieldOnFocus=this.id" id="{@name}" size="{@size}" maxlength="{@size}" value="{.}" />
									</xsl:if>	
								</xsl:if>

								<xsl:if test="@autoenter">
									<xsl:if test="(@columnSelection = 'true')">
										<input class="fixed" style="height:{$coefy - 1}px;width:{$coefx*2}px" type="text"  name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" value="{.}">
											<!--<xsl:attribute name="onclick">show_mySelectMenu(event, this, 'table_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')</xsl:attribute>-->
											<!--<xsl:attribute name="onblur">blur_mySelectMenu('table_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')</xsl:attribute>-->
											<xsl:attribute name="onclick">show_mySelectMenu(event, document.getElementById('<xsl:value-of select="@name"/>'), document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
											<xsl:attribute name="onblur">hide_mySelectMenu(document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>

										</input>
									</xsl:if>	
									<xsl:if test="not(@columnSelection = 'true')">
										<input class="fixed" style="height:{$coefy - 1}px" name="{@name}" onkeyup="checkInputChars(event, {@size}, true, document.javelin_form.{@name})"  onfocus="currentFieldOnFocus=this.id" id="{@name}" size="{@size}" maxlength="{@size}" value="{.}" />
									</xsl:if>	
								</xsl:if>
								
								<xsl:if test="(@hasFocus = 'true')">
									<script type="text/javascript" language="javascript">
										var elt= document.getElementsByTagName("INPUT");
										if(elt['<xsl:value-of select="@name"/>'])
											focusOnField = elt['<xsl:value-of select="@name"/>'];
							          	</script>
								</xsl:if>		
								
						</xsl:when>
						<xsl:when test='(@type = "choice")'>
							<select class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}">
								<xsl:for-each select="child::*">
									<option value="{@action}">
										<xsl:if test="(@selected = 'true')"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
										<xsl:value-of select="@value"/>
									</option>
								</xsl:for-each>				
							</select>
							<xsl:if test="(@hasFocus ='true')">
								<script type="text/javascript" language="javascript">
									var elt= document.getElementsByTagName("SELECT");
									if(elt['<xsl:value-of select="@name"/>'])
										focusOnField = elt['<xsl:value-of select="@name"/>'];
						          	</script>
							</xsl:if>
						</xsl:when>
						<xsl:otherwise>
							<xsl:if test="number(translate(.,',','.'))=number(translate(.,',','.'))">
								<xsl:attribute name="align">right</xsl:attribute>
							</xsl:if>
							<xsl:if test="string-length(.) = 0">
								&#160;
							</xsl:if>
							<xsl:if test="string-length(.) != 0">
								<xsl:value-of select="."/>
							</xsl:if>
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>
	
	<!-- TEMPLATE TITLE -->
	<xsl:template match="Title" priority="1">
		<td class="datatitle">
			<xsl:if test="string-length(.) = 0">
				 
			</xsl:if>
			<xsl:if test="string-length(.) != 0">
				<xsl:value-of select="."/>
			</xsl:if>
		</td>
	</xsl:template>
	
	<!-- TEMPLATE CHOICE -->
	<xsl:template match="*[@type='choice']" priority="1">
		<xsl:param name="fieldSize">
			<xsl:value-of select="@size"/>
		</xsl:param>
		<xsl:param name="offsety"/>
		<xsl:if test="@radio = 'true'">
			<input class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" size="{@size}" maxlength="{@size}" value="{.}"/>
			<span class="panel" style="position:absolute; left:{(@column+@size+1)*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
				<xsl:for-each select="child::*">
					<xsl:if test="@selected = 'true'">
						<input type="radio" checked="true" name="{../@name}_r" id="{../@name}_r"  onclick="document.javelin_form.{../@name}.value = '{@value}'" value="{@value}"/><xsl:value-of select="@value"/> 
					</xsl:if>
					<xsl:if test="string-length(@selected) = 0">
						<input type="radio"  name="{../@name}_r" id="{../@name}_r"  onclick="document.javelin_form.{../@name}.value = '{@value}'" value="{@value}"/><xsl:value-of select="@value"/> 
					</xsl:if>
				</xsl:for-each>				
			</span>
			<xsl:if test="(@hasFocus ='true')">
				<script type="text/javascript" language="javascript">
					var elt= document.getElementsByTagName("INPUT");
					if(elt['<xsl:value-of select="@name"/>'])
						focusOnField = elt['<xsl:value-of select="@name"/>'];
		          	</script>
			</xsl:if>
		</xsl:if>
		<xsl:if test="@radio = 'false'">
			<select class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" style="position:absolute; left:{@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
				<xsl:for-each select="child::*">
					<option value="{@action}">
						<xsl:if test="(@selected = 'true')"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
						<xsl:value-of select="@value"/>
					</option>
				</xsl:for-each>				
			</select>
			<xsl:if test="(@hasFocus ='true')">
				<script type="text/javascript" language="javascript">
					var elt= document.getElementsByTagName("SELECT");
					if(elt['<xsl:value-of select="@name"/>'])
						focusOnField = elt['<xsl:value-of select="@name"/>'];
		          	</script>
			</xsl:if>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
