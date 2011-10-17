<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" encoding="ISO-8859-1" version="1.0" indent="yes"/>
	<xsl:template match="document">
		<xsl:text disable-output-escaping="yes">&lt;xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:output encoding="ISO-8859-1" media-type="text/vnd.wap.wml" doctype-public="-//WAPFORUM//DTD WML 1.1//EN" doctype-system="http://www.wapforum.org/DTD/wml_1.1.xml" /&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:template match="document" &gt;</xsl:text>
		<wml>
			<card title="Convertigo">
				<p>
					<xsl:call-template name="screenLine">
						<xsl:with-param name="line" select="0"/>
					</xsl:call-template>
					<br/>
					<anchor>
						Go!					
						<go href="" method="post">
							<postfield name="__transaction" value="Transaction par défaut"/>
							<xsl:text disable-output-escaping="yes">
								&lt;xsl:for-each select="/document//*[@type = 'field']"&gt;
									&lt;postfield&gt;
										&lt;xsl:attribute name="name"&gt;
											&lt;xsl:value-of select="@name"/&gt;
										&lt;/xsl:attribute&gt;
										&lt;xsl:attribute name="value"&gt;
											$(&lt;xsl:value-of select="@name"/&gt;)
										&lt;/xsl:attribute&gt;
									&lt;/postfield&gt;
								&lt;/xsl:for-each&gt;
							</xsl:text>
						</go>
					</anchor>
				</p>
			</card>
			<card id="customized" title="Menu Item">
				<p>
					This element must be customized by the programer
				</p>
			</card>
		</wml>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:template&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:stylesheet&gt;</xsl:text>
	</xsl:template>
	<xsl:template name="screenLine">
		<xsl:for-each select="blocks/child::*">
			<xsl:call-template name="element"/>
			<br/>
		</xsl:for-each>
	</xsl:template>
	<xsl:template name="element">
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
				<anchor>
					<xsl:value-of select="."/>
					<go href="" method="post">
						<postfield name="__transaction" value="Transaction par défaut"/>
												
						<postfield name="__javelin_action">
							<xsl:attribute name="value">
								<xsl:value-of select="@action"/>	
							</xsl:attribute>
						</postfield>
					
					<xsl:text disable-output-escaping="yes">
						&lt;xsl:for-each select="/document//*[@type = 'field']"&gt;
							&lt;postfield&gt;
								&lt;xsl:attribute name="name"&gt;	&lt;xsl:value-of select="@name"/&gt;&lt;/xsl:attribute&gt;
								&lt;xsl:attribute name="value"&gt;$(&lt;xsl:value-of select="@name"/&gt;)&lt;/xsl:attribute&gt;
							&lt;/postfield&gt;
						&lt;/xsl:for-each&gt;
					</xsl:text>
					</go>
				</anchor>		
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
	</xsl:template>
	
	<xsl:template name="panelLine">
		<xsl:param name="line"/>
		<xsl:if test="$line &lt; 24">
			<xsl:for-each select="*[@line = $line]">
				<xsl:call-template name="element"/>
			</xsl:for-each>
			<xsl:call-template name="panelLine">
				<xsl:with-param name="line" select="$line+1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<xsl:template name="genPanel">
		<xsl:variable name="line" select="@line + 1"/>
		<xsl:call-template name="panelLine">
			<xsl:with-param name="line" select="$line"/>
		</xsl:call-template>
	</xsl:template>
	<xsl:template name="genTable">
		<b>Table</b><br/>
		*----------------*<br/>


		<xsl:text disable-output-escaping="yes">&lt;xsl:for-each select="/document//table[@line = '</xsl:text>
		<xsl:value-of select="@line"/>
		<xsl:text disable-output-escaping="yes">' and @column = '</xsl:text>
		<xsl:value-of select="@column"/>
		<xsl:text disable-output-escaping="yes">']/row[position() > 1]"&gt;</xsl:text>

			<xsl:text disable-output-escaping="yes">&lt;xsl:for-each select="*"&gt;</xsl:text>
				
				<xsl:text disable-output-escaping="yes">
					&lt;xsl:choose&gt;
						&lt;xsl:when test='(position() = 1)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 1]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 2)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 2]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 3)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 3]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 4)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 4]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 5)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 5]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 6)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 6]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 7)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 7]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 8)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 8]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 9)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 9]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:when test='(position() = 10)'&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 10]"/&gt;
						&lt;/xsl:when&gt;
						&lt;xsl:otherwise&gt;
							&lt;xsl:value-of select="../../row/Title[position() = 10]"/&gt;
						&lt;/xsl:otherwise&gt;
					&lt;/xsl:choose&gt;
					&#160;:&#160;
					&lt;xsl:if test="@type = 'static'"&gt;
						&lt;xsl:value-of select="." /&gt;
					&lt;/xsl:if&gt;
					
					&lt;xsl:if test="@type = 'field'"&gt;
						&lt;xsl:choose&gt;
							&lt;xsl:when test='(@foreground = "white")'&gt;
								&lt;input class="fixed" name="{@name}" id="{@name}" type="text" size="{@size}" maxlength="{@size}" value="{.}"/&gt;
							&lt;/xsl:when&gt;
							&lt;xsl:when test='(@hidden = "true")'&gt;
								&lt;input class="fixed" name="{@name}" id="{@name}" type="password" size="{@size}" maxlength="{@size}" value="{.}"/&gt;
							&lt;/xsl:when&gt;
							&lt;xsl:otherwise&gt;
								&lt;input class="fixed" name="{@name}" id="{@name}" size="{@size}" maxlength="{@size}" value="{.}"/&gt;
							&lt;/xsl:otherwise&gt;
						&lt;/xsl:choose&gt;				
					&lt;/xsl:if&gt;
				</xsl:text>
			<br/>
			<xsl:text disable-output-escaping="yes">&lt;/xsl:for-each&gt;</xsl:text>
		*----------------*<br/>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:for-each&gt;</xsl:text>
		
	</xsl:template>
	
	<xsl:template name="genField">
		<xsl:value-of select="name()"/> : 
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
		<xsl:text disable-output-escaping="yes">']}" size="</xsl:text>
		<xsl:value-of select="@size"/>
		<xsl:text disable-output-escaping="yes">" maxlength="</xsl:text>
		<xsl:value-of select="@size"/>
		<xsl:text disable-output-escaping="yes">"/&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template name="genMenu">
		Menu<br/>
		<xsl:text disable-output-escaping="yes">&lt;xsl:for-each select="/document//menu/menuitem"&gt;</xsl:text>
		<anchor>
			<xsl:text disable-output-escaping="yes">&lt;xsl:value-of select="@id"/&gt;</xsl:text>
			<go href="#customized"/>
		</anchor>
		<xsl:text disable-output-escaping="yes">&lt;xsl:value-of select="@command" /&gt;</xsl:text>
		<br/>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:for-each&gt;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
