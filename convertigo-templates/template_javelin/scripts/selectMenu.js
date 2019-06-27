/*
 * Copyright (c) 1999-2004 TWinSoft sarl. All Rights Reserved.
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
 * $Workfile: selectMenu.js $
 * $Author: Davidm $
 * $Revision: 2 $
 * $Date: 22/01/07 9:02 $
 * $Modif: Davidm $
 * $Date: 22/01/07 9:02 $
 */

// JavaScript Document

/**********************/
/* VARIABLES GLOBALES */
/**********************/
if(!document.getElementById && document.all)
	document.getElementById = function(id) {
		return document.all[id];
	}
if(!document.getElementById && document.layer)
	document.getElementById = function(id) {
		return document.layer[id];
	}
var mySelectMenus= new Array();
var mySelectMenusOmbre= new Array();
var nbSelectMenu= 0;
var objSelectedForCurrentSelectMenu= null;
hasTableAction= true;
var currentMenu= "";
var blur= true;
	
/***********************/
/* FONCTIONS GENERALES */
/***********************/
function show_mySelectMenu(e, curField, menu) {
	if (isIE) {
		menu.style.left = (e.clientX + document.documentElement.scrollLeft) + "px";
		menu.style.top = (e.clientY + document.documentElement.scrollTop) + "px";
	}
	else {
		menu.style.left = e.pageX + "px";
		menu.style.top = e.pageY + "px";
	}
	menu.style.visibility="visible";
	objSelectedForCurrentSelectMenu = curField;

	return false;
}

function hide_mySelectMenu(menu) {
   if(blur)
      menu.style.visibility = "hidden";
}

function highlight(obj) {
	if (obj.className == "menuitems") {
		obj.style.backgroundColor = "#316AC5";
		obj.style.color = "white";
   }
   blur= false;
}
function lowlight(obj) {
	if (obj.className == "menuitems") {
		obj.style.backgroundColor = "";
		obj.style.color = "black";
   }
   blur= true;
}

function updateDataInObjSelected(data, menu) {
	if(objSelectedForCurrentSelectMenu != null) {
		if (data == undefined) {
         objSelectedForCurrentSelectMenu.value= '';
		}
		else {
         objSelectedForCurrentSelectMenu.value= data;
		}
      objSelectedForCurrentSelectMenu.focus();
	}
   blur = true;
	hide_mySelectMenu(menu);
}