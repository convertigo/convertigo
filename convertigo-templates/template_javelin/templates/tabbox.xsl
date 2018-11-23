<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE TAB BOX -->
	<xsl:template match="*[@type='tabBox']">
		<xsl:param name="offsety" />
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>0</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<div style="position:absolute;
					left:{./@column*$coefx+$offsetx}px;
					top:{./@line*$coefy + $offsety + 1}px;
					width: {./@width*$coefx}px;
					margin: 0px;
					z-index: {$elDepth * 10};">
					<table style="border-collapse: separate; height: {@height*$coefy}px;" cellpadding="0" cellspacing="0" border="0">
						<tbody>
							<tr>
								<xsl:for-each select="tabBox/*[@type='tabBoxItem']">
									<td class="tabSpacer">&#160;</td>
									<xsl:choose>
										<xsl:when test="./@selected = 'true'">
											<td class="selectedTab">&#160;<xsl:value-of select="."/>&#160;</td>
										</xsl:when>
										<xsl:otherwise>
											<td class="unselectedTab" onclick="currentFieldOnFocus='__field_c{./@column}_l{./@line}'; doAction('KEY_NPTUI');">&#160;<xsl:value-of select="."/>&#160;</td>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:for-each>
								<td width="100%" class="tabSpacer">&#160;</td>
							</tr>
							<tr style="height: 100%">
								<td colspan="{count(tabBox/block)*2 + 1}" class="openedTab">&#160;</td>
							</tr>
						</tbody>
					</table>
			</div>
			<xsl:apply-templates select="*[name() != 'tabBox']">
				<xsl:with-param name="offsety" select="$offsety" />
		</xsl:apply-templates>
	</xsl:template>	
</xsl:stylesheet>