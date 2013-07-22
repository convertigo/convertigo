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
	<!-- TEMPLATE PANEL -->
	<xsl:template match="*[@type='panel']">
		<xsl:param name="offsety" />
		<span class="panelOmbre"
			style="width:{@width*$coefx+$offsetw}px; height:{@height*$coefy+$offseth}px; solid; position:absolute; left:{./@column*$coefx+$offsetx + 4}px; top:{./@line*$coefy + $offsety+4}px" />
		<span class="panel"
			style="width:{@width*$coefx+$offsetw}px; height:{@height*$coefy+$offseth}px; position:absolute; left:{./@column*$coefx + $offsetx}px; top:{./@line*$coefy + $offsety}px" />
		<xsl:apply-templates>
			<xsl:with-param name="offsety" select="$offsety" />
		</xsl:apply-templates>
	</xsl:template>
</xsl:stylesheet>
