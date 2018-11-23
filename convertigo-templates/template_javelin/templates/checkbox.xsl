<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE CHECKBOX -->
	<xsl:template match="*[@type='checkbox']">
		<xsl:param name="offsety" />
		<input type="hidden" id="{./@name}hidden" name="{./@name}" value="">
			<xsl:choose>
				<xsl:when test="@checked='true'">
					<xsl:attribute name="value"><xsl:value-of select="./@checkingPattern"/></xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="value"><xsl:value-of select="./@uncheckingPattern"/></xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
		</input>
		<span style="position: absolute;
					  top: {@line*$coefy + $offsety}px; 
					  left: {@column*$coefx + $offsetx}px;"
				id="{./@name}_n1parent">
			<input type="checkbox" 
					onclick="document.getElementById('{./@name}hidden').value=this.checked?'/':'';"
					onfocus="currentFieldOnFocus=this.id; onInputClick(this);"
					id="{./@name}_n1">
				<xsl:if test="@checked='true'">
					<xsl:attribute name="checked">checked</xsl:attribute>
				</xsl:if>
			</input>
			<span class="color{../@foreground}"><span class="bgcolor{../@background}"><xsl:value-of select="."/></span></span>
		</span>
	</xsl:template>
</xsl:stylesheet>