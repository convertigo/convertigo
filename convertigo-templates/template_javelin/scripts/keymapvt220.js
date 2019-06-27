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
 * $Workfile: keymapvt220.js $
 * $Author: Davidm $
 * $Revision: 1 $
 * $Date: 20/06/06 16:05 $
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
  	
	if (e.keyCode == 13) {
		doAction('RETURN');
	}
	else if (e.keyCode == 09){
			doAction('TAB');
	}
	else if (e.keyCode == 112){
			doAction('F01');
	}
	else if (e.keyCode == 113){
			doAction('F02');
	}
	else if (e.keyCode == 114){
			doAction('F03');
	}
	else if (e.keyCode == 115){
			doAction('F04');
	}
	else if (e.keyCode == 116){
			doAction('F05');
	}
	else if (e.keyCode == 117){
			doAction('F06');
	}
	else if (e.keyCode == 118){
			doAction('F07');
	}
	else if (e.keyCode == 119){
			doAction('F08');
	}
	else if (e.keyCode == 120){
			doAction('F09');
	}
	else if (e.keyCode == 121){
			doAction('F10');
	}
	else if (e.keyCode == 122){
			doAction('F11');
	}
	else if (e.keyCode == 123){
			doAction('F12');
	}
	else if (e.keyCode == 34) doAction('PGDN');
	else if (e.keyCode == 33) doAction('PGUP');
	else if (e.keyCode == 27) doAction('ESC');
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


