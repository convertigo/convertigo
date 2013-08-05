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
	_define: {
		ctf_mark : "__",
		ctf_mark_esc : "\\\\_\\\\_"
	}
});
$.extend(true, C8O, {
	vars : { /** customizable value by adding __name=value in query*/
		xsl_side : "none" /** client/server */
	},
	/**
	 * This will analyze C8O responses and route them to the correct page according to the
	 * routingTable. 
	 * 
	 * When the correct page is found we switch to this page and wait for the pageinit event to
	 * render all the widgets listening to this response.
	 * 
	 * If no page has to be switched, we render on the same page immediately
	 */
	isActivePage: function (fromPage) {
		return true;
	},
	
	changePage: function (goToPage, options, callback) {
		if (typeof(callback) == "function") {
			callback();
		};
	},
	
	_routeResponse: function(xml, c8oData) {
		var $doc = $(xml.documentElement);
		
		for (var i in C8O.routingTable) {
			var entry = C8O.routingTable[i];
			if (C8O.isMatching(c8oData, entry.calledRequest)) {
				for (var j in entry.actions) {
					var action = entry.actions[j];
					var routeFound = true;
					
					if (C8O.isDefined(action.fromPage)) {
						routeFound = C8O.isActivePage(action.fromPage);
					}
					
					if (routeFound) {
						if (C8O.isDefined(action.condition)) {
							var fnCondition = C8O._getFunction(action.condition);
							if (fnCondition != null) {
								routeFound = fnCondition($doc, c8oData);
							} else {
								routeFound = C8O._findAndSelf($doc, action.condition).length > 0;
							}
						}
	
						if (routeFound) {
							var options =  action.options;
							var goToPage = action.goToPage;
							var afterChange = function () {
								C8O._renderBindings($doc, c8oData);
								if (action.afterRendering) {
									action.afterRendering($doc, c8oData);
								}								
							};
							
							// Render in a target page
							if (goToPage) {
								C8O.changePage(goToPage, options, afterChange);
							}
							// Render on the same page
							else {
								afterChange();
							}
							
							return;
						}
					}
				}
			}
		}
		C8O._renderBindings($doc, c8oData);
	},
	
	/**
	 * Scans all the widgets requiring data update and render them
	 * 
	 * If widgets are listviews, refresh them as well as iscroll widgets.
	 */
	_renderBindings: function($xmlData, c8oData) {
		$("[data-c8o-listen]").each( function (index, element) {
			var $element = $(element);
			
			// Get the binded attributes
			var listenRequestables = $element.attr("data-c8o-listen");
			
			// Check called requestable against the list of listen requestables
			if (C8O.isMatching(c8oData, listenRequestables)) {
				// Check listen condition if any
				if (!C8O._checkConditionDomSelectorOrJsFunction(
						$element.attr("data-c8o-listen-condition"),
						$element,
						[$xmlData, c8oData],
						$xmlData)) {
					// The condition failed, so we abort the rendering
					return;
				}

				// Apply the template
				var $c8oListenContainer = $(this);
				
				C8O._manageTemplate($c8oListenContainer);
				
				var functionBeforeRendering = C8O._getFunction($element.attr("data-c8o-before-rendering"));
				if (functionBeforeRendering != null) {
					functionBeforeRendering.call(element, $xmlData, c8oData);
				}

				C8O.renderWidgets($c8oListenContainer, $xmlData);

				// Re attach any click handlers in case of new graphical components
				C8O._attachEventHandlers();
				
				// Check all the iscroll divs and refresh them to resize the iscroll size. 
				$("[data-iscroll]").each( function (index, element) {
					var $element = $(element);
					if (C8O.isDefined($element.iscrollview)) {
						$element.iscrollview("refresh");
					}
				});
				
				var functionAfterRendering = C8O._getFunction($element.attr("data-c8o-after-rendering"));
				if (functionAfterRendering != null) {
					functionAfterRendering.call(element, $xmlData, c8oData);
				}
			}
		});
	},
	
	/**
	 * Returns true if a given requestable is matching one of the requestables
	 * in a list, false otherwise.
	 */
	isMatching: function(c8oData, checkRequestablesList) {
		var checkRequestablesArray = checkRequestablesList.split(C8O._define.re_split_comma_trim);
		
		for (var i in checkRequestablesArray) {
			if (C8O.isMatchingSingle(c8oData, checkRequestablesArray[i])) {
				return true;
			}
		}
		
		return false;
	},
	
	/**
	 * Returns true if requestableObject is matching checkRequestableObject, false otherwise.
	 */
	isMatchingSingle: function(c8oData, checkRequestableObject) {
		if (checkRequestableObject == "*") {
			return true;
		}
		
		var matches = checkRequestableObject.match(C8O._define.re_requestable);
		
		if (matches) {
			var project1 = C8O.isDefined(c8oData.__project) ? c8oData.__project : C8O._define.project;
			var project2 = matches[1].length ? matches[1] : C8O._define.project;
			if (project1 == project2) {
				if (C8O.isDefined(c8oData.__sequence) && C8O.isDefined(matches[2])) {
					return matches[2] == "*" || matches[2] == c8oData.__sequence; 
				}
				if (C8O.isDefined(c8oData.__connector) && C8O.isDefined(c8oData.__transaction) && C8O.isDefined(matches[3])) {
					if (c8oData.__connector == matches[3]) {
						return matches[4] == "*" || matches[4] == c8oData.__transaction;
					}
				}
			}
		} else {
			console.log("Cannot match the invalid requestable '" + checkRequestableObject + "'");
		}
		
		return false;
	},

	_templates : {},
	
	_addTemplate: function(template) {
		var templateID = Math.floor(Math.random() * 16777215).toString(16);
		C8O._templates[templateID] = template;
		return templateID;		
	},
	
	_getTemplate: function(templateID) {
		return C8O._templates[templateID].clone();
	},
	
	_removeTemplate: function(templateID) {
		return delete C8O._templates[templateID];
	},
	
	_manageTemplate: function($element) {
		// We should first save the widget template for future reuse. If the widget has already
		// been rendered, there is a special attribute 'data-c8o-template-id', whose value is the
		// name of the saved template.
		var templateID = $element.attr("data-c8o-template-id");
		
		var accumulate_mode = $element.attr("data-c8o-accumulate");
		if (!accumulate_mode) {
			accumulate_mode = "";
		}
		accumulate_mode = accumulate_mode.match(C8O._define.re_accumulate_mode);
		
		if (templateID) {
			if (accumulate_mode[3] != null) {
				// The widget has already been rendered. We must empty it and reinsert the template.
				$element.empty();
			}
			var $template = C8O._getTemplate(templateID);

			// If we are in an iterator template, we must return the template
			var c8oEachIterator = $element.attr("data-c8o-each");
			if (c8oEachIterator) {
				return $template;
			}

			if (accumulate_mode[2] == null) {
				// In other case, just append the new clean template
				$element.append($template);
			} else {
				$element.prepend($template);
			}
		}
		else {
			// The widget has not yet been rendered: generate an unique template ID and save the
			// template.
			var $template = $element.contents().clone();
			templateID = C8O._addTemplate($template);
			$element.attr("data-c8o-template-id", templateID);
			
			// If we are in an iterator template, late-render or with an accumulate_mode, we must remove the template
			if ($element.attr("data-c8o-each") || $element.attr("data-c8o-late-render") || accumulate_mode[3] == null) {
				$element.empty();
			}
			
			// We must return a cloned instance of the template, otherwise we will
			// modify the template stored in the template repository.
			return $template.clone();
		}
	},
	
	/**
	 * Render the specified widget using the templating engine
	 * 
	 * $html points to the widget dom
	 * $doc is the data to render in the widget
	 */
	renderWidgets: function ($html, $doc) {
		var refs = C8O._handleRef($html, $doc);
		
		// Render simple elements
		C8O._renderElement($html, refs);

		// Render "use" attributes
		C8O._renderUseAttributes($html);

		// Render GUI components
		C8O._renderFinish($html);
	},

	/**
	 * Renders special attributes data-c8o-use-xxx
	 */
	_renderUseAttributes: function($html) {
		// Find the use attribute marker in HTML elements
		C8O._findAndSelf($html, "[data-c8o-use]").each(function () {
			var $this = $(this);
			// Find data-c8o-use-xxx attributes in the found HTML element
			C8O._findUseAttributes($this);
		});
	},
	
	_renderFinish: function($elt) {
		
	},
	
	/**
	 * Finds and renders all the data-c8o-use-xxx attributes in the given component.
	 */
	_findUseAttributes: function($component) {
		$($component[0].attributes).each(function () {
			var attributeName = this.nodeName;
			if (attributeName.indexOf("data-c8o-use-") == 0) {
				attributeName = attributeName.substring(13);
				//console.log("Found data-c8o-use-xxx attribute: " + attributeName);
				var attributeValue = this.nodeValue;
				C8O._renderUseAttribute($component, attributeName, attributeValue);
			}
		});
	},
	
	/**
	 * Renders the data-c8o-use-xxx attribute in the given component,
	 * i.e. removes the data-c8o-use-xxx attribute from the given element
	 * and adds a new attribute xxx with the given value.
	 */
	_renderUseAttribute: function($component, attributeName, attributeValue) {
		$component.removeAttr("data-c8o-use-" + attributeName);
		$component.attr(attributeName, attributeValue);
	},
	
	_renderElement: function($element, refs) {
		refs = $.extend({}, refs);

		// Render simple elements
		C8O.walk($element, {$element: $element, refs: refs}, C8O._renderText);
		
		// Render iterated elements
		C8O._findAndSelf($element, "[data-c8o-each],[data-c8o-late-render]").each(function() {
			var $c8oEachContainer = $(this);
			
			if (C8O.isDefined($c8oEachContainer.attr("data-c8o-each"))) {
				var $template = C8O._manageTemplate($c8oEachContainer);
				
				// Now we can iterate over the XML data
				var c8oEach = $c8oEachContainer.attr("data-c8o-each");
				if (c8oEach.length < 2 || c8oEach.charAt(0) != "{" || c8oEach.charAt(c8oEach.length - 1) != "}") {
					c8oEach = "=" + c8oEach;
				}
				
				var rule = C8O._makeRule(C8O._define.ctf_mark + c8oEach + C8O._define.ctf_mark);
				var $refData = C8O._getRefData(rule, refs);
				var $self = refs._self;
				$refData.find(rule.find).each(function (index) {
					var $data = $(this);
					var $item = $template.clone();
					
					$data.data("index", index);
					
					C8O._handleRef($c8oEachContainer, $data, refs);
					
					C8O._renderElement($item, refs);
					$c8oEachContainer.append($item);
				});
				refs._self = $self;
			} else {
				$c8oEachContainer.data("late-render", function () {
					$c8oEachContainer.removeAttr("data-c8o-late-render");
					C8O._manageTemplate($c8oEachContainer);
					C8O._renderElement($c8oEachContainer, refs);
					
					var renderFunction = $c8oEachContainer.attr("data-c8o-after-late-render-function");
					if (C8O.isDefined(renderFunction)) {
						renderFunction = C8O._getFunction(renderFunction);
						if (renderFunction != null) {
							renderFunction.call($c8oEachContainer[0], refs._self, refs);
						}
					}
					
					C8O._renderFinish($c8oEachContainer);
				});
			}
		});
	},
	
	_makeRule: function (txt) {
		var match = txt.match(C8O._define.re_find_ctf_markers);
		try {
			var rule = undefined;
			if (match[1]) {
				// JSON case
				rule = match[1].replace(C8O._define.re_find_ctf_esc_end, C8O._define.ctf_mark);
				rule = $.parseJSON(rule);
			} else if (match[2]) {
				rule = match[2].replace(C8O._define.re_find_ctf_esc_end, C8O._define.ctf_mark);
				rule = {find : rule};
			}
			rule.template = match[0];
			return rule;
		} catch (e) {
			// console.log(e);
			return null;
		}
	},
	
	_getRefData: function (rule, refs) {
		var $data = refs._self;
		if (C8O.isDefined(rule.ref)) {
			if (C8O.isDefined(refs[rule.ref])) {
				$data = refs[rule.ref];
			} else {
				console.log("unknown ref " + rule.ref + " in parent iteration, use current iteration $data");
			}
		}
		return $data;
	},
	
	_renderText: function (txt, data) {
		var $element = data.$element;
		var refs = data.refs;
		var find = txt.search(C8O._define.re_find_ctf_markers);
		var res = "";
		var elt = this.nodeType == Node.TEXT_NODE ? this.parentElement : this;
		while (find != -1) {
			res += txt.substring(0, find).replace(C8O._define.re_find_ctf_esc_start, C8O._define.ctf_mark + "$1");
			txt = txt.substring(find);
			var rule = C8O._makeRule(txt);
			if (rule != null) {
				var $elt = undefined;
				var value = undefined;
				
				try {
					var $data = C8O._getRefData(rule, refs);
					
					var isFragment = C8O.isDefined(rule.type) || rule.type == "fragment";
					
					if (C8O.isUndefined(rule.mode) || rule.mode == "find") {
						$elt = C8O.isUndefined(rule.find) || rule.find == "." ? $data : $data.find(rule.find);
						if ($elt.length) {
							if (C8O.isDefined(rule.attr)) {
								value = $elt.attr(rule.attr);
							} else if (!isFragment) {
								value = $elt.text();
							}
						}
					} else if (rule.mode == "index") {
						value = $data.data("index");
					}
				} catch (e) {
					console.log("$data.find failed : " + e);
				}
				
				if (C8O.isUndefined(value)) {
					if (isFragment && C8O.isDefined($elt) && $elt.length) {
						$elt.each(function () {
							value = C8O.convertHTML(this, value);
						});
					} else {
						if (C8O.isDefined(rule["default"])) {
							value = rule["default"];
						} else {
							value = "";
						}
						if (isFragment) {
							value = $("<fragment/>").html(value)[0];
						}
					}
				}
				
				var functionFormatter = C8O._getFunction(rule.formatter);
				if (functionFormatter != null) {
					try {
						var formatted = functionFormatter.call(elt, value); // add $data in next release
						
						if (typeof(formatted) == "string") {
							value = formatted;
						}
					} catch (e) {
						console.log("call formatter failed : " + e);
					}
				}
				
				if (isFragment && C8O.isDefined(value.childNodes)) {
					$(this).before(document.createTextNode(res), value.childNodes);
					res = "";
				} else {
					res += value;
				}
				
				txt = txt.substring(rule.template.length);
			} else {
				res += txt.substring(0, 1);
				txt = txt.substring(1);
			}
			find = txt.search(C8O._define.re_find_ctf_markers);
		}
		return res + txt.replace(C8O._define.re_find_ctf_esc_start, C8O._define.ctf_mark + "$1");	
	},
	
	_define: {
		re_accumulate_mode : new RegExp("^(?:(append)|(prepend)|(.*?))$"), // 1: append ; 2: preprend ; 3: replacel
		re_call_mode : new RegExp("^(?:(click)|(auto)|(?:(timer:)(.*)))$"), // 1: click ; 2: auto ; 3: timer ; 4: seconds for timer
		re_requestable : new RegExp("^([^.]*)\\.(?:([^.]+)|(?:([^.]+)\\.([^.]+)))$"), // 1: project ; 2: sequence ; 3: connector ; 4: transaction > 1+2 | 1+3+4
		re_split_comma_trim : new RegExp("\\s*,\\s*"),
		/**
		 * Reg exp selector for templating engine
		 */
		re_find_ctf_markers : new RegExp(C8O._define.ctf_mark + "(?:(\\{[\\d\\D]*?\\})|(?:=(.*?)))" + C8O._define.ctf_mark), // 0: full ; 1: json ; 2: selector
		re_find_ctf_esc_start : new RegExp(C8O._define.ctf_mark_esc + "(=|\\{)", "g"),
		re_find_ctf_esc_end : new RegExp(C8O._define.ctf_mark_esc, "g") // 0: full ; 1: json ; 2: selector
	},

	/**
	 * Returns true if the given condition is true.
	 * 
	 * The condition can be:
	 *    - a JQuery DOM selector in the $dom object
	 *    - a Javascript function with the following signature:
	 *       function(paramsArray)
	 *       this equals thisObject
	 */
	_checkConditionDomSelectorOrJsFunction: function(condition, thisObject, paramsArray, $dom) {
		if (condition) {
			// Function condition
			var functionCondition = C8O._getFunction(condition);
			if (functionCondition != null) {
				if (!functionCondition.apply(thisObject, paramsArray)) {
					// The condition failed, so we abort the rendering
					return false;
				}
			}
			// DOM selector condition
			else {
				if (C8O._findAndSelf($dom, condition + ":first").length == 0) {
					// The condition failed, so we abort the rendering
					return false;
				}
			}
		}
		
		return true;
	},
	
	_attachEventHandlers: function() {
		$("[data-c8o-call]").filter("[data-c8o-call-mode=auto],[data-c8o-call-mode^=timer]").each( function (index, element) {
			var $element = $(element);

			var c8oCallMode = $element.attr("data-c8o-call-mode").match(C8O._define.re_call_mode);
			
			if (c8oCallMode != null) {
				$element.attr("data-c8o-call-mode", "done-" + c8oCallMode[0]);
				if (c8oCallMode[2]) {
					C8O._onC8oCall(element);
				} else if (c8oCallMode[3] && $.isNumeric(c8oCallMode[4])) {
					window.setTimeout(function () {
						C8O._onC8oCall(element);
					}, c8oCallMode[4] * 1000);
				}
			}
		});
	},
	
	_onC8oCall: function(element) {
		var $element = $(element);
		// Check call condition if any
		if (!C8O._checkConditionDomSelectorOrJsFunction(
				$element.attr("data-c8o-call-condition"),
				$element,
				[],
				$(window.document))) {
			// The condition failed, so we abort the rendering
			return;
		}

		var c8oCall = $element.attr("data-c8o-call");
		if (c8oCall) {
			var c8oCallParams = {};
			var matches = c8oCall.match(C8O._define.re_requestable);
			
			if (matches != null) {
				if (matches[1].length) {
					c8oCallParams["__project"] = matches[1];
				}
				if (C8O.isDefined(matches[2])) {
					c8oCallParams["__sequence"] = matches[2];
				} else {
					c8oCallParams["__connector"] = matches[3];
					c8oCallParams["__transaction"] = matches[4];
				}
			} else {
				console.log("data-c8o-call '" + c8oCall + "' is not valid");
				return false;
			}
			
			var context = $element.attr("data-c8o-internal-context");
			if (context) {
				c8oCallParams["__context"] = context;
			}

			var c8oUserReference = $element.attr("data-c8o-internal-user_reference");
			if (c8oUserReference) {
				c8oCallParams["__user_reference"] = c8oUserReference;
			}
			
			// Find whether the call compionent is inside a form
			var $form = $element.closest("form");
			if ($form.length) {
				// Search for input fields in the form
				C8O.formToData($form, c8oCallParams);
			}
			
			// Search for 'data-c8o-variable' tagged elements in the link to use it
			// to build data request to C8O
			C8O._findAndSelf($form.length ? $form : $element, "[data-c8o-variable]").each(function (index, element) {
				var $c8oVariable = $(element);
				var name = $c8oVariable.attr("data-c8o-variable");
				var value = $c8oVariable.text();
				C8O.appendValue(c8oCallParams, name, value);
			});
			
			var variables = ($form.length && !$element.is("form")) ? C8O._silentParseJSON($form.attr("data-c8o-variables")) : null;
			variables = $.extend(variables, C8O._silentParseJSON($element.attr("data-c8o-variables")));
			C8O.appendValues(c8oCallParams, variables);
			
			// now call c8o with constructed arguments
			C8O.call(c8oCallParams);
		}
		return false;
	},
	
	_handleRef: function ($element, $doc, refs) {
		if (C8O.isUndefined(refs)) {
			refs = {};
		}
		refs._self = $doc;
		var refName = $element.attr("data-c8o-ref");
		if (refName) {
			refs[refName] = refs._self;
		}
		return refs;
	},
	
	_onDocumentReadyEnd: function (callback) {
		if (typeof(callback) == "function") {
			callback();
		};
	},
	
	_onSuccess : function (xml, status, jqXHR) {
		var c8oData = jqXHR.C8O_data;
		if (C8O._hook("xml_response", xml, c8oData)) {
			/*
			 * Retrieving last requestable from last call to Convertigo server.
			 * This will help determining which actions have to be achieved: which data to manage in which screen.
			 * The choice can be done depending on the last "requestable", but also on the xml response itself, depending
			 * on its content (see above code).
			 */	
			C8O._routeResponse(xml, c8oData);
		}
	},
	
	_silentParseJSON: function (str) {
		if (typeof(str) == "string") {
			try {
				return $.parseJSON(str);
			} catch (e) {
				console.log("JSON parse failed on " + str + " : " + e);
			}
		}
		return {};
	}
});

/**
 *  Initialize C8O MVC Framework 
 */
C8O.addHook("init_finished", function () {
	C8O.removeRecallParameter("__connector");
	
	$(document).on("click", ":not(form)[data-c8o-call]", function () {
		var callMode = $(this).attr("data-c8o-call-mode");
		if (C8O.isUndefined(callMode) || callMode == "click") {
			return C8O._onC8oCall(this);
		}
	}).on("submit", "form[data-c8o-call]", function () {
		return C8O._onC8oCall(this);
	}).on("click", "[data-c8o-render]", function () {
		var eventName = $(this).attr("data-c8o-render");
		for (var templateID in C8O._templates) {
			var $lateRender = $("[data-c8o-template-id=" + templateID + "][data-c8o-late-render=" + eventName + "]");
			if ($lateRender.length != 0 && $lateRender.children().length == 0) {
				$lateRender.each(function () {
					try {
						$lateRender.data("late-render")();
					} catch (e) {
						//TODO log exception
					}
				});
			}
		}
	});
	
	var onNewPage = function () {
		C8O._attachEventHandlers();

		// Store listen and iteration templates
		// Templates should be managed deeply first (in order to handle nested templates)
		// The search order is reversed because we want to manage templates deeply first.
		// This is not strictly deeply traversal, but it is enough to have deeply first
		// template management on each XML branch.
		$($("[data-c8o-listen],[data-c8o-each],[data-c8o-late-render]").not("[data-c8o-template-id]").get().reverse()).each(function() {
			var $c8oListenContainer = $(this);
			C8O._manageTemplate($c8oListenContainer);
		});
		
		// Empty templates
		var $html = $("html:first");
		C8O._renderElement($html, C8O._handleRef($html, $("<xml/>")));
	};
	
	C8O._onDocumentReadyEnd(onNewPage);
});