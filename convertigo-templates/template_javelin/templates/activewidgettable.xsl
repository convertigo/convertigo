<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!-- TEMPLATE TABLE -->
	
	<xsl:template match="*[@type='table']">
		<xsl:param name="offsety"/>
		<style>
			#myGrid .aw-grid-row {height: 20px; border-bottom: 1px solid #ccc}
			#myGrid .aw-alternate-even {background: #fff;}
			#myGrid .aw-alternate-odd {background: #ddd;}
			#myGrid .aw-grid-cell {border-right: 1px dotted #99ccff} 
			
			/*	Highlight on mouseover, mousedown */
			#myGrid .aw-mouseover-row {background: #ccc;} 
			#myGrid .aw-mousedown-row {background: #999;} 
				
			/* Selected rows */
			#myGrid .aw-rows-selected {background: #316ac5;}
				
			/* Selected cells */
			#myGrid .aw-grid-row .aw-cells-selected {background: #316ac5;}
				
			#myGrid .aw-grid-headers {color: blue}
		
			/*	Highlight on mouseover, mousedown */
			#myGrid .aw-mouseover-header {color: red;}
			#myGrid .aw-mousedown-header {color: yellow;}
		
			#myGrid .aw-header-1 {background: #def}
		
			#myGrid .aw-row-selector {width: 20px; text-align: center}
			#myGrid .aw-mouseover-row .aw-row-selector {color: red;}
		
			/*	Highlight on mouseover, mousedown */
			#myGrid .aw-mouseover-selector {background: green;}
			#myGrid .aw-mousedown-selector {background: yellow;}
		</style>
		
		<script>
			function generateTable()
			{
				alert("test");
			}
			
			var myData = <xsl:text>[</xsl:text>
			<xsl:for-each select="row[position() > 1]">
				[<xsl:for-each select="*">
					<xsl:text>"</xsl:text>
					<xsl:value-of select="." />
					<xsl:text>",</xsl:text>
				</xsl:for-each>],
			</xsl:for-each>
			<xsl:text>];</xsl:text>
		
			var myHeaders = <xsl:text>[</xsl:text>
			<xsl:for-each select="row[1]/Title">
				<xsl:text>"</xsl:text>
				<xsl:for-each select="*">
					<xsl:value-of select="." />
				</xsl:for-each>
				<xsl:text>",</xsl:text>
			</xsl:for-each>
			<xsl:text>];</xsl:text>

			<xsl:if test="count(actionsTable/action) &gt; 0">
				var myActions = <xsl:text>[</xsl:text>
				<xsl:for-each select="actionsTable/action">
					<xsl:text>"</xsl:text>
					<xsl:value-of select="@char" /> - <xsl:value-of select="@label" />
					<xsl:text>",</xsl:text>
				</xsl:for-each>
				<xsl:text>];</xsl:text>
			</xsl:if>
			
			var obj = new AW.Grid.Extended;
			obj.setId("myGrid");								// necessary for CSS rules
			
			obj.setCellText(myData);							// 2-dimensional js array
			
			
			obj.setHeaderCount(1);
			obj.setHeaderText(myHeaders);						// js array (see top of this page)
			obj.getHeadersTemplate().setClass("text", "wrap");	// multiline text
			obj.setHeaderHeight(<xsl:value-of select="@titleheight * $coefy" />);
			
			<xsl:for-each select="row[1]/Title">
				obj.setColumnWidth(<xsl:value-of select="@size * $coefx"></xsl:value-of>, <xsl:value-of select="position()-1" />);
			</xsl:for-each>
			
			<xsl:for-each select="row[position() > 1]">
				<xsl:variable name="rowNum" select="position()" />
				<xsl:for-each select="*">
					<xsl:if test="@columnSelection = 'true'">
						var comboCtl<xsl:value-of select="@name" /> = new AW.UI.Combo;
						
						comboCtl<xsl:value-of select="@name" />.setItemText(myActions);
						comboCtl<xsl:value-of select="@name" />.setItemCount(<xsl:value-of select="count(../../actionsTable/action)" />);
						comboCtl<xsl:value-of select="@name" />.onControlTextChanged = function(value) { 
							alert("test");
							option = comboCtl<xsl:value-of select="@name" />.getControlText();
							option = option.substring(0, option.indexOf(" -"));
							document.getElementById('<xsl:value-of select="@name" />').value = option;
							<xsl:if test="../../actionsTable/action/@key != 'null'" >
								doAction("<xsl:value-of select="../../actionsTable/action/@key" />");
							</xsl:if>
						};
				
						obj.setCellTemplate(comboCtl<xsl:value-of select="@name" />, 0, <xsl:value-of select="$rowNum - 1" />);				
					</xsl:if>
					
					<xsl:if test="(@type = 'field') and (not(@columnSelection = 'true'))">
						var editCtl<xsl:value-of select="@name" /> = new AW.UI.Input;
						
						editCtl<xsl:value-of select="@name" />.setControlValue = "<xsl:value-of select='.' />"
						editCtl<xsl:value-of select="@name" />.onControlValueChanged = function(value) {
							alert(value); 
							document.getElementById('<xsl:value-of select="@name" />').value = value;
						};
						
						obj.setCellTemplate(editCtl<xsl:value-of select="@name" />, <xsl:value-of select="position() -1" />, <xsl:value-of select="$rowNum - 1" />);				
					</xsl:if>
				</xsl:for-each>
			</xsl:for-each>

			
			obj.setCellEditable(false);								// disable editing
			obj.setColumnCount(<xsl:value-of select="count(row[1]/Title)" />);
			obj.setRowCount(<xsl:value-of select="count(row) - 1" />);
			obj.setRowHeight(<xsl:value-of select="$coefy" />);
			
			obj.setFixedLeft(0);
			
			obj.setControlSize(<xsl:value-of select="@width*$coefx + 24" />, <xsl:value-of select="(@height + @titleheight)*$coefy + 7" />);		// width, height
			obj.setControlPosition(<xsl:value-of select="@column*$coefx+$offsetx" />, <xsl:value-of select="(@line - @titleheight)*$coefy + $offsety - 6" />);	// left, top - adds 'position:absolute'
			
			obj.setSelectionMode("single-row");
			obj.onRowDoubleClicked	= function(event, index){ 
				window.status = "Row " + index + " double clicked"
				document.getElementById(comboArray[index]).value = <xsl:value-of select="actionsTable/action[1]/@char" />
				doAction("KEY_ENTER");
			};
			
			document.write(obj);
			
	</script>
	
	<xsl:for-each select="row[position() > 1]">
		<xsl:for-each select="*">
			<xsl:if test="@type = 'field'">
				<input type="hidden" name="{@name}" id ="{@name}" value="" />
			</xsl:if>		
		</xsl:for-each>
	</xsl:for-each>
	
	</xsl:template>
</xsl:stylesheet>
