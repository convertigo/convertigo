<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:lxslt="http://xml.apache.org/xslt"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output encoding="UTF-8" indent="no" media-type="text/html"
		method="html" />
	<xsl:template match="document">
		<html>
		<head>
			<meta name="viewport" content="width=device-width" />
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
			<title>Simple Transactional Email</title>
			<style>
				/* -------------------------------------
				INLINED WITH htmlemail.io/inline
				------------------------------------- */
				/* -------------------------------------
				RESPONSIVE AND MOBILE FRIENDLY STYLES
				------------------------------------- */
				@media only screen and (max-width: 620px) {
				table[class=body] h1 {
				font-size: 28px !important;
				margin-bottom: 10px !important;
				}
				table[class=body] p,
				table[class=body] ul,
				table[class=body] ol,
				table[class=body] td,
				table[class=body] span,
				table[class=body] a {
				font-size: 16px !important;
				}
				table[class=body] .wrapper,
				table[class=body] .article {
				padding: 10px !important;
				}
				table[class=body] .content {
				padding: 0 !important;
				}
				table[class=body] .container {
				padding: 0 !important;
				width: 100% !important;
				}
				table[class=body] .main {
				border-left-width: 0 !important;
				border-radius: 0 !important;
				border-right-width: 0 !important;
				}
				table[class=body] .btn table {
				width: 100% !important;
				}
				table[class=body] .btn a {
				width: 100% !important;
				}
				table[class=body] .img-responsive {
				height: auto !important;
				max-width: 100% !important;
				width: auto !important;
				}
				}
	
				/* -------------------------------------
				PRESERVE THESE STYLES IN THE HEAD
				------------------------------------- */
				@media all {
				.ExternalClass {
				width: 100%;
				}
				.ExternalClass,
				.ExternalClass p,
				.ExternalClass span,
				.ExternalClass font,
				.ExternalClass td,
				.ExternalClass div {
				line-height: 100%;
				}
				.apple-link a {
				color: inherit !important;
				font-family: inherit !important;
				font-size: inherit !important;
				font-weight: inherit !important;
				line-height: inherit !important;
				text-decoration: none !important;
				}
				.btn-primary table td:hover {
				background-color: #34495e !important;
				}
				.btn-primary a:hover {
				background-color: #34495e !important;
				border-color: #34495e !important;
				}
				}
			</style>
		</head>
	
	
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
													<h1>Demande d'acceptation de partage de vol</h1>
													<p
														style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; Margin-bottom: 15px;">
														Bonjour,
													</p>
													<p
														style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; Margin-bottom: 15px;">
														Une demande de partage de vol vous a été adressée par :
														<br />
														<br />
														<xsl:value-of select="//Registrant"></xsl:value-of>
														<br />
														<br />
														Pour le vol de
														<b>
															<xsl:value-of select="//from"></xsl:value-of>
														</b>
														à
														<b>
															<xsl:value-of select="//to"></xsl:value-of>
														</b>
														le
														<b><xsl:value-of select="//date"></xsl:value-of></b>
														à
														<b><xsl:value-of select="//time"></xsl:value-of></b>.
														<br /><br />
														Ce vol sera effectué sur le
														<b>
															<xsl:value-of select="//plane"></xsl:value-of>
														</b>, un
														<b>
															<xsl:value-of select="//TYPE"></xsl:value-of>
														</b>, vitesse de croisère
														<b>
															<xsl:value-of select="//SPEED"></xsl:value-of>KT
														</b>. Il y a
														<b>
															<xsl:value-of select="//SEATS"></xsl:value-of>
														</b>
														place(s) sur cet appareil. dont
														<b>
															<xsl:value-of select="//occupiedSeats"></xsl:value-of>
														</b>
														Sont déjà occupée(s). Le créateur du vol à ajouté le commentaire suivant :
														<br />
														<br />
														<i>
															"
															<xsl:value-of select="//comment"></xsl:value-of>
															"
														</i>
													</p>
													<table border="0" cellpadding="0" cellspacing="0"
														class="btn btn-primary"
														style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; box-sizing: border-box;">
														<tbody>
															<tr>
																<td align="left"
																	style="font-family: sans-serif; font-size: 14px; vertical-align: top; padding-bottom: 15px;">
																	<table border="0" cellpadding="0" cellspacing="0"
																		style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: auto;">
																		<tbody>
																			<tr>
																				<td
																					style="font-family: sans-serif; font-size: 14px; vertical-align: top; background-color: #3498db; border-radius: 5px; text-align: center;">
																					<a href="{//c8o-server}/projects/FlightShare/.cxml?__sequence=finalizeregistration&amp;registerid={//RegisterId}&amp;registrant={//Registrant}" target="_blank"
																						style="display: inline-block; color: #ffffff; background-color: #3498db; border: solid 1px #3498db; border-radius: 5px; box-sizing: border-box; cursor: pointer; text-decoration: none; font-size: 14px; font-weight: bold; margin: 0; padding: 12px 25px; text-transform: capitalize; border-color: #3498db;">Accepter le partage</a>
																				</td>
																			</tr>
																		</tbody>
																	</table>
																</td>
															</tr>
														</tbody>
													</table>
													<p
														style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; Margin-bottom: 15px;">
														Merci d'avoir utilisé Flight Share !
													</p>
													<p
														style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; Margin-bottom: 15px;">
														Good luck! Hope it works.
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
		</html>
	</xsl:template>
</xsl:stylesheet>
		