<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE STATIC -->
	<xsl:template match="*[@type='static']" >
		<xsl:param name="offsety" />
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- Hide non-visible statics -->
		<xsl:if test="@background != @foreground">
			<!-- Container SPAN. Used only for positioning and framework behaviour -->
			<span ondblclick="spanClick(this, {@column}, {@line}); doAction('KEY_ENTER');" >
				<xsl:choose>
					<xsl:when test="@mashup_event != ''">
						<xsl:attribute name="style">position: absolute;
							left: <xsl:value-of select="./@column*$coefx+$offsetx"/>px;
							top: <xsl:value-of select="./@line*$coefy + $offsety"/>px;
							white-space: nowrap;
							z-index: <xsl:value-of select="$elDepth * 10 + 1"/>;
							cursor: pointer;
							text-decoration: underline;
						</xsl:attribute>
						<xsl:call-template name="mashupVars" />
						<xsl:attribute name="onclick">
							spanClick(this, <xsl:value-of select="@column" />, <xsl:value-of select="@line" />);
							<xsl:call-template name="mashupEvent"/>
						</xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="style">position: absolute;
							left: <xsl:value-of select="./@column*$coefx+$offsetx"/>px;
							top: <xsl:value-of select="./@line*$coefy + $offsety"/>px;
							white-space: nowrap;
							z-index: <xsl:value-of select="$elDepth * 10 + 1"/>;
						</xsl:attribute>
						<xsl:attribute name="onclick">spanClick(this, <xsl:value-of select="@column" />, <xsl:value-of select="@line" />);</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:call-template name="mashupTooltipOnOff"/>
				<!-- Background color style SPAN -->
				<span class="bgcolor{@background}">
					<!-- Foreground color style SPAN -->
					<span class="color{@foreground}">
						<xsl:choose>
							<xsl:when test="starts-with(., 'http://')">
								<a target="_blank" href="{.}"><xsl:value-of select="."/></a>
							</xsl:when>
							<xsl:otherwise>
								<xsl:if test="@blink">
									<!-- Blink style SPAN -->
									<span class="blink">
										<xsl:if test="@underline">
											<!-- Underline style SPAN -->
											<span class="underline">
												<span class="staticText">
													<xsl:choose>
														<xsl:when test="@style">
															<span style="{@style}">
																<xsl:value-of select="." />
															</span>
														</xsl:when>
														<xsl:otherwise>
															<xsl:value-of select="." />
														</xsl:otherwise>
													</xsl:choose>
												</span>
											</span>
										</xsl:if>
										<xsl:if test="not(@underline)">
											<span class="staticText">
												<xsl:choose>
													<xsl:when test="@style">
														<span style="{@style}">
															<xsl:value-of select="." />
														</span>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="." />
													</xsl:otherwise>
												</xsl:choose>
											</span>
										</xsl:if>
									</span>
								</xsl:if>
								<xsl:if test="not(@blink)">
									<xsl:if test="@underline">
										<!-- Underline style SPAN -->
										<span class="underline">
											<span class="staticText">
												<xsl:choose>
													<xsl:when test="@style">
														<span style="{@style}">
															<xsl:value-of select="." />
														</span>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="." />
													</xsl:otherwise>
												</xsl:choose>
											</span>
										</span>
									</xsl:if>
									<xsl:if test="not(@underline)">
										<span class="staticText">
											<xsl:choose>
												<xsl:when test="@style">
													<span style="{@style}">
														<xsl:value-of select="." />
													</span>
												</xsl:when>
												<xsl:otherwise>
													<xsl:value-of select="." />
												</xsl:otherwise>
											</xsl:choose>
										</span>
									</xsl:if>
								</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
					</span>
				</span>
			</span>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
