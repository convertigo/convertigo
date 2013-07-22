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

	<xsl:template name="header">
		<xsl:param name="css" select="../../css/sna.css"/>
		<head>
			<title>Convertigo</title>
			<meta http-equiv="Cache-Control" content="no-cache"/>
			<meta http-equiv="Pragma" content="no-cache"/>
			<meta http-equiv="Expires" content="-1" />

			<!-- Common style sheets -->
			<link href="../../css/button.css" rel="styleSheet" type="text/css"><!--stylesheet--></link>
			<link href="../../css/contextMenu.css" rel="styleSheet" type="text/css"><!--stylesheet--></link>
			<link href="css/sna.css" rel="styleSheet" type="text/css"><!--stylesheet--></link>		
			<xsl:if test="//@history = 'true'">
				<link href="../../css/autocomplete.css" rel="styleSheet" type="text/css"><!--stylesheet--></link>
			</xsl:if>
			
			<!-- User defined style sheets -->
			<xsl:call-template name="generate-css">
				<xsl:with-param name="css"><xsl:value-of select="$css"/></xsl:with-param>
			</xsl:call-template>
		</head>
	</xsl:template>

	<xsl:template name="generate-css">
		<xsl:param name="css"/>
		<xsl:if test="string-length($css) > 0">
			<xsl:choose>
				<xsl:when test="contains($css, ',')">
					<xsl:call-template name="generate-css-link">
						<xsl:with-param name="css"><xsl:value-of select="substring-before($css, ',')"/></xsl:with-param>
					</xsl:call-template>
					<xsl:call-template name="generate-css">
						<xsl:with-param name="css"><xsl:value-of select="substring-after($css, ',')"/></xsl:with-param>
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="generate-css-link">
						<xsl:with-param name="css"><xsl:value-of select="$css"/></xsl:with-param>
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="generate-css-link">
		<xsl:param name="css"/>
		<link href="{$css}" rel="styleSheet" type="text/css"><!--stylesheet--></link>
	</xsl:template>
	
	<xsl:template name="loadscripts">
		<script type="text/javascript" language="JavaScript">
			var fontSize= <xsl:value-of select="$coefx"></xsl:value-of>;
			var coefx= <xsl:value-of select="$coefx"></xsl:value-of>;
			var coefy= <xsl:value-of select="$coefy"></xsl:value-of>;
			var offsetx= <xsl:value-of select="$offsetx"></xsl:value-of>;
			var offsety= <xsl:value-of select="$offsety"></xsl:value-of>;
			var offseth= <xsl:value-of select="$offseth"></xsl:value-of>;
			var offsetw= <xsl:value-of select="$offsetw"></xsl:value-of>;
			var hasCalendar= false;
			var hasTableAction= false;
		</script>
		
		<!-- USER customizable scripts -->
		<script src="scripts/scriptlib.js"><!--script--></script >
		<script src="scripts/keymapsna.js"><!--script--></script>
		<script src="scripts/contextMenu.js"><!--script--></script>
		
		
		<!-- Convertigo System scripts -->
		<script src="../../scripts/button.js"><!--script--></script>
		<script src="../../scripts/position.js"><!--script--></script>
		<script src="../../scripts/cookies.js"><!--script--></script>
		<script src="../../scripts/keyboard.js"><!--script--></script>
		
		<xsl:if test="//@history = 'true'">
			<script src="../../scripts/autocomplete.js"><!--script--></script>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="footer">
		<script type="text/javascript" language="javascript">
			// script for showing the HTML page before loading keyboard and context menu
			
			getPositions();
			resize();
			
			document.getElementById('contents').style.visibility = 'visible';
			if(focusOnField != null)
		  		if(focusOnField.focus)
					focusOnField.focus();
		</script>

		<!-- Context Menu -->
		<script type="text/javascript" language="javascript">
			document.write(InitContextMenu());
		</script>

		<!-- Keyboard -->
		<div onMouseOver="stopTimer();" onMouseOut="setTimerMenu();">
			<table width="100%" border="0" cellpadding="0" cellspacing="0" id="keyboard_menu" 
				style="LEFT: 5px; POSITION: absolute; TOP: 0px; visibility: hidden; z-index:10">
			  <tr>
			  	<td width="24" height="100%">
					<table width="24" height="100%" cellpadding="0" cellspacing="0">
						<tr>
							<td vAlign="top" colSpan="2" width="24" height="26" 
								onMouseUp="mousePressed= false;" 
								onMouseOut="mousePressed= false;" 
								onMouseDown="initPos(event);mousePressed= true;" 
								onMouseMove="moveKeyboard(event);"><img height="26" width="24" alt="" src="../../images/ltop_keyboard.gif" border="0"/></td>
						</tr>
						<tr>
						    <td nowrap="true" height="100%" width="8"
								onMouseUp="mousePressed= false;" 
								onMouseOut="mousePressed= false;" 
								onMouseDown="initPos(event);mousePressed= true;" 
								onMouseMove="moveKeyboard(event);"><img src="../../images/border_left.gif" height="100%" width="8"/></td>
						    <td nowrap="true" bgColor="#475298" height="100%" width="16"><img height="100%" width="16" alt="" src="../../images/spacer.gif" border="0"/></td>
						</tr>
						<tr>
							<td vAlign="top" colSpan="2" width="24" height="26"
								onMouseUp="mousePressed= false;" 
								onMouseOut="mousePressed= false;" 
								onMouseDown="initPos(event);mousePressed= true;" 
								onMouseMove="moveKeyboard(event);"><img height="26" width="24" alt="" src="../../images/lbot_keyboard.gif" border="0"/></td>
						</tr>
					</table>
				</td>
			    <td height="100%">
					<table width="100%" height="100%" cellpadding="0" cellspacing="0">
						<tr>
							<td width="100%" align="center" valign="top" background="../../images/border_top.gif" height="10"
								onMouseUp="mousePressed= false;" 
								onMouseOut="mousePressed= false;" 
								onMouseDown="initPos(event);mousePressed= true;" 
								onMouseMove="moveKeyboard(event);"><spacer width="1" height="1" type="block"/></td>
						</tr>
						<tr>
							<td width="100%">
								<script language="JavaScript">
									document.write(createKeyboard());
								</script>				
							</td>
						</tr>
						<tr>
			                  <td width="100%" align="center" valign="top" background="../../images/border_bottom.gif" height="10"
										onMouseUp="mousePressed= false;" 
										onMouseOut="mousePressed= false;" 
										onMouseDown="initPos(event);mousePressed= true;" 
										onMouseMove="moveKeyboard(event);"><img 
							  			align="center" 
										src="../../images/up.gif" 
										name="buttonMenu" 
										width="77" height="10" 
										border="0" 
										onClick="showSmallKeyboard()" 
										onMouseOut="MM_swapImgRestore()" 
										onMouseOver="MM_swapImage('buttonMenu','','../../images/up_selected.gif',1)"/></td>
						</tr>
					</table>
				</td>
			  	<td width="24" height="100%">
					<table width="24" height="100%" cellpadding="0" cellspacing="0">
						<tr>
							<td vAlign="top" colSpan="2" width="24"
								onMouseUp="mousePressed= false;" 
								onMouseOut="mousePressed= false;" 
								onMouseDown="initPos(event);mousePressed= true;" 
								onMouseMove="moveKeyboard(event);"><img id="keyboardStatus" src="../../images/rtop_keyboard.gif" alt="" width="24" height="26" border="0" usemap="#Map"/></td>
						</tr>
						<tr>
							<td nowrap="true" bgColor="#475298" height="100%" width="16"><img height="100%" width="16" alt="" src="../../images/spacer.gif" border="0"/></td>
							<td nowrap="true" height="100%" width="8"
								onMouseUp="mousePressed= false;" 
								onMouseOut="mousePressed= false;" 
								onMouseDown="initPos(event);mousePressed= true;" 
								onMouseMove="moveKeyboard(event);"><img src="../../images/border_right.gif" height="100%" width="8"/></td>
						</tr>
						<tr>
							<td vAlign="top" colSpan="2" width="24"
								onMouseUp="mousePressed= false;" 
								onMouseOut="mousePressed= false;" 
								onMouseDown="initPos(event);mousePressed= true;" 
								onMouseMove="moveKeyboard(event);"><img height="26" width="24" alt="" src="../../images/rbot_keyboard.gif" border="0"/></td>
						</tr>
					</table>
				</td>
			  </tr>
			</table>
			<map name="Map">
			  <area shape="rect" coords="6,12,15,24" href="javascript:closeKeyboard()" alt="Fermer le clavier"
			  	onMouseOver="MM_swapImage('keyboardStatus','','../../images/rtop_keyboard_close.gif',1);"
			  	onMouseOut="MM_swapImgRestore()"/>
			  <area shape="rect" coords="0,12,7,24" href="javascript:minimizeKeyboard()" alt="Minimiser le clavier"
			  	onMouseOver="MM_swapImage('keyboardStatus','','../../images/rtop_keyboard_minimize.gif',1);"
			  	onMouseOut="MM_swapImgRestore()"/>
			</map>
		</div>
		<!--Area used to show keyboard-->
		<div id="keyboard_menu_show" onMouseOver="showLayer();" onMouseOut="setTimerMenu();" style="LEFT: 5px; POSITION: absolute; TOP: 0px; z-index : 0; visibility : visible;"> 
		  <img src="../../images/spacer.gif" height="20" width="100%"/> </div>

		<!--Keyboard minimimze-->
		<div>
			<table width="100%" border="0" cellpadding="0" cellspacing="0" id="keyboard_menu_small" style="LEFT: 5px; POSITION: absolute; TOP: 0px; visibility: hidden">
				<tr>
					  <td width="100%" align="center" valign="top" background="../../images/border_bottom.gif" height="10"
								onMouseUp="mousePressed= false;" 
								onMouseOut="mousePressed= false;" 
								onMouseDown="initPos(event);mousePressed= true;" 
								onMouseMove="moveKeyboard(event);"><img 
								align="center" 
								src="../../images/down.gif" 
								name="keyboardSmall" 
								width="77" height="10" 
								border="0" 
								onClick="showBigKeyboard()" 
								onMouseOut="MM_swapImgRestore()" 
								onMouseOver="MM_swapImage('keyboardSmall','','../../images/down_selected.gif',1)"/></td>
				</tr>
			</table>
		</div>
		
		<!-- Do we have the calendar to load ? -->
		<script type="text/javascript" language="javascript">
			if(hasCalendar) document.write(codeCalendar());
			if(hasTableAction) writeSelectMenus();
		</script>
		
	</xsl:template>
	
	<xsl:template name="sizescript">
			<SCRIPT type="text/javascript" language="javascript">
				if (eval(window.innerWidth)) {
					currentW = window.innerWidth; 
					currentH = window.innerHeight;
				}else{
					currentW = document.body.offsetWidth;
					currentH = document.body.offsetHeight;
				}
			</SCRIPT>
	</xsl:template>
	
	<xsl:template name="form">
		<!-- HTML GENERATED BY XSL -->			
		<form name="javelin_form" id="javelin_form" method="post" onSubmit="doAction('KEY_ENTER');">
			<xsl:for-each select="blocks">
				<xsl:apply-templates>
					<xsl:with-param name="offsety" select="$offsety + @page-number * $coefy * 24"/>
				</xsl:apply-templates>
			</xsl:for-each>
			<input type="hidden" name="__javelin_current_field"/>
			<input type="hidden" name="__javelin_action"/>
			<input type="hidden" name="__transaction"/>
			<input type="hidden" name="__sesskey"/>
			<input type="hidden" name="__context" id="__context" value="{/document/@context}"/>
			<input type="hidden" name="__signature" id="__signature" value="{/document/@signature}"/>
		</form>
	</xsl:template>
</xsl:stylesheet>
