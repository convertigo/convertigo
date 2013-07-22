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

// disable default action while pressing F1
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
	if (e.keyCode == 13) doAction('XMIT');
	else if (e.keyCode == 112) doAction('FKC01');
	else if (e.keyCode == 113) doAction('FKC02');
	else if (e.keyCode == 114) doAction('FKC03');
	else if (e.keyCode == 115) doAction('FKC04');
	else if (e.keyCode == 116) doAction('FKC05');
	else if (e.keyCode == 117) doAction('FKC06');
	else if (e.keyCode == 118) doAction('FKC07');
	else if (e.keyCode == 119) doAction('FKC08');
	else if (e.keyCode == 120) doAction('FKC09');
	else if (e.keyCode == 121) doAction('FKC10');
	else if (e.keyCode == 122) doAction('FKC11');
	else if (e.keyCode == 123) doAction('FKC12');
	else if (e.keyCode == 34) doAction('KEY_ROLLDOWN');
	else if (e.keyCode == 33) doAction('KEY_ROLLUP');
	else{ e.returnValue = true; return true;}
	
	if (isIE) {
		// for IE simply return false in the handler to notify that we handled the event
		e.keyCode= 0;
		return false;	
	} else {
		// for Gecko, cancel propagation and default handlers
		e.stopPropagation();
		e.preventDefault();
		return ;	
	}
}

