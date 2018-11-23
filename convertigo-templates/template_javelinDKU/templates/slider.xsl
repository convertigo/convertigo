<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE SLIDER -->
	<xsl:template match="*[@type='slider']">
		<xsl:param name="offsety" />
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- Top arrow -->
			<span style="position:absolute;
						left: {./@column*$coefx + $offsetx - 1}px; 
						top: {./@line*$coefy + $offsety}px;
						z-index: {$elDepth * 10 + 1};">
				<img style="position:absolute;
							width: {$coefx + 2}px; 
							height: {$coefy}px;"
				src="images/slider-arrow-top.gif"
				onclick="doAction('KEY_ROLLUP');"/>
			</span>
		<!-- Bottom arrow -->
		<span style="position:absolute;
						left: {./@column*$coefx + $offsetx - 1}px; 
						top: {(number(./@line)+number(./@height)-1)*$coefy + $offsety}px;
						z-index: {$elDepth * 10 + 1};">
			<img style="width: {$coefx + 2}px; 
						height: {$coefy}px;" 
				src="images/slider-arrow-bottom.gif"
				onclick="doAction('KEY_ROLLDOWN');"/>
		</span>
		<!-- Empty bar -->
		<span style="position:absolute;
						left: {./@column*$coefx + $offsetx - 1}px; 
						top: {(number(./@line)+1)*$coefy + $offsety}px;
						width: {$coefx + 2}px;
						height: {(number(@height)-2)*$coefy}px;
						background-color: #D4D4D4;
						z-index: {$elDepth * 10 + 1};">
		</span>
		<!-- Selection bar -->
		<span style="position:absolute;
						left: {((number(./@column) - 0)*$coefx) + $offsetx}px; 
						top: {(number(./@line)+1+number(./@sliderPos))*$coefy + $offsety}px;
						width: {$coefx - 2}px;
						height: {(number(./@sliderSize)-1)*$coefy}px;
						border: solid 1px #909090;
						background-color : #D4D0C8;
						z-index: {$elDepth * 10 + 1};">
		</span>
	</xsl:template>
</xsl:stylesheet>