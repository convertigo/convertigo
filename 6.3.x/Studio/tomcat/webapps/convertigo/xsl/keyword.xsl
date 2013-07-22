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

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE KEYWORD -->
	<xsl:template match="*[@type='keyword']">
		<xsl:param name="offsety" />
		
		<xsl:if test="/document/blocks/*/@type ='panel'">
			<xsl:if test="../@type = 'panel'">
				<span
					style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
					<xsl:if test=". = '(F4)'">
						<input class="keywordbutton" name="{@name}"
							id="{@name}" type="button" style="font-size:6pt" value="(F4)">
							<xsl:if
								test="preceding-sibling::*[2]/@type='field' and preceding-sibling::*[2]/@line=./@line">
								<xsl:attribute name="onclick">
									document.javelin_form.<xsl:value-of	select="preceding-sibling::*[2]/@name" />.focus();
									currentFieldOnFocus='<xsl:value-of select="preceding-sibling::*[2]/@name" />';
									doAction('<xsl:value-of select="@action" />');
								</xsl:attribute>
							</xsl:if>
							<xsl:if
								test="not(preceding-sibling::*[2]/@type='field' and preceding-sibling::*[2]/@line=./@line)">
								<xsl:attribute name="onclick">
									doAction('<xsl:value-of select="@action" />');
								</xsl:attribute>
							</xsl:if>
						</input>
					</xsl:if>
					<xsl:if test="not(. = '(F4)')">
						<xsl:if test="$DisplayActionKey='true'">
							<input class="keywordbutton" name="{@name}"
								id="{@name}" onclick="" type="button" style="font-size:6pt"
								value="{substring(@action,6)}={.}">
								<xsl:attribute name="onclick">
									doAction('<xsl:value-of select="@action" />');
								</xsl:attribute>
							</input>
						</xsl:if>
						<xsl:if test="not($DisplayActionKey='true')">
							<input class="keywordbutton" name="{@name}"
								id="{@name}" onclick="" type="button" style="font-size:6pt"
								value="{.}">
								<xsl:attribute name="onclick">
									doAction('<xsl:value-of select="@action" />');
								</xsl:attribute>
							</input>
						</xsl:if>
					</xsl:if>
				</span>
			</xsl:if>
		</xsl:if>
		
		<!-- keywords are not in a panel -->
		<xsl:if test="not(/document/blocks/*/@type ='panel')">
			<span
				style="position:absolute; left:{./@column*$coefx+$offsetx}px; top:{./@line*$coefy + $offsety}px">
				<xsl:if test=". = '(F4)'">
					<input class="keywordbutton" name="{@name}"
						id="{@name}" type="button" style="font-size:6pt" value="(F4)">
						<xsl:if
							test="preceding-sibling::*[2]/@type='field' and preceding-sibling::*[2]/@line=./@line">
							<xsl:attribute name="onclick">
								document.javelin_form.<xsl:value-of	select="preceding-sibling::*[2]/@name" />.focus();
								currentFieldOnFocus='<xsl:value-of	select="preceding-sibling::*[2]/@name" />';
								doAction('<xsl:value-of select="@action" />');
							</xsl:attribute>
						</xsl:if>
						<xsl:if
							test="not(preceding-sibling::*[2]/@type='field' and preceding-sibling::*[2]/@line=./@line)">
							<xsl:attribute name="onclick">
								doAction('<xsl:value-of select="@action" />');
							</xsl:attribute>
						</xsl:if>
					</input>
				</xsl:if>
				<xsl:if test="not(. = '(F4)')">
					<xsl:if test="$DisplayActionKey='true'">
						<input class="keywordbutton" name="{@name}"
							id="{@name}" onclick="" type="button" style="font-size:6pt"
							value="{substring(@action,6)}={.}">
							<xsl:attribute name="onclick">
								doAction('<xsl:value-of select="@action" />');
							</xsl:attribute>
						</input>
					</xsl:if>
					<xsl:if test="not($DisplayActionKey='true')">
						<input class="keywordbutton" name="{@name}"
							id="{@name}" onclick="" type="button" style="font-size:6pt"
							value="{.}">
							<xsl:attribute name="onclick">
								doAction('<xsl:value-of select="@action" />');
							</xsl:attribute>
						</input>
					</xsl:if>
				</xsl:if>
			</span>

		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
