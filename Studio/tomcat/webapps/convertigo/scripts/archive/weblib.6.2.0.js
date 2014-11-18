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
    init_vars : {
        enc : "false", /** enable rsa encoding */
        testplatform : "auto"
    },
    ro_vars : {
        widget_name : "",
        portal_username : ""
    },
    vars : { /** customizable value by adding __name=value in query*/
        ajax_method : "POST", /** POST/GET */
        auto_refresh : "true", /** true/false */
        auto_resize : "true", /** true/false */
        first_call : "true", /** true/false */
        requester_prefix : "",
        resize_offset : "50", /** number */
        send_portal_username : "true", /** true/false */
        target_append : "false", /** true/false */
        target_id : "",
        use_siteclipper_plugin : "true", /** true/false */
        xsl_side : "client" /** client/server */
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
        var key;
        C8O.waitShow();
        if (typeof(data) === "string") {
            data = C8O._parseQuery({}, data);
        } else if (C8O.isUndefined(data)) {
            data = {};
        } else if (!$.isPlainObject(data) && $(data).is("form")) {
            var $form = $(data);
            if ($form.find("input[type=file]").length) {
                var targetName = "tn_" + new Date().getTime() + "_" + Math.floor(Math.random() * 100);
                var action = window.location.pathname.replace(new RegExp("^(.*/).*?$"), "$1") + C8O.vars.requester_prefix + (C8O.vars.xsl_side === "client" ? ".xml":".cxml");
                $form.attr({
                    method : "POST",
                    enctype : "multipart/form-data",
                    action : action,
                    target : targetName
                });
                var $iframe = $("<iframe/>").attr({
                    src : "",
                    style : "display: none"
                }).appendTo("body").on("load", function () {
                    if (action == this.contentWindow.location.pathname) {
                        if (C8O.vars.xsl_side === "client") {
                            var xml = $iframe[0].contentWindow.document.XMLDocument;
                            C8O._onSuccess(xml ? xml : $iframe[0].contentWindow.document, "ok", {responseText : "No responseText for multipart, use XSL or xml_response."});
                        } else {
                            C8O._onSuccess(null, "ok", {responseText : $iframe[0].contentWindow.document.outerHTML});
                        }
                        $iframe.remove();
                    };
                });
                $iframe[0].contentWindow.name = targetName;
                window.setTimeout(function () {
                    $form.trigger("submit." + targetName);
                }, 0);
                return;
            } else {
                data = C8O._parseQuery({}, $(data).serialize());
            }
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
    doMashupEvent : function (event_name, payload) {
        if (payload !== null && typeof(payload) === "object") {
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
        if ($.inArray(type, C8O._define.navigation_var_actions) !== -1) {
            C8O.call({ __event_action : "navbar_" + type });
        }
    },
    doReconnect : function () {
        window.location.reload(false);
    },
    doResize : function (height, options) {
        if (typeof(height) === "number") {
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
    waitHide : function () {
        $("#wait_div").remove();
    },
    waitShow : function () {
        if ($("body #wait_div").length === 0) {
            $("body").append(C8O._define.wait_div);
        }
    },
    _define : {
        clipping_attributs :
            $(["altKey", "ctrlKey", "metaKey", "shiftKey", "clientX", "clientY", "screenX", "screenY", "layerX", "layerY", "pageX", "pageY", "button"]),
        dirty_timer : {},
        hooks : {},
        iframe : false,
        last_call_params : {},
        navigation_var_actions : ["backward", "forward", "stop", "refresh"],
        pendingXhrCpt : 0,
        recall_params : {__context : "", __connector : ""},
        re_plus : new RegExp("\\+", "g")
    },
    _addField : function (params, twsid, value) {
        params["__field_" + twsid] = value;
        return value;
    },
    _call : function (data) {
        C8O._define.last_call_params = data;
        if (C8O._hook("call", data)) {
            $.ajax({
                data : data,
                dataType : C8O.vars.xsl_side === "client" ? "xml":"text",
                success : C8O._onSuccess,
                type : C8O.vars.ajax_method,
                url : C8O.vars.requester_prefix + (C8O.vars.xsl_side === "client" ? ".xml":".cxml")
            });
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
                url : "../../webclipper/" + C8O._define.project + "/" + (C8O._define.connector ? C8O._define.connector : "$") + "/" + (C8O._define.context ? C8O._define.context : "$") + "/d"
            });
        } else {
            C8O._define.dirty_timer = window.setTimeout("C8O._checkDirty(500)", 500);
        }
    },
    _obj_replace : function (object, content) {
        for (var key in object) {
            delete object[key];
        }
        return $.extend(object, content);
    },
    _obj_clone : function (object) {
        return $.extend(true, {}, object);
    },
    _onSuccess : function (xml, status, xhr) {
        if (--C8O._define.pendingXhrCpt <= 0) {
            C8O._define.pendingXhrCpt = 0;
            C8O.waitHide();
        }
        if (C8O.vars.xsl_side === "client") {
            if (C8O._hook("xml_response", xml)) {
                var redirect_location = $(xml.documentElement).attr("redirect_location");
                if (!C8O.isUndefined(redirect_location)) {
                    if (C8O.vars.use_siteclipper_plugin === "true") {
                        C8O._getScript("../../scripts/archive/weblib_plugins.6.1.10/siteclipper.js", function () {
                            C8O._init_siteclipper({redirect_location : redirect_location});
                        });
                    } else {
                        window.location = redirect_location;
                    }
                    return;
                }
                var sheet_uri = C8O._xslStyleSheet(xml);
                if (sheet_uri !== null) {
                    $.ajax({
                        url : sheet_uri,
                        success : function (xsl) {
                            C8O._xslt(xml, xsl);
                        },
                        type : "GET"
                    });
                } else if (!$.support.leadingWhitespace) {
                    C8O._fillBody($("<pre>" + xhr.responseText.replace(/</g, "&lt;").replace(/>/g, "&gt;") + "</pre>"));
                } else {
                    C8O._fillBody($("<pre/>").text(xhr.responseText));
                }
            }
        } else {
            var aText = [xhr.responseText + ""];
            if (C8O._hook("text_response", aText)) {
                C8O._fillBody(aText[0]);
            }
        }
    },
    _onMashupEvent : function (event) {
        if (C8O._hook("receive_mashup_event", event)) {
            if (event.type === "call") {
                C8O.call(event.payload);
            }
        }
    },
    _resize : function (options) {
        var lowest = C8O._hook("resize_calculation");
        if (lowest !== false) {
            if (typeof(lowest) !== "number") {
                lowest = 0;
                $("*:not(html, body)").each(function () {
                    lowest = Math.max(lowest, this.offsetTop + this.offsetHeight);
                });
                lowest += parseInt(C8O.vars.resize_offset);
            }
            C8O.doResize(lowest, options);
        }
    },
    _retrieve_vars : function (data) {
        for (key in C8O.vars) {
            if (!C8O.isUndefined(data["__" + key])) {
                C8O.vars[key] = C8O._remove(data, "__" + key);
            }
        }
    },
    _fillBody : function (content, resize) {
        var $container = C8O.vars.target_id;
        if (typeof($container) === "string") {
            $container = $($container.length === 0 ? "body" : ('#' + $container));
        } else if (C8O.isUndefined($container.jquery)){
            $container = $($container);
        }
        if (C8O.vars.target_append === "true") {
            $container.append(content);
        } else {
            $container.html(content).children("title:first").each(function () {
                window.document.title = $(this).text();
            });
        }
        if (C8O._hook("result_filled", $container)) {
            $("a, input[type=button], input[type=image], input[type=submit]").filter("[twsid]").unbind(".clipping").bind("click.clipping", C8O._handleEvent);
            $("form[twsid]").unbind(".clipping").bind("submit.clipping", C8O._handleEvent);
            if (C8O.vars.auto_resize === "true" && window !== window.parent && (C8O.isUndefined(resize) || resize)) {
                window.setTimeout(C8O._resize, 750);
            }
            if ("true" === C8O.vars.auto_refresh && $("[twsid]:first").length > 0) {
                C8O._checkDirty();
            }
        }
    },
    _getQuery : function () {
        var l = window.location,
            q = l.search.length > 0 ? l.search.substring(1) : "",
            h = l.hash.length > 0 ? l.hash.substring(1) : "";
        return (q.length > 0 && h.length > 0) ? (q + "&" + h) : (q.length > 0 ? q : h);
    },
    _getScript : function (url, callback) {
        var script = document.createElement("script"),
            done = false;
        script.src = url;
        script.onload = script.onreadystatechange = function () {
            if (!done && (!this.readyState ||
                this.readyState === "loaded" ||
                this.readyState === "complete")) {
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
    _xslStyleSheet : function (xml) {
        var node = xml.firstChild;
        while (node !== null) {
            if (node.nodeName === "xml-stylesheet") {
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
        if (event.type === "click") {
            C8O._define.clipping_attributs.each(function () {
                if (!C8O.isUndefined(event[this])) {
                    params["__event_" + this] = event[this];
                }
            });
        }
        (event.type === "submit"?$($.makeArray(this.elements)):$("input, select, textarea").filter("[twsid]:enabled")).each(function () {
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
    _hook : function (name) {
        var ret = true, i, r;
        if ($.isArray(C8O._define.hooks[name])) {
            for (i = 0; i < C8O._define.hooks[name].length && ret; i += 1) {
                r = C8O._define.hooks[name][i].apply(this, $.makeArray(arguments).slice(1));
                if (!C8O.isUndefined(r)) {
                    ret = r;
                }
            }
        }
        return ret;
    },
    _parseQuery : function (params, query) {
        var data = (C8O.isUndefined(params)) ? {}:params,
            vars = (query?query:C8O._getQuery()).split("&"),
            i, id, key, value;
        for (i = 0; i < vars.length; i += 1) {
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
    _postMessage : function (data) {
        if (window.postMessage) {
            try {
                data = JSON.stringify(data);
            } catch(e) {}
            window.parent.postMessage(data, "*");
        }
    },
    _remove : function (object, attribute) {
        var res = object[attribute];
        delete object[attribute];
        return res;
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
    },
    _init : function (params) {
        var value;
        if (C8O._remove(params, "__enc")=="true" || C8O.init_vars.enc=="true") {
            C8O._getScript("../../scripts/archive/weblib_plugins.6.1.10/rsa.js", function () {
                C8O._init_rsa(params);
            });
        } else if (value=C8O._remove(params, "__container")) {
            if (value=="df") {
                C8O._getScript("../../scripts/archive/weblib_plugins.6.1.10/df.js", function () {
                    C8O._init_df(params);
                });
            } else if (value=="mosaic") {
                C8O._getScript("../../scripts/archive/weblib_plugins.6.1.10/mosaic.js", function () {
                    C8O._init_mosaic(params);
                });
            } else if (value=="gatein") {
                C8O._getScript("../../scripts/archive/weblib_plugins.6.1.10/gatein.js", function () {
                    C8O._init_gatein(params);
                });
            } else if (value=="sharepoint") {
                C8O._getScript("../../scripts/archive/weblib_plugins.6.1.10/sharepoint.js", function () {
                    C8O._init_sharepoint(params);
                });
            } else if (value=="standalone") {
                C8O._getScript("../../scripts/archive/weblib_plugins.6.1.10/standalone.js", function () {
                    C8O._init_standalone(params);
                });
            }
        } else {
            C8O._define.connector = params.__connector;
            C8O._define.context = params.__context;
            C8O._retrieve_vars(params);
            if (C8O._hook("init_finished", params) && C8O.vars.first_call === "true") {
                C8O.call(params);
            }
            if (C8O.vars.first_call === "false") {
                C8O.waitHide();
            }
        }
    }
}
$.ajaxSettings.traditional = true;
$.ajaxSetup({
    type : C8O.vars.ajax_method,
    dataType : "xml"
});
try {
    if (window.frameElement.src) {
        C8O._define.iframe = true;
    }
} catch(e){
}
$(document).ready(function () {
    /** No XSLT engine (see #1336) : switch to server mode */
    if (!window.XSLTProcessor && !window.ActiveXObject) {
        C8O.vars.xsl_side = "server";
    }
    /** weblib_wrapper can't access to C8O object with IE (see #1778) */
    if (typeof(C8O_document_ready) !== "undefined") {
        C8O.addHook("document_ready", C8O_document_ready);
    }
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
            if ("false" === testplatform || ("auto" === testplatform && !$.isEmptyObject(params))) {
                C8O._init(params);
            } else {
                loc.href = base + "/project.html#" + C8O._define.project;
            }
        } else {
            window.location.href = base;
        }
    }
});