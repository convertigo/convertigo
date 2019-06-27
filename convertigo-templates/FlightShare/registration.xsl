<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:lxslt="http://xml.apache.org/xslt"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output encoding="UTF-8" indent="no" media-type="text/html"
		method="html" />
	<xsl:template match="document">
		<body class=""
			style="background-color: #f6f6f6; font-family: sans-serif; -webkit-font-smoothing: antialiased; font-size: 14px; line-height: 1.4; margin: 0; padding: 0; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;">
			<table border="0" cellpadding="0" cellspacing="0" class="body"
				style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; background-color: #f6f6f6;">
				<tr>
					<td
						style="font-family: sans-serif; font-size: 14px; vertical-align: top;">
					</td>
					<td class="container"
						style="font-family: sans-serif; font-size: 14px; vertical-align: top; display: block; Margin: 0 auto; max-width: 580px; padding: 10px; width: 580px;">
						<div class="content"
							style="box-sizing: border-box; display: block; Margin: 0 auto; max-width: 580px; padding: 10px;">
							<!-- START CENTERED WHITE CONTAINER -->
							<span class="preheader"
								style="color: transparent; display: none; height: 0; max-height: 0; max-width: 0; opacity: 0; overflow: hidden; mso-hide: all; visibility: hidden; width: 0;">Demande de partage de vol Flight Share...</span>
							<table class="main"
								style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; background: #ffffff; border-radius: 3px;">
								<!-- START MAIN CONTENT AREA -->
								<tr>
									<td class="wrapper"
										style="font-family: sans-serif; font-size: 14px; vertical-align: top; box-sizing: border-box; padding: 20px;">
										<table border="0" cellpadding="0" cellspacing="0"
											style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%;">
											<tr>
												<td
													style="font-family: sans-serif; font-size: 14px; vertical-align: top;">
													<img src="http://www.aeroclub-atcf.com/img/atcf.png"></img>
													<h1>Le demande à été traitée.</h1>
													<p
														style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; Margin-bottom: 15px;">
														Bonjour,
													</p>
													<p
														style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; Margin-bottom: 15px;">
														<xsl:if test="//status = 'OK'">
															Le pilote <xsl:value-of select="//pilot"></xsl:value-of> a bien été inscrit sur ce vol! Merci pour lui :) 
														</xsl:if>
														<xsl:if test="//status = 'KO'">
															Malheureusement, Le pilote <b><xsl:value-of select="//pilot"></xsl:value-of></b> n'a pas été inscrit sur ce vol par manque de place. L'avion comporte
															<b><xsl:value-of select="//availableSeats"></xsl:value-of></b> places, <b><xsl:value-of select="//occupiedSeats"></xsl:value-of></b> ont été réservées par 
															le créateur du vol et <b><xsl:value-of select="//registeredPilots"></xsl:value-of></b> pilotes se sont déjà inscrits. 
														</xsl:if>
													</p>
													<p
														style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; Margin-bottom: 15px;">
														Merci d'avoir utilisé Flight Share !
													</p>
												</td>
											</tr>
										</table>
									</td>
								</tr>
								<!-- END MAIN CONTENT AREA -->
							</table>
							<!-- START FOOTER -->
							<div class="footer"
								style="clear: both; Margin-top: 10px; text-align: center; width: 100%;">
								<table border="0" cellpadding="0" cellspacing="0"
									style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%;">
									<tr>
										<td class="content-block"
											style="font-family: sans-serif; vertical-align: top; padding-bottom: 10px; padding-top: 10px; font-size: 12px; color: #999999; text-align: center;">
											<span class="apple-link"
												style="color: #999999; font-size: 12px; text-align: center;">Flight Share (ATCF)
												Aero Touring Club de France,
												Toussus le Noble, France</span>
										</td>
									</tr>
									<tr>
										<td class="content-block powered-by"
											style="font-family: sans-serif; vertical-align: top; padding-bottom: 10px; padding-top: 10px; font-size: 12px; color: #999999; text-align: center;">
											Powered by
											<a href="http://www.convertigo.com"
												style="color: #999999; font-size: 12px; text-align: center; text-decoration: none;">Convertigo Mobility Platform</a>
											.
										</td>
									</tr>
								</table>
							</div>
							<!-- END FOOTER -->
							<!-- END CENTERED WHITE CONTAINER -->
						</div>
					</td>
					<td
						style="font-family: sans-serif; font-size: 14px; vertical-align: top;">
					</td>
				</tr>
			</table>
		</body>
	</xsl:template>
</xsl:stylesheet>
		