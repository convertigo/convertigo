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
	<!-- TEMPLATE FIELD -->
	<xsl:template match="*[@type='field']">
		<xsl:param name="fieldSize">
			<xsl:if test="(@column + @size) &lt; 80">
				<xsl:value-of select="@size" />
			</xsl:if>
			<xsl:if test="(@column + @size) &gt;= 80">
				<xsl:value-of select="80 - @column" />
			</xsl:if>
		</xsl:param>
		<xsl:param name="offsety" />
		<xsl:param name="checkAutoEnter">
			<xsl:if test='(@autoenter)'>true</xsl:if>
			<xsl:if test='(not(@autoenter))'>false</xsl:if>
		</xsl:param>


		<xsl:if test="not(@history = 'true' and ./history/value)">
			<xsl:if test='not(@hidden = "true")'>
				<input class="fixed" name="{@name}" id="{@name}"
					onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name})"
					onfocus="currentFieldOnFocus=this.id"
					ondblclick="doAction('KEY_ENTER',  {@name})"
					style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px"
					size="{$fieldSize}" maxlength="{@size}" value="{.}" type="text" />
			</xsl:if>
			<xsl:if test='(@hidden = "true")'>
				<input class="fixed" name="{@name}" id="{@name}"
					onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name})"
					onfocus="currentFieldOnFocus=this.id"
					ondblclick="doAction('KEY_ENTER',  {@name})"
					style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px"
					size="{$fieldSize}" maxlength="{@size}" value="{.}"
					type="password" />
			</xsl:if>
		</xsl:if>

		<xsl:if test="@history='true' and ./history/value">
			<xsl:if test='not(@hidden = "true")'>
				<input class="fixed" id="{@name}" name="{@name}"
					autocomplete="off" LANGUAGE="javascript"
					onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name}); autoInput_onkeyup(event,this.form.name,this.name)"
					onkeypress="autoInput_onkeypress(event,this.form.name,this.name)"
					onkeydown="autoInput_onkeydown(event,this.form.name,this.name)"
					onblur="return autoInput_onblur(event,this.form.name,this.name)"
					onfocus="currentFieldOnFocus=this.id"
					ondblclick="autoInput_ondblclick(event,this.form.name,this.name)"
					style="position:absolute; left:{./@column*$coefx+ $offsetx}px; top:{./@line*$coefy + $offsety}px"
					size="{$fieldSize}" maxlength="{@size}" value="{text()}"
					type="text" />
			</xsl:if>
			<xsl:if test='(@hidden = "true")'>
				<input class="fixed" id="{@name}" name="{@name}"
					autocomplete="off" LANGUAGE="javascript"
					onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name}); autoInput_onkeyup(event,this.form.name,this.name)"
					onkeypress="autoInput_onkeypress(event,this.form.name,this.name)"
					onkeydown="autoInput_onkeydown(event,this.form.name,this.name)"
					onblur="return autoInput_onblur(event,this.form.name,this.name)"
					onfocus="currentFieldOnFocus=this.id"
					ondblclick="autoInput_ondblclick(event,this.form.name,this.name)"
					style="position:absolute; left:{./@column*$coefx+ $offsetx}px; top:{./@line*$coefy + $offsety}px"
					size="{$fieldSize}" maxlength="{@size}" value="{text()}"
					type="password" />
			</xsl:if>

			<div id="div_List{@name}" name="div_List{@name}"
				class="autoList"
				style="position:absolute; left:{./@column*$coefx+ $offsetx}px; top:{./@line*$coefy + $offsety + 14}px ; z-index:10">
				<select id="List{@name}" name="List{@name}" size="5"
					LANGUAGE="javascript"
					onChange="autoList_onclick('javelin_form','{@name}');"
					style="font-size:8pt">
					<xsl:apply-templates mode="history"
						select="./history/value" />
				</select>
			</div>
		</xsl:if>

		<xsl:if test="(@hasFocus ='true')">
			<script type="text/javascript" language="javascript">
				var elt= document.getElementsByTagName("INPUT");
				if(elt['<xsl:value-of select="@name" />']) focusOnField = elt['<xsl:value-of select="@name" />'];
			</script>
		</xsl:if>
	</xsl:template>

	<!-- TEMPLATE VALUE FOR HISTORY -->
	<xsl:template match="value" mode="history">
		<option value='{.}'>
			<xsl:value-of select="." />
		</option>
	</xsl:template>

	<!-- TEMPLATE VALUE -->
	<xsl:template match="value" />
</xsl:stylesheet>
