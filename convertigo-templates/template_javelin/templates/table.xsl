<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format">
	
	<!-- TEMPLATE TABLE -->
	<xsl:template match="*[@type='table']">
		<xsl:param name="offsety" />
		<xsl:variable name="tableTop">
			<xsl:choose>
				<xsl:when test="./actionsTable">
					<xsl:value-of select="./@line - ./@titleheight - ./@offset - 1"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="./@line - ./@titleheight - ./@offset"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="elDepth">
			<xsl:choose>
				<xsl:when test="ancestor::*[@type='panel']/@zOrder"><xsl:value-of select="10 - ancestor::*[@type='panel']/@zOrder"/></xsl:when>
				<xsl:otherwise>1</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- Main container -->
		<div class="Grid"
			 style="position: absolute;
			 		left: {@column * $coefx + $offsetx}px;
			 		top: {$tableTop * $coefy + $offsety}px;
					width: {@width * $coefx}px;
					z-index: {$elDepth * 10 + 1};">
			<!-- Titles table -->
			<table cellpadding="0" 
					cellspacing="0" 
					border="0" 
					class="titletable" 
					style="width: {@width * $coefx}px;">
				<tr>
					<xsl:apply-templates select="row/Title" />
				</tr>
			</table>
			<xsl:choose>
				<!-- Data table when number of rows (including title row) is greater than table height -->
				<xsl:when test="count(row) &gt; @height">
					<div id="_ScRoLl_"
						 style="overflow-y: auto;
						 		overflow-x: visible;
								width:{@width * $coefx + $scrollwidth}px;
						 		height: {(@height - 0) * $coefy + 1}px;">
						<table  class="data" 
								cellpadding="0" 
								cellspacing="0" 
								border="0"
								style="width: {@width * $coefx}px;">
							<xsl:choose>
								<xsl:when test="$doSort = 'true'">
									<xsl:if test="$sortDataType = 'number'">
										<xsl:apply-templates	select="row[position() > 1]">
											<xsl:sort select="translate(child::*[position()=$sortColon],',','.')" order="{$sortOrder}" data-type="{$sortDataType}"/>
										</xsl:apply-templates>
									</xsl:if>
									<xsl:if test="$sortDataType = 'text'">
										<xsl:apply-templates	select="row[position() > 1]">
											<xsl:sort select="child::*[position()=$sortColon]" order="{$sortOrder}" data-type="{$sortDataType}"/>
										</xsl:apply-templates>
									</xsl:if>						
								</xsl:when>
								<xsl:otherwise>
									<xsl:apply-templates	select="row[position() > 1]"/>
								</xsl:otherwise>
							</xsl:choose>
						</table>
					</div>
				</xsl:when>
				<!-- Data table when number of rows (including title row) is equal or lower than table height -->
				<xsl:otherwise>
					 <div id="_ScRoLl_"
						 style="overflow: visible;
								width:{@width * $coefx}px;
					 			height: {(count(row) - 1) * $coefy + 2}px;">
						<table  class="data" 
								cellpadding="0" 
								cellspacing="0" 
								border="0" 
								style="width: {round(@width * $coefx)}px;">
							<xsl:choose>
								<xsl:when test="$doSort = 'true'">
									<xsl:if test="$sortDataType = 'number'">
										<xsl:apply-templates	select="row[position() > 1]">
											<xsl:sort select="translate(child::*[position()=$sortColon],',','.')" order="{$sortOrder}" data-type="{$sortDataType}"/>
										</xsl:apply-templates>
									</xsl:if>
									<xsl:if test="$sortDataType = 'text'">
										<xsl:apply-templates	select="row[position() > 1]">
											<xsl:sort select="child::*[position()=$sortColon]" order="{$sortOrder}" data-type="{$sortDataType}"/>
										</xsl:apply-templates>
									</xsl:if>						
								</xsl:when>
								<xsl:otherwise>
									<xsl:apply-templates	select="row[position() > 1]"/>
								</xsl:otherwise>
							</xsl:choose>
						</table>
					</div>
				</xsl:otherwise>
			</xsl:choose>				
		</div>
		<!-- Table icons (page up/down, sort type... -->
		<!--span style="position:absolute;
				left:{round((@column + 1 + @width)*$coefx+$offsetx - $iconSize)}px;
		 		top:{round((@line - @titleheight)*$coefy + $offsety - $iconSize)}px;
		 		width: {5*$iconSize}px;"-->
		 <span style="position:absolute;
				left:{round((@column + 2 + @width)*$coefx+$offsetx)}px;
		 		top:{round((@line - @titleheight)*$coefy + $offsety)}px;
		 		width: {1*$iconSize}px;">
			<!--xsl:if test="$sortDataType = 'text'">
				<img src="images/sort_text.gif" alt=""  onclick="onDataSortClic('number')" onmouseover="tooltip.show(this)" onmouseout="tooltip.hide(this)" title="Sort by number"/>			
			</xsl:if>
			<xsl:if test="not($sortDataType = 'text')">
				<img src="images/sort_num.gif" alt=""  onclick="onDataSortClic('text')" onmouseover="tooltip.show(this)" onmouseout="tooltip.hide(this)" title="Sort by text"/>
			</xsl:if>
			<img src="images/unsort.gif" alt=""  onclick="onUnsortClic()" onmouseover="tooltip.show(this)" onmouseout="tooltip.hide(this)" title="Undo Sort"/-->
			<img src="../../images/hide.gif" alt=""  onclick="doAction('KEY_ROLLUP')" onmouseover="tooltip.show(this)" onmouseout="tooltip.hide(this)" title="Page Up" style="cursor: pointer;"/>
			<br/><br/><img src="../../images/show.gif" alt=""  onclick="doAction('KEY_ROLLDOWN')" onmouseover="tooltip.show(this)" onmouseout="tooltip.hide(this)" title="Page Down" style="cursor: pointer;"/>
			<!--img src="../../images/refresh.gif" alt=""  onclick="doAction('KEY_ROLLUP')" onmouseover="tooltip.show(this)" onmouseout="tooltip.hide(this)" title="Extract"/-->
		</span>
	
		<xsl:apply-templates select="actionsTable" />
	</xsl:template>

	<!-- TEMPLATE ROW -->
	<xsl:template match="row" priority="1">
		<tr>
			<xsl:call-template name="mashupEventOnClick">
				<xsl:with-param name="ref" select=".." /> 
			</xsl:call-template>
			<xsl:if test="../actionsTable">
				<!--xsl:attribute name="ondblclick">
					document.getElementById('<xsl:value-of select="*[1]/@name"/>').value = <xsl:value-of select="../actionsTable/action[1]/@char"/>;
					<xsl:if test="../actionsTable/action[1]/@key != 'null'">
						doAction('<xsl:value-of select="../actionsTable/action[1]/@key"/>');
					</xsl:if>
					return false;
				</xsl:attribute-->
				<xsl:attribute name="ondblclick">getCurrentRowXml('<xsl:value-of select="../@column"/>', '<xsl:value-of select="../@line"/>', '<xsl:value-of select="./@index"/>', '|');</xsl:attribute>
			</xsl:if>
			
			<xsl:if test="(position() mod 2) = 0">
				<xsl:attribute name="class">dataroweven</xsl:attribute>
			</xsl:if>
			<xsl:if test="(position() mod 2) != 0">
				<xsl:attribute name="class">datarowodd</xsl:attribute>
			</xsl:if>
			<xsl:attribute name="onmouseover">
				over_tr(this);
				<xsl:call-template name="mashupTooltipOn">
					<xsl:with-param name="ref" select=".." /> 
				</xsl:call-template>
			</xsl:attribute>
			<xsl:attribute name="onmouseout">
				over_tr(this);
				<xsl:call-template name="mashupTooltipOff">
					<xsl:with-param name="ref" select=".." /> 
				</xsl:call-template>
			</xsl:attribute>			
			<xsl:for-each select="child::*">
				<xsl:variable name="index" select="position()" />
				<td style="width:{round(../../row[1]/Title[$index]/@size * $coefx)}px; height: {round($coefy)}px" class="cell">
					<xsl:choose>
						<!-- Cell content when it is a field -->
						<xsl:when test="@type = 'field'">
							<xsl:attribute name="align">center</xsl:attribute>
							<xsl:if test="(@hasFocus ='true')">
								<input type="hidden" id="focus" value= "{@name}" />
							</xsl:if>
							<!-- Container SPAN. Used only for framework behaviour -->
							<span id="{@name}_n{../@page}parent">
								<input type="text"
										class="fieldText"
										name="{@name}"
										onfocus="getCurrentRowXml('{../../@column}', '{../../@line}', '{../@index}', '|'); currentFieldOnFocus=this.id; onInputClick(this);"
										id="{@name}_n{../@page}"
										maxlength="{@size}"
										style="width: {@size*$coefx}px;"
										value="{.}">
										
									<xsl:choose>
										<xsl:when test="@columnSelection = 'true'">
											<xsl:attribute name="onclick">show_mySelectMenu(event, this, document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'));</xsl:attribute>
											<!--xsl:attribute name="onblur">hide_mySelectMenu2(document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'))</xsl:attribute-->
										</xsl:when>
										<xsl:otherwise>
											<xsl:attribute name="onkeyup">checkInputChars(event, <xsl:value-of select="@size"/>, false, document.javelin_form.<xsl:value-of select="@name"/>);</xsl:attribute>
										</xsl:otherwise>
									</xsl:choose>
								</input>
							</span>
						</xsl:when>
						<!-- Cell content when it is a date field -->
						<xsl:when test="@type = 'date'">
							<xsl:attribute name="align">center</xsl:attribute>
							<xsl:if test="(@hasFocus ='true')">
								<input type="hidden" id="focus" value= "{@name}" />
							</xsl:if>
							<!-- Container SPAN. Used only for framework behaviour -->
							<span id="{@name}_n{../@page}parent">
								<input type="text"
										class="fieldText"
										name="{@name}"
										onfocus="getCurrentRowXml('{../../@column}', '{../../@line}', '{../@index}', '|'); currentFieldOnFocus=this.id; onInputClick(this);"
										id="{@name}_n{../@page}"
										maxlength="{@size}"
										style="width: {@size*$coefx}px;"
										value="{.}"
										onkeyup="checkInputChars(event, {@size}, false, document.javelin_form.{@name});"/>
							</span>
						</xsl:when>
						<!-- Cell content when it is a choice -->
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
						<!-- Default Cell content (static...) -->
						<xsl:otherwise>
							<!-- Use this to force right-align numbers in cells -->
							<!--xsl:if test="number(translate(.,',.','.0'))=number(translate(.,',.','.0'))">
								<xsl:attribute name="align">right</xsl:attribute>
							</xsl:if-->
							<xsl:if test="string-length(.) = 0">&#160;</xsl:if>
							<xsl:if test="string-length(.) != 0">
								<!-- Container SPAN. Used only for positioning and framework behaviour -->
								<span onclick="spanClick(this, {@column}, {@line})"
										ondblclick	="spanClick(this, {@column}, {@line}); doAction('KEY_ENTER');"
										style="white-space: nowrap">
									<!-- Background color style SPAN -->
									<span class="bgcolor{@background}">
										<!-- Foreground color style SPAN -->
										<span class="color{@foreground}">
											<span class="tableText">
												<xsl:value-of select="." />&#160;
											</span>
										</span>
									</span>
								</span>
							</xsl:if>
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>

	<!-- TEMPLATE TITLE -->
	<xsl:template match="Title" priority="1">
		<td  style="width:{round(@size * $coefx)}px;height:{../../@titleheight * $coefy}px;" class="datatitle" valign="bottom" onclick="onSortClicTD({position()})">
			<xsl:if test="string-length(.) = 0">&#160;</xsl:if>
			<xsl:if test="string-length(.) != 0">
				<xsl:for-each select="block">
					<span class="datatitle">
						<xsl:value-of select="." />
						<xsl:if test="position() != last()">
							<br/>
						</xsl:if>
					</span>
				</xsl:for-each>
			</xsl:if>
			<img id="CoLiMg{position()}"  src="images/ascending_sug.gif" alt=""/>
		</td>
	</xsl:template>

	<!-- TEMPLATE ACTIONSTABLE -->
	<xsl:template match="actionsTable" priority="1">
		<div class="actDiv" id="act_c{../@column}_l{../@line}" style="height: { 5 * $coefy}px ; z-index:12;overflow-y:scroll; overflow-x:hidden;">
			<img src="images/croix.gif" alt="Fermer" width="8" height="8" onclick="hide_mySelectMenu(document.getElementById('act_c{../@column}_l{../@line}'));"/>
			<table cellpadding="0" cellspacing="0">
				<xsl:for-each select="./action">
					<tr>
						<td class="menuitems" onmouseover="highlight(this);" onMouseout="lowlight(this);">
							<xsl:choose>
								<xsl:when test="@key = 'null'">
									<xsl:attribute name="onclick">updateDataInObjSelected('<xsl:value-of select="@char"/>', document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>'));</xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="onclick">updateDataInObjSelected('<xsl:value-of select="@char"/>', document.getElementById('act_c<xsl:value-of select="../../@column"/>_l<xsl:value-of select="../../@line"/>')); return doAction('<xsl:value-of select="@key"/>');</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="@label"/>
						</td>
						<td class="menuitems">
							(<xsl:value-of select="@char"/>)&#160;&#160;&#160;&#160;
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>

</xsl:stylesheet>
