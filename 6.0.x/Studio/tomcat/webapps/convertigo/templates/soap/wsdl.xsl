<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" encoding="ISO-8859-1" version="1.0" indent="yes"/>
	<xsl:template match="document">
		<xsl:text disable-output-escaping="yes">&lt;xsd:complexType name="</xsl:text>
		<xsl:value-of select="@transaction"/>
		<xsl:text disable-output-escaping="yes">Response"&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsd:all&gt;</xsl:text>
		<xsl:apply-templates/>
		<xsl:text disable-output-escaping="yes">&lt;/xsd:all&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;/xsd:complexType&gt;</xsl:text>
		<xsl:for-each select="blocks/*[@type = 'table']">
			<xsl:text disable-output-escaping="yes">&lt;xsd:complexType name="ArrayOf</xsl:text>
			<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
			<xsl:text disable-output-escaping="yes">_Row"&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;xsd:complexContent mixed="false"&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;xsd:restriction base="soapenc:Array"&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;xsd:attribute wsdl:arrayType="tns:</xsl:text>
			<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
			<xsl:text disable-output-escaping="yes">_Row[]" ref="soapenc:arrayType"/&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;/xsd:restriction&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;/xsd:complexContent&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;/xsd:complexType&gt;</xsl:text>

			<xsl:text disable-output-escaping="yes">&lt;xsd:complexType name="ArrayOf</xsl:text>
			<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
			<xsl:text disable-output-escaping="yes">_Row_Literal"&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;xsd:sequence&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;xsd:element minOccurs="0" maxOccurs="unbounded" name="row" type="tns:</xsl:text>
			<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
			<xsl:text disable-output-escaping="yes">_Row[]"/&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;/xsd:sequence&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;/xsd:complexType&gt;</xsl:text>

			<xsl:text disable-output-escaping="yes">&lt;xsd:complexType name="</xsl:text>
			<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
			<xsl:text disable-output-escaping="yes">_Row"&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;xsd:sequence&gt;</xsl:text>
			<xsl:for-each select="row[position() = 2]/*">
				<xsl:text disable-output-escaping="yes">&lt;xsd:element minOccurs="1" maxOccurs="1" name="</xsl:text>
				<xsl:value-of select="name()"/>
				<xsl:text disable-output-escaping="yes">" type="xsd:string"/&gt;</xsl:text>
			</xsl:for-each>
			<xsl:text disable-output-escaping="yes">&lt;/xsd:sequence&gt;</xsl:text>
			<xsl:text disable-output-escaping="yes">&lt;/xsd:complexType&gt;</xsl:text>
		</xsl:for-each>
	</xsl:template>
	<xsl:template match="blocks">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="*">
		<xsl:text disable-output-escaping="yes">&lt;xsd:element name="</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:text disable-output-escaping="yes">" type="xsd:string"/&gt;</xsl:text>
	</xsl:template>
	<xsl:template match="*[@type = 'table']">
		<!--xsl:text disable-output-escaping="yes">&lt;xsd:element name="</xsl:text>
		<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
		<xsl:text disable-output-escaping="yes">" type="tns:</xsl:text>
		<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
		<xsl:text disable-output-escaping="yes">_Rows"/&gt;</xsl:text-->
		<xsl:text disable-output-escaping="yes">&lt;xsd:element name="ArrayOf</xsl:text>
		<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
		<xsl:text disable-output-escaping="yes">_Row" type="tns:ArrayOf</xsl:text>
		<xsl:value-of select="/document/@transaction"/>_<xsl:value-of select="name()"/>
		<xsl:text disable-output-escaping="yes">_Row"/&gt;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
