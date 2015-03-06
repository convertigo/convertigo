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
        log_remote: "true",
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
    
    call: function (requestable, data) {
    	if (typeof(requestable) == "string") {
    		 var reqData = C8O._parseRequestable(requestable);
    		 
    		 if (reqData == null) {
    			 data = requestable;
    		 }
    		 
    		 if (typeof(data) == "string") {
    			 C8O.log.trace("c8o.core: call parse string data '" + data + "'");
    			 data = C8O._parseQuery({}, data);
    		 }
    		 
    		 if (reqData != null) {
    			 data = $.extend(reqData, data);
    		 }
    	} else {
    		data = requestable;
    	}
    	
        var key;
        if (C8O.canLog("info")) {
            C8O.log.info("c8o.core: call: " + C8O.toJSON(data));
        }

        C8O.log.trace("c8o.core: call show wait div");
        C8O.waitShow();
        
        if (C8O.isUndefined(data)) {
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
                var triggered = false;
                var $iframe = $("<iframe/>").attr({
                    src: "",
                    style: "display: none"
                }).appendTo("body").on("load", function () {
                    if (!triggered) {
                        C8O.log.debug("c8o.core: iframe load before submit");
                    }
                    
                    C8O.log.debug("c8o.core: call using a form response");
                    
                    var fakeXHR = {
                        C8O_data: data
                    };
                    
                    if (C8O.vars.xsl_side == "server") {
                        fakeXHR.responseText = $iframe[0].contentWindow.document.outerHTML;
                        C8O._onCallSuccess(null, "success", fakeXHR);
                    } else {
                        var xml = $iframe[0].contentWindow.document.XMLDocument;
                        fakeXHR.responseText = "No responseText for multipart, use XSL or xml_response.";
                        C8O._onCallSuccess(xml ? xml: $iframe[0].contentWindow.document, "success", fakeXHR);
                    }
                    C8O._onCallComplete(fakeXHR, "success");
                    $iframe.remove();
                });
                $iframe[0].contentWindow.name = targetName;
                window.setTimeout(function () {
                    var triggered = true;
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
                    data[key] = C8O._define.recall_params[key];
                    C8O.log.debug("c8o.core: add value of recall parameter '" + key + "' = '" + data[key] + "'");
                } else {
                    delete data[key];
                }
            }
        }
        
        C8O._call(data);
    },

    canLog: function (level) {
        return C8O._canLogConsole(level) || C8O._canLogRemote(level);
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
                newelt.setAttribute(input.attributes[i].name, input.attributes[i].value);
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
        return output;
    },
    
    deleteAllCacheEntries: function (success, error) {
        C8O.log.debug("c8o.core: deleteAllCacheEntries");
        
        C8O._get_cache_db(function (db) {
            db.transaction(function (tx) {
                tx.executeSql("DELETE FROM cacheIndex", [], function () {
                    C8O.log.info("c8o.core: deleteAllCacheEntries deleted all entries ok.");
                    
                    if (success) {
                        success()
                    }
                }, function (err) {
                    C8O.log.error("c8o.core: deleteAllCacheEntries failed to DELETE cacheIndex content: " + C8O.toJSON(err));
                    
                    if (error) {
                        error(err);
                    }
                });
            }, function(err) {
                C8O.log.error("c8o.core: deleteAllCacheEntries cannot get tx: " + C8O.toJSON(err));
                
                if (error) {
                    error(err);
                }
            });
        }, function (err) {
            C8O.log.error("c8o.core: deleteAllCacheEntries cannot delete entries (no DB): " + C8O.toJSON(err));

            if (error) {
                error(err);
            }
        });
    },
    
    formToData: function ($form, data) {
        $form = $($form);
        if (C8O.isUndefined(data)) {
            data = {};
        }
        var formArray = $form.serializeArray();
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
    
    serializeXML: function (xmlDom) {
        return (typeof XMLSerializer != "undefined") ?
             (new window.XMLSerializer()).serializeToString(xmlDom) :
             xmlDom.xml;
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
        C8O._hook("wait_hide");
    },
    
    waitShow: function () {
        C8O._hook("wait_show");
    },
    
    /**
     * Walk each node and attribute and call the specified function
     */
    walk: function (node, data, fn, fn_validate) {
        if (node.nodeType && (typeof fn_validate != "function" || fn_validate(node, data))) {
            if (node.nodeType == Node.ELEMENT_NODE) {
                for (var i = 0; i < node.attributes.length; i++) {
                    var fnr = fn.call(node, node.attributes[i].value, data);
                    
                    if (fnr != null) {
                        node.attributes[i].value = fnr;
                    }
                }
                for (var i = 0; i < node.childNodes.length; i++) {
                    C8O.walk(node.childNodes[i], data, fn, fn_validate);
                }
            } else if (node.nodeType == Node.TEXT_NODE) {
                var fnr = fn.call(node, node.nodeValue, data, fn_validate);
                
                if (fnr != null && node.parentNode != null) {
                    node.nodeValue = fnr;
                }
            }
        } else if (node.each) {
            node.each(function () {
                C8O.walk(this, data, fn, fn_validate);
            });
        }
    },
    
    _define: {
    	convertigo_path: null,
        dictionnary: null,
        hooks: {},
        init_wait: [],
        last_call_params: {},
        local_cache_db: null,
        local_cache_db_size: 2 * 1024 * 1024,
        log_levels: ["none", "error", "warn", "info", "debug", "trace"],
        log_buffer: [],
        log_remote_env: null,
        log_remote_init_env: null,
        log_remote_path: null,
        log_remote_pending: null,
        log_remote_level: "trace",
        pendingXhrCpt: 0,
        plugins_path: null,
        project: null,
        recall_params: {__context: "", __connector: ""},
        re_format_time: new RegExp(" *(\\d*?)([\\d ]{4})((?:\\.[\\d ]{3})|(?: {4})) *"), // replace by "$1$2$3"
        re_i18n: new RegExp("__MSG_(.*?)__"),
        re_not_normalized: new RegExp("[^a-zA-Z0-9_]", "g"),
        re_plus: new RegExp("\\+", "g"),
        re_project: new RegExp("/projects/([^/]+).*"),
        re_requestable: new RegExp("^([^.]*)\\.(?:([^.]+)|(?:([^.]+)\\.([^.]+)))$"), // 1: project ; 2: sequence ; 3: connector ; 4: transaction > 1+2 | 1+3+4
        start_time: new Date().getTime(),
        uid: Math.round((new Date().getTime() * Math.random())).toString(36)
    },

    _call: function (data) {
        C8O._define.last_call_params = data;
        if (C8O._hook("call", data)) {
            var doCall = function (doLocal) {
                if (doLocal && C8O.isDefined(data.__localCache)) {
                    if (typeof(data.__localCache) == "string") {
                        data.__localCache = $.parseJSON(data.__localCache);
                    }
                    
                    data.__localCache.enabled = C8O.isUndefined(data.__localCache.enabled) || data.__localCache.enabled == true;
                    
                    C8O._get_cache_db(function () {
                        C8O.log.info("c8o.core: _call request local cache");
                        
                        C8O._local_cache_handle_expired();
                        
                        var cacheOptions = data.__localCache;
                        
                        if (cacheOptions.enabled) {
                            
                            // we have cache options so handle local cache here
                            if (cacheOptions.policy == "priority-local") {
                                
                                // We have to search for the data in the cache before calling the server
                                C8O.log.debug("c8o.core: _call priority-local, first search local cache...");
                                
                                C8O._local_cache_search_entry(data, function (xml) {
                                    C8O.log.debug("c8o.core: _call priority-local, entry found");
                                    
                                    // we found an entry for this key, Create a fake XHR and notify CTF
                                    // with the data found.
                                    delete data.__localCache;
                                    
                                    // add LocalCache attribute to response
                                    $(xml).find("document").attr("localcache", "true");
                                    
                                    var fakeXHR = {
                                            C8O_data: data
                                    };
                                    
                                    C8O._onCallComplete(fakeXHR, "success");
                                    C8O._onCallSuccess(xml, "success", fakeXHR);
                                }, function (err) {
                                    C8O.log.debug("c8o.core: _call priority-local, no entry found, do a network call");
                                    
                                    doCall(false);
                                });
                            } else if (cacheOptions.policy == "priority-server") {
                                // Call the server anyway.. If a network error occurs, we will be notified in the
                                // call_error hook. Then, we will get the data from the cache if we have it.
                                C8O.log.debug("c8o.core: _call priority-server, do a network call");
                                
                                doCall(false);
                            }
                        }
                    }, function () {
                        delete data.__localCache;
                        doCall(false);
                    });
                    return;
                }
                
                var netData = C8O._hook("_call_rsa", data);
                if (typeof netData != "object") {
                    netData = data;
                }
                
                var url = C8O._getCallUrl();
                if (C8O.canLog("trace")) {
                    C8O.log.trace("c8o.core: call " + C8O.toJSON(netData) + " " + C8O.vars.ajax_method + " " + url);
                }
                
                var jqXHR = $.ajax({
                    complete: C8O._onCallComplete,
                    data: netData,
                    dataType: C8O.vars.xsl_side == "server" ? "text" : "xml",
                    error: C8O._onCallError,
                    success: C8O._onCallAjaxSuccess,
                    type: C8O.vars.ajax_method,
                    url: url
                });
                
                jqXHR.C8O_data = data;
                C8O._define.pendingXhrCpt++;
            };
            doCall(true);
        }
    },
    
    _canLogConsole: function (level) {
        return window.console &&
            window.console.log &&
            $.inArray(level, C8O._define.log_levels) <= $.inArray(C8O.vars.log_level, C8O._define.log_levels);
    },
    
    _canLogRemote: function (level) {
        return C8O.vars.log_remote == "true" &&
            C8O._define.log_remote_path != null &&
            $.inArray(level, C8O._define.log_levels) <= $.inArray(C8O._define.log_remote_level, C8O._define.log_levels);
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
                attributes[element.attributes[i].name] = element.attributes[i].value
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
    
    _get_cache_db: function (success, error) {
        if (C8O._define.local_cache_db == null) {
            C8O.log.info("c8o.core: _get_cache_db request local cache, try to open the database, with a size of: " + C8O._define.local_cache_db_size);
            
            try {
                C8O._define.local_cache_db = window.openDatabase("c8o_local_cache", "1.0", "Convertigo Local Cache", C8O._define.local_cache_db_size);
                
                C8O.log.info("c8o.core: local cache database created/opened");
                
                C8O._define.local_cache_db.transaction(function (tx) {
                    tx.executeSql("CREATE TABLE IF NOT EXISTS cacheIndex (key unique, data, expirydate)", [], function () {
                        C8O.log.debug("c8o.core: _get_cache_db created if not exists the 'cacheIndex' table");
                        
                        success(C8O._define.local_cache_db);
                    }, function (err) {
                        C8O.log.error("c8o.core: _get_cache_db error creating 'cacheIndex' table, disable the local cache", err);
                        
                        C8O._define.local_cache_db = false;
                        error(err);
                    });
                }, function (err) {
                    C8O.log.error("c8o.core: _get_cache_db failed to get tx", err);
                    
                    C8O._define.local_cache_db = false;
                    error(err);
                });
                return;
            } catch (err) {
                C8O.log.warn("c8o.core: failed to open the local cache database (log level debug to see more)");
                if (C8O.canLog("debug")) {
                    C8O.log.debug("c8o.core: failed to open the local cache database, details:", err);
                }
                C8O._define.local_cache_db = false;
            }
        }
        
        if (C8O._define.local_cache_db) {
            success(C8O._define.local_cache_db);
        } else {
            error("Failed to get the cache database: " + C8O._define.local_cache_db);
        }
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
    
    _init: {
    	tasks: [],
    	locks: {},
    	params: null,
    	done: false,
    	check: function (params) {
    		if (C8O.isDefined(params)) {
    			C8O._init.params = params;
    		} else {
    			params = C8O._init.params;
    		}
    		
    		if (C8O._init.done) {
    			C8O.log.debug("c8o.core: init already done!");
    		} else if (C8O._init.tasks.length) {
    			var ret;
    			do {
	    			var fun = C8O._init.tasks.shift();    			
	    			if (ret = (fun.call(this, params) === true && C8O._init.tasks.length)) {
	    				C8O._init.tasks.push(fun);
	    			}
    			} while(ret);
    		} else	if ($.isEmptyObject(C8O._init.tasks.locks)) {
    			C8O._init.done = true;
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
    		} else {
    			C8O.log.debug("c8o.core: init not finish, remains locks: " + C8O.toJSON(C8O._init.locks));
    		}
    	}
    },
    
    _jsonToXml: function (key, json, parentElement) {
    	if (key == undefined) {
    		if ($.isPlainObject(json)) {
    			for (var i in json) {
    				C8O._jsonToXml(i, json[i], parentElement)
    			}
    		} else if ($.isArray(json)) {
    			for (var i = 0; i < json.length; i++) {
    				var item = parentElement.ownerDocument.createElement("item");
    				parentElement.appendChild(item);
    				C8O._jsonToXml(undefined, json[i], item);
    			}
    		} else {
    			parentElement.textContent = json;
    		}
    	} else {
    		if ("_attachments" == parentElement.nodeName) {
    			var att = parentElement.ownerDocument.createElement("attachment");
    			var att_name = parentElement.ownerDocument.createElement("name");
    			att_name.textContent = key;
    			att.appendChild(att_name);
    			parentElement.appendChild(att);
    			C8O._jsonToXml(undefined, json, att);
    		} else {
    			var child;
    			try {
    				child = parentElement.ownerDocument.createElement(key);
    			} catch (e) {
    				key = key.replace(C8O._define.re_not_normalized, "_");
    				child = parentElement.ownerDocument.createElement(key);
    			}
    			parentElement.appendChild(child);
    			C8O._jsonToXml(undefined, json, child);
    		}
    	}
    },
    
    _local_cache_handle_expired: function (success, error) {
        var now = new Date();
        C8O.log.debug("c8o.core: _local_cache_handle_expired current time: " + now.toISOString());
        
        C8O._define.local_cache_db.transaction(function (tx) {
            tx.executeSql("DELETE FROM cacheIndex WHERE expirydate < ?", [now.getTime()], function (tx, rs) {
                C8O.log.debug("c8o.core: _local_cache_handle_expired removed all expired entries", rs);
                
                if (success) {
                    success();
                }
            }, function(err) {
                C8O.log.error("c8o.core: _local_cache_handle_expired failed to DELETE old entries", err);
                
                if (error) {
                    error(err);
                }
            });
        }, function(err) {
            C8O.log.error("c8o.core: _local_cache_handle_expired failed to get tx", err);
            
            if (error) {
                error(err);
            }
        });
    },
    
    _local_cache_insert: function (key, data, success, error) {
        var cacheOptions = key.__localCache;
        delete key.__localCache;
        var tKey = C8O.toJSON(key);
        var tData = C8O.serializeXML(data);
        var now = new Date();

        // Compute expiry date
        var expDate = (cacheOptions.ttl) ? cacheOptions.ttl + now.getTime() : new Date("3000-01-01").getTime();

        C8O.log.debug("c8o.core: _local_cache_insert for key: " + tKey + " with expiry date: " + expDate + " (" + new Date(expDate).toISOString()+") in directory: " + now.getTime() + " with data lenght: " + tData.length);
        
        if (C8O.canLog("trace")) {
            C8O.log.trace("c8o.core: _local_cache_insert data to cache is: " + tData);
        }
        
        C8O._define.local_cache_db.transaction(function (tx) {
            tx.executeSql("INSERT INTO cacheIndex (key, data, expirydate) VALUES(? , ?, ?)", [tKey, tData, expDate], function () {
                C8O.log.debug("c8o.core: _local_cache_insert insert in cache done");
                
                if (success) {
                    success();
                }
            }, function (err) {
                tx.executeSql("UPDATE cacheIndex SET data=? , expirydate=? WHERE key = ?", [tData, expDate, tKey], function() {
                    C8O.log.debug("c8o.core: _local_cache_insert update in cache done");
                    
                    if (success) {
                        success();
                    }
                }, function (err) {
                    C8O.log.error("c8o.core: _local_cache_insert update in cache failed", err);
                    
                    if (error) {
                        error(err);
                    }
                });
            });
        }, function (err) {
            C8O.log.error("c8o.core: _local_cache_insert failed to get tx", err);
            
            if (error) {
                error(err);
            }
        });
    },
    
    _local_cache_search_entry: function (key, success, error) {
        key = C8O._obj_clone(key);
        delete key.__localCache;
        var tKey = C8O.toJSON(key);
        
        C8O.log.debug("c8o.core: _local_cache_search_entry search for: " + tKey);
        
        C8O._define.local_cache_db.readTransaction(function (tx) {
            tx.executeSql("SELECT data FROM cacheIndex WHERE key=?", [tKey], function(tx, results) {
                if (results.rows.length == 0) {
                    C8O.log.debug("c8o.core: _local_cache_search_entry no data found for key: " + tKey);
                    
                    error("Key not found");
                } else {
                    C8O.log.debug("c8o.core: _local_cache_search_entry data found for: " + tKey);
                    
                    if (C8O.canLog("trace")) {
                        C8O.log.trace("c8o.core: _local_cache_search_entry data is: " + results.rows.item(0).data);
                    }
                    
                    C8O._local_cache_search_entry_success(results.rows.item(0).data, success, error);
                }
            }, function (err) {
                C8O.log.error("c8o.core: _local_cache_search_entry failed to SELECT the key:" + tKey, err);
                
                error(err);
            });
        }, function (err) {
            C8O.log.error("c8o.core: _local_cache_search_entry failed to get tx", err);
            
            error(err);
        });
    },
    
    _local_cache_search_entry_success: function (data, success, error) {
        try {
            success($.parseXML(data));
        } catch (err) {
            C8O.log.error("c8o.core: _local_cache_search_entry_success to parse the xml", err);
            if (C8O.canLog("debug")) {
                C8O.log.debug("c8o.core: _local_cache_search_entry_success failed to parse: " + data);
            }
            
            if (error) {
                error(error);
            }
        }
    },
    
    _log: function (level, msg, e) {
        var isLogConsole = C8O._canLogConsole(level);
        var isLogRemote = C8O._canLogRemote(level);
        
        if (isLogConsole || isLogRemote) {
            var ret;
            if (ret = C8O._hook("log", level, msg, e)) {
                if (typeof(ret) == "string") {
                    msg = ret;
                }
                if (C8O.isDefined(e)) {
                    var err = "";
                    if (C8O.isDefined(e.stack) && e.stack.length) {
                        err += e.stack;
                    } else if (typeof(e.toString) == "function") {
                        err += e.toString();
                    } else {
                        if (C8O.isDefined(e.name)) {
                            err += e.name + ": ";
                        }
                        if (C8O.isDefined(e.message)) {
                            err += e.message;
                        } else {
                            err += C8O.toJSON(e);
                        }
                    }
                    msg += "\n" + err;
                }
                if (C8O.vars.log_line == "true" && navigator.userAgent.indexOf("Chrome") != -1) {
                    msg += "\n\t\t" + new Error().stack.split("\n")[3];
                }
                var time = (new Date().getTime() - C8O._define.start_time) / 1000;
                
                if (isLogRemote) {
                    C8O._define.log_buffer.push({
                        time:  time,
                        level: level,
                        msg:   msg
                    });
                    C8O._log_remote();
                }
                
                if (isLogConsole) {
                    time = ("    " + time + "    ").replace(    C8O._define.re_format_time, "$1$2$3");
                    
                    if (level.length == 4) {
                        level += " ";
                    }
                    
                    console.log(time + " [" + level + "] " + msg);
                }
            }
        }
    },
    
    _log_remote: function () {
        if (C8O._define.log_buffer.length && !C8O._define.log_remote_pending) {
            var data = {
                logs: C8O.toJSON(C8O._define.log_buffer),
                env: C8O.toJSON($.extend({}, C8O._define.log_remote_env, C8O._define.log_remote_init_env))
            };
            
            C8O._define.log_remote_init_env = null;
            C8O._define.log_remote_pending = C8O._define.log_remote_level;
            C8O._define.log_buffer = [];
            
            $.ajax({
                data: data,
                dataType: "json",
                type: "POST",
                url: C8O._define.log_remote_path
            }).done(function (data) {
                C8O._define.log_remote_level = data.remoteLogLevel;
                
                if (!C8O._canLogRemote(C8O._define.log_remote_pending)) {
                    for (var i = 0; i < C8O._define.log_buffer.length; i++) {
                        if (!C8O._canLogRemote(C8O._define.log_buffer[i].level)) {
                            C8O._define.log_buffer.splice(i--, 1)
                        };
                    }
                }
                
                C8O._define.log_remote_pending = null;
                C8O._log_remote();
            }).fail(function (jqXHR, textStatus, errorThrown) {
                C8O._define.log_remote_pending = null;
                C8O.vars.log_remote = "false";
                C8O.log.error("c8o.core: _log_remote failed to perform remote logging", "textStatus: '" + textStatus + "' errorThrown: '" + errorThrown + "'");
            });
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
    
    _onCallComplete: function (jqXHR, textStatus) {
        if (--C8O._define.pendingXhrCpt <= 0) {
            C8O._define.pendingXhrCpt = 0;
        }
        
        if (C8O._hook("call_complete", jqXHR, textStatus, jqXHR.C8O_data)) {
            if (!C8O._define.pendingXhrCpt) {
                C8O.waitHide();
                C8O.log.debug("c8o.core: Ajax complete, hide the wait div");
            } else {
                C8O.log.trace("c8o.core: Ajax complete, remains " + C8O._define.pendingXhrCpt + " pending requests");
            }
        }
    },
    
    _onCallError: function (jqXHR, textStatus, errorThrown) {
        var data = jqXHR.C8O_data;
        
        if (C8O.isDefined(data.__localCache)) {
            var cacheOptions = data.__localCache;
            delete data.__localCache;
            
            if (cacheOptions.enabled) {
                // we have cache options so handle local cache here
                if (cacheOptions.policy == "priority-server") {
                    C8O._local_cache_search_entry(data, function (xml) {
                        C8O.log.debug("c8o.core: _onCallError priority-server, entry found");
                        
                        // add LocalCache attribute to response
                        $(xml).find("document").attr("localcache", "true");
                        
                        var fakeXHR = {
                                C8O_data: data
                        };
                        
                        C8O._onCallComplete(fakeXHR, "success");
                        C8O._onCallSuccess(xml, "success", fakeXHR);
                    }, function (err) {
                        C8O.log.debug("c8o.core: _onCallError no data found in cache and no network, notify the error", err);
                        
                        C8O._onCallError(jqXHR, textStatus, errorThrown);
                    });
                    return;
                }
            }
        }
        
        if (C8O._hook("call_error", jqXHR, textStatus, errorThrown, jqXHR.data)) {
            C8O.log.debug("c8o.core: network error occured for request: " + C8O.toJSON(data) + " . Error is: " + C8O.toJSON(errorThrown) + "/" + textStatus);
            // seems we do not have network or the server is unreachable.. Lookup in local cache

            C8O.log.error("c8o.core: Ajax error [" + textStatus + "]", errorThrown);
        }
    },
    
    _onCallAjaxSuccess: function (xml, status, jqXHR) {
        C8O.log.debug("c8o.core: Ajax success, request is: ", jqXHR.C8O_data);
        
        if (xml == null) {
            C8O.log.debug("c8o.core: xml is null, call the error handler"); // WP8 case with no network
            C8O._onCallError(jqXHR, "error", "xml is null");
            return;
        }
        
        if (C8O.isDefined(jqXHR.C8O_data.__localCache)) {
            var cacheOptions = jqXHR.C8O_data.__localCache;
            
            if (cacheOptions.enabled) {
                C8O.log.trace("c8o.core: _onCallAjaxSuccess xml will be stored in cache");
                
                C8O._local_cache_insert(jqXHR.C8O_data, xml, function () {
                    C8O.log.trace("c8o.core: _onCallAjaxSuccess xml successfuly stored");
                    
                    delete jqXHR.C8O_data.__localCache;
                    C8O._onCallSuccess(xml, status, jqXHR);
                }, function (err) {
                    C8O.log.error("c8o.core: _onCallAjaxSuccess failed to insert the xml in cache", err);
                    
                    delete jqXHR.C8O_data.__localCache;
                    C8O._onCallSuccess(xml, status, jqXHR);
                });
                
                return;
            }
        }
        
        delete jqXHR.C8O_data.__localCache;
        C8O._onCallSuccess(xml, status, jqXHR);
    },
    
    _onCallSuccess: function (xml, status, jqXHR) {
        C8O._hook("xml_response", xml, jqXHR.C8O_data);
    },
    
    _onDocumentReady: function (params) {
        if (C8O._hook("document_ready", params)) {
            C8O._init.check(params);
        };
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
    
    _parseRequestable: function (requestable) {
         var matches = requestable.match(C8O._define.re_requestable);
         
         if (matches != null) {
    		 var data = {};
             if (matches[1].length) {
            	 data.__project = matches[1];
             }
             if (C8O.isDefined(matches[2])) {
            	 data.__sequence = matches[2];
             } else {
            	 data.__connector = matches[3];
            	 data.__transaction = matches[4];
             }
             return data;
         } else {
             return null;
         }
    },
    
    _remove: function (object, attribute) {
        var res = object[attribute];
        delete object[attribute];
        return res;
    },
    
    _retrieve_vars: function (data) {
        for (key in C8O.vars) {
            if (C8O.isDefined(data["__" + key])) {
                C8O.vars[key] = C8O._remove(data, "__" + key);
                C8O.log.debug("c8o.core: retrieve from parameter C8O.vars." + key + "=" + C8O.vars[key]);
            }
        }
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
    }
}

$.ajaxSettings.traditional = true;
$.ajaxSetup({
    type: C8O.vars.ajax_method,
    dataType: "xml"
});
C8O.addRecallParameter("__uid", C8O._define.uid);

C8O._init.tasks.push(function (params) {
	var value = C8O._remove(params, "__enc");
	
    if (C8O.isDefined(value)) {
        C8O.log.trace("c8o.core: switch request encryption " + value);
        C8O.init_vars.enc = value;
    }
    
    if (C8O.init_vars.enc == "true" && !C8O._init.locks.rsa) {
        C8O.log.debug("c8o.core: request encryption enabled");
        
        C8O._getScript(C8O._define.plugins_path + "rsa.js", function () {
        	C8O._init.check(params);
        });
    } else {
        C8O._init.check(params);    	
    }
});

(function () {
    var start = function(){
        C8O.log.debug("c8o.core: start document ready");
        
        var matcher = window.location.href.match(C8O._define.re_project);
        if (matcher != null) {
            C8O._define.project = matcher[1];
            C8O.log.trace("c8o.core: current project is " + C8O._define.project + " in webapp mode");
            
            C8O._define.convertigo_path = window.location.href.replace(C8O._define.re_project, "");
        } else {
            matcher = C8O.vars.endpoint_url.match(C8O._define.re_project);
            if (matcher != null) {
                C8O._define.project = matcher[1];
                C8O.log.debug("c8o.core: current project is " + C8O._define.project + " in mobile mode");
                
                C8O._define.convertigo_path = C8O.vars.endpoint_url.replace(C8O._define.re_project, "");
            } else {
                C8O.log.warn("c8o.core: cannot determine the current project using " + window.location.href + " or " + C8O.vars.endpoint_url);
            }
        }
        
        if (C8O._define.convertigo_path != null) {
            C8O._define.plugins_path = C8O._define.convertigo_path + "/scripts/7.3.0/c8o.plugin.";
            C8O._define.log_remote_path = C8O._define.convertigo_path + "/admin/services/logs.Add";
        }
        
        C8O._define.log_remote_env = {
            uid: C8O._define.uid
        };
        
        C8O._define.log_remote_init_env = {
            project: C8O._define.project
        };
        
        var params = C8O._parseQuery();
        
        if (C8O.ro_vars.i18n_files.length > 0) {
            C8O.log.trace("c8o.core: i18n enabled");
            
            var lang = C8O._hook("get_language", params);
            
            if (typeof lang != "string") {
                lang = C8O._remove(params, "__i18n");
                if (typeof lang != "string") {
                    lang = C8O.init_vars.i18n;
                    if (lang == "") {
                        lang = C8O.getBrowserLanguage();
                    }
                }
            }
            
            C8O.log.trace("c8o.core: current browser language is " + lang);
            
            C8O.init_vars.i18n = ($.inArray(lang, C8O.ro_vars.i18n_files) == -1) ? C8O.ro_vars.i18n_files[0] : lang;
            
            C8O.log.debug("c8o.core: current active language is " + C8O.init_vars.i18n);
            
            var jqxhr = $.getJSON("i18n/" + C8O.init_vars.i18n + ".json", function (dictionnary) {
                C8O.log.debug("c8o.core: translation dictionnary received for " + C8O.init_vars.i18n);
                
                C8O._define.dictionnary = dictionnary;
                C8O.translate(document.documentElement);
                
                C8O.log.debug("c8o.core: translation done in " + C8O.init_vars.i18n);
            }).always(function () {
                C8O._onDocumentReady(params);
            });
        } else {
            C8O._onDocumentReady(params);
        }
    };
    
    if ("cordova" in window) {
        $(document).on("deviceready", start);
    } else {
        $(document).ready(start);
    }
})();