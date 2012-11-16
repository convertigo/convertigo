<?xml version="1.0" encoding="UTF-8"?>

<!--
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
-->

<!-- TWinSoft Convertigo generic error style sheet -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output method="html" indent="no" media-type="text/html" encoding="UTF-8"/>
	<xsl:template match="error">
		<script type="text/javascript" language="javascript">
			function showErrorDetails() {
			   var detailsDiv = document.getElementById("details");
			   if (eval(detailsDiv)) {
			      if (detailsDiv.style.display == "none") {
						detailsDiv.style.display = "block";
					}
					else {
						detailsDiv.style.display = "none";
					}
				}
			}
		</script>
		<div id="c8o_errorPage">
			<p class="center">
				<img src="../../admin/images/logo_convertigo_mashup_server300_error.png" id="logoConvertigo"/>
			</p>
			<blockquote>
				<p>
					<table cellspacing="0" cellpadding="0">
						<tr>
							<td valign="top">
								<table cellspacing="0" cellpadding="6" border="0">
									<tr>
										<td class="title" colspan="2">Erreur</td>
									</tr>
									<tr>
										<td colspan="2">
											Une erreur inattendue s'est produite alors que Convertigo essayait d'exécuter la transaction.<br/>
											Essayez à nouveau votre requête.<br/>
											Si le problème persiste, veuillez contacter votre administrateur Convertigo, en lui fournissant les informations suivantes.
										</td>
									</tr>
								</table>
							</td>
						</tr>
					</table>
				</p>
			</blockquote>
			<p class="errorDetails">
				<img src="../../admin/images/icon_details_error_xsl.png"/>
				<a href="javascript:showErrorDetails()">Détails</a>
			</p>
			<div id="details" style="display: none">
				<h3>Exception principale</h3>
				<p>
					<b>[<xsl:value-of select="exception"/>]</b><br/>
					<xsl:value-of select="message"/>
				</p>
				<h3>Pile d'éxécution</h3>
				<pre>
					<xsl:value-of select="stacktrace"/>
				</pre>
				<hr/>
				<p class="small center">Copyright @ 2001-2012 Convertigo SA. Tous droits réservés.</p>
			</div>
		</div>
	</xsl:template>
</xsl:stylesheet>
