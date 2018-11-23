<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:template name="absXpath">
		<xsl:if test=".."><xsl:for-each select=".."><xsl:call-template name="absXpath"/></xsl:for-each>/*[<xsl:value-of select="count(preceding-sibling::*)+1"/>]</xsl:if>
	</xsl:template>
	
	<xsl:template name="mashupEventTxt">
		<xsl:param name="mashevent"/>
		doMashupEvent(event,'<xsl:value-of select="$mashevent" />', this);
	</xsl:template>
	
	<xsl:template name="mashupVars">
		<xsl:variable name="mashevent" select="./@mashup_event"/>
		<xsl:if test="//interactions/interaction[./@dfevent=$mashevent]">
			<xsl:attribute name="dfevent"><xsl:value-of select="./@mashup_event"/></xsl:attribute>
			<xsl:for-each select="//interactions/interaction[./@dfevent=$mashevent]/variable">
				<xsl:attribute name="{./@name}"><xsl:value-of select="."/></xsl:attribute>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="mashupEvent">
		<xsl:variable name="mashevent" select="./@mashup_event"/>
		<xsl:if test="//interactions/interaction[./@dfevent=$mashevent]">
			<xsl:call-template name="mashupEventTxt">
				<xsl:with-param name="mashevent" select="$mashevent"/>
			</xsl:call-template>
		</xsl:if>		
	</xsl:template>
	
	<xsl:template name="mashupEventOnClick">
		<xsl:variable name="mashevent" select="./@mashup_event"/>
		<xsl:if test="//interactions/interaction[./@dfevent=$mashevent]">
			<xsl:attribute name="onclick">
				<xsl:call-template name="mashupEventTxt">
					<xsl:with-param name="mashevent" select="$mashevent"/>
				</xsl:call-template>
			</xsl:attribute>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="mashupTooltipOnTxt">showDiv(event);</xsl:template>

	<xsl:template name="mashupTooltipOn">
		<xsl:param name="ref" select="." />
		<xsl:if test="$ref/@mashup_event">
			<xsl:call-template name="mashupTooltipOnTxt"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="mashupTooltipOnOver">
		<xsl:param name="ref" select="." />
		<xsl:if test="$ref/@mashup_event">
			<xsl:attribute name="onmouseover">
				<xsl:call-template name="mashupTooltipOnTxt"/>
			</xsl:attribute>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="mashupTooltipOffTxt">hideDiv(event);</xsl:template>

	<xsl:template name="mashupTooltipOff">
		<xsl:param name="ref" select="." />
		<xsl:if test="$ref/@mashup_event">
			<xsl:call-template name="mashupTooltipOffTxt"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="mashupTooltipOnOut">
		<xsl:param name="ref" select="." />
		<xsl:if test="$ref/@mashup_event">
			<xsl:attribute name="onmouseout">
				<xsl:call-template name="mashupTooltipOffTxt"/>
			</xsl:attribute>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="mashupTooltipOnOff">
		<xsl:param name="ref" select="." />
		<xsl:if test="$ref/@mashup_event">
			<xsl:attribute name="onmouseover">
				<xsl:call-template name="mashupTooltipOnTxt"/>
			</xsl:attribute>
			<xsl:attribute name="onmouseout">
				<xsl:call-template name="mashupTooltipOffTxt"/>
			</xsl:attribute>
		</xsl:if>
	</xsl:template>
	
	
	<xsl:template name="mashupAddTooltip">
		<div id="mashupTooltip" class="mashupTooltip">
                Click here to do mashup event
        </div>
    </xsl:template>
</xsl:stylesheet>
