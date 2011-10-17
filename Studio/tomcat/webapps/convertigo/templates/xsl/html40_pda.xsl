<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" encoding="ISO-8859-1" version="1.0" indent="yes"/>
	<xsl:template match="document">
		<xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "../../dtd/xhtml1-transitional.dtd"&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:output encoding="ISO-8859-1" method="html" media-type="text/html"  /&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:template match="document" &gt;</xsl:text>
		<html>
			<style>
				td {font-family: Verdana, Arial, Helvetica, sans-serif;}
			</style>
			<head>
				<title>Convertigo</title>
			</head>
			<body>
				<script src="../../scripts/scriptlib.js">
					<xsl:text disable-output-escaping="yes">&lt;!-- do not remove this comment --&gt;</xsl:text>
				</script>
				<form method="post" name="javelin_form" onsubmit="return doAction('KEY_ENTER')">
					<input type="hidden" name="__javelin_action" id="__javelin_action" value=""/>
					<input type="hidden" name="__sesskey" id="__sesskey" value=""/>
					<input type="hidden" name="__context" id="__context" value=""/>					
					<input type="hidden" name="__javelin_current_field" id="__javelin_current_field" value=""/>
					<input type="hidden" name="__transaction" id="__transaction"/>
					<table border="0" style="width:320px">
						<xsl:call-template name="screenLine">
							<xsl:with-param name="line" select="0"/>
						</xsl:call-template>
						<tr>
							<td>
								<input type="submit" value="Ok" name="Ok"/>
							</td>
						</tr>
					</table>
				</form>
			</body>
		</html>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:template&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:stylesheet&gt;</xsl:text>
	</xsl:template>
	<xsl:template name="screenLine">
		<xsl:param name="line"/>
		<xsl:if test="$line &lt; 24">
			<xsl:for-each select="blocks/child::*[@line = $line]">
				<tr>
					<xsl:call-template name="element"/>
				</tr>
			</xsl:for-each>
			<xsl:call-template name="screenLine">
				<xsl:with-param name="line" select="$line + 1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<xsl:template name="element">
		<td>
			<xsl:choose>
				<xsl:when test="@type = 'static'">
					<xsl:if test="name() != 'block'">
						<xsl:text disable-output-escaping="yes">&lt;xsl:value-of select="/document//</xsl:text>
						<xsl:value-of select="name()"/>
						<xsl:text disable-output-escaping="yes">[@line = '</xsl:text>
						<xsl:value-of select="@line"/>
						<xsl:text disable-output-escaping="yes">' and @column = '</xsl:text>
						<xsl:value-of select="@column"/>
						<xsl:text disable-output-escaping="yes">']" /&gt;</xsl:text>
					</xsl:if>
				</xsl:when>
				<xsl:when test="@type = 'field'">
					<xsl:call-template name="genField"/>
				</xsl:when>
				<xsl:when test="@type = 'keyword'">
					<input type="button" value="{.}" onclick="return doAction('{@action}', document.javelin_form.__javelin_current_field.value);"/>
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
				<xsl:otherwise>
					&#160;
				</xsl:otherwise>
			</xsl:choose>
		</td>
	</xsl:template>
	<xsl:template name="panelLine">
		<xsl:param name="line"/>
		<xsl:if test="$line &lt; 24">
			<xsl:for-each select="*[@line = $line]">
				<tr>
					<xsl:call-template name="element"/>
				</tr>
			</xsl:for-each>
			<xsl:call-template name="panelLine">
				<xsl:with-param name="line" select="$line+1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<xsl:template name="genPanel">
		<table border="2">
			<xsl:variable name="line" select="@line + 1"/>
			<xsl:call-template name="panelLine">
				<xsl:with-param name="line" select="$line"/>
			</xsl:call-template>
		</table>
	</xsl:template>
	<xsl:template name="genTable">
		<table border="4">
			<tr>
				<xsl:text disable-output-escaping="yes">&lt;xsl:for-each select="/document//table[@line = '</xsl:text>
				<xsl:value-of select="@line"/>
				<xsl:text disable-output-escaping="yes">' and @column = '</xsl:text>
				<xsl:value-of select="@column"/>
				<xsl:text disable-output-escaping="yes">']/row/Title"&gt;</xsl:text>
				<td>
					<xsl:text disable-output-escaping="yes">&lt;xsl:value-of select="." /&gt;</xsl:text>
				</td>
				<xsl:text disable-output-escaping="yes">&lt;/xsl:for-each&gt;</xsl:text>
			</tr>
			<xsl:text disable-output-escaping="yes">&lt;xsl:for-each select="/document//table[@line = '</xsl:text>
			<xsl:value-of select="@line"/>
			<xsl:text disable-output-escaping="yes">' and @column = '</xsl:text>
			<xsl:value-of select="@column"/>
			<xsl:text disable-output-escaping="yes">']/row[position() > 1]"&gt;</xsl:text>
			<tr>
				<xsl:text disable-output-escaping="yes">&lt;xsl:for-each select="*"&gt;</xsl:text>
				<td>
					<xsl:text disable-output-escaping="yes">
						&lt;xsl:if test="@type = 'static'"&gt;
							&lt;xsl:value-of select="." /&gt;
						&lt;/xsl:if&gt;
						
						&lt;xsl:if test="@type = 'field'"&gt;
							&lt;xsl:choose&gt;
								&lt;xsl:when test='(@foreground = "white")'&gt;
									&lt;input class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('KEY_ENTER', {@name})" type="text" size="{@size}" maxlength="{@size}" value="{.}"/&gt;
								&lt;/xsl:when&gt;
								&lt;xsl:when test='(@hidden = "true")'&gt;
									&lt;input class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('KEY_ENTER',  {@name})" type="password" size="{@size}" maxlength="{@size}" value="{.}"/&gt;
								&lt;/xsl:when&gt;
								&lt;xsl:otherwise&gt;
									&lt;input class="fixed" name="{@name}" onfocus="currentFieldOnFocus=this.id" id="{@name}" ondblclick="doAction('KEY_ENTER',  {@name})" size="{@size}" maxlength="{@size}" value="{.}"/&gt;
								&lt;/xsl:otherwise&gt;
							&lt;/xsl:choose&gt;
							&lt;xsl:if test="(@hasFocus ='true')"&gt;
								&lt;script type="text/javascript" language="javascript"&gt;
									var elt= document.getElementsByTagName("INPUT");
									if(elt['&lt;xsl:value-of select="@name"/&gt;'])
										focusOnField = elt['&lt;xsl:value-of select="@name"/&gt;'];
						          	&lt;/script&gt;
							&lt;/xsl:if&gt;					
						&lt;/xsl:if&gt;
					</xsl:text>
				</td>
				<xsl:text disable-output-escaping="yes">&lt;/xsl:for-each&gt;</xsl:text>
			</tr>
			<xsl:text disable-output-escaping="yes">&lt;/xsl:for-each&gt;</xsl:text>
		</table>
	</xsl:template>
	<xsl:template name="genField">
		<table border="0">
			<tr>
				<td>
					<xsl:value-of select="name()"/>
				</td>
				<td>
					<xsl:choose>
						<xsl:when test="@hidden ='true'">
							<xsl:text disable-output-escaping="yes">&lt;input type="password" name="</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text disable-output-escaping="yes">&lt;input type="text" name="</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:value-of select="@name"/>
					<xsl:text disable-output-escaping="yes">" id="</xsl:text>
					<xsl:value-of select="@name"/>
					<xsl:text disable-output-escaping="yes">" value="{/document//*[@name ='</xsl:text>
					<xsl:value-of select="@name"/>
					<xsl:text disable-output-escaping="yes">']}" onfocus="document.javelin_form.__javelin_current_field.value = '</xsl:text>
					<xsl:value-of select="@name"/>
					<xsl:text disable-output-escaping="yes">'" size="</xsl:text>
					<xsl:value-of select="@size"/>
					<xsl:text disable-output-escaping="yes">" maxlength="</xsl:text>
					<xsl:value-of select="@size"/>
					<xsl:text disable-output-escaping="yes">" /&gt;</xsl:text>
					<xsl:choose>
						<xsl:when test="@hasFocus ='true'">
							<xsl:text disable-output-escaping="yes">&lt;script&gt; document.javelin_form.</xsl:text>
							<xsl:value-of select="@name"/>
							<xsl:text disable-output-escaping="yes">.focus(); &lt;/script&gt;</xsl:text>
						</xsl:when>
					</xsl:choose>
				</td>
			</tr>
		</table>
	</xsl:template>
	<xsl:template name="genMenu">
		<table border="4">
			<tr>
				<th>Menu</th>
			</tr>
			<xsl:text disable-output-escaping="yes">&lt;xsl:for-each select="/document//menu/menuitem"&gt;</xsl:text>
			<tr>
				<td>
					<xsl:text disable-output-escaping="yes">&lt;input type="button" onclick="alert(' !!! to be customized !!!');" value="{@id}"/&gt;</xsl:text>
				</td>
				<td>
					<xsl:text disable-output-escaping="yes">&lt;xsl:value-of select="@command" /&gt;</xsl:text>
				</td>
			</tr>
			<xsl:text disable-output-escaping="yes">&lt;/xsl:for-each&gt;</xsl:text>
		</table>
	</xsl:template>
</xsl:stylesheet>
