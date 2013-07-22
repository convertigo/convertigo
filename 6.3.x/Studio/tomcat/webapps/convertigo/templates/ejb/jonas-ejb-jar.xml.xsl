<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output encoding="ISO-8859-1" media-type="text/xml" indent="yes"/>
	<xsl:include href="./EJBUtils.xsl"/>
	
	<xsl:template match="/child::*[local-name()='definitions']">	
	
<xsl:text disable-output-escaping="yes">&lt;!DOCTYPE jonas-ejb-jar PUBLIC "-//ObjectWeb//DTD JOnAS 2.5//EN" "http://www.objectweb.org/jonas/dtds/jonas-ejb-jar_2_5.dtd"&gt;</xsl:text>

<jonas-ejb-jar>

  <!-- Session Beans -->

  <!-- Entity Beans -->

  <!-- Message Driven Beans -->

</jonas-ejb-jar>
	</xsl:template>
</xsl:stylesheet>