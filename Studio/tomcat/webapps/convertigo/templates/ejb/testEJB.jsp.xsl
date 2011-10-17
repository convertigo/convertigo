<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output encoding="ISO-8859-1" media-type="text/jsp" indent="yes"/>
	<xsl:include href="./EJBUtils.xsl"/>
	
	<xsl:template match="/child::*[local-name()='definitions']">
	<xsl:param name="classNameOfCurrent"><xsl:call-template name="getClassNameOfCurrent"/></xsl:param>
	<xsl:param name="project"><xsl:value-of select="@name"/></xsl:param>
<xsl:text disable-output-escaping="yes">&lt;%</xsl:text>@page language="java" 
	import="javax.ejb.CreateException,
		com.twinsoft.convertigo.ejb.*,
		localhost.convertigo.projects.<xsl:value-of select="@name"/>.<xsl:value-of select="@name"/>_ws.*"<xsl:text disable-output-escaping="yes">%&gt;</xsl:text>

<html>
<head>
<title>Convertigo JSP</title>
</head>
<body bgcolor="#FFFFFF">

This page uses the EJB of the project <xsl:call-template name="getClassNameOfCurrent"/> to get some informations.

<xsl:text disable-output-escaping="yes">&lt;%</xsl:text>
<xsl:for-each select="child::*[local-name()='portType']/child::*[local-name()='operation' and position()=1]">
	<xsl:call-template name="getResponseClass"/> b= null;

	try {
		<xsl:value-of select="$classNameOfCurrent"/>EJBLocal my<xsl:value-of select="$classNameOfCurrent"/>EJB= 
			<xsl:value-of select="$classNameOfCurrent"/>EJBUtil.getLocalHome().create(new java.net.URL("http://localhost/convertigo/projects/<xsl:value-of select="$project"/>/<xsl:value-of select="$project"/>.ws"));
		b= my<xsl:value-of select="$classNameOfCurrent"/>EJB.t<xsl:value-of select="@name"/>(<xsl:call-template name="getValuesParameters"/>);
	} catch (Exception e) {
		e.printStackTrace();
	}
<xsl:text disable-output-escaping="yes">%&gt;</xsl:text>

<br/>
The answer of the request for the transaction '<xsl:value-of select="@name"/>' is :<br/>
<xsl:text disable-output-escaping="yes">&lt;%=b.toString()%&gt;</xsl:text><br/>
</xsl:for-each>

</body>
</html>
	</xsl:template>
</xsl:stylesheet>
