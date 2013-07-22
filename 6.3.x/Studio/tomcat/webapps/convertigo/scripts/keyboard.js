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

/* LINKS : button.js
 *         cookies.js
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
	
var KEYBOARD_MINIMIZE= 0;
var KEYBOARD_MOVABLE= 1;
var KEYBOARD_SMALL= 2;
var KEYBOARD_CLOSE= 3;
var keyboardStatus= KEYBOARD_MINIMIZE;
var offsetLeft= 0;
var mykeyboard_menu= null;
var mykeyboard_menu_small= null;
var javelin_formElements  =new Array();
	
/*************************************/
/* FONCTIONS DE CREATION DU KEYBOARD */
/*************************************/
	function initKeyboard(){
		var item= new Array(33);
		for(i= 0 ; i < 33 ; i++) item[i]= new Button();
		item[0].name= 'SysReq';
		item[0].action= 'doAction(\'KEY_SYSREQ\')';
		item[1].name= 'Page&nbsp;UP';
		item[1].action= 'doAction(\'KEY_ROLLUP\')';
		item[2].name= 'Page&nbsp;DOWN';
		item[2].action= 'doAction(\'KEY_ROLLDOWN\')';
		item[3].name= 'En	ter';
		item[3].action= 'doAction(\'KEY_ENTER\')';
		item[4].name= 'PF1';
		item[4].action= 'doAction(\'KEY_PF1\')';
		item[5].name= 'PF2';
		item[5].action= 'doAction(\'KEY_PF2\')';
		item[6].name= 'PF3';
		item[6].action= 'doAction(\'KEY_PF3\')';
		item[7].name= 'PF4';
		item[7].action= 'doAction(\'KEY_PF4\')';
		item[8].name= 'PF5';
		item[8].action= 'doAction(\'KEY_PF5\')';
		item[9].name= 'PF6';
		item[9].action= 'doAction(\'KEY_PF6\')';
		item[10].name= 'PF7';
		item[10].action= 'doAction(\'KEY_PF7\')';
		item[11].name= 'PF8';
		item[11].action= 'doAction(\'KEY_PF8\')';
		item[12].name= 'PF9';
		item[12].action= 'doAction(\'KEY_PF9\')';
		item[13].name= 'PF10';
		item[13].action= 'doAction(\'KEY_PF10\')';
		item[14].name= 'PA1';
		item[14].action= 'doAction(\'KEY_PA1\')';
		item[15].name= 'PA2';
		item[15].action= 'doAction(\'KEY_PA2\')';
		item[16].name= 'PF11';
		item[16].action= 'doAction(\'KEY_PF11\')';
		item[17].name= 'PF12';
		item[17].action= 'doAction(\'KEY_PF12\')';
		item[18].name= 'PF13';
		item[18].action= 'doAction(\'KEY_PF13\')';
		item[19].name= 'PF14';
		item[19].action= 'doAction(\'KEY_PF14\')';
		item[20].name= 'PF15';
		item[20].action= 'doAction(\'KEY_PF15\')';
		item[21].name= 'PF16';
		item[21].action= 'doAction(\'KEY_PF16\')';
		item[22].name= 'PF17';
		item[22].action= 'doAction(\'KEY_PF17\')';
		item[23].name= 'PF18';
		item[23].action= 'doAction(\'KEY_PF18\')';
		item[24].name= 'PF19';
		item[24].action= 'doAction(\'KEY_PF19\')';
		item[25].name= 'PF20';
		item[25].action= 'doAction(\'KEY_PF20\')';
		item[26].name= 'PF21';
		item[26].action= 'doAction(\'KEY_PF21\')';
		item[27].name= 'PF22';
		item[27].action= 'doAction(\'KEY_PF22\')';
		item[28].name= 'PF23';
		item[28].action= 'doAction(\'KEY_PF23\')';
		item[29].name= 'PF24';
		item[29].action= 'doAction(\'KEY_PF24\')';
		item[30].name= 'PA3';
		item[30].action= 'doAction(\'KEY_PA3\')';
		item[31].name= 'R?actualiser';
		item[31].action= 'refresh()';
		item[32].name= 'Attn';
		item[32].action= 'doAction(\'KEY_ATTN\')';
		return item;
	}	
	
	function createKeyboard(){
		var nbElementPerLine= 12;
		var item= initKeyboard();		
		return createBar(item, nbElementPerLine);
	}
	
	function initKeyboardBull(){
		var item= new Array(16);
		for(i= 0 ; i < 16 ; i++) item[i]= new Button();
		item[0].name= 'FKC1';
		item[0].action= 'doAction(\'FKC01\')';
		item[1].name= 'FKC2';
		item[1].action= 'doAction(\'FKC02\')';
		item[2].name= 'FKC3';
		item[2].action= 'doAction(\'FKC03\')';
		item[3].name= 'FKC4';
		item[3].action= 'doAction(\'FKC04\')';
		item[4].name= 'FKC5';
		item[4].action= 'doAction(\'FKC05\')';
		item[5].name= 'FKC6';
		item[5].action= 'doAction(\'FKC06\')';
		item[6].name= 'Home';
		item[6].action= 'doAction(\'CURHOME\')';
		item[7].name= '&nbsp;';
		item[7].action= '';
		item[8].name= 'FKC7';
		item[8].action= 'doAction(\'FKC07\')';
		item[9].name= 'FKC8';
		item[9].action= 'doAction(\'FKC08\')';
		item[10].name= 'FKC9';
		item[10].action= 'doAction(\'FKC09\')';
		item[11].name= 'FKC10';
		item[11].action= 'doAction(\'FKC10\')';
		item[12].name= 'FKC11';
		item[12].action= 'doAction(\'FKC11\')';
		item[13].name= 'FKC12';
		item[13].action= 'doAction(\'FKC12\')';
		item[14].name= 'Break';
		item[14].action= 'doAction(\'BREAK\')';
		
		item[15].name= 'Enter';
		item[15].action= 'doAction(\'XMIT\')';
		return item;
	}	
	
	function createKeyboardBull(){
		var nbElementPerLine= 8;
		var item= initKeyboardBull();		
		return createBar(item, nbElementPerLine);
	}	
	
	
	
	function keyboard_initVar(){
		mykeyboard_menu= document.getElementById('keyboard_menu');
		mykeyboard_menu_small= document.getElementById('keyboard_menu_small');
		
		// r?cup?ration des donn?es du cookie
		var posX= getCookie("keyboard_posX");
		var posY= getCookie("keyboard_posY");
		var status= getCookie("keyboard_status");
		if(posX != null && posY != null && status != null){
			positionLayer(parseInt(posX.substring(0, posX.indexOf('p'))), parseInt(posY.substring(0, posY.indexOf('p'))));
			if(status == "KEYBOARD_MOVABLE")
				showBigKeyboard();
			else if(status == "KEYBOARD_SMALL")
				showSmallKeyboard();
			else if(status == "KEYBOARD_CLOSE")
				closeKeyboard();
		}
		else {
			setCookie("keyboard_posX", "0");
			setCookie("keyboard_posY", "0");
			setCookie("keyboard_status", "KEYBOARD_MINIMIZE");
		}
	}
	
	function keyboard_setCookie(){
		// modification du cookie
		setCookie("keyboard_posX", mykeyboard_menu.style.left);
		setCookie("keyboard_posY", mykeyboard_menu.style.top);
		if(keyboardStatus == KEYBOARD_MOVABLE)
			setCookie("keyboard_status", "KEYBOARD_MOVABLE");
		else if(keyboardStatus == KEYBOARD_SMALL)
			setCookie("keyboard_status", "KEYBOARD_SMALL");
		else if(keyboardStatus == KEYBOARD_CLOSE)
			setCookie("keyboard_status", "KEYBOARD_CLOSE");
		else if(keyboardStatus == KEYBOARD_MINIMIZE)
			setCookie("keyboard_status", "KEYBOARD_MINIMIZE");
	}


/****************************************/
/* FONCTIONS DE DEPLACEMENT DU KEYBOARD */
/****************************************/
	var mousePressed= false;
	var lastPosXofMouse;
	var lastPosYofMouse;
	
	function initPos(e){
		if(!e) e= event;
		//R?cup?ration de la position de la souris
		lastPosXofMouse= e.clientX;
		lastPosYofMouse= e.clientY;
	}
	
	function moveKeyboard(e){
		if(mousePressed){
			if(!e) e= event;
			// Calcul de l'?cart de position de la souris
			var difX= e.clientX - lastPosXofMouse;
			var difY= e.clientY - lastPosYofMouse;
			if((difX != 0 || difY != 0) && keyboardStatus == KEYBOARD_MINIMIZE) keyboardStatus= KEYBOARD_MOVABLE;
			//Assignation de l'anci?nne position de la souris
			lastPosXofMouse= e.clientX;
			lastPosYofMouse= e.clientY;
			//R?cup?ration de la position du div et ajout de l'?cart de position de la souris
			var newX1 = parseInt(mykeyboard_menu.style.left) + difX;
			var newY1 = parseInt(mykeyboard_menu.style.top) + difY;
			// Assignation des nouvelles coordonn?es au div
 			mykeyboard_menu.style.top= newY1 + "px";
			mykeyboard_menu.style.left= newX1 + "px";
 			mykeyboard_menu_small.style.top= newY1 + "px";
			mykeyboard_menu_small.style.left= newX1 + "px";
		}
	}
	
	function positionLayer(x, y){
		mykeyboard_menu.style.top= y + "px";
		mykeyboard_menu.style.left= x + "px";
		mykeyboard_menu_small.style.top= y + "px";
		mykeyboard_menu_small.style.left= x + "px";
	}

/************************************/
/* FONCTIONS DE SWAPING DU KEYBOARD */
/************************************/
	function MM_swapImgRestore() { //v3.0
	  var i,x,a=document.MM_sr; for(i=0;a&&i<a.length&&(x=a[i])&&x.oSrc;i++) x.src=x.oSrc;
	}
	
	function MM_preloadImages() { //v3.0
	  var d=document; if(d.images){ if(!d.MM_p) d.MM_p=new Array();
		var i,j=d.MM_p.length,a=MM_preloadImages.arguments; for(i=0; i<a.length; i++)
		if (a[i].indexOf("#")!=0){ d.MM_p[j]=new Image; d.MM_p[j++].src=a[i];}}
	}
	
	function MM_findObj(n, d) { //v4.01
	  var p,i,x;  if(!d) d=document; if((p=n.indexOf("?"))>0&&parent.frames.length) {
		d=parent.frames[n.substring(p+1)].document; n=n.substring(0,p);}
	  if(!(x=d[n])&&d.all) x=d.all[n]; for (i=0;!x&&i<d.forms.length;i++) x=d.forms[i][n];
	  for(i=0;!x&&d.layers&&i<d.layers.length;i++) x=MM_findObj(n,d.layers[i].document);
	  if(!x && d.getElementById) x=d.getElementById(n); return x;
	}
	
	function MM_swapImage() { //v3.0
	  var i,j=0,x,a=MM_swapImage.arguments; document.MM_sr=new Array; for(i=0;i<(a.length-2);i+=3)
	   if ((x=MM_findObj(a[i]))!=null){document.MM_sr[j++]=x; if(!x.oSrc) x.oSrc=x.src; x.src=a[i+2];}
	}
		
	function closeKeyboard(){
		mykeyboard_menu.style.visibility= 'hidden';
		keyboardStatus= KEYBOARD_CLOSE;		
	}
	function minimizeKeyboard(){
		minimizeMenu();
		mykeyboard_menu.style.left= offsetLeft + "px";
		mykeyboard_menu_small.style.top= "0px";
		mykeyboard_menu_small.style.left= offsetLeft + "px";
		keyboardStatus= KEYBOARD_MINIMIZE;
	}
	function showBigKeyboard(){
		if(parseInt(mykeyboard_menu.style.top) <= 10){
			mykeyboard_menu.style.top= "0px";
			mykeyboard_menu_small.style.top= "0px";
		}
		mykeyboard_menu.style.visibility= 'visible';
		mykeyboard_menu_small.style.visibility= 'hidden';
		keyboardStatus= KEYBOARD_MOVABLE;
	}
	function showSmallKeyboard(){
		mykeyboard_menu.style.visibility= 'hidden';
		mykeyboard_menu_small.style.visibility= 'visible';
		keyboardStatus= KEYBOARD_SMALL;
	}


/****************************************/
/* FONCTIONS DE AUTO-HIDDEN DU KEYBOARD */
/****************************************/
	var showTimerID= null;
	var showTimerRunning= false;
	var showDelay= 10;
	var showPixel= 25;
	
	var hideTimerID= null;
	var hideTimerRunning= false;
	var hideDelay= 10;
	var hidePixel= 15;
	
	var minimizeTimerID= null;
	var minimizeTimerRunning= false;
	var minimizeDelay= 10;
	var minimizePixel= 40;
	
	var timerID= null;
	var timerRunning= false;
	var delay= 250;
	
	function stopShowTimer(){
		if(showTimerRunning)
			clearTimeout(showTimerID);
		showTimerRunning= false;
	}
	function setShowTimerMenu(){
		stopShowTimer();
		showTimerID= setTimeout("upLayer()", showDelay);
		showTimerRunning= true;
	}
	function upLayer(){
		stopShowTimer();
		mykeyboard_menu.style.top = ((showPixel + parseInt(mykeyboard_menu.style.top)) > 0 ? 0 : (showPixel + parseInt(mykeyboard_menu.style.top))) + "px";
		if(parseInt(mykeyboard_menu.style.top) >= 0){return;}
		setShowTimerMenu();
	}
	function showLayer(){
		if(keyboardStatus == KEYBOARD_MINIMIZE){
			mykeyboard_menu.style.top = 0- mykeyboard_menu.offsetHeight;
			mykeyboard_menu.style.visibility = 'visible';
			document.getElementById('keyboard_menu_show').style.visibility = 'hidden';
			setShowTimerMenu();
		}
	}
	
	function stopHideTimer(){
		if(hideTimerRunning)
			clearTimeout(hideTimerID);
		hideTimerRunning= false;
	}
	function setHideTimerMenu(){
		stopHideTimer();
		hideTimerID= setTimeout("downLayer()", hideDelay);
		hideTimerRunning= true;
	}
	function downLayer(){
		stopHideTimer();
		mykeyboard_menu.style.top = (parseInt(mykeyboard_menu.style.top)- hidePixel) + "px";
		if(parseInt(mykeyboard_menu.style.top) <= (-mykeyboard_menu.offsetHeight)){
			document.getElementById('keyboard_menu_show').style.visibility = 'visible';
			mykeyboard_menu.style.visibility= 'visible';
			mykeyboard_menu_small.style.visibility= 'hidden';
			keyboardStatus= KEYBOARD_MINIMIZE;
			return;
		}
		setHideTimerMenu();
	}
	function hideLayer(){
		if(keyboardStatus == KEYBOARD_MINIMIZE)	setHideTimerMenu();
	}	
	
	function stopTimer(){
		if(timerRunning)
			clearTimeout(timerID);
		timerRunning= false;
	}
	function setTimerMenu(){
		if(showTimerRunning || hideTimerRunning) return;
		stopTimer();
		timerID= setTimeout("hideMenu()", delay);
		timerRunning= true;
	}
	function hideMenu(){
		stopTimer();
		hideLayer();
	}
	
	function minimizeMenu(){
		if(keyboardStatus != KEYBOARD_MINIMIZE)	setMinimizeTimerMenu();
	}	
	function stopMinimizeTimer(){
		if(minimizeTimerRunning)
			clearTimeout(minimizeTimerID);
		minimizeTimerRunning= false;
	}
	function setMinimizeTimerMenu(){
		stopMinimizeTimer();
		minimizeTimerID= setTimeout("downMinimizeLayer()", minimizeDelay);
		minimizeTimerRunning= true;
	}
	function downMinimizeLayer(){
		stopMinimizeTimer();
		mykeyboard_menu.style.top = (parseInt(mykeyboard_menu.style.top)- minimizePixel) + "px";
		if(parseInt(mykeyboard_menu.style.top) <= (-mykeyboard_menu.offsetHeight)){
			document.getElementById('keyboard_menu_show').style.visibility = 'visible';
			mykeyboard_menu.style.visibility= 'visible';
			mykeyboard_menu_small.style.visibility= 'hidden';
			keyboardStatus= KEYBOARD_MINIMIZE;
			return;
		}
		setMinimizeTimerMenu();
	}
