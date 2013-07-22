<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:include href="../../tomcat/webapps/convertigo/xsl/variables.xsl"/>
	<xsl:include href="../../tomcat/webapps/convertigo/xsl/headerfooter.xsl"/>
	
	<xsl:output method="xml" encoding="ISO-8859-1" version="1.0" indent="yes"/>
	<xsl:template match="document">
		<xsl:text disable-output-escaping="yes">&lt;!DOCTYPE xsl:stylesheet PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "../../dtd/xhtml1-transitional.dtd"&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt"&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:include href="../../xsl/status.xsl"/&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:include href="../../xsl/variables.xsl"/&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:include href="../../xsl/rules.xsl"/&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:include href="../../xsl/headerfooter.xsl"/&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:output method="html" media-type="text/html" encoding="ISO-8859-1"/&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:template match="document"&gt;</xsl:text>
		<html>
			<xsl:call-template name="header">
				<xsl:with-param name="css">../../css/sna.css</xsl:with-param>
			</xsl:call-template>
			
			<!-- HTML page -->
			<body onResize="resize();" onLoad="MM_preloadImages('../../images/down_selected.gif','../../images/up_selected.gif', '../../images/rtop_keyboard.gif');keyboard_initVar();if(eval(parent.frames['check'])) parent.frames['check'].checkOnLoad();" onUnLoad="keyboard_setCookie();" topmargin="5" leftmargin="5">
				<!-- Scripts -->
				<xsl:text disable-output-escaping="yes">&lt;xsl:call-template name="loadscripts"/&gt;</xsl:text>
				
				<div id="contents" style="visibility: hidden">
					<xsl:text disable-output-escaping="yes">&lt;xsl:copy-of select="document('template.html')/html/body/*"/&gt;</xsl:text>
				</div>
				
				<div id="generated_page">
					<xsl:text disable-output-escaping="yes">&lt;xsl:call-template name="sizescript"/&gt;</xsl:text>
					<form name="javelin_form" method="post" onSubmit="doAction('KEY_ENTER');">
						
						<xsl:text disable-output-escaping="yes">&lt;!-- =================   Comment out this code if you want dynamic rule processing in this page =========</xsl:text>
						<xsl:text disable-output-escaping="yes">&lt;xsl:for-each select="blocks"&gt;</xsl:text>
							<xsl:text disable-output-escaping="yes">&lt;xsl:apply-templates&gt;</xsl:text>
								<xsl:text disable-output-escaping="yes">&lt;xsl:with-param name="offsety" select="$offsety + @page-number * $coefy * 24"/&gt;</xsl:text>
							<xsl:text disable-output-escaping="yes">&lt;/xsl:apply-templates&gt;</xsl:text>
						<xsl:text disable-output-escaping="yes">&lt;/xsl:for-each&gt;</xsl:text>
						<xsl:text disable-output-escaping="yes">   =================   Comment out this code if you want dynamic rule processing in this page ========= --&gt;</xsl:text>
						
						<input type="hidden" name="__javelin_current_field" id="__javelin_current_field"/>
						<input type="hidden" name="__javelin_action" id="__javelin_action"/>
						<input type="hidden" name="__transaction" id="__transaction"/>
						<input type="hidden" name="__sesskey" id="__sesskey"/>
						<xsl:text disable-output-escaping="yes">&lt;input type="hidden" name="__context" id="__context" value="{/document/@context}"/&gt;</xsl:text>
						<xsl:text disable-output-escaping="yes">&lt;input type="hidden" name="__signature" id="__signature" value="{/document/@signature}"/&gt;</xsl:text>
								
						<xsl:text disable-output-escaping="yes">&lt;!-- ================================= Customize this part to create your own screen template  BEGIN=========================================================--&gt;</xsl:text>
						<xsl:for-each select="blocks/*">
							<xsl:call-template name="element"/>
						</xsl:for-each>
						<xsl:text disable-output-escaping="yes">&lt;!-- ================================= Customize this part to create your own screen template  END  =========================================================--&gt;</xsl:text>
					</form>
					<xsl:text disable-output-escaping="yes">&lt;xsl:call-template name="footer"/&gt;</xsl:text>
				</div>
			</body>
		</html>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:template&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:stylesheet&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template name="screenLine">
		<xsl:param name="line"/>
		<xsl:if test="$line &lt; 24">
			<xsl:for-each select="blocks/child::*[@line = $line]">
				<xsl:call-template name="element"/>
			</xsl:for-each>
			<br/>
			<xsl:call-template name="screenLine">
				<xsl:with-param name="line" select="$line + 1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="element">
				<xsl:choose>
					<xsl:when test="@type = 'static'">
						<div class="staticText" style="position:absolute; left:{@column*$coefx+$offsetx}px; top:{@line*$coefy + $offsety}px">
							<xsl:if test="name() = 'block'">
								<xsl:value-of select="."/>
							</xsl:if>
							<xsl:if test="name() != 'block'">
									<xsl:text disable-output-escaping="yes">&lt;!--value--&gt;&lt;xsl:value-of select="/document//</xsl:text>
									<xsl:value-of select="name()"/>
									<xsl:text disable-output-escaping="yes">"/&gt;&lt;!--value--&gt;</xsl:text>
							</xsl:if>
						</div>					
					</xsl:when>
					<xsl:when test="@type = 'field'">
						<div style="position:absolute; left:{@column*$coefx+$offsetx}px; top:{@line*$coefy + $offsety}px">
							<xsl:call-template name="genField"/>
						</div>
					</xsl:when>
					<xsl:when test="@type = 'keyword'">
						<div style="position:absolute; left:{@column*$coefx+$offsetx}px; top:{@line*$coefy + $offsety}px">
							<input type="button" value="{.}" onclick="return doAction('{@action}', document.javelin_form.__javelin_current_field.value);"/>
						</div>
					</xsl:when>
					<xsl:when test="@type = 'panel'">
						<xsl:call-template name="genPanel"/>
					</xsl:when>
					<xsl:when test="@type = 'table'">
						<xsl:call-template name="genTable"/>
					</xsl:when>
					<xsl:when test="@type = 'snamenu'">
							<xsl:call-template name="genMenu"/>
					</xsl:when>
					<xsl:when test="@type = 'date'">
							<xsl:call-template name="genDate"/>
					</xsl:when>
					<xsl:when test="@type = 'choice'">
						<div style="position:absolute; left:{@column*$coefx+$offsetx}px; top:{@line*$coefy + $offsety}px">
							<xsl:call-template name="genChoice"/>
						</div>
					</xsl:when>
					<xsl:otherwise>
						&#160;
					</xsl:otherwise>
				</xsl:choose>
	</xsl:template>
	

	<xsl:template name="testPanels">
		<xsl:for-each select="blocks/*[@type='panel']">
			<div id="layerPanelo" class="panelOmbre" style="z-index:-2;position:absolute; left:{@column*$coefx+$offsetx+5}px; top:{@line*$coefy + $offsety+5}px;width:{@width*$coefx+10}px;height:{@height*$coefy+10}px"><xsl:text disable-output-escaping="yes">&lt;!--DIV--&gt;</xsl:text></div>
			<div id="layerPanel"  class="panel"      style="z-index:-1;position:absolute; left:{@column*$coefx+$offsetx}px;   top:{@line*$coefy + $offsety}px;  width:{@width*$coefx+10}px;height:{@height*$coefy+10}px"><xsl:text disable-output-escaping="yes">&lt;!--DIV--&gt;</xsl:text></div>
   	</xsl:for-each>
	</xsl:template>

	
	<xsl:template name="genPanel">
		<xsl:for-each select="child::*">
			<xsl:call-template name="element" />
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="genField">
		<xsl:param name="fieldSize">
			<xsl:if test="(@column + @size) &lt; 80">
				<xsl:value-of select="@size"/>
			</xsl:if>
			<xsl:if test="(@column + @size) &gt;= 80">
				<xsl:value-of select="80 - @column"/>
			</xsl:if>
		</xsl:param>
		
		
		<xsl:param name="checkAutoEnter">
			<xsl:if test='(@autoenter)'>true</xsl:if>
			<xsl:if test='(not(@autoenter))'>false</xsl:if>
		</xsl:param>

		<xsl:param name="value">
			<xsl:text disable-output-escaping="yes">{/document//*[@name = '</xsl:text><xsl:value-of select="@name"/><xsl:text disable-output-escaping="yes">']}</xsl:text>
		</xsl:param>
		
		<xsl:choose>
			<xsl:when test='(@hidden = "true")'>
				<input class="fixed" name="{@name}" onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name})" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('KEY_ENTER',  {@name})" type="password" size="{$fieldSize}" maxlength="{@size}" value="{$value}"/>
			</xsl:when>
			<xsl:otherwise>
				<input class="fixed" name="{@name}" onkeyup="checkInputChars(event, {@size}, {$checkAutoEnter}, document.javelin_form.{@name})" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('KEY_ENTER', {@name})"  type="text" size="{$fieldSize}" maxlength="{@size}" value="{$value}"/>
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

	<xsl:template name="genMenu">
		<xsl:for-each select="menu/menuitem">
			<div id="menuid{@id}" style="position:absolute; color:red; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
				<a style="line-height: {$coefy - 1}px" class="menuItem" href="javascript:doMenu('KEY_ENTER','{@id}')"><xsl:value-of select="@id"/>. <xsl:value-of select="@literal"/> - <xsl:value-of select="@command"/></a>
			</div>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="genChoice">
		<xsl:param name="fieldSize">
			<xsl:value-of select="@size"/>
		</xsl:param>
		
		<xsl:param name="value">
			<xsl:text disable-output-escaping="yes">{/document//*[@name = '</xsl:text><xsl:value-of select="@name"/><xsl:text disable-output-escaping="yes">']}</xsl:text>
		</xsl:param>

		<xsl:if test="@radio = 'true'">
			<input class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" size="{@size}" maxlength="{@size}" value="{$value}"/>
			<span class="panel" style="position:absolute; left:{(@column+@size+1)*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
				<xsl:for-each select="child::*">
					<xsl:if test="@selected = 'true'">
						<input type="radio" checked="true" name="{../@name}_r" id="{../@name}_r"  onclick="document.javelin_form.{../@name}.value = '{@value}'" value="{@value}"/><xsl:value-of select="@value"/> 
					</xsl:if>
					<xsl:if test="string-length(@selected) = 0">
						<input type="radio"  name="{../@name}_r" id="{../@name}_r"  onclick="document.javelin_form.{../@name}.value = '{@value}'" value="{@value}"/><xsl:value-of select="@value"/> 
					</xsl:if>
				</xsl:for-each>				
			</span>
			<xsl:if test="(@hasFocus ='true')">
				<script type="text/javascript" language="javascript">
					var elt= document.getElementsByTagName("INPUT");
					if(elt['<xsl:value-of select="@name"/>'])
						focusOnField = elt['<xsl:value-of select="@name"/>'];
		          	</script>
			</xsl:if>
		</xsl:if>
		
		<xsl:if test="@radio = 'false'">
			<select class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}">
				<xsl:for-each select="child::*">
					<option value="{@action}">
						<xsl:if test="(@selected = 'true')"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
						<xsl:value-of select="@value"/>
					</option>
				</xsl:for-each>				
			</select>
			
			<xsl:if test="(@hasFocus ='true')">
				<script type="text/javascript" language="javascript">
					var elt= document.getElementsByTagName("SELECT");
					if(elt['<xsl:value-of select="@name"/>'])
						focusOnField = elt['<xsl:value-of select="@name"/>'];
          	</script>
			</xsl:if>
		</xsl:if>
		<xsl:text disable-output-escaping="yes">&lt;!--Choice--&gt;</xsl:text>
	</xsl:template>

	<xsl:template name="genDate">
		<script src="../../scripts/calendar.js">
			<xsl:text disable-output-escaping="yes">&lt;!--script--&gt;</xsl:text>
		</script >
		<input style="color:black; position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px" class="fixed"
		 	name="{@name}" 
		 	onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('KEY_ENTER')" onclick="window.dateField= {@name};show_myCalendar(event, '{@pattern}');"
		 	type="text" size="{@size}" maxlength="{@size}" value="{.}"/>
	</xsl:template>
	
	
	<!-- TEMPLATE TABLE -->
	<xsl:template name="genTable">
		<div id="layerTable{name()}" style="position:absolute; left:{@column*$coefx+$offsetx}px; top:{(@line - @offset)*$coefy + $offsety}px ;  height:{@rows * $coefy}px; width:{$coefx*@width}px">
			<table cellpadding="0" cellspacing="0" width="100%" height="100%" border="0">
				<tr>
					<td>
						<table border="0" cellspacing="0" cellpadding="0" class="data"  width="100%" height="100%">
							<tr>
								<xsl:text disable-output-escaping="yes">&lt;xsl:apply-templates select="blocks//</xsl:text>
								<xsl:value-of select="name()"/>
								<xsl:text disable-output-escaping="yes">/row/Title"/&gt;</xsl:text>
							</tr>
							<xsl:text disable-output-escaping="yes">&lt;xsl:apply-templates select="blocks//</xsl:text>
							<xsl:value-of select="name()"/>
							<xsl:text disable-output-escaping="yes">/row[position() > 1]"/&gt;</xsl:text>
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
		</div>
	</xsl:template>
	
</xsl:stylesheet>
