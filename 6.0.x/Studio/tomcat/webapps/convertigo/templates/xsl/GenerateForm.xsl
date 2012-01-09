<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" encoding="UTF-8" version="1.0" indent="yes" />
	<xsl:template match="variables">
		<xsl:text disable-output-escaping="yes">&lt;xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt"&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:output method="xml" encoding="UTF-8" media-type="text/html" /&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;xsl:template match="document"&gt;</xsl:text>
			<div class="body">				<table>					<xsl:for-each select="variable" >						<tr>							<td><xsl:value-of select="VariableDefinitionDescription" /></td>
							<td>
								<input type="text" class="field" size="15" >
									<xsl:attribute name="name"><xsl:value-of select="variableDefinitionName"/></xsl:attribute>
									<xsl:attribute name="value"><xsl:value-of select="variableDefinitionDefaultValue"/></xsl:attribute>
								</input>
							</td>
						</tr>
					</xsl:for-each>
					<tr>						<td align="right">							<input type="button" name="delete"								value="Annuler" id="btn"
								onclick="parent.dfRefreshView('YamseForm');" />
						</td>
						<td align="left">
							<input type="button" name="search"
								value="Rechercher" id="btn"
								onclick="window.parent.tcFormResponseSubmitted( window.name, document, 'FormSubmitted_{transactionName}' );" />
						</td>
					</tr>
				</table>
			</div>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:template&gt;</xsl:text>
		<xsl:text disable-output-escaping="yes">&lt;/xsl:stylesheet&gt;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
