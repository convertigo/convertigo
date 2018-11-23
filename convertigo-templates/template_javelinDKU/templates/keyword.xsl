<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE KEYWORD -->
	<xsl:template match="*[@type='keyword']">
		<xsl:param name="offsety" />
		<xsl:param name="enable"/>
		
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:variable name="displayMe">
			<xsl:choose>
				<!-- Case this is not a 5250 extended button -->
				<xsl:when test="$enable = ''">
					<xsl:choose>
						<!-- Case project is parametered explicitly to display disabled buttons when outside of panels -->
						<xsl:when test="$DisplayDisabledButtons = 'true'">true</xsl:when>
						<!-- Normal case -->
						<xsl:otherwise>
							<xsl:choose>
								<!-- Case there is a panel in the screen -->
								<xsl:when test="/document/blocks/*/@type ='panel'">
									<xsl:choose>
										<!-- Case button is in the panel -->
										<xsl:when test="ancestor::*/@type = 'panel'">true</xsl:when>
										<!-- Case button is not in the panel -->
										<xsl:otherwise>false</xsl:otherwise>
									</xsl:choose>
								</xsl:when>
								<!-- Case there is not any panel is the screen -->
								<xsl:otherwise>true</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<!-- Case this is a 5250 extended button -->
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="$enable = 'true'">true</xsl:when>
						<xsl:otherwise>false</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- Container SPAN. Used only for positioning and framework behaviour -->
		<span style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px; z-index: {$elDepth * 10 + 1};">
			<input type="button"
					name="{@name}"
					id="{@name}">
				<xsl:call-template name="mashupTooltipOnOff"/>
				<xsl:choose>
					<!-- Case the button was disabled -->
					<xsl:when test="$displayMe = 'false'">
						<xsl:attribute name="disabled">disabled</xsl:attribute>
						<xsl:attribute name="class">disabledKeywordButton</xsl:attribute>
					</xsl:when>
					<!-- Case the button is selectable -->
					<xsl:otherwise>
						<xsl:attribute name="class">keywordButton</xsl:attribute>
						<xsl:choose>
							<!-- Is this an help/F4/popup button ? -->
							<xsl:when test="(starts-with(., $helpKeywordString)) and (preceding-sibling::*[1][@type = 'field']/@line = ./@line)">
								<xsl:attribute name="onclick">
										document.javelin_form.<xsl:value-of	select="preceding-sibling::*[1]/@name" />.focus();
										currentFieldOnFocus='<xsl:value-of select="preceding-sibling::*[1]/@name" />';
										doAction('<xsl:value-of select="@action" />');
										<xsl:call-template name="mashupEvent"/>
									</xsl:attribute>
							</xsl:when>
							<!-- Standard button -->
							<xsl:otherwise>
								<xsl:choose>
									<!-- Does this button trigger a transaction ? -->
									<xsl:when test="@dotransaction = 'true'">
										<xsl:attribute name="onclick">document.getElementById('__transaction').value='<xsl:value-of select="@action" />'; doAction('');<xsl:call-template name="mashupEvent"/></xsl:attribute>
									</xsl:when>
									<!-- Is this a 5250 Extended button ? -->
									<xsl:when test="not($enable = '')">
										<xsl:attribute name="onclick">currentFieldOnFocus='__field_c<xsl:value-of select="@column"/>_l<xsl:value-of select="@line"/>'; doAction('KEY_NPTUI');<xsl:call-template name="mashupEvent"/></xsl:attribute>
									</xsl:when>
									<!-- Standard button onclick behaviour -->
									<xsl:otherwise>
										<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action" />');<xsl:call-template name="mashupEvent"/></xsl:attribute>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:choose>
					<!-- Display the action key (PF1=, PF14=, PA2=...) on the button -->
					<xsl:when test="$DisplayActionKey='true' and $enable = ''">
						<xsl:attribute name="value"><xsl:value-of select="substring-after(@action, 'KEY_')"/>=<xsl:value-of select="."/></xsl:attribute>
					</xsl:when>
					<!-- Or not -->
					<xsl:otherwise>
						<xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
			</input>
		</span>
		
	</xsl:template>
</xsl:stylesheet>
