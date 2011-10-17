<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- ############################# -->
	<!-- TEMPLATE getClassNameOfCurrent -->
	<!-- ############################# -->
	<xsl:template name="getClassNameOfCurrent">
		<xsl:choose>
			<xsl:when test="starts-with(@name, 'a')">A<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'b')">B<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'c')">C<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'd')">D<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'e')">E<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'f')">F<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'g')">G<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'h')">H<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'i')">I<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'j')">J<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'k')">K<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'l')">L<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'm')">M<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'n')">N<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'o')">O<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'p')">P<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'q')">Q<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'r')">R<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 's')">S<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 't')">T<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'u')">U<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'v')">V<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'w')">W<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'x')">X<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'y')">Y<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:when test="starts-with(@name, 'z')">Z<xsl:value-of select="substring(@name, 2)"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="@name"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ############################ -->
	<!-- TEMPLATE getClassNameOfParam -->
	<!-- ############################ -->
	<xsl:template name="getClassNameOfParam">
		<xsl:param name="myClass"/>
		<xsl:choose>
			<xsl:when test="starts-with($myClass, 'a')">A<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'b')">B<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'c')">C<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'd')">D<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'e')">E<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'f')">F<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'g')">G<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'h')">H<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'i')">I<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'j')">J<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'k')">K<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'l')">L<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'm')">M<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'n')">N<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'o')">O<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'p')">P<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'q')">Q<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'r')">R<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 's')">S<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 't')">T<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'u')">U<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'v')">V<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'w')">W<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'x')">X<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'y')">Y<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:when test="starts-with($myClass, 'z')">Z<xsl:value-of select="substring($myClass, 2)"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$myClass"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ############################ -->
	<!-- TEMPLATE buildClassMethod -->
	<!-- ############################ -->
	<xsl:template name="buildClassMethod">
		<xsl:param name="myMethod"/>
		<xsl:choose>
			<xsl:when test="starts-with($myMethod, 'A')">a<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'B')">b<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'C')">c<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'D')">d<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'E')">e<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'F')">f<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'G')">g<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'H')">h<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'I')">i<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'J')">j<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'K')">k<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'L')">l<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'M')">m<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'N')">n<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'O')">o<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'P')">p<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'Q')">q<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'R')">r<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'S')">s<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'T')">t<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'U')">u<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'V')">v<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'W')">w<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'X')">x<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'Y')">y<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:when test="starts-with($myMethod, 'Z')">z<xsl:value-of select="substring($myMethod, 2)"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$myMethod"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- ############################ -->
	<!-- TEMPLATE getTypeAndParameters -->
	<!-- ############################ -->
<xsl:template name="getTypeAndParameters">
	<xsl:param name="transactionSoapRequest"><xsl:value-of select="@name"/>SoapRequest</xsl:param>
	<xsl:for-each select="/child::*[local-name()='definitions']/child::*[local-name()='message' and @name=$transactionSoapRequest]/child::*">
		<xsl:if test="position() != 1">,</xsl:if>
		String <xsl:value-of select="@name"/>
	</xsl:for-each>
</xsl:template>
	<!-- ########################################### -->
	<!-- TEMPLATE getTypeAndParametersWithoutContext -->
	<!-- ########################################### -->
<xsl:template name="getTypeAndParametersWithoutContext">
	<xsl:param name="transactionSoapRequest"><xsl:value-of select="@name"/>SoapRequest</xsl:param>
	<xsl:for-each select="/child::*[local-name()='definitions']/child::*[local-name()='message' and @name=$transactionSoapRequest]/child::*[position() != 1]">
		<xsl:if test="position() != 1">,</xsl:if>
		String <xsl:value-of select="@name"/>
	</xsl:for-each>
</xsl:template>
	<!-- ###################### -->
	<!-- TEMPLATE getParameters -->
	<!-- ###################### -->
	<xsl:template name="getParameters">
		<xsl:param name="transactionSoapRequest"><xsl:value-of select="@name"/>SoapRequest</xsl:param>
		<xsl:for-each select="/child::*[local-name()='definitions']/child::*[local-name()='message' and @name=$transactionSoapRequest]/child::*">
			<xsl:if test="position() != 1">,</xsl:if><xsl:value-of select="@name"/>
		</xsl:for-each>
	</xsl:template>
	<!-- ############################ -->
	<!-- TEMPLATE getValuesParameters -->
	<!-- ############################ -->
	<xsl:template name="getValuesParameters">
		<xsl:param name="transactionSoapRequest"><xsl:value-of select="@name"/>SoapRequest</xsl:param>
		<xsl:for-each select="/child::*[local-name()='definitions']/child::*[local-name()='message' and @name=$transactionSoapRequest]/child::*">
			<xsl:if test="position() != 1">,</xsl:if>"<xsl:value-of select="@name"/>"
		</xsl:for-each>
	</xsl:template>
	<!-- ########################################## -->
	<!-- TEMPLATE getValuesParametersWithoutContext -->
	<!-- ########################################## -->
	<xsl:template name="getValuesParametersWithoutContext">
		<xsl:param name="transactionSoapRequest"><xsl:value-of select="@name"/>SoapRequest</xsl:param>
		<xsl:for-each select="/child::*[local-name()='definitions']/child::*[local-name()='message' and @name=$transactionSoapRequest]/child::*[position() != 1]">
			<xsl:if test="position() != 1">,</xsl:if>"<xsl:value-of select="@name"/>"
		</xsl:for-each>
	</xsl:template>
	<!-- ######################### -->
	<!-- TEMPLATE getResponseClass -->
	<!-- ######################### -->
	<xsl:template name="getResponseClass">
		<xsl:param name="transactionSoapResponse"><xsl:value-of select="@name"/>SoapResponse</xsl:param>
		<xsl:call-template name="getClassNameOfParam">
			<xsl:with-param name="myClass">
				<xsl:value-of select="substring(/child::*[local-name()='definitions']/child::*[local-name()='message' and @name=$transactionSoapResponse]/child::*/@type, 5)"/>
			</xsl:with-param>
		</xsl:call-template>
	</xsl:template>	
	
</xsl:stylesheet>
