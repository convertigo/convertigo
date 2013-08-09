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

C8O = {
	init_vars: {
		enc: "false", /** enable rsa encoding */
		i18n: ""
	},
	
	ro_vars: {
		i18n_files: []
	},
	
	vars: { /** customizable value by adding __name=value in query*/
		ajax_method: "POST", /** POST/GET */
		endpoint_url: "",
		first_call: "false",
		log_level: "warn", /** none, error, warn, info, debug, trace */
		log_line: "false",
		requester_prefix: "",
		xsl_side: "none"
	},
	
	addHook: function (name, fn) {
		if ($.isFunction(fn)) {
			if (!$.isArray(C8O._define.hooks[name])) {
				C8O._define.hooks[name] = [];
			}
			C8O._define.hooks[name].push(fn);
		}
	},
	
	addRecallParameter: function (parameter_name, parameter_value) {
		if (C8O.isUndefined(parameter_value)) {
			parameter_value = "";
		}
		C8O._define.recall_params[parameter_name] = parameter_value;
	},
	
	appendValue: function (data, key, value) {
		if (!$.isArray(value)) {
			value = [value];
		}
		for (var i in value) {
			if (C8O.isUndefined(data[key])) {
				data[key] = value[i];
			} else if ($.isArray(data[key])) {
				data[key].push(value[i]);
			} else {
				data[key] = [data[key], value[i]];
			}
		}
	},
	
	appendValues: function (data, source) {
		if ($.isPlainObject(data) && $.isPlainObject(source)) {
			for (var key in source) {
				C8O.appendValue(data, key, source[key]);
			}
		}
	},
	
	call: function (data) {
		var key;
		if (C8O.canLog("info")) {
			C8O.log.info("c8o.core: call: " + C8O.toJSON(data));
		}

		C8O.log.trace("c8o.core: call show wait div");
		C8O.waitShow();
		
		if (typeof(data) == "string") {
			C8O.log.trace("c8o.core: call parse string data '" + data + "'");
			data = C8O._parseQuery({}, data);
		} else if (C8O.isUndefined(data)) {
			C8O.log.trace("c8o.core: call without data");
			data = {};
		} else if (!$.isPlainObject(data) && $(data).is("form")) {
			var $form = $(data);
			data = C8O.formToData($form);
			if ($form.find("input[type=file]").length) {
				C8O.log.debug("c8o.core: call using a form with an input file");
				
				var targetName = "tn_" + new Date().getTime() + "_" + Math.floor(Math.random() * 100);
				var action = C8O._getCallUrl();
				$form.attr({
					method: "POST",
					enctype: "multipart/form-data",
					action: action,
					target: targetName
				});
				var $iframe = $("<iframe/>").attr({
					src: "",
					style: "display: none"
				}).appendTo("body").one("load", function () {
					C8O.log.debug("c8o.core: call using a form response");
					
					var fakeXHR = {
						C8O_data: data
					};
					
					if (C8O.vars.xsl_side == "server") {
						fakeXHR.responseText = $iframe[0].contentWindow.document.outerHTML;
						C8O._onSuccess(null, "success", fakeXHR);
					} else {
						var xml = $iframe[0].contentWindow.document.XMLDocument;
						fakeXHR.responseText = "No responseText for multipart, use XSL or xml_response.";
						C8O._onSuccess(xml ? xml: $iframe[0].contentWindow.document, "success", fakeXHR);
					}
					C8O._onComplete(fakeXHR, "success");
					$iframe.remove();
				});
				$iframe[0].contentWindow.name = targetName;
				window.setTimeout(function () {
					$form.trigger("submit." + targetName);
				}, 0);
				return;
			} else {
				C8O.log.debug("c8o.core: call using a form");
			}
		}
		
		C8O._retrieve_vars(data);
		
		for (key in C8O._define.recall_params) {
			if (C8O._define.recall_params.hasOwnProperty(key)) {
				if (!C8O.isUndefined(data[key])) {
					C8O.log.debug("c8o.core: new value of recall parameter '" + key + "' is '" + data[key] + "'");
					C8O._define.recall_params[key] = data[key];
				}
				if (C8O._define.recall_params[key]) {
					C8O.log.debug("c8o.core: add value of recall parameter '" + key + "' = '" + data[key] + "'");
					data[key] = C8O._define.recall_params[key];
				} else {
					delete data[key];
				}
			}
		}
		
		C8O._call(data);
	},

	canLog: function (level) {
		return $.inArray(level, C8O._define.log_levels) <= $.inArray(C8O.vars.log_level, C8O._define.log_levels);
	},
	
	convertHTML: function (input, output) {
		if (C8O.isUndefined(output)) {
			output = document.createElement("fragment");
		}
		switch (input.nodeType) {
		case Node.ELEMENT_NODE:
			var i;
			var newelt = document.createElement(input.tagName);
			for (i = 0 ; i < input.attributes.length ; i++) {
				newelt.setAttribute(input.attributes[i].nodeName, input.attributes[i].nodeValue);
			}
			for (i = 0 ; i < input.childNodes.length ; i++) {
				C8O.convertHTML(input.childNodes[i], newelt);
			}
			output.appendChild(newelt);
			break;
		case Node.TEXT_NODE:
			output.appendChild(document.createTextNode(input.nodeValue))
			break;
		}
		return output
	},
	
	formToData: function ($form, data) {
		if (!$form.jquery) {
			$form = $($form);
		}
		if (C8O.isUndefined(data)) {
			data = {};
		}
		var formArray = ($form.jquery ? $form : $($form)).serializeArray();
		for (var i in formArray) {
			C8O.appendValue(data, formArray[i].name, formArray[i].value);
		}
		return data;
	},
	
	getBrowserLanguage: function () {
		var lang, nav = navigator;
	    if (nav && nav.userAgent && (lang = nav.userAgent.match(/android.*\W(\w\w)-(\w\w)\W/i))) {
	        lang = lang[1];
	    }

	    if (!lang && nav) {
	        if (nav.language) {
	            lang = nav.language;
	        } else if (nav.browserLanguage) {
	            lang = nav.browserLanguage;
	        } else if (nav.systemLanguage) {
	            lang = nav.systemLanguage;
	        } else if (nav.userLanguage) {
	            lang = nav.userLanguage;
	        }
	        lang = lang.substr(0, 2);
	    }
	    
	    return lang;
	},
	
	getLastCallParameter: function (key) {
		if (C8O.isUndefined(key)) {
			return C8O._obj_clone(C8O._define.last_call_params);
		} else {
			return C8O._define.last_call_params[key];
		}
	},
	
	isDefined: function (obj) {
		return typeof(obj) != "undefined";
	},
	
	isUndefined: function (obj) {
		return typeof(obj) == "undefined";
	},
	
	log: {
		error: function (msg, e) {
			C8O._log("error", msg, e);
		},
		warn: function (msg, e) {
			C8O._log("warn", msg, e);
		},
		info: function (msg, e) {
			C8O._log("info", msg, e);
		},
		debug: function (msg, e) {
			C8O._log("debug", msg, e);
		},
		trace: function (msg, e) {
			C8O._log("trace", msg, e);
		} 
	},
	
	removeRecallParameter: function (parameter_name) {
		delete C8O._define.recall_params[parameter_name];
	},
	
	toJSON: function (data) {
		try {
			if (window.JSON && window.JSON.stringify) {
				return window.JSON.stringify(data);
			} else {
				if ($.isPlainObject(data)) {
					var s = "{";
					for (var k in data) {
						s += '"' + k + '":' + C8O.toJSON(data[k]) + ',';
					}
					if (s.length > 1) {
						s = s.substring(0, s.length - 1);
					}
					return s + "}";
				} else if ($.isArray(data)) {
					var s = "[";
					for (var k in data) {
						s += C8O.toJSON(data[k]) + ',';
					}
					if (s.length > 1) {
						s = s.substring(0, s.length - 1);
					}
					return s + "]";
				}
			}
		} catch (e) { }
		return '"' + data + '"';
	},
	
	translate: function (elt) {
		if (C8O._define.dictionnary != null) {
			if (typeof(elt) == "string") {
				return C8O._translate(elt);
			} else {
				C8O.walk(elt, null, function (txt) {
					var find = txt.search(C8O._define.re_i18n);
					var res = "";
					while (find != -1) {
						res += txt.substring(0, find);
						txt = txt.substring(find);
						
						var match = txt.match(C8O._define.re_i18n);
						var value = C8O._translate(match[1]);
												
						res += value;
						txt = txt.substring(match[0].length);
						find = txt.search(C8O._define.re_i18n);
					}
					return res + txt;
				});
			}
		}
	},
	
	waitHide: function () {
	},
	
	waitShow: function () {
	},
	
	/**
	 * Walk each node and attribute and call the specified function
	 */
	walk: function (node, data, fn) {
		if (node.nodeType) {
			if (node.nodeType == Node.ELEMENT_NODE) {
				for (var i = 0; i < node.attributes.length; i++) {
					var fnr = fn.call(node, node.attributes[i].nodeValue, data);
					
					if (fnr != null) {
						node.attributes[i].nodeValue = fnr;
					}
				}
				for (var i = 0; i < node.childNodes.length; i++) {
					C8O.walk(node.childNodes[i], data, fn);
				}
			} else if (node.nodeType == Node.TEXT_NODE) {
				var fnr = fn.call(node, node.nodeValue, data);
				
				if (fnr != null) {
					node.nodeValue = fnr;
				}
			}
		} else if (node.each) {
			node.each(function () {
				C8O.walk(this, data, fn);
			});
		}
	},
	
	_define: {
		hooks: {},
		last_call_params: {},
		log_levels: ["none", "error", "warn", "info", "debug", "trace"],
		pendingXhrCpt: 0,
		plugins_path: "",
		project: null,
		recall_params: {__context: "", __connector: ""},
		re_format_time: new RegExp(" *(\\d*?)([\\d ]{4})((?:\\.[\\d ]{3})|(?: {4})) *"), // replace by "$1$2$3"
		re_i18n: new RegExp("__MSG_(.*?)__"),
		re_plus: new RegExp("\\+", "g"),
		start_time: new Date().getTime(),
		dictionnary: null
	},

	_call: function (data) {
		C8O._define.last_call_params = data;
		if (C8O._hook("call", data)) {
			var url = C8O._getCallUrl();
			if (C8O.canLog("trace")) {
				C8O.log.trace("c8o.core: call " + C8O.toJSON(data) + " " + C8O.vars.ajax_method + " " + url);
			}
			var jqXHR = $.ajax({
				complete: C8O._onComplete,
				data: data,
				dataType: C8O.vars.xsl_side == "server" ? "text" : "xml",
				error: C8O._onError,
				success: C8O._onSuccess,
				type: C8O.vars.ajax_method,
				url: url
			});
			jqXHR.C8O_data = data;
			C8O._define.pendingXhrCpt++;
		}
	},
	
	_log: function (level, msg, e) {
		if (C8O.canLog(level)) {
			var ret;
			if (ret = C8O._hook("log", level, msg, e)) {
				if (window.console && window.console.log) {
					if (typeof(ret) == "string") {
						msg = ret;
					}
					if (C8O.isDefined(e)) {
						msg += " (catch: " + C8O.toJSON(e) + ")";
					}
					if (C8O.vars.log_line == "true" && navigator.userAgent.indexOf("Chrome") != -1) {
						msg += "\n\t\t" + new Error().stack.split("\n")[3];
					}
					var time = (new Date().getTime() - C8O._define.start_time) / 1000;
					time = ("    " + time + "    ").replace(	C8O._define.re_format_time, "$1$2$3");
					
					if (level.length == 4) {
						level += " ";
					}
					
					console.log(time + " [" + level + "] " + msg);
				}
			}
		}
	},
	
	_obj_replace: function (object, content) {
		for (var key in object) {
			delete object[key];
		}
		return $.extend(object, content);
	},
	
	_obj_clone: function (object) {
		return $.extend(true, {}, object);
	},
	
	_onComplete: function (jqXHR, textStatus) {
		if (C8O._hook("complete", jqXHR, textStatus)) {
			if (--C8O._define.pendingXhrCpt <= 0) {
				C8O._define.pendingXhrCpt = 0;
				C8O.waitHide();
				C8O.log.debug("c8o.core: Ajax complete, hide the wait div");
			} else {
				C8O.log.trace("c8o.core: Ajax complete, remains " + C8O._define.pendingXhrCpt + " pending requests");
			}
		}
	},
	
	_onDocumentReady: function () {
		if (C8O._hook("document_ready")) {
			C8O._init({});
		};
	},
	
	_onError: function (jqXHR, textStatus, errorThrown) {
		if (C8O._hook("error", jqXHR, textStatus, errorThrown)) {
			C8O.log.error("c8o.core: Ajax error [" + textStatus + "]", errorThrown);
		}
	},
	
	_onSuccess: function (xml, status, jqXHR) {
		C8O._hook("xml_response", xml, jqXHR.C8O_data);
	},
	
	_retrieve_vars: function (data) {
		for (key in C8O.vars) {
			if (!C8O.isUndefined(data["__" + key])) {
				C8O.vars[key] = C8O._remove(data, "__" + key);
				C8O.log.debug("c8o.core: retrieve from parameter C8O.vars." + key + "=" + C8O.vars[key]);
			}
		}
	},
	
	_findAndSelf: function ($elt, selector) {
		return $elt.filter(selector).add($elt.find(selector));
	},
	
	_getAttributes: function (element) {
		if (element.jquery) {
			return element.length ?
					C8O._getAttributes(element[0]) :
					{};
		} else {
			var attributes = {};
			for (var i = 0 ; i < element.attributes.length ; i++) {
				attributes[element.attributes[i].nodeName] = element.attributes[i].nodeValue
			}
			return attributes;
		}
	},
	
	_getCallUrl: function () {
		return C8O.vars.endpoint_url + C8O.vars.requester_prefix + (C8O.vars.xsl_side == "client" ? ".xml" : C8O.vars.xsl_side == "server" ? ".cxml" : ".pxml");
	},
	
	_getFunction: function (functionObject) {
		try {
			if (typeof(functionObject) == "function") {
				return functionObject;
			} else if (typeof(functionObject) == "string") {
				var parts = functionObject.split(".");
				var fn = window;
				for (var i in parts) {
					fn = fn[parts[i]];
				}
				
				if (typeof(fn) == "function") {
					C8O.log.trace("c8o.core: success to retrieve function " + functionObject);
					return fn;
				}
			}
		} catch (e) {
			C8O.log.warn("c8o.core: failed to get function " + functionObject, e);
		}
		return null;
	},
	
	_getQuery: function () {
		var l = window.location,
			q = l.search.length > 0 ? l.search.substring(1) : "",
			h = l.hash.length > 0 ? l.hash.substring(1) : "";
		return (q.length > 0 && h.length > 0) ? (q + "&" + h) : (q.length > 0 ? q : h);
	},
	
	_getScript: function (url, callback) {
		var script = document.createElement("script"),
			done = false;
		script.src = url;

		C8O.log.trace("c8o.core: get script " + url);
		
		script.onload = script.onreadystatechange = function () {
			if (!done && (!this.readyState ||
				this.readyState == "loaded" ||
				this.readyState == "complete")) {
				
				C8O.log.trace("c8o.core: script loaded " + url);
				
				done = true;
				if (callback) {
					callback();
				}
				// Handle memory leak in IE
				script.onload = script.onreadystatechange = null;
			}
		};
		document.getElementsByTagName("head")[0].appendChild(script);
	},
	
	_hook: function (name) {
		var ret = true, i, r;
		if (name != "log") {
			C8O.log.debug("c8o.core: notify hook " + name);
		}
		
		if ($.isArray(C8O._define.hooks[name])) {
			for (i = 0; i < C8O._define.hooks[name].length && ret; i += 1) {
				r = C8O._define.hooks[name][i].apply(this, $.makeArray(arguments).slice(1));
				
				if (name != "log") {
					C8O.log.trace("c8o.core: hook " + name + " return " + ret);
				}
				
				if (!C8O.isUndefined(r)) {
					ret = r;
				}
			}
		}
		return ret;
	},
	
	_parseQuery: function (params, query) {
		var data = C8O.isUndefined(params) ? {} : params,
			vars = (query ? query : C8O._getQuery()).split("&"),
			i, id, key, value;
		for (i = 0; i < vars.length; i += 1) {
			if (vars[i].length > 0) {
				id = vars[i].indexOf("=");
				key = (id > 0) ? vars[i].substring(0, id) : vars[i];
				value = "";
				if (id > 0) {
					value = vars[i].substring(id + 1);
					if (value.length) {
						value = value.replace(C8O._define.re_plus, " ");
						try {
							value = decodeURIComponent(value);
						} catch (err1) {
							try {
								value = unescape(value);
							} catch (err2) {}
						}
					}
				}
				C8O.appendValue(data, key, value);
			}
		}
		return data;
	},
	
	_remove: function (object, attribute) {
		var res = object[attribute];
		delete object[attribute];
		return res;
	},
	
	_translate: function (str) {
		var value = C8O._define.dictionnary[str];
		if (C8O.isUndefined(value)) {
			value = str;
			C8O.log.info("c8o.core: not found in dictionnary '" + str + "'");
		} else {
			C8O.log.trace("c8o.core: translate '" + str + "' in '" + value + "'");
		}
		return value;
	},
	
	_init: function (params) {
		var value = C8O._remove(params, "__enc");
		if (C8O.isDefined(value)) {
			C8O.log.trace("c8o.core: switch request encryption " + value);
			C8O.init_vars.enc = value;
		}
		if (C8O.init_vars.enc == "true" && C8O.isUndefined(C8O._define.publickey)) {
			C8O.log.debug("c8o.core: request encryption enabled");
			
			if (C8O.isUndefined(C8O._init_rsa)) {
				C8O._getScript(C8O._define.plugins_path + "rsa.js", function () {
					C8O._init_rsa(params);
				});
			} else {
				C8O._init_rsa(params);
			}
		} else {
			C8O._define.connector = params.__connector;
			C8O._define.context = params.__context;

			C8O._retrieve_vars(params);
			
			if (C8O._hook("init_finished", params) && C8O.vars.first_call == "true") {
				C8O.log.debug("c8o.core: make the first_call");
				C8O.call(params);
			} else {
				C8O.log.trace("c8o.core: hide the initial wait div");
				C8O.waitHide();
			}
		}
	}
}

$.ajaxSettings.traditional = true;
$.ajaxSetup({
	type: C8O.vars.ajax_method,
	dataType: "xml"
});

$(document).ready(function () {
	C8O.log.debug("c8o.core: start document ready");
	
	var matcher = window.location.href.match(new RegExp("/projects/([^/]+)"));
	if (matcher != null) {
		C8O._define.project = matcher[1];
		C8O.log.trace("c8o.core: current project is " + C8O._define.project);
	}
	
	C8O._define.plugins_path = window.location.href.replace(new RegExp("/projects/.*"), "/scripts/6.3.0/c8o.plugin.");
	if (C8O.ro_vars.i18n_files.length > 0) {
		C8O.log.trace("c8o.core: i18n enabled");
		
		if (C8O.init_vars.i18n == "") {
			var lang = C8O.getBrowserLanguage();
			C8O.log.trace("c8o.core: current browser language is " + lang);
			
			C8O.init_vars.i18n = ($.inArray(lang, C8O.ro_vars.i18n_files) == -1) ? C8O.ro_vars.i18n_files[0] : lang;
			
			C8O.log.debug("c8o.core: current active language is " + C8O.init_vars.i18n);
		}
		var jqxhr = $.getJSON("i18n/" + C8O.vars.i18n + ".json", function (dictionnary) {
			C8O.log.debug("c8o.core: translation dictionnary received for " + C8O.init_vars.i18n);
			
			C8O._define.dictionnary = dictionnary;
			C8O.translate(document.documentElement);
			
			C8O.log.debug("c8o.core: translation done in " + C8O.init_vars.i18n);
		}).always(function () {
			C8O._onDocumentReady();
		});
	} else {
		C8O._onDocumentReady();
	}
});