<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE COMMANDS -->
	<xsl:template match="commands[@type='container']" priority="1">
		<xsl:choose>
			<xsl:when test="$GroupActionButtons = 'true'">
				<div class="commandsButtons">
					<table cellpadding="1" cellspacing="1">
					<xsl:for-each select="*[@type='keyword']">
						<tr>
							<td>
								<table class="buttontab" cellpadding="0" cellspacing="0" border="0" style="width: 90px;" title="{.}">
									<xsl:attribute name="onclick">doAction('<xsl:value-of select="@action" />');<xsl:call-template name="mashupEvent"/></xsl:attribute>
									<tr>
										<td class="buttonl"><img src="images/pixel.gif" width="3" height="1" border="0"/></td>
										<td class="buttonc">
											<input type="button" class="insidebutton">
												<xsl:choose>
													<!-- Display the action key (PF1=, PF14=, PA2=...) on the button -->
													<xsl:when test="$DisplayActionKey='true'">
														<xsl:attribute name="value"><xsl:value-of select="substring-after(@action, 'KEY_')"/>=<xsl:value-of select="."/></xsl:attribute>
													</xsl:when>
													<!-- Or not -->
													<xsl:otherwise>
														<xsl:choose>
															<xsl:when test="string-length(.) &gt; 10 ">
																<xsl:attribute name="value"><xsl:value-of select="substring(., 0, 10)"/>...</xsl:attribute>
															</xsl:when>
															<xsl:otherwise>
																<xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
															</xsl:otherwise>
														</xsl:choose>
													</xsl:otherwise>
												</xsl:choose>
											</input>
										</td>
										<td class="buttonr"><img src="images/pixel.gif" width="3" height="1" border="0"/></td>
									</tr>
								</table>
								<!--<input type="button" class="keywordButton" value="{.}" style="cursor: pointer; width: 80px;" onclick="doAction('{@action}');"/> -->
							</td>
						</tr>
					</xsl:for-each>
					</table>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates>
					<xsl:with-param name="offsety" select="$offsety" />
				</xsl:apply-templates>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>