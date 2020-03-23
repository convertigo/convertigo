<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt">
	<xsl:output method="html" indent="no" media-type="text/html" encoding="UTF-8" />

	<!-- Convertigo System Includes -->
	<xsl:include href="../../xsl/status.xsl" />
	<xsl:include href="variables.xsl" />

	<xsl:param name="doSort">false</xsl:param>
	<xsl:param name="sortColon">-1</xsl:param>	
	<xsl:param name="sortDataType">text</xsl:param>		
	<xsl:param name="sortOrder">ascending</xsl:param>

	<!-- TEMPLATE DOCUMENT -->
	<xsl:template match="error">
		<!-- add of the CSS file link -->
		<link type="text/css" rel="styleSheet" href="css/exception.css" />
		
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
			<div id="c8o_errorPage">
				<div style	="position:absolute;
								  left:{$offsetx}px;
								  top:{$offsety}px;">
					<form name="javelin_form" method="post" onSubmit="doAction('KEY_ENTER');">
						<input type="hidden" name="__javelin_current_field" id="__javelin_current_field" />
						<input type="hidden" name="__javelin_modified_fields" id="__javelin_modified_fields"/>
						<input type="hidden" name="__javelin_action" />
						<input type="hidden" name="__transaction" />
						<input type="hidden" name="__sesskey" />
						<input type="hidden" name="__context" value="{/document/@context}" />
						<input type="hidden" name="__signature" id="__signature" value="{/document/@signature}" />
						<br/>
						<p class="center">
							<img src="images/logo_convertigo_mashup_server300_error.png" id="logoConvertigo"/>
						</p>
						<blockquote>
							<p>
								<table cellspacing="0" cellpadding="0">
									<tr>
										<td valign="top">
											<table cellspacing="0" cellpadding="6" border="0">
												<tr>
													<td class="title" colspan="2">Error</td>
												</tr>
												<tr>
													<td colspan="2">
														<p>
															An unexpected error occured while Convertigo was trying to execute the transaction :  '<xsl:value-of select="@transaction"/>'
															<br/>Try again your request.
															<br/>If the issue is still occuring, please contact Convertigo administrator, providing the following information.
														</p>
													</td>
												</tr>
											</table>
										</td>
									</tr>
								</table>
							</p>
						</blockquote>
						<p class="errorDetails">
							<img src="images/icon_details_error_xsl.png"/>
							<a href="javascript:showErrorDetails()">Click here to display error details</a>
						</p>
						<div id="details" style="display: none">
							<h3>Main exception</h3>
							<p>
								<b>[<xsl:value-of select="exception"/>]</b><br/>
								<xsl:value-of select="message"/>
							</p>
							<h3>Stack trace</h3>
							<pre>
								<xsl:value-of select="stacktrace"/>
							</pre>
							<hr/>
							<p class="small center">Copyright © 2001-2020 Convertigo SA. All rights reserved.</p>
						</div>
					</form>
				</div>
			</div>
		</div>
	</xsl:template>

</xsl:stylesheet>
