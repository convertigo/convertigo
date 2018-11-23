<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE IMAGE -->
	<xsl:template match="*[@type='image']" priority="1">
		<span style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px;">
			<img src="{@url}" alt="{@alt}">
				<xsl:if test="@width">
					<xsl:attribute name="width"><xsl:value-of select="@width*$coefx"/></xsl:attribute>
					<xsl:attribute name="height"><xsl:value-of select="@height*$coefy"/></xsl:attribute>
				</xsl:if>
				<xsl:if test="@action">
					<xsl:attribute name="style">cursor: hand;</xsl:attribute>
					<xsl:choose>
						<xsl:when test="@dotransaction='true'">
							<xsl:attribute name="onclick">document.getElementById('__transaction').value='<xsl:value-of select="@action"/>'; doAction('');</xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action"/>');</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
			</img>
		</span>
	</xsl:template>
</xsl:stylesheet>