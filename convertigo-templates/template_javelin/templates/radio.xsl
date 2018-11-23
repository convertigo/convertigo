<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE RADIO -->
	<xsl:template match="*[@type='radio']">
		<xsl:param name="offsety" />
		<xsl:variable name="local_offset" select="number(./item[position()=1]/@column) - number(./@column)"/>
		<xsl:for-each select="./item">
			<span style="position: absolute;
							top: {@line*$coefy + $offsety}px;
							left: {(@column - $local_offset)*$coefx + $offsetx}px;"
					id="{../@name}_n{./@action}parent">
					<input type="radio" 
							name="{../@name}" 
							id="{../@name}_n{./@action}" 
							value="{@action}"
							onfocus="currentFieldOnFocus=this.id; onInputClick(this);">
						<xsl:choose>
							<xsl:when test="@selected='true'">
								<xsl:attribute name="checked">checked</xsl:attribute>
							</xsl:when>
							<xsl:otherwise>
								<xsl:if test="../@autoenter='true'">
									<xsl:attribute name="onclick">doAction('KEY_ENTER');</xsl:attribute>
								</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
					</input>
				<span class="color{../@foreground}"><span class="bgcolor{../@background}"><xsl:value-of select="@value"/></span></span>
			</span>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>