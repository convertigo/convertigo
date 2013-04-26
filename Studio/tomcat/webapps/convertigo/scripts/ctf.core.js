$.extend(true, C8O, {
	/**
	 * This will analyze C8O responses and route them to the correct page according to the
	 * routingTable. 
	 * 
	 * When the correct page is found we switch to this page and wait for the pageinit event to
	 * render all the widgets listening to this response.
	 * 
	 * If no page has to be switched, we render on the same page immediately
	 */
	routeResponse: function(c8oRequestable, xml) {
		var $doc = $(xml.documentElement);
		var routeFound = false;
		
		for (var i in C8O.routingTable) {
			var entry = C8O.routingTable[i];
			if (C8O.isMatching(c8oRequestable, entry.calledRequest)) {
				for (var j in entry.actions) {
					var action = entry.actions[j];
					var transition =  action.transition;
					var fnCondition = C8O._getFunction(action.condition);
					if (fnCondition != null) {
						routeFound = fnCondition($doc);
					} else {
						var element = $doc.find(action.condition);
						routeFound = (element.length != 0 || $doc.is(action.condition));
					}

					if (routeFound) {
						var goToPage = action.gopage;

						// Render in a target page
						if (goToPage) {
							// Bind a listener on the 'pagebeforeshow' event in order
							// to render bindings only after the page is shown
							$(document).one('pagebeforeshow', function (event){
								C8O.renderBindings(c8oRequestable, $doc);
								if (action.afterRendering) {
									action.afterRendering($doc);
								}
							});
							
							// Change page
							$.mobile.changePage(goToPage, transition);
						}
						// Render on the same page
						else {
							C8O.renderBindings(c8oRequestable, $doc);
							if (action.afterRendering) {
								action.afterRendering($doc);
							}
						}
						
						return;
					}
				}
			}
		}
	},
	
	/**
	 * Scans all the widgets requiring data update and render them
	 * 
	 * If widgets are listviews, refresh them as well as iscroll widgets.
	 */
	renderBindings: function(calledRequestable, $xmlData) {
		$("[data-c8o-listen]").each( function (index, element) {
			var $element = $(element);
			
			// Get the binded attributes
			var listenRequestables = $element.attr("data-c8o-listen");
			
			// Check called requestable against the list of listen requestables
			if (C8O.isMatching(calledRequestable, listenRequestables)) {
				// Check listen condition if any
				if (!C8O._checkConditionDomSelectorOrJsFunction(
						$element.attr("data-c8o-listen-condition"),
						$element,
						[ $xmlData ],
						$xmlData)) {
					// The condition failed, so we abort the rendering
					return;
				}

				// Apply the template
				var $c8oListenContainer = $(this);
				
				C8O.manageTemplate($c8oListenContainer);
				
				var functionBeforeRendering = C8O._getFunction($element.attr("data-c8o-before-rendering"));
				if (functionBeforeRendering != null) {
					functionBeforeRendering.call(element, $xmlData);
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
					functionAfterRendering.call(element, $xmlData);
				}
			}
		});
	},
	
	/**
	 * Returns true if a given requestable is matching one of the requestables
	 * in a list, false otherwise.
	 */
	isMatching: function(requestable, checkRequestablesList) {
		var checkRequestablesArray = checkRequestablesList.split(",");
		
		for (var i = 0; i < checkRequestablesArray.length; i++) {
			var checkRequestable = $.trim(checkRequestablesArray[i]);
			var requestableObject = C8O.getRequestableObject(requestable);
			var checkRequestableObject = C8O.getRequestableObject(checkRequestable);
			if (C8O.isMatchingSingle(requestableObject, checkRequestableObject)) {
				return true;
			}
		}
		
		return false;
	},

	getRequestableObject: function(requestable) {
		if (requestable === "*") {
			return {
				"fullTextName": requestable,
				"type": "*",
			}
		}
	    
		var requestableParts = requestable.match(C8O._define.re_requestable);
		if (requestableParts != null) {
			if (requestableParts[3]) {
				return {
					"fullTextName": requestable,
					"type": "transaction",
					"project": requestableParts[1],
					"connector": requestableParts[2],
					"transaction": requestableParts[3]
				};
	    	}
			else if (requestableParts[2]) {
				return {
					"fullTextName": requestable,
					"type": "sequence",
					"project": requestableParts[1],
					"sequence": requestableParts[2]
				};
			}
		}

		// TODO: handle error: should never go there
	},
	
	/**
	 * Returns true if requestableObject is matching checkRequestableObject, false otherwise.
	 */
	isMatchingSingle: function(requestableObject, checkRequestableObject) {
		// Universal case
		if (checkRequestableObject.fullTextName === "*") {
			return true;
		}
		// Fully qualified requestable (e.g. project.sequence, or project.connector.sequence)
		else if (checkRequestableObject.fullTextName === requestableObject.fullTextName) {
			return true;
		}
		else {
			if (requestableObject.type === "sequence") {
				// Any sequence (.* or project.*)
				if (requestableObject.project != checkRequestableObject.project) return false;
				if (checkRequestableObject.sequence === "*") return true;
				
				// Explicit sequence
				return (requestableObject.sequence === checkRequestableObject.sequence);
			}
			else if (requestableObject.type === "transaction") {
				// Any transaction (..* or project..* or project.connector.*)
				if (requestableObject.project != checkRequestableObject.project) return false;
				if (requestableObject.connector != checkRequestableObject.connector) return false;
				if (checkRequestableObject.transaction === "*") return true;

				// Explicit transaction
				return (requestableObject.transaction === checkRequestableObject.transaction);
			}
			else {
				// TODO: handle error: unknown requestable type
			}
		}
		
		// TODO: handle error: should never go there
		return false;
	},

	templates : {},
	
	addTemplate: function(template) {
		var templateID = Math.floor(Math.random() * 16777215).toString(16);
		C8O.templates[templateID] = template;
		return templateID;		
	},
	
	getTemplate: function(templateID) {
		return C8O.templates[templateID].clone();
	},
	
	removeTemplate: function(templateID) {
		return delete C8O.templates[templateID];
	},
	
	manageTemplate: function($element) {
		// We should first save the widget template for future reuse. If the widget has already
		// been rendered, there is a special attribute 'data-c8o-template-id', whose value is the
		// name of the saved template.
		var templateID = $element.attr("data-c8o-template-id");
		if (templateID) {
			// The widget has already been rendered. We must empty it and reinsert the template.
			$element.empty();
			var $template = C8O.getTemplate(templateID);

			// If we are in an iterator template, we must return the template
			var c8oEachIterator = $element.attr("data-c8o-each");
			if (c8oEachIterator) {
				return $template;
			}

			// In other case, just append the new clean template
			$element.append($template);
		}
		else {
			// The widget has not yet been rendered: generate an unique template ID and save the
			// template.
			var $template = $element.contents().clone();
			templateID = C8O.addTemplate($template);
			$element.attr("data-c8o-template-id", templateID);
			
			// If we are in an iterator template, we must remove the template
			var c8oEachIterator = $element.attr("data-c8o-each");
			if (c8oEachIterator) {
				$element.children().detach();
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
		C8O.renderElement($html, refs);

		// Render "use" attributes
		C8O.renderUseAttributes($html);

		// Render GUI components
		//$html.find("[data-role='collapsible-set']").collapsibleset();
		//$html.find("[data-role='listview']").listview();
		$html.trigger("create");
	},

	/**
	 * Renders special attributes data-c8o-use-xxx
	 */
	renderUseAttributes: function($html) {
		// Find the use attribute marker in HTML elements
		C8O._findAndSelf($html, "[data-c8o-use]").each(function () {
			var $this = $(this);
			// Find data-c8o-use-xxx attributes in the found HTML element
			C8O.findUseAttributes($this);
		});
	},
	
	/**
	 * Finds and renders all the data-c8o-use-xxx attributes in the given component.
	 */
	findUseAttributes: function($component) {
		$($component[0].attributes).each(function () {
			var attributeName = this.nodeName;
			if (attributeName.indexOf("data-c8o-use-") == 0) {
				attributeName = attributeName.substring(13);
				//console.log("Found data-c8o-use-xxx attribute: " + attributeName);
				var attributeValue = this.nodeValue;
				C8O.renderUseAttribute($component, attributeName, attributeValue);
			}
		});
	},
	
	/**
	 * Renders the data-c8o-use-xxx attribute in the given component,
	 * i.e. removes the data-c8o-use-xxx attribute from the given element
	 * and adds a new attribute xxx with the given value.
	 */
	renderUseAttribute: function($component, attributeName, attributeValue) {
		$component.removeAttr("data-c8o-use-" + attributeName);
		$component.attr(attributeName, attributeValue);
	},
	
	renderElement: function($element, refs) {		
		// Render iterated elements
		C8O._findAndSelf($element, "[data-c8o-each]").each(function() {
			var $c8oEachContainer = $(this);
			
			var $template = C8O.manageTemplate($c8oEachContainer);

			// Now we can iterate over the XML data
			var c8oEachIterator = $c8oEachContainer.attr("data-c8o-each");
			refs._self.find(c8oEachIterator).each(function () {
				var $data = $(this);
				var $item = $template.clone();
				
				C8O._handleRef($c8oEachContainer, $data, refs);
				
				C8O.renderElement($item, refs);
				$c8oEachContainer.append($item);
			});
		});

		// Render simple elements
		C8O.walk($element, {$element: $element, refs: refs}, C8O.renderText);
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
	
	renderText: function (txt, data) {
		var $element = data.$element;
		var refs = data.refs;
		var find = txt.search(C8O._define.re_find_brackets);
		var res = "";
		while (find != -1) {
			res += txt.substring(0, find);
			txt = txt.substring(find);
			var match = txt.match(C8O._define.re_find_brackets);
			var rule = undefined;
			if (match[2]) {
				// JSON case
				var part = (match[2] == "'") ? match[0].replace(C8O._define.re_replace_simple_quote, "$1\"") : match[0];
				part = part.replace(C8O._define.re_replace_escaped_bracket_simple_quote, "$1");
				try {
					rule = $.parseJSON(part);
				} catch (e) {
					console.log("JSON parse failed on " + part + " : " + e);
				}
			}
			if (C8O.isUndefined(rule)) {
				rule = {find : match[1].replace(C8O._define.re_replace_escaped_bracket_simple_quote, "$1")};
			}
			
			var value = undefined;
			
			try {
				var $data = refs._self;
				if (C8O.isDefined(rule.ref)) {
					if (C8O.isDefined(refs[rule.ref])) {
						$data = refs[rule.ref];
					} else {
						console.log("unknown ref " + rule.ref + " in parent iteration, use current iteration $data");
					}
				}
				
				if (C8O.isDefined(rule.find)) {
					var $elt = rule.find == "." ? $data : $data.find(rule.find);
					if ($elt.length) {
						if (C8O.isDefined(rule.attr)) {
							value = $elt.attr(rule.attr);
						} else {
							value = $elt.text();
						}
					}
				}
			} catch (e) {
				console.log("$data.find failed : " + e);
			}
			
			if (C8O.isUndefined(value)) {
				if (C8O.isDefined(rule.def)) {
					value = rule.def;
				} else {
					value = "";
				}
			}
			
			var functionFormatter = C8O._getFunction(rule.formatter);
			if (functionFormatter != null) {
				try {
					var formatted = functionFormatter.call($element[0], value);
					if (typeof(formatted) == "string") {
						value = formatted;
					}
				} catch (e) {
					console.log("call formatter failed : " + e);
				}
			}
			
			res += value;
			txt = txt.substring(match[0].length);
			find = txt.search(C8O._define.re_find_brackets);
		}
		return res + txt;	
	},
	
	_define: {
		re_attr_plus : new RegExp("^data-c8o-(?:use-(.*$)|variable-(.*$)|internal-(.*$))"), // 1: use ; 2: variable ; 3: internal
		re_call_mode : new RegExp("^(?:(click)|(auto)|(?:(timer:)(.*)))$"), // 1: click ; 2: auto ; 3: timer ; 4: seconds for timer
		re_requestable : new RegExp("^(.*?)\\.(.*?)(?:\\.(.*))?$"), // 1: project ; 2: sequence | connector ; 3: transaction
		/**
		 * Reg exp selector for templating engine
		 */
		re_find_brackets : new RegExp("{(\\s*(?:('|\").*?\\2\\s*:)?.*?(?!\\\\).)}"), // 0: full ; 1: content ; 2: is json // {(\s*(?:('|").*?\2\s*:)?.*?(?!\\).)}
		re_replace_escaped_bracket_simple_quote : new RegExp("\\\\(}|')", "g"), // replace with "$1"
		re_replace_simple_quote : new RegExp("(^|(?!\\\\).)'", "g") // replace with "$1""
	},
	
	_decodeStore: function(store) {
		// Requestable format:
		// [<project name>].(([connector name].[<transaction name>]) | <sequence name>)[:<store name>]
		var storeParts = store.split(":");
		var requestable = storeParts[0];
		var storeID = null;
		if (storeParts.length == 2) {
			storeID = storeParts[1];
		}
		
		var requestableParts = requestable.split(".");
		
		switch (requestableParts.length) {
		// Transaction case: project.connector.transaction
		// project, connector and transaction are optional: if not set, we will use the current project, the default connector or the default transaction
		// E.g.:
		//    myproject.myconnector.mytransaction defines the transaction named "mytransaction" from the connector named "myconnector" from the project named "myproject"
		//    myproject..mytransaction defines the transaction named "mytransaction" from the default connector from the project named "myproject"
		//    .myconnector.mytransaction defines the transaction named "mytransaction" from the connector named "myconnector" from the current project
		//    ..mytransaction defines the transaction named "mytransaction" from the default connector from the current project
		//    .. defines the default transaction from the default connector from the current project
		case 3:
			return { storeID: storeID, project: requestableParts[0], connector: requestableParts[1], transaction: requestableParts[2] };
		// Sequence case: project.sequence
		// project is optional: if not set, we will use the current project
		case 2:
			return { storeID: storeID, project: requestableParts[0], sequence: requestableParts[1] };
		default:
			return {};
		}
	},
	
	/**
	 * Todo : Save in local storage any input field having the
	 * data-c8O-autosave attriute
	 */
	handleAutoSaveInputs: function(target) {
		
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
		$("[data-c8o-call]").each( function (index, element) {
			var $element = $(element);
			
			var onC8oCall = function() {
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
					var store = C8O._decodeStore(c8oCall);
					
					var c8oCallParams = {};
					
					if (store.project)
						c8oCallParams["__project"] = store.project;
					if (store.connector)
						c8oCallParams["__connector"] = store.connector;
					if (store.transaction)
						c8oCallParams["__transaction"] = store.transaction;
					if (store.sequence)
						c8oCallParams["__sequence"] = store.sequence;

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
					if ($form.length > 0) {
						// Cancel form submissions as we are going to handle them by ourselves
						$form.submit(function () {
						    return false;
						});
						
						// Search for input fields in the form
						var formArray = $form.serializeArray();
						for (var i = 0; i < formArray.length; i++) {
							c8oCallParams[formArray[i].name] = formArray[i].value;
						}						
					}
					
					// Search for 'data-c8o-variable' tagged elements in the link to use it
					// to build data request to C8O
					C8O._findAndSelf($element, "[data-c8o-variable]").each(function (index, element) {
						var $c8oVariable = $(element);
						var name = $c8oVariable.attr("data-c8o-variable");
						var value = $c8oVariable.text();
						c8oCallParams[name] = value;
					});
					
					var attributes = C8O._getAttributes(element);
					for (var attributeName in attributes) {
						var attributeMatch = attributeName.match(C8O._define.re_attr_plus);
						if (attributeMatch != null) {
							if (attributeMatch[2] != null) {
								c8oCallParams[attributeMatch[2]] = attributes[attributeName];
							} else if (attributeMatch[3] != null) {
								c8oCallParams["__" + attributeMatch[3]] = attributes[attributeName];
							}
						}
					}
					
					// now call c8o with constructed arguments
					C8O.call(c8oCallParams);
				}
				return false;
			};
			
			var c8oCallMode = ($element.attr("data-c8o-call-mode") || "click").match(C8O._define.re_call_mode);
			if (c8oCallMode != null) {
				$element.attr("data-c8o-call-mode", "done-" + c8oCallMode[0]);
				if (c8oCallMode[1]) {
					if (element.tagName.toLowerCase() == "form") {
						$element.off('submit.c8o').on('submit.c8o', onC8oCall);
					} else {
						$element.off('click.c8o').on('click.c8o', onC8oCall);
					}
				} else if (c8oCallMode[2]) {
					onC8oCall();
				} else if (c8oCallMode[3] && $.isNumeric(c8oCallMode[4])) {
					window.setTimeout(onC8oCall, c8oCallMode[4] * 1000);
				}
			}
		});
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
	}
});

/**
 *  Initialize C8O MVC Framework 
 */
C8O.addHook("document_ready", function () {
	C8O.removeRecallParameter("__connector");
//	$(document).on("pagebeforecreate", "[data-role = page]", function(event){
		C8O._attachEventHandlers();
		
		var $document = $(document);

		// Store listen and iteration templates
		// Templates should be managed deeply first (in order to handle nested templates)
		// The search order is reversed because we want to manage templates deeply first.
		// This is not strictly deeply traversal, but it is enough to have deeply first
		// template management on each XML branch.
		$($document.find("[data-c8o-listen],[data-c8o-each]").get().reverse()).each(function() {
			var $c8oListenContainer = $(this);
			C8O.manageTemplate($c8oListenContainer);
		});
//	});
	
});

/**
 * Hook the XML response for C8O MVC framework
 */
C8O.addHook("xml_response", function (xml, c8oCall) {
	/*
	 * Retrieving last requestable from last call to Convertigo server.
	 * This will help determining which actions have to be achieved: which data to manage in which screen.
	 * The choice can be done depending on the last "requestable", but also on the xml response itself, depending
	 * on its content (see above code).
	 */
	var c8oRequestable = project + "." + sequence;

	var project = c8oCall.__project;
	if (C8O.isUndefined(project)) project = "";

	var sequence = c8oCall.__sequence;
	if (C8O.isUndefined(sequence)) {
		var connector = c8oCall.__connector;
		if (C8O.isUndefined(connector)) connector = "";
		var transaction = c8oCall.__transaction;
		if (C8O.isUndefined(transaction)) transaction = "";
		c8oRequestable = project + "." + connector + "." + transaction;
	}
	else {
		c8oRequestable = project + "." + sequence;
	}
	
	C8O.routeResponse(c8oRequestable, xml);
	
	return false;
});