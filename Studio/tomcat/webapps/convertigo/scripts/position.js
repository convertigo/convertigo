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

	if(!document.getElementById && document.all)
		document.getElementById = function(id) {
			return document.all[id];
		}
	if(!document.getElementById && document.layer)
		document.getElementById = function(id) {
			return document.layer[id];
		}

	document.getSnaClassCSS= function(name) {
		if(document.styleSheets.length == 0)
			return null;
		nbStyleSheetsVisited= 0;
		while(nbStyleSheetsVisited < document.styleSheets.length) {
			if(document.styleSheets[nbStyleSheetsVisited].cssRules)// (Netscape 6)
				var rules= document.styleSheets[nbStyleSheetsVisited].cssRules;
			if(document.styleSheets[nbStyleSheetsVisited].rules)// (IE 4+) 
				var rules= document.styleSheets[nbStyleSheetsVisited].rules;
			nbStyleSheetsVisited++;
			i= 0;
			while(i < rules.length && rules[i].selectorText.toLowerCase() != name.toLowerCase()) { // Il y a des diff?rences de case entre ie et ns
				i++;
			}
			if(i < rules.length)
				return rules[i];
		}
		return null;
	}

	var bResize = true;	
	var border = 5;
	var offsetLeft= 10;
	var offsetRight= 30;
	var offsetBottom= 10;
	var offsetTop= 10;
	var bottomMargin= 10;
	var footer= 70;
	var windowSizeW= (80*fontSize) + border + offsetLeft + offsetRight + border;
	var windowSizeH= (25*coefy) + border + offsetTop + offsetBottom + bottomMargin + footer + border;
	var screenH = 15;
	var screenW = 80;
	
	var formerSizeFont= fontSize;	
	
	var objectsResizabled= new Array();
	var nbElementRezisabled= 0;

	// initialize window size, subject to change for 132 column screens
	setWindowSize(80, 25);

	function ElementRezisabled(){
		this.X= 0;
		this.width= 0;
		this.Y= 0;
		this.height= 0;
		this.obj= null;
		this.type= '';
		this.fontSize= 0;
	}

	function setWindowSize(width, height)
	{
		screenW = width;
		screenH = height;
		windowSizeW= (width*fontSize) + border + offsetLeft + offsetRight + border;
		windowSizeH= (height*coefy) + border + offsetTop + offsetBottom + bottomMargin + footer + border;
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
		if (document.getElementsByTagName) {
			var mydocument= document.getElementById("generated_page");
			var elt;
			nbElementRezisabled= 0;
			
			elt= mydocument.getElementsByTagName("SPAN");
			i= 0;			
			while( elt[i] ){			
				if(elt[i].style.top != "" && elt[i].style.left != ""){
					objectsResizabled[nbElementRezisabled]= new ElementRezisabled();
					objectsResizabled[nbElementRezisabled].obj= elt[i];
					objectsResizabled[nbElementRezisabled].type= "SPAN";				
					objectsResizabled[nbElementRezisabled].Y= (parseInt(elt[i].style.top) - offsety)/coefy;					
					objectsResizabled[nbElementRezisabled].X= (parseInt(elt[i].style.left) - offsetx)/coefx;	
					if(elt[i].style.height != "" && elt[i].style.width != ""){		
						objectsResizabled[nbElementRezisabled].height= (parseInt(elt[i].style.height) - offseth)/coefy;					
						objectsResizabled[nbElementRezisabled].width= (parseInt(elt[i].style.width) - offsetw)/coefx;
					}
					nbElementRezisabled++;
				}
				i++;
			}
			elt= mydocument.getElementsByTagName("INPUT");
			i= 0;			
			while( elt[i] ){	
				objectsResizabled[nbElementRezisabled]= new ElementRezisabled();
				objectsResizabled[nbElementRezisabled].obj= elt[i];
				objectsResizabled[nbElementRezisabled].type= "INPUT";	
				if(elt[i].style.top != "" && elt[i].style.left != ""){				
					objectsResizabled[nbElementRezisabled].Y= (parseInt(elt[i].style.top) - offsety)/coefy;					
					objectsResizabled[nbElementRezisabled].X= (parseInt(elt[i].style.left) - offsetx)/coefx;
				}
				if(elt[i].style.height != ""){				
					objectsResizabled[nbElementRezisabled].height= (parseInt(elt[i].style.height) + 1)/coefy;
				}
				if(elt[i].style.width != ""){				
					objectsResizabled[nbElementRezisabled].width= (parseInt(elt[i].style.width) + 1)/coefx;
				}
				nbElementRezisabled++;
				i++;
			}
			elt= mydocument.getElementsByTagName("TD");
			i= 0;			
			while( elt[i] ){	
				objectsResizabled[nbElementRezisabled]= new ElementRezisabled();
				objectsResizabled[nbElementRezisabled].obj= elt[i];
				objectsResizabled[nbElementRezisabled].type= "TD";	
				/*
				if(elt[i].style.height != ""){
					objectsResizabled[nbElementRezisabled].height= (parseInt(elt[i].style.height) + 1) / coefy;
				}
				*/
				if(elt[i].style.width != ""){				
					objectsResizabled[nbElementRezisabled].width= (parseInt(elt[i].style.width) + 1) / coefx;
				}
				nbElementRezisabled++;
				i++;
			}
			elt= mydocument.getElementsByTagName("DIV");
			i= 0;			
			while( elt[i] ){
				objectsResizabled[nbElementRezisabled]= new ElementRezisabled();
				objectsResizabled[nbElementRezisabled].obj= elt[i];
				objectsResizabled[nbElementRezisabled].type= "DIV";
				if(elt[i].style.top != "" && elt[i].style.left != ""){				
					objectsResizabled[nbElementRezisabled].Y= (parseInt(elt[i].style.top) - offsety)/coefy;					
					objectsResizabled[nbElementRezisabled].X= (parseInt(elt[i].style.left) - offsetx)/coefx;
				}
				if(elt[i].style.height != ""){				
					objectsResizabled[nbElementRezisabled].height= (parseInt(elt[i].style.height) + 1)/coefy;
				}
				if(elt[i].style.width != ""){				
					objectsResizabled[nbElementRezisabled].width= (parseInt(elt[i].style.width) + 1)/coefx;
				}
				nbElementRezisabled++;
				i++;
			}
			elt= mydocument.getElementsByTagName("SELECT");
			i= 0;			
			while( elt[i] ){
				objectsResizabled[nbElementRezisabled]= new ElementRezisabled();
				objectsResizabled[nbElementRezisabled].obj= elt[i];
				objectsResizabled[nbElementRezisabled].type= "SELECT";
				if(elt[i].style.top != "" && elt[i].style.left != ""){				
					objectsResizabled[nbElementRezisabled].Y= (parseInt(elt[i].style.top) - offsety)/coefy;					
					objectsResizabled[nbElementRezisabled].X= (parseInt(elt[i].style.left) - offsetx)/coefx;
				}
				if(elt[i].style.height != ""){				
					objectsResizabled[nbElementRezisabled].height= (parseInt(elt[i].style.height) + 1)/coefy;
				}
				if(elt[i].style.width != ""){				
					objectsResizabled[nbElementRezisabled].width= (parseInt(elt[i].style.width) + 1)/coefx;
				}
				nbElementRezisabled++;
				i++;
			}
			elt= mydocument.getElementsByTagName("BUTTON");		
			i= 0;			
			while( elt[i] ){			
				if(elt[i].style.top != "" && elt[i].style.left != ""){
					objectsResizabled[nbElementRezisabled]= new ElementRezisabled();
					objectsResizabled[nbElementRezisabled].obj= elt[i];
					objectsResizabled[nbElementRezisabled].type= "BUTTON";				
					objectsResizabled[nbElementRezisabled].Y= (parseInt(elt[i].style.top) - offsety)/coefy;					
					objectsResizabled[nbElementRezisabled].X= (parseInt(elt[i].style.left) - offsetx)/coefx;
					if(elt[i].style.height != ""){				
						objectsResizabled[nbElementRezisabled].height= (parseInt(elt[i].style.height) + 1)/coefy;
					}
					if(elt[i].style.width != ""){				
						objectsResizabled[nbElementRezisabled].width= (parseInt(elt[i].style.width) + 1)/coefx;
					}
					nbElementRezisabled++;
				}
				i++;
			}
			elt= mydocument.getElementsByTagName("TABLE");		
			i= 0;			
			while( elt[i] ){			
				if(elt[i].style.top != "" && elt[i].style.left != ""){
					objectsResizabled[nbElementRezisabled]= new ElementRezisabled();
					objectsResizabled[nbElementRezisabled].obj= elt[i];
					objectsResizabled[nbElementRezisabled].type= "TABLE";				
					objectsResizabled[nbElementRezisabled].Y= (parseInt(elt[i].style.top) - offsety)/coefy;					
					objectsResizabled[nbElementRezisabled].X= (parseInt(elt[i].style.left) - offsetx)/coefx;	
					if(elt[i].style.height != ""){
						objectsResizabled[nbElementRezisabled].height= (parseInt(elt[i].style.height) - offseth)/coefy;
					}
					if(elt[i].style.width != "") {
						objectsResizabled[nbElementRezisabled].width= (parseInt(elt[i].style.width) - offsetw)/coefx;
					}
					nbElementRezisabled++;
				}
				i++;
			}
			elt= mydocument.getElementsByTagName("A");		
			i= 0;			
			while( elt[i] ){			
				if(elt[i].style.lineHeight != ""){
					objectsResizabled[nbElementRezisabled]= new ElementRezisabled();
					objectsResizabled[nbElementRezisabled].obj= elt[i];
					objectsResizabled[nbElementRezisabled].type= "A";
					nbElementRezisabled++;
				}
				i++;
			}
		}
	}
		
	/************************************************************************/
	/* positionne les contr? en fonction de la taille de la fen?e      */
	/* positionne la taille de la police en fonction de celle de la fen?e */
	/************************************************************************/
	function resize(){
		var mydocument= document.getElementById("generated_page");
		var winH, winW;
		if(document.body.offsetWidth && document.body.offsetHeight){
			winW = document.body.offsetWidth - document.body.scrollLeft;
			winH = document.body.offsetHeight - document.body.scrollTop;
		}
		if(window.innerHeight && window.innerWidth){
			winW = window.innerWidth - document.body.scrollLeft;
			winH = window.innerHeight - document.body.scrollTop;		
		}
		
		var pointW;
		var pointH;
		var newSizeFont;
		
		if (bResize == true) {
			pointW= (winW - (border + offsetLeft + offsetRight + border))/ screenW;
			pointH= (winH - (border + offsetTop + offsetBottom + bottomMargin + 10 + footer + border)) / screenH;
			newSizeFont= parseInt((winW/windowSizeW)*fontSize);
		} else {
			pointW= 8;
			pointH= 10;
			newSizeFont = fontSize;
		}
		
		
		/* Verification de non-recouvrement */
		if( newSizeFont < fontSize ){
			newSizeFont= fontSize;
			winW= parseInt(newSizeFont*windowSizeW/fontSize);
			pointW= (winW - (border + offsetLeft + offsetRight + border)) / screenW;
		}
		if(pointH < newSizeFont + fontSize){
			winH= (newSizeFont+fontSize)*26 + (border + offsetTop + offsetBottom + bottomMargin + 10 + footer + border);
			pointH= (winH - (border + offsetTop + offsetBottom + bottomMargin + 10 + footer + border)) / screenH;
		}
		
		/* decalage des objets */
		var indice= 0;
		var i= 0;
		
		var elt= document.getElementById("cadreH");
		if (elt != null) {
			elt.height= (winH - (border + footer + border));
			elt.width= 9;
		}
		
		elt= document.getElementById("cadreW");		
		if (elt != null) {
			elt.width= winW - (border + 10 + 25 + 10 + border);
			elt.height= 9;
		}
	
		if((newSizeFont != formerSizeFont) && (bResize == true)){
			if((SnaClassCSS= document.getSnaClassCSS(".data")) != null){
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
				SnaClassCSS.style.lineHeight= pointH - 1 + 'px';
			}
			
			if((SnaClassCSS= document.getSnaClassCSS("A.menuobjectsResizabled:hover")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("A.menuobjectsResizabled:link")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("A.menuobjectsResizabled:visited")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("A.menuobjectsResizabled:active")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
				
			if((SnaClassCSS= document.getSnaClassCSS("A.menuItem:hover")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("A.menuItem:link")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("A.menuItem:visited")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("A.menuItem:active")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
				
			if((SnaClassCSS= document.getSnaClassCSS(".fixed")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS(".datatitle")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS(".keywordbutton")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("TD")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("TABLE")) != null)
				SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
			if((SnaClassCSS= document.getSnaClassCSS("SPAN")) != null)
				if (SnaClassCSS.style.fontSize != "")
					SnaClassCSS.style.fontSize= parseInt(SnaClassCSS.style.fontSize) + (newSizeFont-formerSizeFont) + 'pt';
		}

		for(i=0 ; i < nbElementRezisabled ; i++){
			if(objectsResizabled[i].type == "A")
				objectsResizabled[i].obj.style.lineHeight= pointH - 1 + 'px';
			else {
				// Positionnement de l'objet
				objectsResizabled[i].obj.style.top = objectsResizabled[i].Y*pointH + border + offsetTop;
				objectsResizabled[i].obj.style.left= objectsResizabled[i].X*pointW + border + offsetLeft;
			
				// Hauteur de l'objet
				if(objectsResizabled[i].height != 0)
					objectsResizabled[i].obj.style.height= objectsResizabled[i].type == "INPUT" ? objectsResizabled[i].height*pointH - 1 : objectsResizabled[i].height*pointH + offseth;

				// Largeur de l'objet
				if(objectsResizabled[i].width != 0)
					objectsResizabled[i].obj.style.width= objectsResizabled[i].width*pointW + offsetw;


				// Taille de la police de l'objet
				if(objectsResizabled[i].obj.style.fontSize != "")
					objectsResizabled[i].obj.style.fontSize= parseInt(objectsResizabled[i].obj.style.fontSize) + (newSizeFont-formerSizeFont) + "pt";
			}
		}
		formerSizeFont= newSizeFont;
	}