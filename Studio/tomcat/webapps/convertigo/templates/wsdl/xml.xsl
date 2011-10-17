<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
<xsl:output version="1.0" method="xml" indent="yes" encoding="ISO-8859-1" omit-xml-declaration="no" />
<xsl:strip-space elements="xsd:all xsd:annotation xsd:any xsd:anyAttribute xsd:appinfo xsd:attribute xsd:attributeGroup xsd:choice xsd:complexContent xsd:complexType xsd:documentation xsd:element xsd:extension xsd:field xsd:group xsd:key xsd:keyref xsd:list xsd:notation xsd:redefine xsd:restriction xsd:schema xsd:selector xsd:sequence xsd:simpleContent xsd:simpleType xsd:union xsd:unique xsd:pattern"/>

<xsl:template match="/">
	<xsl:apply-templates select="//xsd:schema"/>
</xsl:template>

<xsl:template match="xsd:schema">
	<xsl:variable name="type" select="./*[@name='targetRequestable']/@type"/>
	<xsl:element name="document">
		<xsl:attribute name="context"/>
		<xsl:attribute name="contextId"/>
		<xsl:attribute name="fromcache"/>
		<xsl:attribute name="project"/>
		<xsl:attribute name="connector"/>
		<xsl:attribute name="sequence"/>
		<xsl:attribute name="transaction"/>
		<xsl:attribute name="generated"/>
		<xsl:attribute name="version"/>
		<xsl:apply-templates select="//*[@name=substring-after($type,':')]"/>
	</xsl:element>
</xsl:template>

<xsl:template match="xsd:attribute">
	<xsl:attribute name="{@name}"/>
</xsl:template>

<xsl:template match="xsd:element">
	<xsl:if test="@name">
		<xsl:if test="(contains(@name,'Response')) or (@name='response')">
			<xsl:call-template name="branch"/>
		</xsl:if>
		<xsl:if test="(not(contains(@name,'Response')) and (not(@name='response')))">
			<xsl:element name="{@name}">
				<xsl:if test="@mixed">
					<xsl:text>text</xsl:text>
				</xsl:if>
				<xsl:call-template name="branch"/>
			</xsl:element>
		</xsl:if>
	</xsl:if>
	<xsl:if test="not(@name)">
		<xsl:call-template name="branch"/>
	</xsl:if>
</xsl:template>

<xsl:template match="xsd:group">
	<xsl:call-template name="branch"/>
</xsl:template>

<xsl:template match="xsd:complexType">
  <xsl:apply-templates select="./*[name()='xsd:attribute']" />
  <xsl:apply-templates select="./*[not(name()='xsd:attribute')]" />
  <xsl:if test="@mixed">
    <xsl:text />
  </xsl:if>
</xsl:template>

<xsl:template match="xsd:extension">
	<xsl:if test="parent::*[name()='xsd:complexContent']">
		<xsl:call-template name="branch"/>
	</xsl:if>
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="xsd:restriction">
	<xsl:call-template name="branch"/>
</xsl:template>

<xsl:template name="branch">
	<xsl:if test="@type">
		<xsl:call-template name="definedType">
			<xsl:with-param name="type" select="@type"/>
		</xsl:call-template>
	</xsl:if>
	<xsl:if test="@ref">
		<xsl:call-template name="definedType">
			<xsl:with-param name="type" select="@ref"/>
		</xsl:call-template>
	</xsl:if>
	<xsl:if test="@base">
		<xsl:call-template name="definedType">
			<xsl:with-param name="type" select="@base"/>
		</xsl:call-template>
	</xsl:if>
	<xsl:if test="not(@type) and not(@ref) and not (@base)">
		<xsl:apply-templates/>
	</xsl:if>
</xsl:template>

<xsl:template name="definedType">
	<xsl:param name="type"/>
	<xsl:if test="contains($type,':')">
		<xsl:if test="starts-with($type,'xsd:')">
			<xsl:text />
		</xsl:if>
		<xsl:if test="not(starts-with($type,'xsd:'))">
			<xsl:apply-templates select="//*[@name=substring-after($type,':') and not(@type)]" />
		</xsl:if>
	</xsl:if>
	<xsl:if test="not(contains($type,':'))">
		<xsl:apply-templates select="//*[@name=$type and not(@type)]" />
	</xsl:if>
	
</xsl:template>

<xsl:template match="xsd:annotation|xsd:documentation">
</xsl:template>

<xsl:template match="xsd:simpleType">
	<xsl:text />
</xsl:template>

</xsl:stylesheet>