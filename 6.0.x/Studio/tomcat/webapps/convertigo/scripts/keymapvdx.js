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
document.onhelp = function() { // add this to prevent help from popping up
    return false;
}
function handleKeypress(e) {
	if(!e) e= window.event;
	e.returnValue = false;
	if (e.keyCode == 13) doAction('KEnvoi');
	else if (e.keyCode == 40) doAction('KSuite');
	else if (e.keyCode == 38) doAction('KRetour');
	else if (e.keyCode == 36) doAction('KSommaire');
	else if (e.keyCode == 34) doAction('KSuite');
	else if (e.keyCode == 33) doAction('KRetour');
	else{ e.returnValue = true; return true;}
	e.keyCode= 0;
	return false;
}

document.onkeydown = handleKeypress;

