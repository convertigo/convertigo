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

function C8O_document_ready () {
	getOriginalFontSize();
	var _ajaxReadyStateListener = ajaxReadyStateListener;
	ajaxReadyStateListener = function () {
		_ajaxReadyStateListener();
		if (xmlhttp.readyState == 4) {
			C8O.doResize();
		}
	};
	C8O.addHook("call", function (data) {
		ajaxXmlPostData(xmlhttp, $.param(data));
		return false;
	});
	
	doMashupEvent = function (event, event_name, data) {
		C8O.doMashupEvent(event_name, data);
	};
	
	C8O.log.debug("c8o.scriptlib: initialization finished");
	return true;
}

document.write("<script type=\"text/javascript\" src=\"../../scripts/7.2.0/jquery.min.js\"><!--script--></script>");
document.write("<script type=\"text/javascript\" src=\"../../scripts/7.2.0/c8o.core.js\"><!--script--></script>");
document.write("<script type=\"text/javascript\" src=\"../../scripts/7.2.0/c8o.desktop.js\"><!--script--></script>");
