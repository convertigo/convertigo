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

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">

	<!--- WARNING : if you modify these constants, display may be wrong -->
	<xsl:variable name="coefx">8</xsl:variable>
	<xsl:variable name="coefy">18</xsl:variable>
	<xsl:param name="offsetx">10</xsl:param>
	<xsl:param name="offsety">45</xsl:param>
	<xsl:param name="offsetw">10</xsl:param>
	<xsl:param name="offseth">10</xsl:param>
	<!--Set this value to true if you want to display the action key in action buttons-->
	<xsl:param name="DisplayActionKey">false</xsl:param>
	<xsl:param name="doKeyboard">true</xsl:param>
	
</xsl:stylesheet>
