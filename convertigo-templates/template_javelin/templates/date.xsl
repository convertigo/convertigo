<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE DATE -->
	<xsl:template match="*[@type='date']">
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
				id="{@name}_n1parent">
			<input class="fixed" 
				name="{@name}" 
				id="{@name}_n1" 
				onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name})" 
				onfocus="currentFieldOnFocus=this.id;onInputClick(this)" 
				ondblclick="doAction('KEY_ENTER',  {@name})" 
				size="{$fieldSize}" 
				maxlength="{@size}" 
				value="{.}" 
				type="text" />
    	</span>
	</xsl:template>

</xsl:stylesheet>
