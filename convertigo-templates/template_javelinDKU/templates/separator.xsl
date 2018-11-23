<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE SEPARATOR -->
	<xsl:template match="*[@type='separator']" priority="1">
		<span style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety + 8}px; z-index: -1;">
			<hr style="width: {@width*$coefx}px;"/>
		</span>
	</xsl:template>
</xsl:stylesheet>