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
	<!-- TEMPLATE DATE -->
	<xsl:template match="*[@type='date']">
		<xsl:param name="offsety" />
		<script src="scripts/calendar.js"><!--script--></script>
		<input class="fixed"
			style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px"
			name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}"
			ondblclick="doAction('KEY_ENTER')"
			onclick="window.dateField= {@name};show_myCalendar(event, '{@pattern}');"
			type="text" size="{@size}" maxlength="{@size}" value="{.}" />
	</xsl:template>
</xsl:stylesheet>
