<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	
	<!-- TEMPLATE MENU -->
	<xsl:template match="*[@type='menu']">
		<xsl:param name="offsety" />
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- There is a panel in the screen. We only display buttons that are in that panel -->
		<!--
		<xsl:if test="((/document/blocks/*/@type ='panel') and (../@type = 'panel')) or (not(/document/blocks/*/@type ='panel'))">
		-->
			<!-- Container SPAN. Used only for positioning and framework behaviour -->
			<span style="position:absolute; 
						left:{./@column*$coefx+$offsetx}px; 
						top:{./@line*$coefy + $offsety}px;
						z-index: {$elDepth * 10 + 1};">
				<div class="extmenu">
					<table cellspacing="0">
						<tbody>
							<tr>
								<xsl:apply-templates>
									<xsl:with-param name="offsety" select="$offsety + ancestor::blocks[1]/@page-number * $coefy * 24" />
									<xsl:with-param name="extended">true</xsl:with-param>
									<xsl:with-param name="elDepth"><xsl:value-of select="$elDepth"/></xsl:with-param>
								</xsl:apply-templates>
							</tr>
						</tbody>
					</table>
				</div>
			</span>
		<!--
		</xsl:if>
		-->
	</xsl:template>
	
	<!-- TEMPLATE MENUITEM -->
	<xsl:template match="*[@type='menuItem']">
		<xsl:param name="offsety" />
		<xsl:param name="elDepth" />
		<td>
			<table cellspacing="0" cellpadding="0" border="0" style="width: auto;">
				<tbody>
					<tr>
						<td><img src="../images/pixel.gif" width="3" height="1"/></td>
						<td style="vertical-align: middle; text-align: left;">
							<em class="extmenu">
								<input type="button" 
										class="extmenu" 
										value="    {@value}">
									<xsl:attribute name="onclick">currentFieldOnFocus='__field_c<xsl:value-of select="./@column"/>_l<xsl:value-of select="./@line"/>'; doAction('KEY_ENTER');</xsl:attribute>
								</input>
							</em>
						</td>
						<td><img src="../images/pixel.gif" width="3" height="1"/></td>
					</tr>
				</tbody>
			</table>
		</td>
	</xsl:template>
</xsl:stylesheet>