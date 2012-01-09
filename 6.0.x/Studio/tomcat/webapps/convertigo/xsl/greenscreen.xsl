<?xml version="1.0" encoding="ISO-8859-1"?>

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


<!-- comment out the following line if you want to edit this style sheet WYSIWYG in dreamweaver 6.x -->
<!--<!DOCTYPE xsl:stylesheet PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "../dtd/xhtml1-transitional.dtd">-->
<!-- comment out the preceding line if you want to edit this style sheet WYSIWYG in dreamweaver 6.x -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt">
	<xsl:output method="html" media-type="text/html" encoding="UTF-8"/>
	
	<!-- comment out the following lines if this file is in a project directory -->
	<!--
	<xsl:include href="../../xsl/status.xsl"/>
	<xsl:include href="../../xsl/variables.xsl"/>
	<xsl:include href="../../xsl/rules.xsl"/>
	<xsl:include href="../../xsl/headerfooter.xsl"/>
	-->
	
	<!-- comment out the following lines if this file is in the global xsl directory -->
	<xsl:include href="status.xsl"/>
	<xsl:include href="variables.xsl"/>
	<xsl:include href="rules.xsl"/>
	<xsl:include href="headerfooter.xsl"/>
	
	<!-- TEMPLATE DOCUMENT -->
	<xsl:template match="document">
		<html>
			<xsl:call-template name="header">
				<xsl:with-param name="css">../../css/greenscreen.css</xsl:with-param>
			</xsl:call-template>
			<!-- HTML page -->
			<body onResize="resize();" onLoad="MM_preloadImages('../../images/down_selected.gif','../../images/up_selected.gif', '../../images/rtop_keyboard.gif');keyboard_initVar();if(eval(parent.frames['check'])) parent.frames['check'].checkOnLoad();" onUnLoad="keyboard_setCookie();" topmargin="5" leftmargin="5">
				<!-- Scripts -->
				<xsl:call-template name="loadscripts"/>
				<!-- ======================================= Customize this part to adjust Viewport and params BEGIN =========================================================-->
				<script>
					offsetLeft		= 10;		// Size of the left border
					offsetRight		= 10;		// Size of right border
					offsetBottom	= 10;		// Size of he bottom border
					offsetTop		= 10;		// Size of the top Border
					bResize			= false;		// Set this value to false if you want to disable automatic font resize.
				</script>
				<!-- ======================================= Customize this part to adjust Viewport and params END   =========================================================-->
				<!--HTML page-->
				<div id="contents" style="visibility: hidden">
					<!--================================= Customize this part to create your own screen template  BEGIN =========================================================-->
					<!--================================= Customize this part to create your own screen template  END   =========================================================-->
				</div>
				
				<div id="generated_page">
					<xsl:call-template name="sizescript"/>
					<xsl:call-template name="form"/>
					<xsl:call-template name="footer"/>
				</div>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
