<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE SNAMENU -->
	<xsl:template match="*[@type='snamenu']">
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:for-each select="menu/menuitem">
			<span style="position:absolute; 
						left:{./@column*$coefx+$offsetx}px; 
						top:{./@line*$coefy + $offsety}px;
						z-index: {$elDepth * 10 + 1};">
				<a style="line-height: {$coefy - 1}px" 
					class="menuItem"
					href="javascript:doMenu('KEY_ENTER','{@id}')">
					<xsl:value-of select="@id" />.<xsl:value-of select="@literal" />
					<xsl:if test="not(@command = '')">-<xsl:value-of select="@command" /></xsl:if>
				</a>
			</span>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
