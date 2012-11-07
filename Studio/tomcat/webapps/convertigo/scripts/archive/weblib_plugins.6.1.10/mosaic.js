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

var McVERSION = "1.0.0";
var mosaicApp;
var parentView;
var parentPanel;
var thisTile;

try{
	registerHTMLTile(McVERSION);
} catch(e) {
	this.parent.registerHTMLTile(frameElement.parentNode.id,McVERSION);
}

C8O._init_mosaic = function (params) {
	if (params.__hub_page) {
		C8O._getScript("../../scripts/weblib_plugins/hub.js", function () {
			C8O._hub.init(params);
			mosaicApp.addMessageListener("convertigo", "mashup", function (message) {
				C8O._hub.receive_event(message.payload);
			});
			
			C8O._hub.publish_event = function (target, message) {
				mosaicApp.sendMessage({nameSpace: "convertigo", name: target, payload: message});
			}
		});
	} else {
		var widget_name = C8O.ro_vars.widget_name = thisTile.getNodeName();
		
		C8O.addHook("mashup_event", function (eventName, payload) {
			mosaicApp.sendMessage({
				nameSpace: "convertigo",
				name: "mashup",
				payload: {
					payload: payload,
					name: eventName,
					origin: widget_name,
					type: "mashup"
				}
			});
		});
		
		C8O.addHook("result_filled", function () {
			try {
				if (navigator.userAgent.indexOf("AdobeAIR") !== -1) {
					$("*[onclick]").unbind("click").click(function () {
						eval(this.getAttribute("onclick"));
						return false;
					});
				}
			} catch(e){}
		});
		
		mosaicApp.addMessageListener("convertigo", widget_name, function (message) {
			if (message.name === widget_name) {
				C8O._onMashupEvent(message.payload);
			};
		});
		
		C8O.vars.auto_resize = "false";
		
		if (!params.__context) {
			params.__context = widget_name;
		}
		C8O._init(params);
	}
};