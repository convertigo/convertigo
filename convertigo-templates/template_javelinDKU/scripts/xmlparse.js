/*
 * Copyright (c) 1999-2007 TWinSoft sarl. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of TWinSoft sarl.
 * The program(s) may  be used  and/or copied  only with the written
 * permission  of TWinSoft  sarl or in accordance with the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * TWinSoft  makes  no  representations  or  warranties  about   the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness  for  a particular purpose, or non-infringement. TWinSoft
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */

/*
 * $Workfile: xmlparse.js $
 * $Author: Nicolasa $
 * $Revision: 2 $
 * $Date: 22/02/08 10:29 $
 */

// mozXPath [http://km0ti0n.blunted.co.uk/mozxpath/] km0ti0n@gmail.com
// Code licensed under Creative Commons Attribution-ShareAlike License 
// http://creativecommons.org/licenses/by-sa/2.5/
if( document.implementation.hasFeature("XPath", "3.0") ) {
	
	XMLDocument.prototype.selectNodes = function(cXPathString, xNode) {
		if ( !xNode ) {
			xNode = this;
		} 

		var oNSResolver = this.createNSResolver(this.documentElement);
		var aItems = this.evaluate(cXPathString, xNode, oNSResolver, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
		var aResult = [];
		for (var i = 0; i < aItems.snapshotLength; i++)	{
			aResult[i] =  aItems.snapshotItem(i);
			
		}
		
		return aResult;
	}
	
	XMLDocument.prototype.selectSingleNode = function(cXPathString, xNode) {
		if(!xNode) {
			xNode = this;
		} 

		var xItems = this.selectNodes(cXPathString, xNode);
		if (xItems.length > 0) {
			return xItems[0];
		}
		else {
			return null;
		}
	}

	Element.prototype.selectNodes = function(cXPathString) {
		if (this.ownerDocument.selectNodes)	{
			return this.ownerDocument.selectNodes(cXPathString, this);
		}
		else {
			throw "For XML Elements Only";
		}
	}

	Element.prototype.selectSingleNode = function(cXPathString) {
		if (this.ownerDocument.selectSingleNode) {
			return this.ownerDocument.selectSingleNode(cXPathString, this);
		}
		else{
			throw "For XML Elements Only";
		}
	}
	
}

// Emulates IE xml dom method 'text' in FireFox
function getText(oNode) {
	if(isIE)return oNode.text;
	var sText = "";
	for (var i=0; i < oNode.childNodes.length; i++) {
		if (oNode.childNodes[i].hasChildNodes()) {
			sText += getText(oNode.childNodes[i]);
		}
		else {
			if (oNode.childNodes[i].nodeValue != null) {
				sText += oNode.childNodes[i].nodeValue;
			}
		}
	}
	return sText;
}
	
function getCurrentRowXml(col, lin, ind, separator) {
	var myXpath = "blocks/table[@column='" + col + "'][@line='" + lin +"']/row[@index='" + ind + "']/*";
	var myNodes = null;
	var sValues = "";
	myNodes = xmlDocument.documentElement.selectNodes(myXpath);
	for (var i=0; i < myNodes.length; i++) {
		sValues += getText(myNodes[i]) + separator;	
	}
	sValues = sValues.substring(0, (sValues.length-1));
	document.getElementById("table_values").value = sValues;
}
