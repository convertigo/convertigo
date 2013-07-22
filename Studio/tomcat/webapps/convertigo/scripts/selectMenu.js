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
   menu.style.top = e.clientY;
	menu.style.left = e.clientX;
	objSelectedForCurrentSelectMenu = curField;
	menu.style.visibility="visible";
	return false;
   /*
    if(!e) e= event;
      var rightedge = document.body.clientWidth- e.clientX;
      var bottomedge = document.body.clientHeight- e.clientY;
      if (rightedge < document.getElementById(menu).offsetWidth)
         document.getElementById(menu).style.left = document.body.scrollLeft + e.clientX - document.getElementById(menu).offsetWidth;
      else
         document.getElementById(menu).style.left = document.body.scrollLeft + e.clientX;
      document.getElementById(menu + 'Ombre').style.left = parseInt(document.getElementById(menu).style.left) + 2;
	if (bottomedge < document.getElementById(menu).offsetHeight)
		document.getElementById(menu).style.top = document.body.scrollTop + e.clientY - document.getElementById(menu).offsetHeight;
	else
		document.getElementById(menu).style.top = document.body.scrollTop + e.clientY;
	document.getElementById(menu + 'Ombre').style.top = parseInt(document.getElementById(menu).style.top) + 1;
	document.getElementById(menu).style.visibility = "visible";
	document.getElementById(menu + 'Ombre').style.visibility = "visible";
	currentMenu= menu;
	objSelectedForCurrentSelectMenu= obj;
         return false;
         */
}

function hide_mySelectMenu(menu) {
   if(blur)
      menu.style.visibility = "hidden";
   
}

function blur_mySelectMenu(menu) {
   if(blur){
		document.getElementById(menu).style.visibility = "hidden";
		document.getElementById(menu + 'Ombre').style.visibility = "hidden";
	}
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

function updateDataInObjSelected(data, menu){
	if(objSelectedForCurrentSelectMenu != null){
		if (data == undefined)
         objSelectedForCurrentSelectMenu.value= '';
		else
         objSelectedForCurrentSelectMenu.value= data;
      objSelectedForCurrentSelectMenu.focus();
	}
   blur = true;
	hide_mySelectMenu(menu);
}

function BeginSelect(menu){
	var code="";
	code='<DIV class="skin1" style="LEFT: 5px; POSITION: absolute; TOP: 5px; z-index:10">';
	code+='<table id="' + menu + '" class="skin1" cellpadding="0" cellspacing="0">';
	mySelectMenus[nbSelectMenu]= code;
	
	code= "";
	code+='<DIV class="ombre" style="LEFT: 5px; POSITION: absolute; TOP: 5px; z-index:9">';
	code+='<table id="' + menu + 'Ombre" class="ombre" cellpadding="0" cellspacing="0">';
	mySelectMenusOmbre[nbSelectMenu]= code;
}

function AddSelect(chr, key, label){
	var code="";
	
	if (key == '')
		key ='KEY_ENTER';
		
	code+='	<tr><td class="menuitems" ';
	code+='		onClick="updateDataInObjSelected(\'' + chr + '\');';
	if(key != 'null') code+= 'return doAction(\''+ key + '\');" ';
	else code+= '"';
	code+='		onMouseover="highlight(this)"';
	code+='		onMouseout="lowlight(this)"';
	code+='		url="chr:' + chr + ', key:' + key + ', label:' + label + '">' + label + ' (' + chr + ')</td></tr>';
	mySelectMenus[nbSelectMenu]+= code;
	
	code= "";
	code+='	<tr><td class="menuitems">' + label + '</td></tr>';
	mySelectMenusOmbre[nbSelectMenu]+= code;
}

function EndSelect(){
	var code="";
	code+='</table>';
	code+='</DIV>';
	mySelectMenus[nbSelectMenu]+= code;
	mySelectMenusOmbre[nbSelectMenu]+= code;
	nbSelectMenu++;
}

function writeSelectMenus(){
	for(i= 0; i < nbSelectMenu; i++){
		document.write(mySelectMenus[i]);
		document.write(mySelectMenusOmbre[i]);
	}
}


function show_mySelectMenu2(e, curField, menu) {
   menu.style.top = e.clientY;
	menu.style.left = e.clientX;
	objSelectedForCurrentSelectMenu = curField;
	menu.style.visibility="visible";
	return false;
}


function hide_mySelectMenu2(menu) {
   if(blur)
      menu.style.visibility = "hidden";
}

function updateDataInObjSelected2(data, menu){
	if(objSelectedForCurrentSelectMenu != null){
		if (data == undefined)
         objSelectedForCurrentSelectMenu.value= '';
		else
         objSelectedForCurrentSelectMenu.value= data;
      objSelectedForCurrentSelectMenu.focus();
	}
   blur = true;
	hide_mySelectMenu2(menu);
}
