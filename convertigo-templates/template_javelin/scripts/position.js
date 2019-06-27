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
 * $Workfile: position.js $
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

	var bResize = false;	
	
	var objectsResizabled= new Array();
	var nbElementRezisabled= 0;
	
	var originalFontSize= new Array();
	var nbFontSize= 0;
	function ElementRezisabled(){
		this.X= 0;
		this.width= 0;
		this.Y= 0;
		this.height= 0;
		this.obj= null;
	}
		
	function getOriginalFontSize(){
		originalFontSize= new Array();
		nbFontSize= 0;
		if(!document.styleSheets)return;
		for(var i=0;i<document.styleSheets.length;i++){
			var rules = document.styleSheets[i].rules ? document.styleSheets[i].rules : document.styleSheets[i].cssRules; // (IE 4+) : (Netscape 6)
			if(!rules)return;
			for(var j=0;j<rules.length;j++)	if(rules[j].style.fontSize)	originalFontSize[nbFontSize++] = rules[j].style.fontSize;
		}
	}

	/************************************************/
	/* calcule les positions initiales des objets   */
	/* les valeurs sont stock? dans les tableaux    */
	/* positionX et positionY                       */
	/* les indices correspondent ?'ordre donn?      */
	/* par le navigateur avec les ?ments suivants   */
	/* SPAN, INPUT, DIV, SELECT, BUTTON, TABLE, A   */
	/************************************************/
	function getPositions(){
		bResize = (document.getElementById("resize").value == "true")?true:false;
		if (!bResize) return;

		var type = ["SPAN","INPUT","TD","DIV","SELECT","BUTTON","TABLE","A"];
				
		objectsResizabled= new Array();
		nbElementRezisabled= 0;	
		var mydocument= document.getElementById("generated_page");
		for(var j=0;type[j];j++){
			var elt = mydocument.getElementsByTagName(type[j]);
			for(var i=0;elt[i];i++){
				objectsResizabled[nbElementRezisabled] = new ElementRezisabled();
				objectsResizabled[nbElementRezisabled].obj			= elt[i];
				objectsResizabled[nbElementRezisabled].X			 	= elt[i].style.left;
				objectsResizabled[nbElementRezisabled].Y		 		= elt[i].style.top;
				objectsResizabled[nbElementRezisabled].width	 	= elt[i].style.width;
				objectsResizabled[nbElementRezisabled].height 	= elt[i].style.height;
				nbElementRezisabled++;
			}
		}
	}
		
	/************************************************************************/
	/* positionne les contr? en fonction de la taille de la fen?e      */
	/* positionne la taille de la police en fonction de celle de la fen?e */
	/************************************************************************/	
	function resize(){
		if(!bResize)return;
		
		var sWidth		= parseInt(document.getElementById("screenWidth").value);
		var sHeight		= parseInt(document.getElementById("screenHeight").value);
		var ex_coefx	= parseInt(document.getElementById("coefx").value);
		var ex_coefy	= parseInt(document.getElementById("coefy").value);
		var offsetx		= parseInt(document.getElementById("offsetx").value);
		var offsetr		= parseInt(document.getElementById("offsetr").value);
		var offsety		= parseInt(document.getElementById("offsety").value);
		var scrollwidth		= parseInt(document.getElementById("scrollwidth").value);
		
		var winW;
		var winH;
		var coefx;
		var coefy;
		var scaleX = 1;
		var scaleY = 1;		
		
		if(document.body)	winW = document.body.clientWidth - document.body.scrollLeft;
		else	winW = window.innerWidth;
		
		
		
		coefx = (winW - offsetx - offsetr) / sWidth;
		coefy = coefx*2;
		
		if(coefx > ex_coefx)scaleX = coefx / ex_coefx;
		if(coefy > ex_coefy)scaleY = coefy / ex_coefy;
		
		//window.status = "Colonnes="+sWidth+" Lignes="+sHeight+" scaleX="+scaleX+" scaleY="+scaleY;

		for(var i=0;objectsResizabled[i];i++){
			var elt = objectsResizabled[i];
			if(elt.obj.style.top != "" && elt.obj.style.left != "" && elt.Y && elt.X){				
				elt.obj.style.top = Math.round( (parseInt(elt.Y) - offsety) * scaleY + offsety ) + "px";
				elt.obj.style.left= Math.round( (parseInt(elt.X) - offsetx) * scaleX + offsetx ) + "px";
			}
			if(elt.obj.style.height)	elt.obj.style.height	= Math.round( parseInt(elt.height) * scaleY ) + "px";
			if(elt.obj.style.width)		elt.obj.style.width		= Math.round( parseInt(elt.width) * scaleX ) + "px";
			
			if(elt.obj.id == "_ScRoLl_"){
				//alert("_ScRoLl_");
				if(elt.obj.style.width)		elt.obj.style.width		= Math.round( (parseInt(elt.width)-scrollwidth) * scaleX +scrollwidth) + "px";
			}	
		}
		
		var nbfs=0;
		if(document.styleSheets){
			for(var i=0;i<document.styleSheets.length;i++){
				var rules = document.styleSheets[i].rules ? document.styleSheets[i].rules : document.styleSheets[i].cssRules; // (IE 4+) : (Netscape 6)
				if(rules)
					for(var j=0;j<rules.length;j++)	if(rules[j].style.fontSize) rules[j].style.fontSize = Math.round( parseInt(originalFontSize[nbfs++]) * scaleY )+ "pt";
			}
		}
	}
	