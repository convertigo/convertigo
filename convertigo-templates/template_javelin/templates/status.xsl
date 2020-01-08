<?xml version="1.0" encoding="UTF-8"?>
<!-- TWinSoft Convertigo generic status style sheet -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:template match="status">
		<html>
			<head>
				<meta name="GENERATOR" content="TWinSoft Convertigo"/>
				<meta http-equiv="expires" content="0"/>
				<meta http-equiv="refresh" content="2; url={@refresh-url}"/>
				<style> p {font-family:verdana;} </style>
			</head>
			<body bgcolor="#FFFFFF" background="../../images/marbre.gif">
				<p>
					<img src="../../images/twinsoft.gif"/>
				</p>
				<table border="0" cellpadding="8" 
					style="border: 2px solid #FA8072;">
					<tr>
						<td width="100%">
							<xsl:apply-templates/>
						</td>
					</tr>
				</table>
				<p>
					<a href="http://www.convertigo.com">
						<img border="0" src="../../images/convertigo.gif"/>
					</a>
					<br/>
					<font size="-1">Copyright © 2001-2020 Convertigo SA. All rights reserved.</font>
				</p>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="job">
		<p>
			<b>La transaction (job #<xsl:value-of select="@id"/>) est en cours 
				d'éxécution ; veuillez patienter...</b>
		</p>
		<p>
			<xsl:apply-templates/>
		</p>
	</xsl:template>
	<xsl:template match="step">
		<br>
			<xsl:apply-templates/>
		</br>
	</xsl:template>
</xsl:stylesheet>