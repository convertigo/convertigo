/*
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
 */

/**
 *  Autocomplete handlers for custom input
 */
//var n = ((navigator.userAgent.indexOf("MSIE") == -1) ? true:false);
var ns6 = document.getElementById && !document.all;

function getObject(oName,formName,inputName)
{
	var ob = null;
	switch (oName) {
		case "oInput":
			if (ns6)
				ob = document.getElementById(inputName);
			else  // ie4+
				ob = eval("document.javelin_form." + inputName);
			break;
		case "oList":
			if (ns6)
				ob = document.getElementById("List" + inputName);
			else // ie4+
				ob = eval("document.javelin_form.List" + inputName);
			break;
		case "odivList":
			if (ns6)
				ob = document.getElementById("div_List" + inputName);
			else // ie4+
				ob = eval("div_List" + inputName);
			break;
		default:
			break;
	}
	return ob;
}

function autoList_onclick(formName,inputName)
{
	var oInput, oList, selectedIndex, selectedValue;
	
	oInput = getObject("oInput",formName,inputName);
	oList = getObject("oList",formName,inputName);
	selectedIndex = oList.selectedIndex;
	if (selectedIndex != -1) {
		selectedValue = oList.options[selectedIndex].value;
		oInput.value = selectedValue;
	}
	oInput.focus();
	hideListBox(formName,inputName);
}
function autoInput_onkeydown(event,formName,inputName)
{
	var oInput, oList, odivList, selectedIndex, selectedValue, keyCode;
	
	keyCode = event.keyCode;

	oInput = getObject("oInput",formName,inputName);
	oList = getObject("oList",formName,inputName);
	odivList = getObject("odivList",formName,inputName);
	
	// ENTER
	if (keyCode == 13) {
		if (odivList.style.visibility == "visible") {
			event.cancelBubble = true;
			if (event.stopPropagation)
				event.stopPropagation();
		}
	}
	// ECHAP
	else if (keyCode == 27) {
		if (odivList.style.visibility == "visible") {
			event.cancelBubble = true;
			if (event.stopPropagation)
				event.stopPropagation();
		}
	}
	// TAB
	else if (keyCode == 9) {
		selectedIndex = oList.selectedIndex;
		typedValue = oInput.value;
		if (selectedIndex != -1) {
			selectedValue = oList.options[selectedIndex].value;
			oInput.value = selectedValue;
		}
		hideListBox(formName,inputName);
	}
	// LEFT
	else if (keyCode == 37) {
		hideListBox(formName,inputName);
	}
	// UP
	else if (keyCode == 38) {
		if (odivList.style.visibility == "visible") {
			if (oList.selectedIndex > 0)
				oList.selectedIndex--;
		}
	}
	//RIGHT
	else if (keyCode == 39) {
		//oList.selectedIndex = -1;
		showListBox(event,formName,inputName);
	}
	// DOWN
	else if (keyCode == 40) {
		if (odivList.style.visibility != "visible")
			showListBox(event,formName,inputName);
		else if (oList.selectedIndex < oList.options.length -1)
			oList.selectedIndex++;
	}
}
function autoInput_onkeyup(event,formName,inputName)
{
	var oInput, odivList, keyCode;
	
	keyCode = event.keyCode;

	oInput = getObject("oInput",formName,inputName);
	odivList = getObject("odivList",formName,inputName);

	if ((keyCode == 37) || (keyCode == 38) || (keyCode == 39) || (keyCode == 40))
		return true;
	if (odivList.style.visibility == "visible")
		search(formName,inputName,oInput.value);
	return true;
}
function autoInput_onkeypress(event,formName,inputName)
{
	var odivList, keyCode;

	keyCode = event.keyCode;
	
	odivList = getObject("odivList",formName,inputName);
	
	// ENTER
	if (keyCode == 13) {
		if (odivList.style.visibility == "visible") {
			autoList_onclick(formName,inputName);
			return false;
		}
	}
	// ECHAP
	else if (keyCode == 27) {
		if (odivList.style.visibility == "visible") {
			hideListBox(formName,inputName);
			return true;
		}
		return true;
	}
	
	showListBox(event,formName,inputName);

	return true;
}
function autoInput_ondblclick(event,formName,inputName)
{
	showListBox(event,formName,inputName);
}
function autoInput_onblur(event,formName,inputName)
{
	hideListBox(formName,inputName);
}
function search(formName,inputName,str)
{
	var oList, oOptions, option, i, bFound;
	
	oList = getObject("oList",formName,inputName);
	bFound = false;
	if (str != "") {
		oOptions = oList.options;
		for (i=0;i<oOptions.length;i++) {
			option = oOptions[i];
			if (option.text.toUpperCase().indexOf(str.toUpperCase()) == 0) {
				option.selected = true;
				bFound = true;
				break;
			}
		}
	}
	if (!bFound)
		oList.selectedIndex = -1;
}
function showListBox(event,formName,inputName)
{
	var odivList, oInput;
	var x, y;
	
	oInput = getObject("oInput",formName,inputName);
	oList = getObject("oList",formName,inputName);
	odivList = getObject("odivList",formName,inputName);
	
	if (oList.options.length == 0)
		return;

	if( ns6 ) {
		x = event.target.style.top ;
		y = event.target.style.left;
		odivList.style.top  = y+'px';
		odivList.style.left = x+'px';
		odivList.style.visibility = "visible";
 	} else { // ie4+
		x = event.srcElement.style.left;
		y = event.srcElement.offsetTop + event.srcElement.offsetHeight + "px";
		odivList.style.left = x;
		odivList.style.top = y;
		odivList.style.visibility = "visible";
	}
}
function hideListBox(formName,inputName)
{
	var odivList, oList;
	
	odivList = getObject("odivList",formName,inputName);
	oList = getObject("oList",formName,inputName);

	if (oList.options.length == 0)
		return;
	
	odivList.style.visibility = "hidden";
}
