<?xml version="1.0" encoding="ISO-8859-1"?>

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

<!DOCTYPE xsl:stylesheet [ <!ENTITY nbsp "&#160;"> ]>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:lxslt="http://xml.apache.org/xslt">

	<xsl:output method="html" media-type="text/html" encoding="UTF-8"/>
	
	<xsl:include href="status.xsl"/>

	<xsl:template match="document">
	<html>
	<head>
	<meta http-equiv="expires" content="0"/>
	<title><xsl:value-of select="title" /></title>
	
	<script src="../../scripts/scriptlib.js"></script>
	
  	</head>
      <body bgcolor="#FFF7DE">
	      	<script src="../../scripts/keymapvdx.js"></script>
			<table border="10">
		      <tr>
			        <td>
				        <form name="javelin_form" method="post" onsubmit="doAction('KEnvoi')">
				          <xsl:apply-templates />
				          <input type="hidden" name="__javelin_current_field" />
				          <input type="hidden" name="__javelin_action" />
				        </form>
				    </td>
				    <td>
				    	<table border="0">
				            <tr><input  type="image" href="#" src="../../images/BtnUp_Envoi.gif" onclick="doAction('KEnvoi')" /></tr>
				            <tr><input  type="image" href="#" src="../../images/BtnUp_Suite.gif" onclick="doAction('KSuite')" /></tr>
				            <tr><input  type="image" href="#" src="../../images/BtnUp_Retour.gif" onclick="doAction('KRetour')" /></tr>
				            <tr><input  type="image" href="#" src="../../images/BtnUp_Annul.gif" onclick="doAction('KAnnulation')" /></tr>
				            <tr><input  type="image" href="#" src="../../images/BtnUp_Rep.gif" onclick="doAction('KRepetition')" /></tr>
				            <tr><input  type="image" href="#" src="../../images/BtnUp_Cor.gif" onclick="doAction('KCorrection')" /></tr>
				            <tr><input  type="image" href="#" src="../../images/BtnUp_Som.gif" onclick="doAction('KSommaire')" /></tr>
				            <tr><input  type="image" href="#" src="../../images/BtnUp_Guide.gif" onclick="doAction('KGuide')" /></tr>
			            </table>
			         </td>
		      </tr>
	      </table>
      </body>
    </html>
	</xsl:template>

	<xsl:template match="blocks">
		<xsl:call-template name="handleLineOfBlocks">
			<xsl:with-param name="i" select="0"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="handleLineOfBlocks">
		<xsl:param name="i" select="0"/>
		<xsl:if test="$i &lt; 25">
			<table cellpadding="0" cellspacing="0" border="0">
				<tr>
					<xsl:for-each select="block[@line=$i]">
						<xsl:apply-templates select="."/>
					</xsl:for-each>
				</tr>
			</table>
			<xsl:call-template name="handleLineOfBlocks">
				<xsl:with-param name="i" select="$i+1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
  
	<xsl:template match="*[@type='static']">
		<xsl:choose>
			<xsl:when test="@foreground='green'">
				<td bgcolor='{./@background}'><font face="courier new" color="ltgreen"><xsl:value-of select="." /></font></td>
			</xsl:when>
			
			<xsl:otherwise>
	 			<td bgcolor='{./@background}'><font face="courier new" color="{@foreground}"><xsl:value-of select="." /></font></td>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
  
	<xsl:template match="*[@type='delimiters']">
 		<td bgcolor='{./@background}'><font face="courier new" color="{@foreground}"><xsl:value-of select="." /></font></td>
	</xsl:template>

 	<xsl:template match="*[@type='keyword']">
 		<td bgcolor='{./@background}'><font face="courier new" color="{@foreground}"><a href="#" onclick="return doAction('{./@action}')"><xsl:value-of select="."/></a></font></td>
	</xsl:template>
	
 	<xsl:template match="*[@type='menu']">
 		<td bgcolor='{./@background}'><font face="courier new" color="{@foreground}"><a href="#" onclick="return sendMenu('{.}')"><xsl:value-of select="@item"/></a></font></td>
	</xsl:template>
      
	<xsl:template match="*[@type='field']">
			<td bgcolor='{./@background}'>
			<xsl:if test="(@column + @size) &lt; 40">
		        <input name="{@name}" id="editField" ondblclick="doAction('KEnvoi')" type="text" size="{@size}" maxlength="{@size}" value="{.}" />
	        </xsl:if>
			<xsl:if test="(@column + @size) &gt; 39">
		        <input name="{@name}" id="editField" ondblclick="doAction('KEnvoi')" type="text" size="{40 -@column}" maxlength="{@size}" value="{.}" />
	        </xsl:if>
			<script>
				document.all.<xsl:value-of select="@name" />.focus();
	      	</script>
	    	</td>
	</xsl:template>
  
	<xsl:template match="statistics">
	    <table  style="position:absolute; top:30px; left:900px" border="2">
	        <tr>
	            <td>Task</td>
	            <td>Time</td>
	            <td>average</td>
	        </tr>
	        <xsl:apply-templates />
	    </table>
	</xsl:template>
  
	<xsl:template match="task">
		    <tr>
		        <td><xsl:value-of select="@name"    /></td>
		        <td><xsl:value-of select="@duration"/></td>
		        <td><xsl:value-of select="@average" /></td>
		    </tr>
	</xsl:template>

    <xsl:template match="*[@type='table']">
      <table class="data"   border="1">
        <tr>
          <td>
            <table border="0" width="100%" height="100%">
                 <xsl:apply-templates />
            </table>
          </td>
        </tr>
      </table>
    </xsl:template>
    
  	<xsl:template match="row" priority="1">
  		<tr>
          <xsl:apply-templates />
        </tr>
  	</xsl:template>
  	
  	<xsl:template match="item" priority="1">
          <td class="dataitem">
  			<xsl:choose>
  				<xsl:when test='(@type = "field")'>
  	    		          <input name="{@name}" id="{@name}"  type="text" />      
  				</xsl:when>
  				<xsl:otherwise>
  				     <xsl:if test="string-length(.) = 0">
  				       &#160;
  				     </xsl:if>
    				     <xsl:if test="string-length(.) != 0">
      				  <xsl:value-of select="." />
  				     </xsl:if>
  				</xsl:otherwise>
  			</xsl:choose>
          </td>
    </xsl:template>
    
  	<xsl:template match="Title" priority="1">
        <td class="datatitle">
              <xsl:apply-templates />
        </td>
    </xsl:template>
  
</xsl:stylesheet>
