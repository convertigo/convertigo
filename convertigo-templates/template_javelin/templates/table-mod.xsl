<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	
	<!-- TEMPLATE TABLE -->
	<xsl:template match="*[@type='table']">
		<xsl:param name="offsety" />
		<xsl:variable name="scrollBarSize" select="30" />
		<div class="Grid"
			 style="
			 		border-left: solid 1px;
			 		border-right: solid 1px;
			 		border-bottom: solid 1px;
			 		position:absolute;
			 		left:{@column*$coefx+$offsetx}px;
			 		top:{(@line - @offset)*$coefy + $offsety}px;
				 	width:{@width * $coefx + $scrollBarSize}px
			 		">
 
			<table style="border-top:gray solid 1px" class="data" border="2" cellspacing="0" cellpadding="0" width="100%">
				<tr>
					<xsl:apply-templates select="row/Title" />
					<td class="datatitle" style="border-right: white">&#160;</td>
				</tr>
			</table>
<!--
			<div
				 style="overflow:auto;
							 		">
				<table  class="data" cellpadding="0" cellspacing="0" border="2"
						style="
							height:400px;						
						">
					<xsl:apply-templates	select="row[position() > 1]" />
				</table>
			</div>
-->			
			<div
				 style="overflow:auto;
				 		height: {(@height - @titleheight +1) * $coefy}px;
				 		">
				<table  class="data" cellpadding="0" cellspacing="0" border="0">
					<xsl:apply-templates	select="row[position() > 1]" />
				</table>
			</div>
		
		</div>
		<div style="position:absolute;
					left:{(@column + @width)*$coefx+$offsetx + $scrollBarSize + 3}px;
			 		top:{(@line - @offset)*$coefy + $offsety}px;
				">
			<table>
					<tr valign="top">
						<img src="../../images/hide.gif" alt=""  onclick="doAction('KEY_ROLLUP')"/>
					</tr>
					<tr valign="bottom">
						<img src="../../images/show.gif" alt=""  onclick="doAction('KEY_ROLLDOWN')"/>
					</tr>
					<tr  valign="middle">
						<img src="../../images/refresh.gif" alt=""  onclick="doAction('KEY_ROLLUP')"/>
					</tr>
			</table>
		</div>
		
		<xsl:apply-templates select="actionsTable" />
	</xsl:template>

	<!-- TEMPLATE ROW -->
	<xsl:template match="row" priority="1">
<!--		<tr style="line-height: {$coefy - 1}px"> -->
		<tr>
			<xsl:attribute name="ondblclick">
				document.getElementById('<xsl:value-of select="*[1]/@name"/>').value = <xsl:value-of select="../actionsTable/action[1]/@char"/>;
				<xsl:if test="../actionsTable/action[1]/@key != 'null'">
					doAction('<xsl:value-of select="../actionsTable/action[1]/@key"/>');
				</xsl:if>
				return false;
			</xsl:attribute>
			
			<xsl:if test="(@line mod 2) = 0">
				<xsl:attribute name="class">dataroweven</xsl:attribute>
			</xsl:if>
			<xsl:if test="(@line mod 2) != 0">
				<xsl:attribute name="class">datarowodd</xsl:attribute>
			</xsl:if>

			<xsl:for-each select="child::*">
				<xsl:variable name="index" select="position()" />
<!--				<td style="width:{../../row[1]/Title[$index]/@size * $coefx}px" class="cell"> -->
				<td style="width:{../../row[1]/Title[$index]/@size * $coefx}px; height: {$coefy * 1.5}px" class="cell">
					<xsl:choose>
						<xsl:when test='(@type = "field")'>
							<xsl:attribute name="align">center</xsl:attribute>

							<xsl:if test="not(@autoenter)">
								<xsl:if
									test="(@columnSelection = 'true')">
									<input class="fixed"
										style="width:{1*$coefx}"
										type="text"
										name="{@name}"
										onfocus="currentFieldOnFocus=this.id"
										id="{@name}"
										maxlength="{@size}"
										value="{.}">
										<xsl:attribute name="onclick">show_mySelectMenu2(event, document.getElementById('<xsl:value-of select="@name"/>'), document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
										<xsl:attribute name="onblur">hide_mySelectMenu2(document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
									</input>
								</xsl:if>
								<xsl:if
									test="not(@columnSelection = 'true')">
									<input class="fixed"
										name="{@name}"
										onkeyup="checkInputChars(event, {@size}, false, document.javelin_form.{@name})"
										onfocus="currentFieldOnFocus=this.id"
										id="{@name}"
										size="{@size}"
										maxlength="{@size}"
										value="{.}" />
								</xsl:if>
							</xsl:if>

							<xsl:if test="@autoenter">
								<xsl:if
									test="(@columnSelection = 'true')">
									<input class="fixed"
										type="text"
										name="{@name}"
										onfocus="currentFieldOnFocus=this.id"
										id="{@name}"
										maxlength="{@size}"
										value="{.}">
										<xsl:attribute name="onclick">show_mySelectMenu2(event, document.getElementById('<xsl:value-of select="@name"/>'), document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
										<xsl:attribute name="onblur">hide_mySelectMenu2(document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute>
									</input>
								</xsl:if>
								<xsl:if
									test="not(@columnSelection = 'true')">
									<input class="fixed"
										name="{@name}"
										onkeyup="checkInputChars(event, {@size}, true, document.javelin_form.{@name})"
										onfocus="currentFieldOnFocus=this.id"
										id="{@name}"
										size="{@size}"
										maxlength="{@size}"
										value="{.}" />
								</xsl:if>
							</xsl:if>

							<xsl:if test="(@hasFocus = 'true')">
								<script type="text/javascript"
									language="javascript">
									var elt=document.getElementsByTagName("INPUT");
									if(elt['<xsl:value-of select="@name" />'])
										focusOnField = elt['<xsl:value-of select="@name" />'];
								</script>
							</xsl:if>

						</xsl:when>
						<xsl:when test='(@type = "choice")'>
							<select class="fixed" name="{@name}"
								onfocus="currentFieldOnFocus=this.id" id="{@name}">
								<xsl:for-each select="child::*">
									<option value="{@action}">
										<xsl:if
											test="(@selected = 'true')">
											<xsl:attribute
												name="selected">
												selected
											</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="@value" />
									</option>
								</xsl:for-each>
							</select>
							<xsl:if test="(@hasFocus ='true')">
								<script type="text/javascript"
									language="javascript">
									var elt = document.getElementsByTagName("SELECT");
									if(elt['<xsl:value-of select="@name" />'])
										focusOnField = elt['<xsl:value-of select="@name" />'];
								</script>
							</xsl:if>
						</xsl:when>
						<xsl:otherwise>
							<xsl:if test="number(translate(.,',','.'))=number(translate(.,',','.'))">
								<xsl:attribute name="align">right</xsl:attribute>
							</xsl:if>
							<xsl:if test="string-length(.) = 0">&#160;</xsl:if>
							<xsl:if test="string-length(.) != 0"><xsl:value-of select="." /></xsl:if>&#160;
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>

	<!-- TEMPLATE TITLE -->
	<xsl:template match="Title" priority="1">
		<td  valign="bottom" style="width:{@size * $coefx}px;border-right: solid 1px" class="datatitle">
			<xsl:if test="string-length(.) = 0"> </xsl:if>
			<xsl:if test="string-length(.) != 0">
				<xsl:value-of select="." />
			</xsl:if>
		</td>
	</xsl:template>

	<!-- TEMPLATE ACTIONSTABLE IN DIV -->
	<xsl:template match="actionsTable" priority="1">
		<script src="../../scripts/selectMenu.js"><!--script--></script >
		<div class="actDiv" id="act_c{../@column}_l{../@line}">
			<img src="../../images/croix.gif" alt="Fermer" width="8" height="8" onclick="hide_mySelectMenu2(document.getElementById('act_c{../@column}_l{../@line}'));"/>
			<table cellpadding="0" cellspacing="0">
				<xsl:for-each select="./action">
					<tr>
						<td class="menuitems" onmouseover="highlight(this);" onMouseout="lowlight(this);">
							<xsl:choose>
								<xsl:when test="@key = 'null'">
									<xsl:attribute name="onclick">updateDataInObjSelected2('<xsl:value-of select="@char"/>', document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'));</xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="onclick">updateDataInObjSelected2('<xsl:value-of select="@char"/>', document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')); return doAction('<xsl:value-of select="@key"/>');</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="@label"/>
						</td>
						<td>
							(<xsl:value-of select="@char"/>)&#160;
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>

</xsl:stylesheet>
