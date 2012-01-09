<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output encoding="ISO-8859-1" media-type="text/xml" indent="yes"/>
	<xsl:include href="./EJBUtils.xsl"/>
	
	<xsl:template match="/child::*[local-name()='definitions']">	
	
<xsl:text disable-output-escaping="yes">&lt;!DOCTYPE jboss PUBLIC "-//JBoss//DTD JBOSS 3.0//EN" "http://www.jboss.org/j2ee/dtd/jboss_3_0.dtd"&gt;</xsl:text>

<jboss>

   <unauthenticated-principal>nobody</unauthenticated-principal>

   <enterprise-beans>

     <!--
       To add beans that you have deployment descriptor info for, add
       a file to your XDoclet merge directory called jboss-beans.xml that contains
       the <session></session>, <entity></entity> and <message-driven></message-driven>
       markup for those beans.
     -->

      <session>
         <ejb-name><xsl:call-template name="getClassNameOfCurrent"/>EJB</ejb-name>
         <jndi-name><xsl:call-template name="getClassNameOfCurrent"/>Bean</jndi-name>
         <local-jndi-name><xsl:call-template name="getClassNameOfCurrent"/>EJBLocal</local-jndi-name>
      </session>

   </enterprise-beans>

   <resource-managers>
   </resource-managers>

</jboss>
	</xsl:template>
</xsl:stylesheet>