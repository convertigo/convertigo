<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE PANEL -->
	<xsl:template match="*[@type='panel']">
		<xsl:param name="elDepth">
			<xsl:choose>
				<xsl:when test="@zOrder">
					<xsl:value-of select="10 - number(./@zOrder)"/>
				</xsl:when>
				<xsl:otherwise>
					0
				</xsl:otherwise>
			</xsl:choose>
		</xsl:param>
		<xsl:param name="offsety" />
		<!-- Background panel used for drop shadow graphical effect-->
		<span style="width: {@width*$coefx+$offsetw}px; 
					height:{@height*$coefy+$offseth}px;
					solid; 
					position:absolute; 
					left:{./@column*$coefx+$offsetx + 4}px; 
					top:{./@line*$coefy + $offsety + 4}px;
					z-index: {$elDepth * 10 - 1};">
			<xsl:choose>
				<xsl:when test="@shadow = 'true'">
					<xsl:attribute name="class">disabledPanelOmbre</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="class">panelOmbre</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
		</span>
		<!-- Front panel -->
		<span style="width:{@width*$coefx+$offsetw}px; 
					height:{@height*$coefy+$offseth}px; 
					position:absolute; 
					left:{./@column*$coefx + $offsetx}px; 
					top:{./@line*$coefy + $offsety}px;
					z-index: {$elDepth * 10};">
			<xsl:choose>
				<xsl:when test="@shadow = 'true'">
					<xsl:attribute name="class">disabledPanel</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="class">panel</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
		</span>
		<!-- Panel top title -->
		<span style="position:absolute; 
					left:{(number(./@column)+1)*$coefx + $offsetx}px; 
					top:{round((number(./@line) - 0.3)*$coefy + $offsety)}px;
					z-index: {$elDepth * 10};">
			<xsl:choose>
				<xsl:when test="@shadow = 'true'">
					<xsl:attribute name="class">disabledPanelTopTitle</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="class">panelTopTitle</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:value-of select="./@topTitle" />
		</span>
		<!-- Panel bottom title -->
		<span style="position:absolute; 
					left:{(number(./@column)+1)*$coefx + $offsetx}px; 
					top:{round((number(./@line) + number(./@height) - 0.3)*$coefy + $offsety)}px;">
			<xsl:choose>
				<xsl:when test="@shadow = 'true'">
					<xsl:attribute name="class">disabledPanelBottomTitle</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="class">panelBottomTitle</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:value-of select="./@bottomTitle" />
		</span>
		<xsl:apply-templates>
			<xsl:with-param name="offsety" select="$offsety" />
		</xsl:apply-templates>
	</xsl:template>
</xsl:stylesheet>
