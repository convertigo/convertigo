<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output encoding="ISO-8859-1" media-type="text/xml" indent="yes"/>
	<xsl:include href="./EJBUtils.xsl"/>
	
	<xsl:template match="/child::*[local-name()='definitions']">	
		<xsl:param name="project"><xsl:call-template name="getClassNameOfCurrent"/></xsl:param>
<xsl:text disable-output-escaping="yes">&lt;!DOCTYPE orion-ejb-jar PUBLIC "-//Evermind//DTD Enterprise JavaBeans 1.1 runtime//EN" "http://www.orionserver.com/dtds/orion-ejb-jar.dtd"&gt;</xsl:text>

<orion-ejb-jar deployment-version="1.5.0">

   <enterprise-beans>

      <!-- Session Beans -->
      <session-deployment
      	name="{$project}EJB" location="{$project}Bean"
	>
      </session-deployment>

      <!-- Entity Beans -->

      <!-- Message Driven Beans -->

   </enterprise-beans>

</orion-ejb-jar>
	</xsl:template>
</xsl:stylesheet>