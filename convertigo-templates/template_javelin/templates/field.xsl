<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE FIELD -->
	<xsl:template match="*[@type='field']">
		<xsl:param name="fieldSize">
			<xsl:variable name="sWidth" select="/document/@screenWidth"/>
			<xsl:if test="(@column + @size) &lt; $sWidth">
				<xsl:value-of select="@size" />
			</xsl:if>
			<xsl:if test="(@column + @size) &gt;= $sWidth">
				<xsl:value-of select="$sWidth - @column" />
			</xsl:if>
		</xsl:param>
		<xsl:param name="offsety" />
		<xsl:param name="checkAutoEnter">
			<xsl:if test='(@autoenter)'>true</xsl:if>
			<xsl:if test='(not(@autoenter))'>false</xsl:if>
		</xsl:param>
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:if test="(@hasFocus ='true')">
			<input type="hidden" id="focus" value= "{@name}" />
		</xsl:if>
		
		<!-- Container SPAN. Used only for positioning and framework behaviour -->
		<span style="position:absolute; 
						left:{./@column*$coefx+$offsetx}px; 
						top:{./@line*$coefy + $offsety}px;
						z-index: {$elDepth * 10 + 1};" 
				id="{@name}_n1parent" >
			<input style="width: {$fieldSize*$coefx}px;"
					maxlength="{@size}"
					name="{@name}"
					id="{@name}_n1"
					onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name});"
					onfocus="currentFieldOnFocus=this.id; onInputClick(this);"
					ondblclick="doAction('KEY_ENTER',  {@name});"
					value="{.}">
				<xsl:choose>
					<xsl:when test="./@mashup_event">
						<xsl:attribute name="class">mashupableFieldText</xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="class">extField</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:if test="./@mashup_event">
					<xsl:call-template name="mashupVars" />
					<xsl:call-template name="mashupTooltipOnOff"/>
					<xsl:attribute name="onclick"><xsl:call-template name="mashupEvent"/></xsl:attribute>
				</xsl:if>
				<xsl:if test="./@previousContinuous">
					<xsl:attribute name="previousField"><xsl:value-of select="./@previousContinuous"/>_n1</xsl:attribute>
				</xsl:if>
				<xsl:if test="./@nextContinuous">
					<xsl:attribute name="nextField"><xsl:value-of select="./@nextContinuous"/>_n1</xsl:attribute>
				</xsl:if>
				<xsl:choose>
					<xsl:when test="@hidden = 'true'">
						<xsl:attribute name="type">password</xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="type">text</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
			</input>
		</span>
	</xsl:template>
	
	<!--
	<xsl:if test="not(@history = 'true' and ./history/value)">
		
	</xsl:if>
	<xsl:if test="@history='true' and ./history/value">
				<input class="fieldText"
						id="{@name}_n1"
						name="{@name}"
						autocomplete="off"
						LANGUAGE="javascript"
						onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name}); autoInput_onkeyup(event,this.form.name,this.name)"
						onkeypress="autoInput_onkeypress(event,this.form.name,this.name)"
						onkeydown="autoInput_onkeydown(event,this.form.name,this.name)"
						onblur="return autoInput_onblur(event,this.form.name,this.name)"
						onfocus="currentFieldOnFocus=this.id;onInputClick(this)"
						ondblclick="autoInput_ondblclick(event,this.form.name,this.name)"
						style="width: {$fieldSize*$coefx}px"
						maxlength="{@size}"
						value="{text()}">
					<xsl:choose>
						<xsl:when test="@hidden = 'true'">
							<xsl:attribute name="type">password</xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="type">text</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
				</input>

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
	-->
	<!-- TEMPLATE VALUE FOR HISTORY -->
	<xsl:template match="value" mode="history">
		<option value='{.}'>
			<xsl:value-of select="." />
		</option>
	</xsl:template>

	<!-- TEMPLATE VALUE -->
	<xsl:template match="value" />
</xsl:stylesheet>
