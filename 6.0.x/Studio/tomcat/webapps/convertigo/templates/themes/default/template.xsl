<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt">
	<xsl:output method="html" indent="no" media-type="text/html" encoding="ISO-8859-1"/>
	
	<xsl:include href="../../xsl/sna.xsl"/>
	
	<xsl:template match="/" priority="1">
		<xsl:call-template name="main-template">
			<xsl:with-param name="css">template.css</xsl:with-param>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="/document" priority="1">
		<div id="contents" style="visibility: hidden">
			<xsl:copy-of select="document('template.html')/html/body/*"/>
		</div>

		<xsl:call-template name="generate-page"/>
	</xsl:template>

</xsl:stylesheet>
