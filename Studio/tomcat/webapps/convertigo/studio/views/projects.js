var Injector = {
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
	addDoubleSlash: function (text) {
		return "// " + text;
	},
	unescapeHTML: function (text) {
		var unescapedHTML = $.parseHTML(text);
		return unescapedHTML ? unescapedHTML[0].nodeValue : "";
	}
};

var ProjectsView = {
		// Variables
		lastSelectedNode: null,
		tree: null,
		url: {
			baseUrl: null,
			baseUrlConvertigo: null,
			baseUrlConvertigoStudio: null,
			baseUrlConvertigoServices: null
		},
		
		// Functions
		init: function (baseUrl, jstreeId, authUserName, authPassword) {
			// Initialize URLs
			ProjectsView.url.baseUrl = baseUrl;
			ProjectsView.url.baseUrlConvertigo = ProjectsView.url.baseUrl + "convertigo/";
			ProjectsView.url.baseUrlConvertigoStudio = ProjectsView.url.baseUrlConvertigo + "studio/";
			ProjectsView.url.baseUrlConvertigoServices = ProjectsView.url.baseUrlConvertigo + "admin/services/";
			
			// Inject CSS
			Injector.injectLinkStyle(ProjectsView.url.baseUrlConvertigoStudio + "css/jstree/themes/default-dark/style.min.css");
			Injector.injectLinkStyle(ProjectsView.url.baseUrlConvertigoStudio + "css/style.css");
			
			Injector.injectScript(ProjectsView.url.baseUrlConvertigoStudio + "views/studio.js", function () {
				// Load jQuery library
				Injector.injectScript(ProjectsView.url.baseUrlConvertigo + "scripts/jquery-2.1.4.js", function () {
					// Inject jstree library
					Injector.injectScript(ProjectsView.url.baseUrlConvertigoStudio + "js/jstree/jstree.js", function () {
						// Delay because jstree.min.js is not correctly loaded sometimes...
						setTimeout(function () {
							// Inject jstree grid plugin
							Injector.injectScript(ProjectsView.url.baseUrlConvertigoStudio + "js/jstree/jstreegrid-3.5.14.js", function () {
								// Define AJAX setup
								$.ajaxSetup({
									type: "POST",
									dataType: "xml",
									xhrFields: {
										withCredentials: true
									}
								});
								
								// Create first level of the tree : project nodes
								ProjectsView.loadProjects(authUserName, authPassword, function () {
									ProjectsView.initJstree(jstreeId);
								});									
							});
						}, 1500);
					});
				});
			});
		},
		// Add a new node type for jstree
		addJstreeNodeType: function (classname) {
			// Replace all "." by "-"
			var nodeType = classname.replace(/\./g, "-");
			
			// Add the node type and specify the icon for these nodes
			ProjectsView.tree.jstree().settings.types[nodeType] = {
				icon: nodeType
			}
			
			return nodeType;
		},
		createChildNodes: function ($dbo, node) {
			var nodes = [];
			nodes.children = [];
			var categories = [];
			$dbo.children().each(function () {
				var categoryName = $(this).attr("category");
				
				// Check if category (=Steps, Connector, Sequence, etc.) exists
				if (!categories[categoryName]) {
					var newCategoryName = ProjectsView.computeCategoryName(categoryName);
					// Check if one of the parents of the selected node is a category
					var parentCategoryFound = node.parents.find(function (parent) {
						return newCategoryName === ProjectsView.tree.jstree().get_text(parent);
					});
					
					var createCategory = false;

					// If the dbo is a screen class, create adequate category : Screen classes/Inherited screen classes
					if ($dbo.attr("isScreenClass") == "true") {
						var categoryNode = {
							text: parentCategoryFound ? "Inherited screen classes" : newCategoryName,
							children: []
						};
						createCategory = true;
					}
					// If category not found, we create it
					else if (!parentCategoryFound) {
						var categoryNode = {
							text: newCategoryName,
							children: []
						};
						createCategory = true;
					}
					
					// Create category if needed
					if (createCategory) {
						categories[categoryName] = categoryNode;
						nodes.push(categoryNode);
					}
				}
				
				// Create the node and "set" its parent: the new category or the selected node
				var dboNode = ProjectsView.createNodeJsonDbo(this);
				if (categories[categoryName]) {
					categories[categoryName].children.push(dboNode);
				}
				else {
					nodes.push(dboNode);
				}
			});
			
			return nodes;
		},
		createConvertigoServiceUrl: function (serviceName) {
			return ProjectsView.url.baseUrlConvertigoServices + serviceName;
		},
		createNodeJsonDbo: function (dboElt) {
			var comments = ProjectsView.computeComment($(dboElt).attr("comment"));
			var enabled = ProjectsView.computeEnabled($(dboElt).attr("isEnabled"));

			return {
				text: $(dboElt).attr("name"),
				// Add a new type for icons
				type: ProjectsView.addJstreeNodeType($(dboElt).attr("classname")),
				li_attr: {
					"class": enabled.liClass,
				},
				children: $(dboElt).attr("hasChildren") === "true",
				data: {
					qname: $(dboElt).attr("qname"),
					comment: comments.comment,
					restOfComment: comments.restOfComment,
					isEnabled: enabled.isEnabled
				}
			};
		},
		/* 
		 * Function highly inspired from the _edit(...) function of jstree-grid plugin.
		 * 
		 * NOTE : If you use another version of the jstree-grid plugin, you migh have
	     *        to update this function as it uses functions defined in the jstree-grid
		 *        plugin.
		*/
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
			var rtl = ProjectsView.tree.jstree()._data.core.rtl,
			    w = ProjectsView.tree.jstree().element.width(),
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
			            "height": (ProjectsView.tree.jstree()._data.core.li_height) + "px",
			            "lineHeight": (ProjectsView.tree.jstree()._data.core.li_height) + "px",
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
			                obj.data[col.value] = v.length ? StringUtils.addDoubleSlash(StringUtils.escapeHTML(v)) : v;
			                ProjectsView.tree.jstree().element.trigger('update_cell.jstree-grid', {
			                    node: obj,
			                    col: col.value,
			                    value: v,
			                    old: t
			                });
			                ProjectsView.tree.jstree()._prepare_grid(this.get_node(obj, true));
			            }
			            h2.remove();
			            element.show();
			        }, ProjectsView.tree.jstree()),
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
		computeCategoryName: function(category) {
			var tmpCategory = category.toLowerCase();
			if (tmpCategory == "screenclass") {
				return "Screen classes";
			}
			if (tmpCategory == "extractionrule") {
				return "Extraction rules";
			}
			if (tmpCategory == "criteria") {
				return "Criteria";
			}
			if (tmpCategory == "mobileapplication") {
				return "Mobile applications";
			}
			if (tmpCategory == "mobileplatform") {
				return "Mobile platforms";
			}
			if (tmpCategory == "mobilecomponent") {
				return "Mobile components";
			}
			if (tmpCategory == "testcase") {
				return "Test cases";
			}
			if (tmpCategory == "statement") {
				return "Functions";
			}
			
			var newName = category.substring(0,1).toUpperCase() + category.substring(1);	
			newName += category.substring(category.length-1) == "s" ? "es" : "s";

			return newName;
		},
		handleSelectNodeEvent: function (node) {
			if (ProjectsView.lastSelectedNode != node) {
				ProjectsView.lastSelectedNode = node;
				PropertiesView.updateProperties2(node);
			}
		},
		initJstree: function (jstreeId) {
			// Inject CSS the icons of each type of nodes
			Injector.injectLinkStyle(ProjectsView.createConvertigoServiceUrl("studio.database_objects.GetCSS"));
			
			$(ProjectsView).on("set_property.database-object-manager", function (event, data, property, value) {
				var selectedNode = ProjectsView.tree.jstree().get_selected(true)[0];
				
				// Comment
				var comments = ProjectsView.computeComment($(data).find("[name='comment']>[value]").attr("value"));
				selectedNode.data.comment = comments.comment;
				selectedNode.data.restOfComment = comments.restOfComment;

				// Text node
				selectedNode.text = $(data).attr("name");
				
				// Enabled
				var isEnabled = $(data).find("[name='isEnabled']>[value]").attr("value");
				if (isEnabled == "null") {
					selectedNode.data.isEnabled = null;
				}
				// Database object can be enabled or not
				else {
					selectedNode.data.isEnabled = isEnabled == "false" ? false : true;
					if (!selectedNode.data.isEnabled) {
						// Node is disable
						selectedNode.li_attr = {
							"class": "nodeDisable"
						};
					}
					else {
						delete selectedNode.li_attr["class"];
					}
				}
				
				// Redraw node
				ProjectsView.tree.jstree().redraw_node(selectedNode.id);
			});
			
			// Initialize jstree
			ProjectsView.tree = $(jstreeId);
			$(ProjectsView.tree)
				.jstree({
					core: {
						check_callback: true,
						force_text: true,
						animation : 0,
						themes: {
							name: "default-dark",
							dots: false
						},
						data: function (node, cb) {
							var isRoot = node.id == "#";
							$.ajax({
								url: ProjectsView.createConvertigoServiceUrl("studio.database_objects.GetChildren"),
								data: isRoot ? {} : {qname: node.data.qname},
								success: function (data, textStatus, jqXHR) {
									// Node is root : we create project nodes
									if (isRoot) {
										var nodes = [];
										// Create the nodes for each project
										$(data).find("admin>*").each(function () {
											nodes.push(ProjectsView.createNodeJsonDbo(this));
										});
									}
									else {
										var nodes = ProjectsView.createChildNodes($(data).find("admin>*"), node);
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
						items: function (node) {
							var items = {};
							
							if (node.data && node.data.isEnabled != null) {
								items.enable = {
									label: "Enable",
									_disabled: node.data.isEnabled,
									action: function () {
										DatabaseObjectManager.setProperty(node.data.qname, "isEnabled", true);
									}
								}
								
								items.disable = {
									label: "Disable",
									_disabled: !node.data.isEnabled,
									action: function () {
										DatabaseObjectManager.setProperty(node.data.qname, "isEnabled", false);
									}
								}
							}

							return items;
						}
					},
					types: {
						// default type = Node folder (=Steps, Sequences, Connectors...) 
						"default": {
							icon: "folder"
						}
					},
					grid: {
					    columns: [{	
					    }, {
					        value: "comment",
					        cellClass: "comment"
					    }] // Project - Comment
					}
				})
				.on("select_node.jstree", function (event, data) {
					ProjectsView.handleSelectNodeEvent(data.node);
				})
				.on("select_cell.jstree-grid", function (event, data) {
					var node = ProjectsView.tree.jstree().get_node(data.node[0].id);
					// Check if the node has a comment (node.data(.comment))
					// If it haven't, it is a folder
					if (node.data) {
						// Removes "// "
						var editComment = StringUtils.unescapeHTML(node.data.comment.substr(3));
						ProjectsView.editCell(node, {
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
					
					DatabaseObjectManager.setProperty(data.node.data.qname, "comment", newComment);
				})
				.on("ready.jstree", function (event, data) {
					ProjectsView.tree.jstree().element.on('keydown.jstree', '.jstree-anchor', $.proxy(function (e) {
					    if(e.target.tagName && e.target.tagName.toLowerCase() === "input") { return true; }
					    if(e.which !== 32 && e.which !== 13 && (e.shiftKey || e.ctrlKey || e.altKey || e.metaKey)) { return true; }
					    var o = null;
					    if(ProjectsView.tree.jstree()._data.core.rtl) {
					        if(e.which === 37) { e.which = 39; }
					        else if(e.which === 39) { e.which = 37; }
					    }
					    
					    if (e.which === 113) {
					       console.log("f2");
					    }
					}));
				});
				
			// Property view jstree
			PropertiesView.init(".informationView");
			DatabaseObjectManager.addListener(ProjectsView);
			DatabaseObjectManager.addListener(PropertiesView);
		},
		loadProjects: function (userName, password, callback) {
			// We first have to authenticate to get the projects
			$.ajax({
				url: ProjectsView.createConvertigoServiceUrl("engine.Authenticate"),
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
		},
		computeComment: function (comment) {
			var comment = StringUtils.escapeHTML(comment);
			var restOfComment = null;
	
			var indexOfNewLine = comment.indexOf("\r");
			if (indexOfNewLine === -1) {
				indexOfNewLine = comment.indexOf("\n");
			}
			
			// If it is a multi-lines comment
			if (indexOfNewLine !== -1) {
				// The other lines of the comment
				restOfComment = comment.substr(indexOfNewLine);
				
				// The fist line
				comment = comment.substr(0, indexOfNewLine);
			}

			// Add "// " at the beginning of the comment
			comment = comment.length ? StringUtils.addDoubleSlash(comment) : "";
			
			return {
				comment,
				restOfComment
			};
		},
		computeEnabled: function (isEnabled) {
			var enabled = {
				liClass: ""
			};
			
			if (isEnabled == "null") {
				enabled.isEnabled = null;
			}
			// Database object can be enabled or not
			else {
				var realIsEnabled = isEnabled == "false" ? false : true;
				if (!realIsEnabled) {
					// Node is disable
					enabled.liClass = "nodeDisable";
				}
				
				enabled.isEnabled = realIsEnabled;
			}
			
			return enabled;
		}
	};

var convertigoMachineUrl = "http://localhost:18080/";
var jstreeId = ".projectsView";
var authUserName = "admin";
var authPassword = "admin";

//var dbos = {};
ProjectsView.init(convertigoMachineUrl, jstreeId, authUserName, authPassword);