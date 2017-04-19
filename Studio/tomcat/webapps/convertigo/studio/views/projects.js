function ProjectsView(jstreeId) {
	/***
	 * Variable(s)
	 */
	this.lastSelectedNode = null;
	this.tree = null;
	var that = this;
	
	/***
	 * "Constructor"
	 */
	// Property updated
	$(this).on("set_property.dbo-manager", function (event, qnames, property, value, data) {
		for (var j = 0; j < qnames.length; ++j) {
			var nodeId = that.computeNodeId(qnames[j]);				
			var idNodes = that.tree.jstree().getIdNodes(nodeId);
			
			// Do update for each nodes
			for (var i = 0; i < idNodes.length; ++i) {
				var node = that.tree.jstree().get_node(idNodes[i]);
				var $nodeData = $(data).find(">*[qname='" + qnames[j] + "']").children();
				
				// Text node
				var textNode = $nodeData.attr("name");
				if (typeof textNode !== "undefined") {
					node.text = textNode;
				}
				
				var newValue = $nodeData.find("[value]").attr("value").toString();
				
				// Comment
				if (property == "comment") {
					var comments = that.computeComment(newValue);
					node.data.comment = comments.comment;
					node.data.restOfComment = comments.restOfComment;
				}
				// Enabled
				else if (property == "isEnabled") {
					var enabled = that.computeEnabled(newValue);
					if (typeof enabled.isEnabled !== "undefined") {
						node.data.isEnabled = enabled.isEnabled;
						
						// Remove CSS -> node is enable
						if (node.data.isEnabled) {
							node.li_attr["class"] = node.li_attr["class"].replace(/\s*nodeDisable\s*/, "");
						}
						// Add CSS -> node is disable
						else if (!/nodeDisable/.test(node.li_attr["class"])) {
						    node.li_attr["class"] += " nodeDisable";
						}
					}
				}
				
				// Redraw node
				that.tree.jstree().redraw_node(node.id);
			}
		}
	});
	
	// Databse object deleted
	$(this).on("database_object_delete.dbo-manager", function (event, qnamesDbosToDelete) {
		for (var i = 0; i < qnamesDbosToDelete.length; ++i) {
			var qnId = that.computeNodeId(qnamesDbosToDelete[i]);
			
			// A node can be referenced multiple times in case of inherited Screen Classes (Criteria, Extraction rules...)
			var idNodes = ResponseActionManager.projectViews.tree.jstree().getIdNodes(qnId);
			for (var j = 0; j < idNodes.length; ++j) {
				// Get the parent
				var parentNodeId = that.tree.jstree().get_parent(idNodes[j]);
				var parentNode = that.tree.jstree().get_node(parentNodeId);
				
				that.tree.jstree().delete_node(parentNode.children.length == 1 ?
					// Remove the parent node if the current element is the last child
				    parentNode :
				    // Just remove the element
				    idNodes[j]
				);
			}
		}
	});
	
	// Initialize jstree
	that.tree = $(jstreeId);
	$(that.tree)
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
						url: Main.createConvertigoServiceUrl("studio.database_objects.GetChildren"),
						data: isRoot ? {} : {qname: node.data.qname},
						success: function (data, textStatus, jqXHR) {
							// Node is root : we create project nodes
							if (isRoot) {
								var nodes = [];
								// Create the nodes for each project
								$(data).find("admin>*").each(function () {
									nodes.push(that.createNodeJsonDbo(this));
								});
							}
							else {
								var nodes = that.createChildNodes($(data).find("admin>*"), node);
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
				"types",
				"utils"
			],
			contextmenu: {
				show_at_node: false,
				items: function (node) {
					// Get all nodes
					var selectedNodes = that.tree.jstree().get_selected(true);
					
					// Get qnames and folderTypes to send to the server to get the context menu
					var qnames = [];
					var folderTypes = [];
					for (var i = 0; i < selectedNodes.length; ++i) {
						var node = selectedNodes[i];
						if (typeof node.data.qname !== "undefined") {
							qnames.push(node.data.qname);
						}
						else {
							folderTypes.push(node.data.folderType);
						}
					}
					
					var items = {};
					if ((folderTypes.length === 1 && qnames.length === 0) ||
						(qnames.length > 0 && folderTypes.length === 0)) {
						
						// Get menu
						$.ajax({
						    url: Main.createConvertigoServiceUrl("studio.database_objects.GetMenu"),
						    // TODO : FIND A SOLUTION TO GENERATE THE MENU ASYNCHRONOUS (need to finish the jstreecontextmenuajax plugin)
						    async: false,
							data: {
						    	qnames: qnames,
						    	folderTypes: folderTypes
						    },
							success: function (data, textStatus, jqXHR) {										
								// Create the menu if it has correctly been generated
								if ($(data).find("admin>response").attr("state") == "success") {
									that.createContextMenu(items, $(data).find("admin>menu"));
								}
							}
						});
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
			that.handleSelectNodeEvent(data.node);
		})
		.on("select_cell.jstree-grid", function (event, data) {
			var node = that.tree.jstree().get_node(data.node[0].id);
			// Check if the node has a comment (if it hasn't, it's  a folder)
			if (typeof node.data.comment !== "undefined") {
				// Removes "// "
				var editComment = StringUtils.unescapeHTML(node.data.comment.substr(3));
				that.editCell(node, {
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
			
			DatabaseObjectManager.setProperty([data.node.data.qname], "comment", newComment);
		});
	
	/***
	 * Function(s)
	 */
	ProjectsView.prototype.addJstreeNodeType = function (classname) {
		var nodeType = StringUtils.replaceDotByMinus(classname);
		
		// Add the node type and specify the icon for these nodes
		that.tree.jstree().settings.types[nodeType] = {
			icon: nodeType
		}
		
		return nodeType;
	};
	
	ProjectsView.prototype.callServiceCallAction = function (qnames, classAction, response = null) {
		$.ajax({
			url: Main.createConvertigoServiceUrl("studio.database_objects.CallAction"),
			data: {
				qnames: qnames,
				action: classAction,
				response: response
			},
			success: function (data, textStatus, jqXHR) {
				var $responses = $(data).find("admin");
				$responses.find(">*").each(function () {
					if ($(this).attr("state") === "success") {
						ResponseActionManager.handleResponse(
							$(this).attr("name"),
							$(data),
							that
						);
					}
					// Show error
					else {
						var $response = $responses.find("admin>*>*").first();
						Modal.createMessageBox(
							$response.find("title").text(),
							$response.find("message").text()
						);
					}
				});
			}
		});
	};
	
	ProjectsView.prototype.createContextMenu = function (parent, $menu) {
		var children = $menu.children();
		for (var i = 0; i < children.length; ++i) {
			var indexNextNode = i + 1;
			var isValidIndex = indexNextNode < children.length;
	
			// Create sub-menu
			if (children[i].nodeName == "menu") {
				var label = StringUtils.escapeHTML($(children[i]).attr("label"));
	
				var menuItem = {
					label: label,
					separator_after: isValidIndex,
					submenu: {}
				};
				parent[label.replace(/\s/g, "")] = menuItem;
				that.createContextMenu(menuItem.submenu, $(children[i]));
			}
			// Create action
			else {
				var label = StringUtils.escapeHTML($(children[i]).attr("label"));
				var iconClass = $(children[i]).attr("isChecked") == "true" ?
						// If is checked : show check maker
					    "contextmenu-entry-default" :
					    // Show classic icon
					    $(children[i]).attr("icon");
				
				var isDisabled = $(children[i]).attr("isEnabled") == "false";
				if (isDisabled) {
					iconClass += " contextmenu-entry-disable";
				}
	
				(function () {
					// Get the action to call
					var classAction = $(children[i]).attr("class");
					parent[label.replace(/\s/g, "")] = {
						label: label,
						icon: iconClass,
						action: function (node) {
							// Get the qnames
							var selectedNodes = that.tree.jstree().get_selected(true);
							var qnames = [];
							for (var i = 0; i < selectedNodes.length; ++i) {
								var node = selectedNodes[i];
								if (typeof node.data.qname !== "undefined") {
									qnames.push(node.data.qname);
								}
							}
							
							that.callServiceCallAction(qnames, classAction, null);
						},
						_disabled: isDisabled,
						// menubarPath = category
						separator_after: isValidIndex && $(children[i]).attr("menubarPath") !== $(children[indexNextNode]).attr("menubarPath")
					}
				}());
			}
		}
	};
	
	ProjectsView.prototype.createChildNodes = function ($dbo, node) {
		var nodes = [];
		nodes.children = [];
		var categories = [];
		$dbo.children().each(function () {
			var categoryName = $(this).attr("category");
			
			if (categoryName != "BlockFactory") {
				// Check if category (=Steps, Connector, Sequence, etc.) exists
				if (!categories[categoryName]) {
					var newCategoryName = that.computeCategoryName(categoryName);
					// Check if one of the parents of the selected node is a category
					var parentCategoryFound = node.parents.find(function (parent) {
						return newCategoryName === that.tree.jstree().get_text(parent);
					});
					
					var createCategory = false;

					// If the dbo is a screen class, create adequate category : Screen classes/Inherited screen classes
					if (categoryName == "ScreenClass") {
						var categoryNode = that.createNodeJsonFolder(
								parentCategoryFound ? "Inherited screen classes" : newCategoryName,
								parentCategoryFound ? "Inherited" + categoryName : categoryName
						);
						createCategory = true;
					}
					// If category not found, we create it
					else if (!parentCategoryFound) {
						var categoryNode = that.createNodeJsonFolder(newCategoryName, categoryName);
						createCategory = true;
					}

					// Create category if needed
					if (createCategory) {
						categories[categoryName] = categoryNode;
						nodes.push(categoryNode);
					}
				}
			}
			
			// Create the node and "set" its parent: the new category or the selected node
			var dboNode = that.createNodeJsonDbo(this);
			if (categories[categoryName]) {
				categories[categoryName].children.push(dboNode);
			}
			else {
				nodes.push(dboNode);
			}
		});
		
		return nodes;
	};
	
	ProjectsView.prototype.createNodeJsonFolder = function (text, categoryName) {
		return {
			text: text,
			children: [],
			data: {
				folderType: categoryName
			}
		};
	};
	
	ProjectsView.prototype.computeCategoryName = function(category) {
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
			return "Platforms";
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
		if (tmpCategory == "urlmapping") {
			return "Mappings";
		}
		if (tmpCategory == "urlmappingoperation") {
			return "Operations";
		}
		if (tmpCategory == "urlmappingparameter") {
			return "Parameters";
		}
		if (tmpCategory == "urlmappingresponse") {
			return "Responses";
		}
		
		var newName = category.substring(0,1).toUpperCase() + category.substring(1);	
		newName += category.substring(category.length-1) == "s" ? "es" : "s";
	
		return newName;
	};
	
	ProjectsView.prototype.createNodeJsonDbo = function (dboElt) {
		var qname = $(dboElt).attr("qname");
		var comments = that.computeComment($(dboElt).attr("comment"));
		var enabled = that.computeEnabled($(dboElt).attr("isEnabled"));
		var inherited = that.computeInherited($(dboElt).attr("isInherited"));

		var nodeId = that.computeNodeId(qname);
		var classes = that.computeLiClasses(enabled.liClass, inherited.liClass);
		
		var nodeJsonDbo = {
			id: that.tree.jstree().generateId(nodeId),
			text: $(dboElt).attr("name"),
			// Add a new type for icons
			type: that.addJstreeNodeType($(dboElt).attr("classname")),
			li_attr: {
				"class": classes,
			},
			children: $(dboElt).attr("hasChildren") === "true",
			data: {
				qname: qname,
				comment: comments.comment,
				restOfComment: comments.restOfComment
			}
		};
		
		if (typeof enabled.isEnabled !== "undefined") {
			nodeJsonDbo.data.isEnabled = enabled.isEnabled;
		}
		
		return nodeJsonDbo;
	};
	
	ProjectsView.prototype.computeComment = function (comment) {
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
	};
	
	ProjectsView.prototype.computeEnabled = function (isEnabled) {
		var enabled = {
			liClass: ""
		};
		
		if (typeof isEnabled !== "undefined") {
			var realIsEnabled = isEnabled == "false" ? false : true;
			if (!realIsEnabled) {
				// Node is disable
				enabled.liClass = "nodeDisable";
			}
			
			enabled.isEnabled = realIsEnabled;
		}
		
		return enabled;
	};
	
	ProjectsView.prototype.computeInherited = function (isInherited) {
		var inherited = {
			liClass: ""
		};
		
		if (typeof isInherited !== "undefined") {
			if (isInherited === "true") {
				inherited.liClass = "nodeIsInherited";
			}
		}
		
		return inherited;
	};
	
	ProjectsView.prototype.computeLiClasses = function (...classes) {
		return $.grep(classes, Boolean).join(" ");
	};
	
	ProjectsView.prototype.computeNodeId =  function (qname) {
		return "qn-" + StringUtils.replaceDotByMinus(qname);
	};
	
	/* 
	 * Function highly inspired from the _edit(...) function of jstree-grid plugin.
	 * 
	 * NOTE : If you use another version of the jstree-grid plugin, you migh have
	 *        to update this function as it uses functions defined in the jstree-grid
	 *        plugin.
	 */
	ProjectsView.prototype.editCell = function (obj, col, element, editText) {
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
		var rtl = that.tree.jstree()._data.core.rtl,
		    w = that.tree.jstree().element.width(),
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
		            "height": (that.tree.jstree()._data.core.li_height) + "px",
		            "lineHeight": (that.tree.jstree()._data.core.li_height) + "px",
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
		                that.tree.jstree().element.trigger('update_cell.jstree-grid', {
		                    node: obj,
		                    col: col.value,
		                    value: v,
		                    old: t
		                });
		                that.tree.jstree()._prepare_grid(this.get_node(obj, true));
		            }
		            h2.remove();
		            element.show();
		        }, that.tree.jstree()),
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
	};
	
	ProjectsView.prototype.handleSelectNodeEvent = function (node) {
		// Don't update properties view if we select the same node again
		if (that.lastSelectedNode != node) {
			that.lastSelectedNode = node;
			PropertiesView.refresh(node);
		}
	};
};
