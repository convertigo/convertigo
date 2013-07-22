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

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt">
	<xsl:output encoding="UTF-8" method="html" media-type="text/html"/>
	
	<xsl:include href="status.xsl"/>
	
	<!--- WARNING : if you modify these constants, display may be wrong -->
	<xsl:variable name="coefx">8</xsl:variable>
	<xsl:variable name="coefy">18</xsl:variable>
	<xsl:param name="offsetx">10</xsl:param>
	<xsl:param name="offsety">45</xsl:param>
	<xsl:param name="offsetw">10</xsl:param>
	<xsl:param name="offseth">10</xsl:param>
	
	<!-- TEMPLATE DOCUMENT -->
	<xsl:template match="document">
		<html>
			<head>
				<title><xsl:value-of select="title"/></title>
				<meta http-equiv="Cache-Control" content="no-cache"/>
				<meta http-equiv="Pragma" content="no-cache"/>
				<meta http-equiv="Expires" content="-1" />
				<!-- StyleSheet -->
				<link id="sna" href="../../css/sna.css" rel="styleSheet" type="text/css"><!--stylesheet--></link>
				<link href="../../css/button.css" rel="styleSheet" type="text/css"><!--stylesheet--></link>
				<link href="../../css/contextMenu.css" rel="styleSheet" type="text/css"><!--stylesheet--></link>
			</head>
			<!-- HTML page -->
			<body
				bgColor="#ffffff" 
				onResize="resize();" 
				onLoad="MM_preloadImages('../../images/down_selected.gif','../../images/up_selected.gif', '../../images/rtop_keyboard.gif');
					keyboard_initVar();if(eval(parent.frames['check'])) parent.frames['check'].checkOnLoad();"
				onUnLoad="keyboard_setCookie();"
				topmargin="5" leftmargin="5">
								
				<!-- Scripts -->
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
				<script src="../../scripts/cookies.js"><!--script--></script>
				<script src="../../scripts/scriptlib.js"><!--script--></script >
				<script src="../../scripts/button.js"><!--script--></script>
				<script src="../../scripts/keyboard.js"><!--script--></script>
				<script src="../../scripts/keymapdku.js"><!--script--></script>
				<script src="../../scripts/contextMenu.js"><!--script--></script>
				<script src="../../scripts/position.js"><!--script--></script>
				
				<!--HTML page-->
				<div id="contents" style="visibility: hidden">
					<table width="100%" heigth="100" align="center" border="0" cellpadding="0" cellspacing="0">
						<!--Global Array -->
						<tr>
							<td width="100%" height="50">
								<table cellSpacing="0" cellPadding="0" border="0" height="50">
									<tr> 
										<td vAlign="top" colSpan="2" rowSpan="2" width="10"><img height="10" width="10" alt="" src="../../images/ltop.gif" border="0"/></td>
										<td noWrap="true"  bgColor="#cccc99" colSpan="1" height="1" width="100%"><SPACER width="1" height="1" type="block"/></td>
										<td vAlign="top" colSpan="2" rowSpan="2" width="10"><img height="10" width="10" alt="" src="../../images/rtop.gif" border="0"/></td>
									</tr>
									<tr> 
										<td nowrap="true" bgColor="#f7f7e5" colSpan="1" height="9" align="center" valign="top"></td>
									</tr>
									<tr> 
										<td nowrap="true" bgColor="#cccc99" width="1"><img width="1" alt="" src="../../images/rtop.gif" border="0"/></td>
										<td nowrap="true" bgColor="#f7f7e5" width="9"><img id="cadreH" alt="" src="../../images/spacer.gif" width="9" border="0"/></td>
										<td nowrap="true" bgColor="#f7f7e5" width="100%" height="100%"></td>
										<td nowrap="true" bgColor="#f7f7e5" width="9"><img width="9" alt="" src="../../images/spacer.gif" border="0"/></td>
										<td nowrap="true" bgColor="#cccc99" width="1"><img width="1" alt="" src="../../images/rtop.gif" border="0"/></td>
									</tr>
									<tr> 
										<td vAlign="top" bgColor="#f7f7e5" colSpan="2" rowSpan="2" width="10"><img height="10" width="10" alt="" src="../../images/lbot.gif" border="0"/></td>
										<td nowrap="true" bgColor="#f7f7e5" colSpan="1" height="9"><img id="cadreW" src="../../images/spacer.gif" height="9" width="100%"/></td>
										<td vAlign="top" bgColor="#f7f7e5" colSpan="2" rowSpan="2" width="10"><img height="10" width="10" alt="" src="../../images/rbot.gif" border="0"/></td>
									</tr>
									<tr> 
										<td nowrap="true" bgColor="#cccc99" colSpan="1" height="1" width="100%"><SPACER width="1" height="1" type="block"/></td>
									</tr>
								</table>			
							</td>
						</tr>
						<tr>
							<td height="20">
								<table width="100%" height="20">
									<tr>
										<td colspan="1" width="50%">
											<span class="footerText">&#169;Twinsoft 2003</span>
										</td>
										<td colspan="1" width="50%" align="right">
											<span class="footerText">Powered by Convertigo&#174;</span>
										</td>
									</tr>
								</table>
							</td>		
						</tr>
					</table>
					<!--End of global array -->
					
					<SCRIPT type="text/javascript" language="javascript">
						if (eval(window.innerWidth)) {
							currentW = window.innerWidth;
							currentH = window.innerHeight;
						}else{
							currentW = document.body.offsetWidth;
							currentH = document.body.offsetHeight;
						}
					</SCRIPT>
				
					<div id="generated_page">
						<!-- HTML GENERATED BY XSL -->			
						<form name="javelin_form" method="post" onSubmit="doAction('XMIT');">
							<xsl:for-each select="blocks">
								<xsl:apply-templates>
									<xsl:with-param name="offsety" select="$offsety + @page-number * $coefy * 24"/>
								</xsl:apply-templates>
							</xsl:for-each>
							<input type="hidden" name="__javelin_current_field"/>
							<input type="hidden" name="__javelin_action"/>
							<input type="hidden" name="__sesskey"/>
							<input type="hidden" name="__context"/>
						</form>
						
						<table style="width:{$coefx*80}; position:absolute; left:10px; top:{(blocks[position() = last()]/@page-number + 1)* 24*$coefy + $offsety}px" border="0" cellpadding="0" cellspacing="0">
							<tr>
								<td align="center">
									<input name="__validate" id="__validate" onclick="doAction('XMIT')" type="button" value="Valider"/>
								</td>
								<xsl:for-each select="/document/blocks/block[@type='keyword']">
									<td align="center">
										<input name="{@name}" id="{@name}" onclick="doAction('{@action}')" type="button" value="{.}"/>
									</td>
								</xsl:for-each>
							</tr>
						</table>
					</div>
				</div>


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
											document.write(createKeyboardBull());
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
			</body>
		</html>
	</xsl:template>
	
	<!-- TEMPLATE STATIC -->
	<xsl:template match="*[@type='static']">
		<xsl:param name="offsety"/>
		<xsl:choose>
			<xsl:when test='(@foreground = "yellow")'>
				<span style="position:absolute; color:red; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:value-of select="."/>
				</span>
			</xsl:when>
			<xsl:when test='(@foreground = "white")'>
				<span style="position:absolute; color:black; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:value-of select="."/>
				</span>
			</xsl:when>
			<xsl:when test='(@foreground = "cyan")'>
				<span style="position:absolute; color:#475298; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:value-of select="."/>
				</span>
			</xsl:when>
			<xsl:when test='(@foreground = "magenta")'>
				<span style="position:absolute; color:red; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:value-of select="."/>
				</span>
			</xsl:when>
			<xsl:otherwise>
				<span style="position:absolute; color:{@foreground}; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:value-of select="."/>
				</span>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- TEMPLATE FIELD -->
	<xsl:template match="*[@type='field']">
		<xsl:param name="fieldSize">
			<xsl:value-of select="@size"/>
		</xsl:param>
		<xsl:param name="offsety"/>
		<xsl:choose>
			<xsl:when test='(@foreground = "white")'>
				<input class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('XMIT', {@name})" style="color:black; position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" type="text" size="{$fieldSize}" maxlength="{@size}" value="{.}"/>
			</xsl:when>
			<xsl:when test='(@hidden = "true")'>
				<input class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('XMIT',  {@name})" style="color:black; position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" type="password" size="{$fieldSize}" maxlength="{@size}" value="{.}"/>
			</xsl:when>
			<xsl:otherwise>
				<input class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('XMIT',  {@name})" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" size="{$fieldSize}" maxlength="{@size}" value="{.}"/>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="(@hasFocus ='true')">
			<script type="text/javascript" language="javascript">
				var elt= document.getElementsByTagName("INPUT");
				if(elt['<xsl:value-of select="@name"/>'])
					focusOnField = elt['<xsl:value-of select="@name"/>'];
	          	</script>
		</xsl:if>
	</xsl:template>
	
	<!-- TEMPLATE DATE -->
	<xsl:template match="*[@type='date']">
		<xsl:param name="offsety"/>
		<script src="../../scripts/calendar.js"><!--script--></script >
		<input style="color:black; position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" class="fixed"
		 	name="{@name}" 
		 	onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('XMIT')" onclick="window.dateField= {@name};show_myCalendar(event, '{@pattern}');"
		 	type="text" size="{@size}" maxlength="{@size}" value="{.}"/>
	</xsl:template>
	
	<!-- TEMPLATE KEYWORD -->
	<xsl:template match="*[@type='keyword']"/>
	
	<!-- TEMPLATE MENU ITEM -->
	<xsl:template match="menuitem">
		<xsl:param name="offsety"/>
		<tr style="color:{@foreground}">
			<td>
				<img width="14" height="10" src="../../images/puce.gif"/>
			</td>
			<td>
				<a style="line-height: {$coefy - 1}px" class="menuItem" href="javascript:doMenu('XMIT','{@id}')"><xsl:value-of select="@id"/>.&#160;<xsl:value-of select="@literal"/> - <xsl:value-of select="@command"/></a>
			</td>
			<xsl:if test="@command !='' ">
				<td style="color:black">
				<!--
					<input style="width:100%;font-size:{$coefx  - 2}pt" onclick="doMenu('XMIT', '{@command}')" type="button" value="{@command}"/>
					<xsl:value-of select="@command"/>
				-->
				</td>
			</xsl:if>
		</tr>
	</xsl:template>
	
	<!-- TEMPLATE PANEL -->
	<xsl:template match="*[@type='panel']">
		<xsl:param name="offsety"/>
		<span class="panelOmbre" style="width:{@width*$coefx+$offsetw}px; height:{@height*$coefy+$offseth}px; solid; position:absolute; left:{./@column*$coefx+$offsetx + 4}px; top:{./@line*$coefy + $offsety+4}px"/>
		<span class="panel" style="width:{@width*$coefx+$offsetw}px; height:{@height*$coefy+$offseth}px; position:absolute; left:{./@column*$coefx + $offsetx}px; top:{./@line*$coefy + $offsety}px"/>
		<xsl:apply-templates>
			<xsl:with-param name="offsety" select="$offsety"/>
		</xsl:apply-templates>
		<xsl:for-each select=".//*[@type='keyword']">
			<input style="position:absolute; left:{@column*$coefx+$offsetx - 10}px; top:{@line*$coefy + $offsety}px; font-size:{$coefx  - 1}pt" name="{@name}" id="{@name}" onclick="doAction('{@action}')" type="button" value="{.}"/>
		</xsl:for-each>
	</xsl:template>
	
	<!-- TEMPLATE TABLE -->
	<xsl:template match="*[@type='table']">
		<xsl:param name="offsety"/>
		<table cellpadding="0" cellspacing="0" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{(./@line - 2)*$coefy + $offsety}px ;  height:{@rows * $coefy}px; width:{$coefx*@width}px" border="0">
			<tr>
				<td>
					<table border="0" cellspacing="0" cellpadding="0" class="data"  width="100%" height="100%">
						<tr>
							<xsl:apply-templates select="row/Title"/>
						</tr>
						<xsl:apply-templates select="row[position() > 1]"/>
					</table>
				</td>
				<td>
					<table height="100%" width="16" border="0">
						<tr> 
							<td height="16" width="16" valign="top"><img width="16" height="16" src="../../images/hide.gif" onclick="return doAction('KEY_ROLLUP')"/></td>
						</tr>
						<tr>
							<td height="98%" valign="middle"><img width="16" height="98%" src="../../images/barre.gif"/></td>
						</tr>
						<tr> 
							<td height="16" width="16" valign="bottom"><img width="16" height="16" src="../../images/show.gif" onclick="return doAction('KEY_ROLLDOWN')"/></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
		<xsl:apply-templates select="actionsTable"/>
	</xsl:template>
	
	<!-- TEMPLATE ACTIONSTABLE -->
	<xsl:template match="actionsTable">
		<xsl:comment>ajout de la table des actions</xsl:comment>
		<script src="../../scripts/selectMenu.js"><!--script--></script >
		<script type="text/javascript" language="javascript">
			BeginSelect('table_c<xsl:value-of select="../@column"/>_l<xsl:value-of select="../@line"/>');
			<xsl:for-each select="child::*">
				AddSelect('<xsl:value-of select="@char"/>', '<xsl:value-of select="@key"/>', '<xsl:value-of select="@label"/>');
			</xsl:for-each>
			EndSelect();
          	</script>
	</xsl:template>
	
	<!-- TEMPLATE ROW -->
	<xsl:template match="row" priority="1">
		<tr style="line-height: {$coefy - 1}px">
			<xsl:if test="(@line mod 2) = 0">
				<xsl:attribute name="class">dataroweven</xsl:attribute>
			</xsl:if>
			<xsl:if test="(@line mod 2) != 0">
				<xsl:attribute name="class">datarowodd</xsl:attribute>
			</xsl:if>

			<xsl:for-each select="child::*">
				<td>
					<xsl:choose>
						<xsl:when test='(@type = "field")'>
							<xsl:attribute name="align">center</xsl:attribute>
								<input style="height:{$coefy - 1}px" class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" size="{@size}" maxlength="{@size}" value="{.}">
								<xsl:if test="(@columnSelection = 'true')">
									<xsl:attribute name="onclick">show_mySelectMenu(event, this, 'table_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')</xsl:attribute>
									<xsl:attribute name="onblur">blur_mySelectMenu('table_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')</xsl:attribute>
								</xsl:if>	
								</input>
								<xsl:if test="(@hasFocus = 'true')">
									<script type="text/javascript" language="javascript">
										var elt= document.getElementsByTagName("INPUT");
										if(elt['<xsl:value-of select="@name"/>'])
											focusOnField = elt['<xsl:value-of select="@name"/>'];
							          	</script>
								</xsl:if>		
						</xsl:when>
						<xsl:when test='(@type = "choice")'>
							<xsl:choose>
								<xsl:when test='(@foreground = "white")'>
									<select class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" style="color:black;">
										<xsl:for-each select="child::*">
											<option value="{@action}">
												<xsl:if test="(@selected = 'true')"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
												<xsl:value-of select="@value"/>
											</option>
										</xsl:for-each>				
									</select>
								</xsl:when>
								<xsl:otherwise>
									<select class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}">
										<xsl:for-each select="child::*">
											<option value="{@action}">
												<xsl:if test="(@selected = 'true')"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
												<xsl:value-of select="@value"/>
											</option>
										</xsl:for-each>				
									</select>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:if test="(@hasFocus ='true')">
								<script type="text/javascript" language="javascript">
									var elt= document.getElementsByTagName("SELECT");
									if(elt['<xsl:value-of select="@name"/>'])
										focusOnField = elt['<xsl:value-of select="@name"/>'];
						          	</script>
							</xsl:if>
						</xsl:when>
						<xsl:otherwise>
							<xsl:if test="string-length(.) = 0">
								&#160;
							</xsl:if>
							<xsl:if test="string-length(.) != 0">
								<xsl:value-of select="."/>
							</xsl:if>
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>	
	
	<!-- TEMPLATE TITLE -->
	<xsl:template match="Title" priority="1">
		<td class="datatitle">
			<xsl:if test="string-length(.) = 0">
				&#160;
			</xsl:if>
			<xsl:if test="string-length(.) != 0">
				<xsl:value-of select="."/>
			</xsl:if>
		</td>
	</xsl:template>
	
	<!-- TEMPLATE CHOICE -->
	<xsl:template match="*[@type='choice']" priority="1">
		<xsl:param name="fieldSize">
			<xsl:value-of select="@size"/>
		</xsl:param>
		<xsl:param name="offsety"/>
		<xsl:choose>
			<xsl:when test='(@foreground = "white")'>
				<select class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" style="color:black; position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:for-each select="child::*">
						<option value="{@action}">
							<xsl:if test="(@selected = 'true')"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
							<xsl:value-of select="@value"/>
						</option>
					</xsl:for-each>				
				</select>
			</xsl:when>
			<xsl:otherwise>
				<select class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:for-each select="child::*">
						<option value="{@action}">
							<xsl:if test="(@selected = 'true')"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
							<xsl:value-of select="@value"/>
						</option>
					</xsl:for-each>				
				</select>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="(@hasFocus ='true')">
			<script type="text/javascript" language="javascript">
				var elt= document.getElementsByTagName("SELECT");
				if(elt['<xsl:value-of select="@name"/>'])
					focusOnField = elt['<xsl:value-of select="@name"/>'];
	          	</script>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
