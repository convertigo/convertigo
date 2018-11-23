<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	
	<!-- TEMPLATE BUTTONS PANEL -->
	<xsl:template match="*[@type='buttonsPanel']">
		<xsl:param name="offsety" />
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		
			<xsl:apply-templates>
				<xsl:with-param name="offsety" select="$offsety + ancestor::blocks[1]/@page-number * $coefy * 24" />
				<xsl:with-param name="elDepth"><xsl:value-of select="$elDepth"/></xsl:with-param>
				<!-- There is a panel in the screen. We only display buttons that are in that panel -->
				<xsl:with-param name="enable">
				<xsl:choose>
					<xsl:when test="@shadow = 'true'">false</xsl:when>
					 <xsl:otherwise>true</xsl:otherwise>
				</xsl:choose>
				</xsl:with-param>
			</xsl:apply-templates>
	</xsl:template>
	
</xsl:stylesheet>