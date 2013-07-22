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

// build an associative array containing the javelin_form.elements[] indices
// used for keyup and keydown navigation through javelin_form elements
var javelin_formElements  = new Array();

function initElements() {
   if (document.javelin_form.elements.length>0) {
      for (i=0;i<document.javelin_form.elements.length;i++) {
         javelin_formElements[document.javelin_form.elements[i].id] = i;
      }
   }
}

isIE =  navigator.userAgent.indexOf('Gecko') == -1 ? true: false;
if (isIE) {
	// Microsoft IE KeyHandler
	document.onkeydown 	= handleKeypress;
	// disable default action while pressing F1 fo IE
	document.onhelp 	= function() { // add this to prevent help from popping up
		return false;
	} 
}
else {
	// Gecko based browsers (FireFOX, Safari, Mozilla, ....);
	document.addEventListener('keypress', handleKeypress, true);
}

function handleKeypress(e) {
	if(!e) e= window.event;
	e.returnValue = false;
  	var shiftKeyDown = e.shiftKey;
  	
  	
	if (e.keyCode == 13) doAction('KEY_ENTER');
	else if (e.keyCode == 112){
		if(shiftKeyDown)
			doAction('KEY_PF13');
		else
			doAction('KEY_PF1');
	}
	else if (e.keyCode == 113){
		if(shiftKeyDown)
			doAction('KEY_PF14');
		else
			doAction('KEY_PF2');
	}
	else if (e.keyCode == 114){
		if(shiftKeyDown)
			doAction('KEY_PF15');
		else
			doAction('KEY_PF3');
	}
	else if (e.keyCode == 115){
		if(shiftKeyDown)
			doAction('KEY_PF16');
		else
			doAction('KEY_PF4');
	}
	else if (e.keyCode == 116){
		if(shiftKeyDown)
			doAction('KEY_PF17');
		else
			doAction('KEY_PF5');
	}
	else if (e.keyCode == 117){
		if(shiftKeyDown)
			doAction('KEY_PF18');
		else
			doAction('KEY_PF6');
	}
	else if (e.keyCode == 118){
		if(shiftKeyDown)
			doAction('KEY_PF19');
		else
			doAction('KEY_PF7');
	}
	else if (e.keyCode == 119){
		if(shiftKeyDown)
			doAction('KEY_PF20');
		else
			doAction('KEY_PF8');
	}
	else if (e.keyCode == 120){
		if(shiftKeyDown)
			doAction('KEY_PF21');
		else
			doAction('KEY_PF9');
	}
	else if (e.keyCode == 121){
		if(shiftKeyDown)
			doAction('KEY_PF22');
		else
			doAction('KEY_PF10');
	}
	else if (e.keyCode == 122){
		if(shiftKeyDown)
			doAction('KEY_PF23');
		else
			doAction('KEY_PF11');
	}
	else if (e.keyCode == 123){
		if(shiftKeyDown)
			doAction('KEY_PF24');
		else
			doAction('KEY_PF12');
	}
	else if (e.keyCode == 107)doAction('KEY_FIELDPLUS');
	else if (e.keyCode == 34) doAction('KEY_ROLLDOWN');
	else if (e.keyCode == 33) doAction('KEY_ROLLUP');
	else if (e.keyCode == 27) doAction('KEY_ATTN');
	// overrides keyup -> shift-tab
	else if (e.keyCode == 38) {
		if (document.javelin_form.elements.length > 0) {
			var myid = javelin_formElements[currentFieldOnFocus];
			while (myid > 0) {
				myid--;
				if (isField(document.javelin_form.elements[myid])) {
					document.javelin_form.elements[myid].focus();
					document.javelin_form.elements[myid].select();
					break;
				}
			}
		}
	}
	//overrides keydown -> tab
	else if (e.keyCode == 40) {
		if (document.javelin_form.elements.length > 0) {
			var myid = javelin_formElements[currentFieldOnFocus];
			while (myid < document.javelin_form.elements.length-1) {
				myid++;
				if (isField(document.javelin_form.elements[myid])) {
					document.javelin_form.elements[myid].focus();
					document.javelin_form.elements[myid].select();
					break;
				}
			}
		}
	}
	else{ 
		e.returnValue = true;
		return true;
	}
	
	if (isIE) {
		// for IE simply return false in the handler to notify that we handled the event
		if (e.keyCode != 17) // hack for control key ==> have to ignore the folowing line
			e.keyCode= 0;
		return false;	
	} else {
		// for Gecko, cancel propagation and default handlers
		e.stopPropagation();
		e.preventDefault();
		return ;	
	}
}


