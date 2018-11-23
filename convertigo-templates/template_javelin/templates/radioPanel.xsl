<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	
	<!-- TEMPLATE RADIO PANEL -->
	<xsl:template match="*[@type='radioPanel']">
		<xsl:param name="offsety" />
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:for-each select="./item[not(@value = '')]">
			<!-- Container SPAN. Used only for positioning and framework behaviour -->
			<span style="position: absolute;
						top: {@line*$coefy + $offsety}px;
						left: {(@column)*$coefx + $offsetx}px;
						z-index: {$elDepth * 10 + 1};"
					id="radio__field_c{./@column}_l{./@line}_n1parent">
				<input type="radio" 
						name="radio__field_c{../@column}_l{../@line}" 
						id="radio__field_c{./@column}_l{./@line}_n1" 
						value="{./@value}"
						onfocus="currentFieldOnFocus=this.id; onInputClick(this);">
					<!-- Disable radio button if radioPanel is shadowed -->
					<xsl:if test="../@shadow = 'true'">
						<xsl:attribute name="disabled">disabled</xsl:attribute>
					</xsl:if>
					<xsl:choose>
						<xsl:when test="@selected='true'">
							<xsl:attribute name="checked">checked</xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:choose>
								<xsl:when test="../@autoEnter = 'true'">
									<xsl:attribute name="onclick">currentFieldOnFocus='__field_c<xsl:value-of select="@column+2"/>_l<xsl:value-of select="@line"/>'; doAction('KEY_NPTUI');</xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="onclick">currentFieldOnFocus='__field_c<xsl:value-of select="@column+2"/>_l<xsl:value-of select="@line"/>'; doAction('');</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</input>
				<!-- Background color style SPAN for label -->
				<span class="bgcolor{./@background}">
					<!-- Foreground color style SPAN -->
					<span class="color{./@foreground}">
						<span class="radioText">
							<xsl:value-of select="@value"/>
						</span>
					</span>
				</span>
			</span>
			<xsl:if test="(@hasFocus ='true')">
				<input type="hidden" id="focus" value= "radio__field_c{./@column}_l{./@line}" />
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>