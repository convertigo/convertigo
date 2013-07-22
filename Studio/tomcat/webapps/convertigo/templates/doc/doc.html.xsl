<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="html" encoding="UTF-8" indent="no" omit-xml-declaration="no"/>	
	<xsl:variable name="sizeCorrection" select="1000"/>
	
	<xsl:template match="areaTree">
		<html>
			<head>
				<title>
					Convertigo printing project
				</title>
				<link rel="stylesheet" type="text/css" href="ressources/style.css" />
			</head>
			<body>						
				<xsl:apply-templates/>
			</body>			
		</html>		
	</xsl:template>
	
	<xsl:template match="*" >
		<xsl:apply-templates/>
	</xsl:template>
		
	<xsl:template match="regionBefore" >
		<!-- not index pages except the first -->		
		<xsl:if test="count(./ancestor::page//flow//block[lineArea][(.//word)[1]='-' and (.//word)[last()]='-'])=0 or count(.//ancestor::pageViewport[@nr='2'])=1">
			<div  class="pageSeparation">
				<a>
					<xsl:attribute name="name">
						P<xsl:value-of select="./ancestor::pageViewport/@nr"/>
					</xsl:attribute>
					<hr/>
				</a>
			</div>	
		</xsl:if>	
	</xsl:template>			
	<xsl:template match="regionAfter" ></xsl:template>	
		
	<xsl:template match="lineArea[.//text]">
		<xsl:choose>
			<xsl:when test="(.//word)[1]='-' and (.//word)[last()]='-'">
				<hr class="dashes"/> 
			</xsl:when>
			<xsl:otherwise>	
				<div>
					<xsl:apply-templates/>	
				</div>				
			</xsl:otherwise>		
		</xsl:choose>		
	</xsl:template>	
	
	<xsl:template match="page">
		<div class="page">					
			<xsl:apply-templates/>			
		</div>
	</xsl:template>		
	
	  
	<xsl:template match="text">							
		<xsl:attribute name="style">
			<xsl:if test="./ancestor::block[@background]">						
				background-color:<xsl:value-of select="substring-after(./ancestor::block[@background]/@background,'=')"></xsl:value-of>;						
			</xsl:if>
			font-size:
			<xsl:variable name="originalSize" select="./@font-size"/>				
			<xsl:value-of select="$originalSize div $sizeCorrection" />;
			font-family:							
			<xsl:value-of select="./@font-name" />;
			color:							
			<xsl:value-of select="./@color" />;							
		</xsl:attribute>
		<xsl:apply-templates/>				
	</xsl:template>  
	
	
	  
	<xsl:template match="word">				
			<xsl:value-of select="." />					
	</xsl:template>
		
	<xsl:template match="space">	
		<xsl:text> </xsl:text>
	</xsl:template>	
	
	
	<xsl:template match="pageViewport[@nr='1']//flow">
		<xsl:for-each select="block">
			<xsl:if test="count(.//text) &gt; 0 or count(.//image) &gt; 0">
				<div>
					<xsl:apply-templates></xsl:apply-templates>
				</div>
			</xsl:if>
		</xsl:for-each>	
	</xsl:template>
	
	
	
	<!-- index -->
	<xsl:template match="pageViewport[.//block[(.//word)[1]='-' and (.//word)[last()]='-']]//mainReference/span/flow">
	
		<xsl:if test="./ancestor::pageViewport/@nr='2'">
			<xsl:for-each select="block[1]">
				<div id="indexTitle">
					<xsl:apply-templates></xsl:apply-templates>
				</div>				
			</xsl:for-each>
		</xsl:if>
		
<!--			number of pages ignored-->
		<xsl:for-each select=".//block[lineArea/inlineparent]">	
			<xsl:choose>
				<xsl:when test="count(lineArea/inlineparent/@internal-link) &gt; 0">
					<a>
						<xsl:attribute name="href">
							#<xsl:value-of select="substring-after(substring-before(lineArea/inlineparent/@internal-link,','),'(')"></xsl:value-of>
						</xsl:attribute>
						<div>
							<span>
								<xsl:apply-templates></xsl:apply-templates>
							</span>
						</div>						
					</a>
				</xsl:when>
				<xsl:otherwise>
					<div>
						<span>
							<xsl:apply-templates></xsl:apply-templates>
						</span>
					</div>					
				</xsl:otherwise>
			</xsl:choose>	
				
		</xsl:for-each>	
		
	</xsl:template>
	
	
	<!-- data -->
	<xsl:template match="pageViewport[(count(.//block[(.//word)[1]='-' and (.//word)[last()]='-'])=0) and @nr!=1]//mainReference/span/flow">		
		<xsl:for-each select="block">
			<xsl:choose>
<!--				 titles and comments -->
				<xsl:when test="count(./lineArea) &gt; 0 or count(./*)=0">	
					<xsl:if test="count(./*) &gt; 0">						
							<xsl:choose>
								<xsl:when test="count(./preceding-sibling::block) &gt; 0">	
									<div class="section">							
										<xsl:apply-templates></xsl:apply-templates>	
									</div>							
								</xsl:when>
								<xsl:otherwise>
									<h1>
										<xsl:apply-templates></xsl:apply-templates>
									</h1>
								</xsl:otherwise>
							</xsl:choose>						
					</xsl:if>					
				</xsl:when>
<!--				 links -->
				<xsl:when test="count(.//lineArea[inlineparent]) &gt; 0">
					<a>
						<xsl:attribute name="href">
							#<xsl:value-of select="substring-after(substring-before(.//lineArea/inlineparent/@internal-link,','),'(')"></xsl:value-of>						
						</xsl:attribute>						
						<div>
							<xsl:apply-templates></xsl:apply-templates>
						</div>					
					</a>					
				</xsl:when>				
				<xsl:otherwise>					
					<table>
						<tr>
							<xsl:attribute name="style">
								background-color:<xsl:value-of select="substring-after(.//block[@background]/@background,'=')"></xsl:value-of>;									
							</xsl:attribute>							
							<xsl:for-each select="(.//lineArea)[position() &lt; 3]">
								<th>									
									<xsl:apply-templates></xsl:apply-templates>
								</th>									
							</xsl:for-each>														
						</tr>						
						<xsl:for-each select="((.//lineArea)[2]/ancestor::block[@is-reference-area]/following-sibling::block)[(position() mod 2)=1]">
								<tr class="tabLine">
									<xsl:attribute name="style">
										background-color:<xsl:value-of select="substring-after(@background,'=')"></xsl:value-of>;									
									</xsl:attribute>
									<td>
										<xsl:apply-templates></xsl:apply-templates>
									</td>																	
									<td>
										<xsl:for-each select="(./following-sibling::block)[1]//block[lineArea]">
											<xsl:apply-templates></xsl:apply-templates>
										</xsl:for-each>	
									</td>
								</tr>									
						</xsl:for-each>
					</table>
				</xsl:otherwise>					
			</xsl:choose>				
		</xsl:for-each>
		<a href="#P2" class="retunToIndex"><div>index</div></a>		
	</xsl:template>
		
	
	<xsl:template match="image">
		<img>
			<xsl:attribute name="src">
				ressources/<xsl:value-of select="./@url" />			
			</xsl:attribute>		
		</img>
	</xsl:template>	

		
</xsl:stylesheet>

	
	