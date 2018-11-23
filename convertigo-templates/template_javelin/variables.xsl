<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<!--- WARNING : if you modify these constants, display may be wrong -->
	<!-- Set this to false if you want to disable dynamic screen content resizing -->
	<xsl:variable name="resize">false</xsl:variable>
	
	<xsl:variable name="coefx">9.7</xsl:variable>
	<xsl:variable name="coefy">22</xsl:variable>
	
	<!-- Viewport left margin -->
	<xsl:param name="offsetx">100</xsl:param>
	<!-- Viewport right margin -->
	<xsl:param name="offsetr">22</xsl:param>
	<!-- Viewport top margin -->
	<xsl:param name="offsety">20</xsl:param>
	
	<!-- Width of a scroll bar -->
	<xsl:param name="scrollwidth">17</xsl:param>
	
	<!-- Width of icons (like  page up/down incons in table XSL template) -->
	<xsl:variable name="iconSize" select="16" />
	
	<!-- Panel width enlargement -->
	<xsl:param name="offsetw">10</xsl:param>
	<!-- Panel height enlargement -->
	<xsl:param name="offseth">10</xsl:param>
	
	<!--Set this value to true if you want to display the action key in action buttons-->
	<xsl:variable name="DisplayActionKey">false</xsl:variable>
	
	<!-- Set this value to true if you want to display action buttons outside of panels -->
	<xsl:variable name="DisplayDisabledButtons">false</xsl:variable>
	
	<!-- Set this to true if you want to gather action buttons in a panel on the left of the screen -->
	<!-- Note : requires offsetx (left margin) to be at least 110px  -->
	<xsl:variable name="GroupActionButtons">false</xsl:variable>
	
	<!-- Help keyword string -->
	<xsl:variable name="helpKeywordString">(F4)</xsl:variable>
</xsl:stylesheet>
