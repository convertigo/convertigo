C8O = {	
	vars : { /** customizable value */
		ajax_method : "POST", /** POST/GET */
		requester_prefix : ""
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
		
	call : function (data) {
		C8O._loadingStart();
		if (typeof(data) === "string") {
			data = C8O._parseQuery({}, data);
		} else if (C8O.isUndefined(data)) {
			data = {};
		} else if (!$.isPlainObject(data) && $(data).is("form")) {
			data = C8O._parseQuery({}, $(data).serialize());
		}
		
		for (key in C8O.vars) {
			if (!C8O.isUndefined(data["__" + key])) {
				C8O.vars[key] = C8O._remove(data, "__" + key);
			}
		}
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
	
	_define : {
		hooks : {},
		last_call_params : {},
		recall_params : {__context : "", __connector : ""}
	},
	
	_call : function (data) {
		C8O._define.last_call_params = data;
		if (C8O._hook("call", data)) {
			$.ajax({
				data : data,
				dataType : "xml",
				success : function (xml, status, xhr) {
					if (C8O._hook("xml_response", xml)) {
						C8O._loadingStop();
					}
				},
				type : C8O.vars.ajax_method,
				url : "../../" + C8O.vars.requester_prefix + ".xml"
			});
		}
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
				$.mobile.showPageLoadingMsg();
			} catch (e) {}
		}
	},
	
	_loadingStop : function () {
		if (C8O._hook("loading_stop")) {
			try {
				$.mobile.hidePageLoadingMsg();
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
					try {
						value = decodeURIComponent(vars[i].substring(id + 1));
					}catch (err1) {
						try {
							value = unescape(vars[i].substring(id + 1));
						}catch (err2) {}
					}
				}
				if (C8O.isUndefined(data[key])) {
					data[key] = value;
				} else if ($.isArray(data[key])) {
					data[key].push(value);
				} else {
					data[key] = [data[key]].concat([value]);
				}
			}
		}
		return data;
	},
	
	_remove : function (object, attribute) {
		var res = object[attribute];
		delete object[attribute];
		return res;
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
	C8O._hook("document_ready");
});