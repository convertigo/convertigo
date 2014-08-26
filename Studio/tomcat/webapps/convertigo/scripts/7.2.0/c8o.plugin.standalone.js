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

C8O._init_standalone = function (params) {
	var $common = $(window.frameElement).closest("body");
	if (typeof(window.frameElement.ownerDocument.C8O_mashup) === "undefined") {
		window.frameElement.ownerDocument.C8O_mashup = {};
	}
	var C8O_mashup = window.frameElement.ownerDocument.C8O_mashup;
	if (params.__hub_page) {
		C8O._getScript(C8O._define.plugins_path + "hub.js", function () {
			C8O._hub.init(params);
			
			C8O_mashup.HUB = function (message) {
				C8O._hub.receive_event(message);
			};
			
			$(window).unload(function() {
				delete C8O_mashup.HUB;
			});
			
			C8O._hub.publish_event = function (target, message) {
				C8O_mashup[target](message);
			};
		});
	} else {
		var widget_name = params.__widget_name;
		
		C8O.addHook("mashup_event", function (eventName, payload) {
			C8O_mashup.HUB({
				payload : payload,
				name : eventName,
				origin : widget_name,
				type : "mashup"
			});
		});

		C8O_mashup[widget_name] = function (message) {
			C8O._onMashupEvent(message);
		};
		$(window).unload(function() {
			delete C8O_mashup[widget_name]; 
		});

		if (!params.__context) {
			params.__context = widget_name;
		}
		
		C8O.ro_vars.widget_name = widget_name;
		
		C8O._init(params);
	}
}