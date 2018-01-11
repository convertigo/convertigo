<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" encoding="UTF-8" indent="no" omit-xml-declaration="no"/>
	
	<xsl:template match="*">			
		<xsl:copy>				
			<xsl:copy-of select="@qname"/>	
			<xsl:copy-of select="@classname"/>
			<xsl:if test="./property[@name='accessibility']/java.lang.String">
				<xsl:attribute name="accessibility">		
					<xsl:value-of select="./property[@name='accessibility']/java.lang.String/@value"/>	
				</xsl:attribute>
			</xsl:if>
			<!--get the name from its property-->
			<xsl:for-each select="./property[@name='name']">
				<xsl:attribute name="name">
					<xsl:value-of select="./java.lang.String/@value"/>
				</xsl:attribute>
			</xsl:for-each>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="admin">
		<xsl:apply-templates/>
	</xsl:template>
		
	<xsl:template match="property"/><!--dont save the node wsdltype and its childs-->
	<xsl:template match="wsdltype"/><!--dont save the node wsdltype and its childs-->
	<xsl:template match="handlers"/><!-- dont save the node handlers and its childs-->
	
	
</xsl:stylesheet>
