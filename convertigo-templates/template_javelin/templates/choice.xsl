<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE CHOICE TEST-->
	<xsl:template match="*[@type='choice']" priority="1">
		<xsl:param name="fieldSize">
			<xsl:value-of select="@size" />
		</xsl:param>
		<xsl:param name="offsety" />
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:if test="@radio = 'true'">
			<input class="fixed" name="{@name}"
				onfocus="currentFieldOnFocus=this.id" id="{@name}"
				style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px;z-index: {$elDepth * 10 + 1};"
				size="{@size}" maxlength="{@size}" value="{.}" />
			<span class="panel"
				style="position:absolute; left:{(@column+@size+1)*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px; z-index: {$elDepth * 10 + 1};">
				<xsl:for-each select="child::*">
					<xsl:if test="@selected = 'true'">
						<input type="radio" checked="true"
							name="{../@name}_r" id="{../@name}_r"
							onclick="document.javelin_form.{../@name}.value = '{@value}'"
							value="{@value}" />
						<xsl:value-of select="@value" />
						 
					</xsl:if>
					<xsl:if test="string-length(@selected) = 0">
						<input type="radio" name="{../@name}_r"
							id="{../@name}_r"
							onclick="document.javelin_form.{../@name}.value = '{@value}'"
							value="{@value}" />
						<xsl:value-of select="@value" />
						 
					</xsl:if>
				</xsl:for-each>
			</span>
			<xsl:if test="(@hasFocus ='true')">
				<script type="text/javascript" language="javascript">
					var elt= document.getElementsByTagName("INPUT");
					if(elt['<xsl:value-of select="@name" />']) focusOnField = elt['<xsl:value-of select="@name" />'];
				</script>
			</xsl:if>
		</xsl:if>
		<xsl:if test="@radio = 'false'">
			<select class="fixed" name="{@name}"
				onfocus="currentFieldOnFocus=this.id" id="{@name}"
				style="position:absolute; left:{@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px; z-index: {$elDepth * 10 + 1};">
				<xsl:for-each select="child::*">
					<option value="{@action}">
						<xsl:if test="(@selected = 'true')">
							<xsl:attribute name="selected">selected</xsl:attribute>
						</xsl:if>
						<xsl:value-of select="@value" />
					</option>
				</xsl:for-each>
			</select>
			<xsl:if test="(@hasFocus ='true')">
				<script type="text/javascript" language="javascript">
					var elt= document.getElementsByTagName("SELECT");
					if(elt['<xsl:value-of select="@name" />']) focusOnField = elt['<xsl:value-of select="@name" />'];
				</script>
			</xsl:if>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
