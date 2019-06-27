<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt">
	<xsl:output method="html" indent="no" media-type="text/html" encoding="UTF-8" />

	<!-- Convertigo System Includes -->
	<xsl:include href="templates/status.xsl" />
	<xsl:include href="variables.xsl" />

	<xsl:include href="templates/mashupUtils.xsl"/>
	
	<!-- HTML block rendering templates -->
	<xsl:include href="templates/static.xsl" />
	<xsl:include href="templates/field.xsl" />
	<xsl:include href="templates/date.xsl" />
	<xsl:include href="templates/keyword.xsl" />
	<xsl:include href="templates/snamenu.xsl" />
	<xsl:include href="templates/panel.xsl" />
	<xsl:include href="templates/table.xsl" />
	<xsl:include href="templates/choice.xsl" />
	<xsl:include href="templates/separator.xsl" />
	<xsl:include href="templates/slider.xsl" />
	<xsl:include href="templates/tabbox.xsl" />
	<xsl:include href="templates/image.xsl" />
	<xsl:include href="templates/commands.xsl" />
	<xsl:include href="templates/radioPanel.xsl" />
	<xsl:include href="templates/checkboxesPanel.xsl" />
	<xsl:include href="templates/menu.xsl" />
	
	<xsl:param name="doSort">false</xsl:param>
	<xsl:param name="sortColon">-1</xsl:param>	
	<xsl:param name="sortDataType">text</xsl:param>		
	<xsl:param name="sortOrder">ascending</xsl:param>

	<!-- TEMPLATE DOCUMENT -->
	<xsl:template match="document">
		<!--  these two inputs are generated to hold the screen Dimension !-->
		<input type="hidden" id="screenWidth" value= "{@screenWidth}" />
		<input type="hidden" id="screenHeight" value= "{@screenHeight}" />

		<input type="hidden" id="resize" value= "{$resize}" />
		<input type="hidden" id="coefx" value= "{$coefx}" />
		<input type="hidden" id="coefy" value= "{$coefy}" />
		<input type="hidden" id="offsetx" value= "{$offsetx}" />
		<input type="hidden" id="offsetr" value= "{$offsetr}" />
		<input type="hidden" id="offsety" value= "{$offsety}" />
		<input type="hidden" id="scrollwidth" value= "{$scrollwidth}" />

		<xsl:call-template name="generate-page" />
	</xsl:template>
	
	<xsl:template name="generate-page">
		<div id="generated_page">
			<form id="javelin_form" name="javelin_form" method="post" onSubmit="doAction('KEY_ENTER');">
				<xsl:for-each select="blocks[position()=1]">
					<!-- hidden element containing calendar fields -->
					<input type="hidden" id="calendar_fields" name="calendar_fields">
						<xsl:attribute name="value">
							<xsl:for-each select="block[@type='date']"><xsl:value-of select="@name"/>|<xsl:value-of select="@pattern"/>;</xsl:for-each>
						</xsl:attribute>
					</input>
					<!--Hidden inputs for cursor position-->
					<input type="hidden" id="cursorColumn" value="{/document/@cursorColumn}"/>
					<input type="hidden" id="cursorLine" value="{/document/@cursorLine}"/>
					<xsl:apply-templates>
						<xsl:with-param name="offsety" select="$offsety + @page-number * $coefy * 24" />
					</xsl:apply-templates>
				</xsl:for-each>
				<input type="hidden" name="__javelin_current_field" id="__javelin_current_field"/>
				<input id="__javelin_modified_fields" name="__javelin_modified_fields" type="hidden"/>
				<input type="hidden" name="__javelin_action" id="__javelin_action"/>
				<input type="hidden" name="__transaction" id="__transaction"/>
				<input type="hidden" name="__sesskey" />
				<input type="hidden" name="__context" id="__context" value="{/document/@context}" />
				<input type="hidden" name="__signature" id="__signature" value="{/document/@signature}" />
				<input type="hidden" name="table_values" id="table_values" value=""/>
			</form>
		</div>
		<xsl:call-template name="mashupAddTooltip"/>
	</xsl:template>

</xsl:stylesheet>
