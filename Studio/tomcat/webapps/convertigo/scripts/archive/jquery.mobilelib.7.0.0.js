C8O = {	
	vars : { /** customizable value */
		ajax_method : "POST", /** POST/GET */
		requester_prefix : "",
		i18n : "",
		endpoint_url : "../../"
	},
	
	options : {
		loading : {}
	},
		
	addHook : function (name, fn) {
		if ($.isFunction(fn)) {
			if (!$.isArray(C8O._define.hooks[name])) {
				C8O._define.hooks[name] = [];
			}
			C8O._define.hooks[name].push(fn);
		}
	},
	
	addRecallParameter : function (parameter_name, parameter_value) {
		if (C8O.isUndefined(parameter_value)) {
			parameter_value = "";
		}
		C8O._define.recall_params[parameter_name] = parameter_value;
	},
	
	appendValue : function (data, key, value) {
		if (C8O.isUndefined(data[key])) {
			data[key] = value;
		} else if ($.isArray(data[key])) {
			data[key].push(value);
		} else {
			data[key] = [data[key], value];
		}
	},
		
	call : function (data) {
		C8O._loadingStart();
		if (typeof(data) === "string") {
			data = C8O._parseQuery({}, data);
		} else if (C8O.isUndefined(data)) {
			data = {};
		} else if (!$.isPlainObject(data) && $(data).is("form")) {
			data = C8O.formToData(data);
		}
		
		C8O._retrieve_vars(data);
		
		for (key in C8O._define.recall_params) {
			if (C8O._define.recall_params.hasOwnProperty(key)) {
				if (!C8O.isUndefined(data[key])) {
					C8O._define.recall_params[key] = data[key];
				}
				if (C8O._define.recall_params[key]) {
					data[key] = C8O._define.recall_params[key];
				} else {
					delete data[key];
				}
			}
		}
		
		C8O._call(data);
	},
	
	formToData : function ($form, data) {
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
	
	getLastCallParameter : function (key) {
		if (C8O.isUndefined(key)) {
			return C8O._obj_clone(C8O._define.last_call_params);
		} else {
			return C8O._define.last_call_params[key];
		}
	},
	
	isDefined : function (obj) {
		return typeof(obj) !== "undefined";
	},
	
	isUndefined : function (obj) {
		return typeof(obj) === "undefined";
	},
	
	removeRecallParameter : function (parameter_name) {
		delete C8O._define.recall_params[parameter_name];
	},
	
	translate : function (elt) {
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
	
	/**
	 * Walk each node and attribute and call the specified function
	 */
	walk: function (elt, data, fn) {
		if (elt.nodeType) {
			if (elt.nodeType == Node.ELEMENT_NODE) {
				for (var i = 0; i < elt.attributes.length; i++) {
					var fnr = fn(elt.attributes[i].nodeValue, data);
					
					if (fnr != null) {
						elt.attributes[i].nodeValue = fnr;
					}
				}
				for (var i = 0; i < elt.childNodes.length; i++) {
					C8O.walk(elt.childNodes[i], data, fn);
				}
			} else if (elt.nodeType == Node.TEXT_NODE) {
				var fnr = fn(elt.nodeValue, data);
				
				if (fnr != null) {
					elt.nodeValue = fnr;
				}
			}
		} else if (elt.each) {
			elt.each(function () {
				C8O.walk(this, data, fn);
			});
		}
	},
	
	_define : {
		hooks : {},
		last_call_params : {},
		pendingXhrCpt : 0,
		recall_params : {__context : "", __connector : ""},
		re_plus : new RegExp("\\+", "g"),
		re_i18n : new RegExp("__MSG_(.*?)__"),
		dictionnary : null
	},
	
	_call : function (data) {
		C8O._define.last_call_params = data;
		if (C8O._hook("call", data)) {
			var jqXHR = $.ajax({
				data : data,
				dataType : "xml",
				success : C8O._onSuccess,
				type : C8O.vars.ajax_method,
				url : C8O.vars.endpoint_url + C8O.vars.requester_prefix + ".pxml"
			});
			jqXHR.C8O_data = data;
			C8O._define.pendingXhrCpt++;
		}
	},
	
	_onSuccess : function (xml, status, jqXHR) {
		if (--C8O._define.pendingXhrCpt <= 0) {
			C8O._define.pendingXhrCpt = 0;
			C8O._loadingStop();
		}
		C8O._hook("xml_response", xml, jqXHR.C8O_data);
	},
	
	_retrieve_vars : function (data) {
		for (key in C8O.vars) {
			if (!C8O.isUndefined(data["__" + key])) {
				C8O.vars[key] = C8O._remove(data, "__" + key);
			}
		}
	},
	
	_findAndSelf : function ($elt, selector) {
		return $elt.filter(selector).add($elt.find(selector));
	},
	
	_getAttributes : function (element) {
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
	
	_getFunction : function (functionObject) {
		try {
			if (typeof(functionObject) == "function") {
				return functionObject;
			} else {
				var parts = functionObject.split(".");
				var fn = window;
				for (var i in parts) {
					fn = fn[parts[i]];
				}
				
				return typeof(fn) == "function" ? fn : null;
			}
		} catch (e) {
			return null;
		}
	},
	
	_getQuery : function () {
		var l = window.location,
			q = l.search.length > 0 ? l.search.substring(1) : "",
			h = l.hash.length > 0 ? l.hash.substring(1) : "";
		return (q.length > 0 && h.length > 0) ? (q + "&" + h) : (q.length > 0 ? q : h);
	},
	
	_hook : function (name) {
		var ret = true, i, r;
		if ($.isArray(C8O._define.hooks[name])) {
			for (i = 0;i < C8O._define.hooks[name].length && ret;i += 1) {
				r = C8O._define.hooks[name][i].apply(this, $.makeArray(arguments).slice(1));
				if (!C8O.isUndefined(r)) {
					ret = r;
				}
			}
		}
		return ret;
	},
	
	_loadingStart : function () {
		if (C8O._hook("loading_start")) {
			$("#c8oloading").show();
			try {
				$.mobile.loading("show", C8O.options.loading);
			} catch (e) {}
		}
	},
	
	_loadingStop : function () {
		if (C8O._hook("loading_stop")) {
			try {
				$.mobile.loading("stop");
			} catch (e) {}
			$("#c8oloading").hide();
		}
	},
	
	_parseQuery : function (params, query) {
		var data = (C8O.isUndefined(params)) ? {}:params,
			vars = (query?query:C8O._getQuery()).split("&"),
			i, id, key, value;
		for (i = 0;i < vars.length; i += 1) {
			if (vars[i].length > 0) {
				id = vars[i].indexOf("=");
				key = (id > 0)?vars[i].substring(0, id):vars[i];
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
	
	_remove : function (object, attribute) {
		var res = object[attribute];
		delete object[attribute];
		return res;
	},
	
	_translate : function (str) {
		var value = C8O._define.dictionnary[str];
		if (C8O.isUndefined(value)) {
			value = str;
			console.log(value + " not found in dictionnary");
		}
		return value;
	}
};

$.ajaxSettings.traditional = true;
$.ajaxSetup({
	type : C8O.vars.ajax_method,
	dataType : "xml"
});

$(document).ready(function () {
	if (!$.mobile.ajaxBlacklist) {
		$("<div id=\"c8oloading\"/>").css({backgroundColor : "grey", position : "absolute", width : "100%", height : "100%", opacity : 0.5, "z-index" : 99}).hide().appendTo("body");
	}
	
	if (C8O.vars.i18n != "") {
		$.getJSON("i18n/" + C8O.vars.i18n + ".json", function (dictionnary) {
			C8O._define.dictionnary = dictionnary;
			C8O.translate(document.documentElement);
			C8O._hook("document_ready");
		});
	} else {
		C8O._hook("document_ready");
	}
});