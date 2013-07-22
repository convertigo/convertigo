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

$.extend(true, C8O, {
	init_vars : {
		testplatform : "auto"
	},
	
	ro_vars : {
		portal_username : "",
		widget_name : ""
	},
	
	vars : { /** customizable value by adding __name=value in query*/
		auto_refresh : "true", /** true/false */
		auto_resize : "true", /** true/false */
		resize_offset : "50", /** number */
		send_portal_username : "true", /** true/false */
		target_append : "false", /** true/false */
		target_id : "",
		use_siteclipper_plugin : "true", /** true/false */
		xsl_side : "client" /** client/server */
	},
	
	doMashupEvent : function (event_name, payload) {
		if (payload != null && typeof(payload) == "object") {
			if (!$.isPlainObject(payload)) {
				try {
					var attributes = {};
					for (var i = 0; i < payload.attributes.length; i++) {
						var attribute = payload.attributes[i];
						attributes[attribute.nodeName] = attribute.nodeValue;
					}
					payload = attributes;
				} catch (e) {
					// maybe not a DOM element
				}
			}
		} else {
			payload = {};
		}
		C8O._hook("mashup_event", event_name, payload);
	},
	
	doNavigationBarEvent : function (type) {
		if ($.inArray(type, C8O._define.navigation_var_actions) != -1) {
			C8O.call({ __event_action : "navbar_" + type });
		}
	},
	
	doReconnect : function () {
		window.location.reload(false);
	},
	
	doResize : function (height, options) {
		if (typeof(height) == "number") {
			if (C8O.isUndefined(options)) {
				options = {};
			}
			if (C8O._define.iframe) {
				$(window.frameElement).animate({height : height}, options);
			} else {
				C8O._postMessage({type : "resize", height : height});
			}
		} else {
			C8O._resize(options);
		}
	},
		
	waitHide : function () {
		$("#wait_div").remove();
	},
	
	waitShow : function () {
		if ($("body #wait_div").length == 0) {
			$("body").append(C8O._define.wait_div);
		}
	},
	
	_define : {
		clipping_attributs :
			$(["altKey", "ctrlKey", "metaKey", "shiftKey", "clientX", "clientY", "screenX", "screenY", "layerX", "layerY", "pageX", "pageY", "button"]),
		dirty_timer : {},
		navigation_var_actions : ["backward", "forward", "stop", "refresh"],
		iframe : false,
		webclipper_path : ""
	},
	
	_addField : function (params, twsid, value) {
		params["__field_" + twsid] = value;
		return value;
	},

	_call : function (data) {
		C8O._define.last_call_params = data;
		if (C8O._hook("call", data)) {
			var jqXHR = $.ajax({
				data : data,
				dataType : C8O.vars.xsl_side == "client" ? "xml" : "text",
				success : C8O._onSuccess,
				type : C8O.vars.ajax_method,
				url : C8O.vars.requester_prefix + (C8O.vars.xsl_side == "client" ? ".xml" : ".cxml")
			});
			jqXHR.C8O_data = data;
			C8O._define.pendingXhrCpt++;
		}
	},
	
	_checkDirty : function (tim) {
		clearTimeout(C8O._define.dirty_timer);
		if (tim) {
			if (tim < 15000) {
				tim *= 1.25;
			}
			$.ajax({
				dataType : "text",
				success : function (data) {
					switch (data) {
					case "false":
						C8O._define.dirty_timer = window.setTimeout("C8O._checkDirty(" + tim + ")", tim);
						break;
					case "true":
						C8O.call();
						break;
					}
				},
				url : C8O._define.webclipper_path + C8O._define.project + "/" + (C8O._define.connector ? C8O._define.connector : "$") + "/" + (C8O._define.context ? C8O._define.context : "$") + "/d"
			});
		} else {
			C8O._define.dirty_timer = window.setTimeout("C8O._checkDirty(500)", 500);
		}
	},
	
	__init : C8O._init,
	_init : function (params) {
		var value;
		if (value = C8O._remove(params, "__container")) {
			if (value == "df") {
				C8O._getScript(C8O._define.plugins_path + "df.js", function () {
					C8O._init_df(params);
				});
			} else if (value == "mosaic") {
				C8O._getScript(C8O._define.plugins_path + "mosaic.js", function () {
					C8O._init_mosaic(params);
				});
			} else if (value == "gatein") {
				C8O._getScript(C8O._define.plugins_path + "gatein.js", function () {
					C8O._init_gatein(params);
				});
			} else if (value == "sharepoint") {
				C8O._getScript(C8O._define.plugins_path + "sharepoint.js", function () {
					C8O._init_sharepoint(params);
				});
			} else if (value == "standalone") {
				C8O._getScript(C8O._define.plugins_path + "standalone.js", function () {
					C8O._init_standalone(params);
				});
			}
		} else {
			C8O.__init(params);
		}
	},
	
	_onDocumentReady : function () {
		/** No XSLT engine (see #1336) : switch to server mode */
		if (!window.XSLTProcessor && !window.ActiveXObject) {
			C8O.vars.xsl_side = "server";
		}
		
		/** weblib_wrapper can't access to C8O object with IE (see #1778) */
		if (typeof(C8O_document_ready) != "undefined") {
			C8O.addHook("document_ready", C8O_document_ready);
		}
		
		C8O._define.webclipper_path = window.location.href.replace(new RegExp("/projects/.*"), "/webclipper/");
		
		// retrieve the wait_div element in memory
		C8O._define.wait_div = $("#wait_div").clone();
		
		if (C8O._hook("document_ready")) {
			var loc = window.location,
				base = loc.href.substring(0, loc.href.indexOf("/projects/")),
				match = loc.pathname.match(/\/projects\/(.*)\/.*/);
			if (match.length > 1) {
				var params = C8O._parseQuery();
				C8O._define.project = match[1];
				
				var testplatform = C8O._remove(params, "__testplatform");
				testplatform = (testplatform == null) ? C8O.init_vars.testplatform : (C8O.init_vars.testplatform = testplatform);
				
				if ("false" == testplatform || ("auto" == testplatform && !$.isEmptyObject(params))) {
					C8O._init(params);
				} else {
					loc.href = base + "/project.html#" + C8O._define.project;
				}
			} else {
				window.location.href = base;
			}
		}
	},
	
	_onMashupEvent : function (event) {
		if (C8O._hook("receive_mashup_event", event)) {
			if (event.type == "call") {
				C8O.call(event.payload);
			}	
		}
	},
	
	__onSuccess : C8O._onSuccess,
	_onSuccess : function (xml, status, jqXHR) {
		C8O.__onSuccess(xml, status, jqXHR);
		if (C8O.vars.xsl_side == "server") {
			var aText = [jqXHR.responseText + ""];
			if (C8O._hook("text_response", aText)) {
				C8O._fillBody(aText[0]);
			}
		} else {	
			if (C8O._hook("xml_response", xml, jqXHR.C8O_data)) {
				var redirect_location = $(xml.documentElement).attr("redirect_location");
				if (!C8O.isUndefined(redirect_location)) {
					if (C8O.vars.use_siteclipper_plugin == "true") {
						C8O._getScript(C8O._define.plugins_path + "siteclipper.js", function () {
							C8O._init_siteclipper({redirect_location : redirect_location});
						});
					} else {
						window.location = redirect_location;
					}
					return;
				}
				var sheet_uri = C8O._xslStyleSheet(xml);
				if (sheet_uri != null) {
					$.ajax({
						url : sheet_uri,
						success : function (xsl) {
							C8O._xslt(xml, xsl);
						},
						type : "GET"
					});
				} else if (!$.support.leadingWhitespace) {
					C8O._fillBody($("<pre>" + jqXHR.responseText.replace(/</g, "&lt;").replace(/>/g, "&gt;") + "</pre>"));
				} else {
					C8O._fillBody($("<pre/>").text(jqXHR.responseText));
				}
			}
		}
	},
	
	_resize : function (options) {
		var lowest = C8O._hook("resize_calculation");
		if (lowest != false) {
			if (typeof(lowest) != "number") {
				lowest = 0;
				$("*:not(html, body)").each(function () {
					lowest = Math.max(lowest, this.offsetTop + this.offsetHeight);
				});
				lowest += parseInt(C8O.vars.resize_offset);
			}
			C8O.doResize(lowest, options);
		}
	},
	
	_fillBody : function (content, resize) {
		var $container = C8O.vars.target_id;
		if (typeof($container) == "string") {
			$container = $($container.length == 0 ? "body" : ('#' + $container));	
		} else if (C8O.isUndefined($container.jquery)){
			$container = $($container);
		}
		
		if (C8O.vars.target_append == "true") {
			$container.append(content);
		} else {
			$container.html(content).children("title:first").each(function () {
				window.document.title = $(this).text();
			});
		}
		if (C8O._hook("result_filled", $container)) {
			$("a, input[type=button], input[type=image], input[type=submit]").filter("[twsid]").unbind(".clipping").bind("click.clipping", C8O._handleEvent);
			$("form[twsid]").unbind(".clipping").bind("submit.clipping", C8O._handleEvent);
			if (C8O.vars.auto_resize == "true" && window != window.parent && (C8O.isUndefined(resize) || resize)) {
				window.setTimeout(C8O._resize, 750);
			}
			if ("true" == C8O.vars.auto_refresh && $("[twsid]:first").length > 0) {
				C8O._checkDirty();
			}
		}
	},
	
	_xslStyleSheet : function (xml) {
		var node = xml.firstChild;
		while (node != null) {
			if (node.nodeName == "xml-stylesheet") {
				return node.data.replace(/.*href="(.*)".*/, "$1");
			} else {
				node = node.nextSibling;
			}
		}
		return null;
	},
	
	_handleEvent : function (event) {
		var params = {
			__event_action : event.type,
			__event_srcid : $(this).attr("twsid")
		};
		if (event.type == "click") {
			C8O._define.clipping_attributs.each(function () {
				if (!C8O.isUndefined(event[this])) {
					params["__event_" + this] = event[this];
				}
			});
		}
		(event.type == "submit" ? $($.makeArray(this.elements)) : $("input, select, textarea").filter("[twsid]:enabled")).each(function () {
			var twsid = $(this).attr("twsid"), j;
			switch (this.type) {
				case 'text' : case 'password' : case 'hidden' : case 'textarea':
					C8O._addField(params, twsid, this.value);
					break;
				case 'select-one':
					if (this.selectedIndex>=0) C8O._addField(params, twsid, this.selectedIndex);
					break;
				case 'select-multiple':
					var selected = [];
					for (j=0; j < this.options.length; j++) {
						if (this.options[j].selected) {
							selected.push(j);
						}
						C8O._addField(params, twsid, selected.join(";"));
					}
					break;
				case 'checkbox' : case 'radio':
					C8O._addField(params, twsid, this.checked?"true":"false");
					break;			
			}
		});
		C8O.call(params);
		return false;
	},
	
	_postMessage : function (data) {
		if (window.postMessage) {
			try {
				data = JSON.stringify(data);
			} catch(e) {}
			window.parent.postMessage(data, "*");
		}
	},
	
	_xslt : function (xml, xsl) {
		if (window.XSLTProcessor) {
			var xsltProcessor = new XSLTProcessor();
			xsltProcessor.importStylesheet(xsl);
			var $doc = $(xsltProcessor.transformToFragment(xml, document.implementation.createDocument("", "", null)));
			C8O._fillBody($doc.contents().detach());
		} else {
			C8O._fillBody(xml.transformNode(xsl));
		}
	}
});

try {
	if (window.frameElement.src) {
		C8O._define.iframe = true;
	}
} catch(e) {
}