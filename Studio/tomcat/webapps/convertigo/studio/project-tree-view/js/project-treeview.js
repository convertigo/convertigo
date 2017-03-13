(function () {
	// Injector
	var INJ = {
		// Inject a CSS
		injectLinkStyle: function (cssUrl, callback) {
			var linkStyleTag = document.createElement("link");
			if (callback) {
				linkStyleTag.addEventListener("load", callback);
			}
			linkStyleTag.rel = "stylesheet";
			linkStyleTag.href = cssUrl;
			document.head.appendChild(linkStyleTag);
		},
		// Inject a JS script
		injectScript: function (jsUrl, callback) {
			var foundScriptTag = document.head.querySelectorAll('script[src="' + jsUrl + '"]');
			if (foundScriptTag.length !== 0 && callback) {
				foundScriptTag[0].addEventListener("load", callback);
			}
			else {
				var scriptTag = document.createElement("script");
				if (callback) {
					scriptTag.addEventListener("load", callback);
				}
				scriptTag.src = jsUrl;
				scriptTag.type = "text/javascript";
				document.head.appendChild(scriptTag);
			}
		}
	};
	
	var StringUtils = {
		escapeHTML: function (text) {
			var fakeDiv = $("<div/>", {
				text: text
			});
			
			return fakeDiv.html();
		},
		addCommentDoubleSlash: function (text) {
			return "// " + text;
		},
		unescapeHTML: function (text) {
			var unescapedHTML = $.parseHTML(text);
			return unescapedHTML ? unescapedHTML[0].nodeValue : "";
		}
	};
	
	// Project Tree View
	var PTV = {
			// Variables
			tree: null,
			url: {
				baseUrl: null,
				baseUrlConvertigo: null,
				baseUrlConvertigoStudio: null,
				baseUrlConvertigoServices: null
			},
			callJstree: null,
			
			// Functions
			init: function (baseUrl, jstreeId, authUserName, authPassword) {
				// Initialize URLs
				PTV.url.baseUrl = baseUrl;
				PTV.url.baseUrlConvertigo = PTV.url.baseUrl + "convertigo/";
				PTV.url.baseUrlConvertigoStudio = PTV.url.baseUrlConvertigo + "studio/";
				PTV.url.baseUrlConvertigoServices = PTV.url.baseUrlConvertigo + "admin/services/";
				
				// Inject CSS
				INJ.injectLinkStyle(PTV.url.baseUrlConvertigoStudio + "project-tree-view/css/jstree/themes/default-dark/style.min.css");
				INJ.injectLinkStyle(PTV.url.baseUrlConvertigoStudio + "project-tree-view/css/style.css");
				
				// Load jQuery library
				INJ.injectScript(PTV.url.baseUrlConvertigo + "scripts/jquery-2.1.4.js", function () {
					// Inject colResizable library
					INJ.injectScript(PTV.url.baseUrlConvertigoStudio + "project-tree-view/js/colResizable-1.6.min.js", function () {
						// Inject jstree library
						INJ.injectScript(PTV.url.baseUrlConvertigoStudio + "project-tree-view/js/jstree/jstree.min.js", function () {
							setTimeout(function () {
								// Inject jstree grid plugin
								INJ.injectScript(PTV.url.baseUrlConvertigoStudio + "project-tree-view/js/jstree/jstreegrid-3.5.14.js", function () {
									// Define AJAX setup
									$.ajaxSetup({
										type: "POST",
										dataType: "xml",
										xhrFields: {
											withCredentials: true
										}
									});
									
									// Create first level of the tree : project nodes
									PTV.loadProjects(authUserName, authPassword, function () {
										PTV.initJstree(jstreeId);
									});									
								});
							}, 1000);
						});
					});
				});
			},
			// Add a new node type for jstree
			addJstreeNodeType: function (classname) {
				// Replace all "." by "-"
				var nodeType = classname.replace(/\./g, "-");
				
				// Add the node type and specify the icon for these nodes
				PTV.callJstree.settings.types[nodeType] = {
					icon: nodeType
				}
				
				return nodeType;
			},
			createConvertigoServiceUrl: function (serviceName) {
				return PTV.url.baseUrlConvertigoServices + serviceName;
			},
			createNodeJsonDbo: function (dboElt) {
				var dboEltComment = StringUtils.escapeHTML($(dboElt).attr("comment"));
				var dboEltRestOfComment = null;
		
				var indexOfNewLine = dboEltComment.indexOf("\r");
				if (indexOfNewLine === -1) {
					indexOfNewLine = dboEltComment.indexOf("\n");
				}
				
				// If it is a multi-lines comment
				if (indexOfNewLine !== -1) {
					// The other lines of the comment
					dboEltRestOfComment = dboEltComment.substr(indexOfNewLine);
					
					// The fist line
					dboEltComment = dboEltComment.substr(0, indexOfNewLine);
				}

				// Add "// " at the beginning of the comment
				dboEltComment = dboEltComment.length ? StringUtils.addCommentDoubleSlash(dboEltComment) : "";

				return {
					text: $(dboElt).attr("name"),
					// Add a new type for icons
					type: PTV.addJstreeNodeType($(dboElt).attr("classname")),
					children: $(dboElt).attr("hasChildren") === "true",
					data: {
						qname: $(dboElt).attr("qname"),
						comment: dboEltComment,
						restOfComment: dboEltRestOfComment
					}
				}
			},
			createChildNodes: function ($dbo, node) {
				var nodes = [];
				nodes.children = [];
				var categories = [];
				$dbo.children().each(function () {
					var category = $(this).attr("category");
					
					// Check if category (=Steps, Connector, Sequence, etc.) exists
					if (!categories[category]) {
						var categoryPlural = category + "s";
						// Check if one of the parents of the selected node is a category
						var parentCategoryFound = node.parents.find(function (parent) {
							return categoryPlural === PTV.callJstree.get_text(parent);
						});

						// If category not found, we create it
						if (!parentCategoryFound) {
							var categoryNode = {
								text: categoryPlural,
								children: []
							};
							
							categories[category] = categoryNode;
							nodes.push(categoryNode);
						}
					}
					
					// Create the node and "set" its parent: the new category or the selected node
					var dboNode = PTV.createNodeJsonDbo(this);
					if (categories[category]) {
						categories[category].children.push(dboNode);
					}
					else {
						nodes.push(dboNode);
					}
				});
				
				return nodes;
			},
			// Function highly inspired from the _edit(...) function of jstreegrid plugin
			editCell: function (obj, col, element, editText) {
				if (!obj) {
				    return false;
				}
				if (element) {
				    element = $(element);
				    if (element.prop("tagName").toLowerCase() === "div") {
				        element = element.children("span:first");
				    }
				}
				else {
				    // need to find the element - later
				    return false;
				}
				var rtl = PTV.callJstree._data.core.rtl,
				    w = PTV.callJstree.element.width(),
				    t = editText,
				    h1 = $("<div/>", {
				        css: {
				            "position": "absolute",
				            "top": "-200px",
				            "left": (rtl ? "0px" : "-1000px"),
				            "visibility": "hidden"
				        }
				    }).appendTo("body"),
				    h2 = $("<input/>", {
				        "value": t,
				        "class": "jstree-rename-input",
				        "css": {
				            "padding": "0",
				            "border": "1px solid silver",
				            "box-sizing": "border-box",
				            "display": "inline-block",
				            "height": (PTV.callJstree._data.core.li_height) + "px",
				            "lineHeight": (PTV.callJstree._data.core.li_height) + "px",
				            "width": "150px" // will be set a bit further down
				        },
				        "blur": $.proxy(function() {
				            var v = h2.val();

				            // save the value if changed
				            if (v === t) {
				                v = t;
				            }
				            else {
				            	// New value of the comment
				                obj.data[col.value] = v.length ? StringUtils.addCommentDoubleSlash(StringUtils.escapeHTML(v)) : v;
				                PTV.callJstree.element.trigger('update_cell.jstree-grid', {
				                    node: obj,
				                    col: col.value,
				                    value: v,
				                    old: t
				                });
				                PTV.callJstree._prepare_grid(this.get_node(obj, true));
				            }
				            h2.remove();
				            element.show();
				        }, PTV.callJstree),
				        "keydown": function(event) {
				            var key = event.which;
				            if (key === 27) {
				                this.value = t;
				            }
				            if (key === 27 || key === 13 || key === 37 || key === 38 || key === 39 || key === 40 || key === 32) {
				                event.stopImmediatePropagation();
				            }
				            if (key === 27 || key === 13) {
				                event.preventDefault();
				                this.blur();
				            }
				        },
				        "click": function(e) {
				            e.stopImmediatePropagation();
				        },
				        "mousedown": function(e) {
				            e.stopImmediatePropagation();
				        },
				        "keyup": function(event) {
				            h2.width(Math.min(h1.text("pW" + this.value).width(), w));
				        },
				        "keypress": function(event) {
				            if (event.which === 13) {
				                return false;
				            }
				        }
				    }),
				    fn = {
				        fontFamily: element.css('fontFamily') || '',
				        fontSize: element.css('fontSize') || '',
				        fontWeight: element.css('fontWeight') || '',
				        fontStyle: element.css('fontStyle') || '',
				        fontStretch: element.css('fontStretch') || '',
				        fontVariant: element.css('fontVariant') || '',
				        letterSpacing: element.css('letterSpacing') || '',
				        wordSpacing: element.css('wordSpacing') || ''
				    };
				element.hide();
				element.parent().append(h2);
				h2.css(fn).width("100%")[0].select();
			},
			handleSelectNodeEvent: function (node) {	
				//var $bottomPanel = $("#bottomPanel");
				if (node.type !== "default") {
					// Get properties of the object
					$.ajax({
						url: PTV.createConvertigoServiceUrl("studio.database_objects.Get"),
						data: {
							qname: node.data.qname
						},
						success: function (data, textStatus, jqXHR) {
//							var $basepropertiesTBody = $("#basePropertiesTBody");
//							$basepropertiesTBody.find("tr:first").nextAll().remove();
//							
//							var $expertPropertiesTBody = $("#expertPropertiesTBody");
//							$expertPropertiesTBody.find("tr:first").nextAll().remove();
//							$(data).find("property").each(function () {
//								var $trLine = $("<tr/>");
//								var $tdProperty = $("<td/>", {
//									text: $(this).attr("displayName")
//								});
//								
//								var $tdValue = $("<td/>").append($("<input/>", {
//									type: "text",
//									val: $(this).find("[value]").attr("value")
//								}));
//								
//								$trLine
//									.append($tdProperty)
//									.append($tdValue);
//								
//								if ($(this).attr("isExpert") == "true") {
//									$expertPropertiesTBody.append($trLine);
//								}
//								else {
//									$basepropertiesTBody.append($trLine);
//								}
//							});
//							
//							$bottomPanel.find("#propertyView>table").colResizable({
//								resizeMode: "overflow",
//								partialRefresh: true,
//								liveDrag:true
//							});
						}
					});
				}
				else {
//					$("#basePropertiesTBody").find("tr:first").nextAll().remove();
//					$("#expertPropertiesTBody").find("tr:first").nextAll().remove();
				}
			},
			initJstree: function (jstreeId) {
				// Inject CSS the icons of each type of nodes
				INJ.injectLinkStyle(PTV.createConvertigoServiceUrl("studio.database_objects.GetCSS"));
				
				// Initialize jstree
				PTV.tree = $(jstreeId);
				$(PTV.tree)
					.jstree({
						core: {
							check_callback: true,
							force_text: true,
							themes: {
								name: "default-dark",
								dots: false
							},
							data: function (node, cb) {
								var isRoot = node.id == "#";
								$.ajax({
									url: PTV.createConvertigoServiceUrl("studio.database_objects.GetChildren"),
									data: isRoot ? {} : {qname: node.data.qname},
									success: function (data, textStatus, jqXHR) {
										// Node is root : we creates project nodes
										if (isRoot) {
											var nodes = [];
											// Create the nodes for each project
											$(data).find("admin>dbo").each(function () {
												nodes.push(PTV.createNodeJsonDbo(this));
											});
										}
										else {
											var nodes = PTV.createChildNodes($(data).find("admin>dbo"), node);
										}
										
										// Creation of the nodes
										cb.call(this, nodes);
									}
								});
							}
						},
						plugins: [
							"grid",
							"contextmenu",
							"dnd",
							"types"
						],
						contextmenu: {
							// Do not trigger select node event when right click mouse is pressed
							select_node: false
						},
						types: {
							// default type = Node folder (=Steps, Sequences, Connectors...) 
							"default": {
								icon: "folder"
							}
						},
						grid: {
						    columns: [{}, {
						        value: "comment",
						        cellClass: "comment"
						    }] // Project - Comment
						}
					})
					.on("select_node.jstree", function (event, data) {
						PTV.handleSelectNodeEvent(data.node);
					})
					.on("select_cell.jstree-grid", function (event, data) {
						var node = PTV.callJstree.get_node(data.node[0].id);
						// Check if the node has a comment (node.data(.comment))
						// If it haven't, it is a folder
						if (node.data) {
							// Removes "// "
							var editComment = StringUtils.unescapeHTML(node.data.comment.substr(3));
							PTV.editCell(node, {
							    value: data.sourceName
							}, data.grid, editComment);
						}
						
				        event.preventDefault();
					})
					.on("update_cell.jstree-grid", function (event, data) {
						var newComment = data.value;
						
						// Add other lines if they exist
						if (data.node.data.restOfComment) {
							newComment += data.node.data.restOfComment;
						}
						// TODO : CALL webservice to update comment property
						console.log("Update with new comment : '" + newComment + "'");
					});
				
				// Keep reference to jstree function
				PTV.callJstree = $(PTV.tree).jstree();
			},
			loadProjects: function (userName, password, callback) {
				// We first have to authenticate to get the projects
				$.ajax({
					url: PTV.createConvertigoServiceUrl("engine.Authenticate"),
					data: {
						authUserName: userName,
						authPassword: password,
						authType: "login"
					},
					success: function (data, textStatus, jqXHR) {
						// User is authenticated : we can create project nodes
						if ($(data).find("role[name='AUTHENTICATED']").length !== 0) {
							callback();
						}
					}
				});
			}
		};
	
	var convertigoMachineUrl = "http://localhost:18080/";
	var jstreeId = "#projectTreeView";
	var authUserName = "admin";
	var authPassword = "admin";
	PTV.init(convertigoMachineUrl, jstreeId, authUserName, authPassword);
})();