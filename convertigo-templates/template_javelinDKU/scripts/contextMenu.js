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
 * $Workfile: contextMenu.js $
 * $Author: Davidm $
 * $Revision: 1 $
 * $Date: 20/06/06 16:05 $
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
	
/***********************/
/* FONCTIONS GENERALES */
/***********************/
function toggleMenu(e){
	if((e.button == 2) ||(e.keyCode == 93)) return show_myContextmenu(e);
	else hide_myContextmenu();
}
	
function show_myContextmenu(e) {
	if(!e) e= event;
	var rightedge = document.body.clientWidth- e.clientX;
	var bottomedge = document.body.clientHeight- e.clientY;
	if (rightedge < document.getElementById('ie5menu').offsetWidth)
		document.getElementById('ie5menu').style.left = document.body.scrollLeft + e.clientX - document.getElementById('ie5menu').offsetWidth;
	else
		document.getElementById('ie5menu').style.left = document.body.scrollLeft + e.clientX;
	document.getElementById('ie5menuOmbre').style.left = parseInt(document.getElementById('ie5menu').style.left) + 2;
	if (bottomedge < document.getElementById('ie5menu').offsetHeight)
		document.getElementById('ie5menu').style.top = document.body.scrollTop + e.clientY - document.getElementById('ie5menu').offsetHeight;
	else
		document.getElementById('ie5menu').style.top = document.body.scrollTop + e.clientY;
	document.getElementById('ie5menuOmbre').style.top = parseInt(document.getElementById('ie5menu').style.top) + 1;
	document.getElementById('ie5menu').style.visibility = "visible";
	document.getElementById('ie5menuOmbre').style.visibility = "visible";
	return false;
}
function hide_myContextmenu() {
	document.getElementById('ie5menu').style.visibility = "hidden";
	document.getElementById('ie5menuOmbre').style.visibility = "hidden";
}
function highlight(obj) {
	if (obj.className == "menuitems") {
		obj.style.backgroundColor = "#316AC5";
		obj.style.color = "white";
		window.status = obj.url;
   }
}
function lowlight(obj) {
	if (obj.className == "menuitems") {
		obj.style.backgroundColor = "";
		obj.style.color = "black";
		window.status = "";
   }
}

function InitContextMenu(){
	var code="";
	code='<DIV class="skin1" style="LEFT: 5px; POSITION: absolute; TOP: 5px;">';
	code+='<table id="ie5menu" class="skin1" cellpadding="0" cellspacing="0">';
	code+='	<tr><td class="menuitems" ';
	code+='		onClick="showBigKeyboard();" ';
	code+='		onMouseover="highlight(this)"';
	code+='		onMouseout="lowlight(this)"';
	code+='		url="Cette propriété affiche le clavier du terminal">Afficher le clavier</td></tr>';
	code+='	<tr><td height="4" align="center"><img width="95%" height="4" align="middle" valign="top" src="../../images/separator.gif"></td></tr>';
	code+='	<tr><td class="menuitems" ';
	code+='		onClick="window.print();"  ';
	code+='		onMouseover="highlight(this)"';
	code+='		onMouseout="lowlight(this)"';
	code+='		url="Cette propriété vous permet d\'imprimer la page en cours">Imprimer</td></tr>';
	code+='	<tr><td height="4" align="center"><img width="95%" height="4" align="middle" valign="top" src="../../images/separator.gif"></td></tr>';
	code+='	<tr><td class="menuitems" ';
	code+='		onClick="window.close();" ';
	code+='		onMouseover="highlight(this)"';
	code+='		onMouseout="lowlight(this)"';
	code+='		url="Cette propriété ferme la fenêtre du navigateur">Fermer la fenêtre</td></tr>';
	code+='</table>';
	code+='</DIV>';
	code+='<DIV class="ombre" style="LEFT: 5px; POSITION: absolute; TOP: 5px;">';
	code+='<table id="ie5menuOmbre" class="ombre" cellpadding="0" cellspacing="0">';
	code+='	<tr><td class="menuitems">Afficher le clavier</td></tr>';
	code+='	<tr><td height="4" align="center"><img width="95%" height="4" align="middle" valign="top" src="../../images/separator.gif"></td></tr>';
	code+='	<tr><td class="menuitems">Imprimer</td></tr>';
	code+='	<tr><td height="4" align="center"><img width="95%" height="4" align="middle" valign="top" src="../../images/separator.gif"></td></tr>';
	code+='	<tr><td class="menuitems">Fermer la fenêtre</td></tr>';
	code+='</table>';
	code+='</DIV>';
	code+='<script language="JavaScript1.2">';
	code+='if (document.all){';
	code+='document.oncontextmenu = show_myContextmenu;';
	code+='document.onclick = hide_myContextmenu;';
	code+='}';
	code+='else{';
	code+='document.onclick = toggleMenu;';
	code+='document.onkeypress = toggleMenu;';
	code+='}';
	code+='</script>';
	return code;
}
