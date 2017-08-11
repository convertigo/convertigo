function ProjectsView(propertiesView, palettes, jstreeTheme = "default") {
	TreeViewContainer.call(this, "projects-tree-view", jstreeTheme);

	this.propertiesView = propertiesView;

	this.palettes = {};
	for (var i = 0; i < palettes.length; ++i) {
		this.palettes[palettes[i].getId()] = palettes[i];
		palettes[i].setProjectsView(this);
	}

	var that = this;

	this.resetDndData();
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
				if (VariableUtils.isDefined(textNode)) {
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
					if (VariableUtils.isDefined(enabled.isEnabled)) {
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

	// Database object deleted
	$(this).on("database_object_delete.dbo-manager", function (event, qnamesDbosToDelete) {
		for (var i = 0; i < qnamesDbosToDelete.length; ++i) {
			var qnId = that.computeNodeId(qnamesDbosToDelete[i]);

			// A node can be referenced multiple times in case of inherited Screen Classes (Criteria, Extraction rules...)
			var idNodes = that.tree.jstree().getIdNodes(qnId);
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

	var lastSelectedNodeId = null;

    var updatePropertiesView = function (node) {
        lastSelectedNodeId = node.id;
        that.propertiesView.refresh(node);
    };

    var handleDropPalette = function (operation, node, node_parent, node_position, more) {
        if (operation === "copy_node") {
            if (!that.dnd.palette.canCreate) {
                return false;
            }

            // By default, insert at the end
            var afterPriority = null;
            // Insert at a specific position
            if (node_position > 0) {
                var nodeInsertAfter = that.tree.jstree().get_node(node_parent.children[node_position - 1]);
                var afterPriority = nodeInsertAfter.data.priority;
            }
            // Insert at first position
            else if (node_position == 0 && that.dnd.palette.markerBetweenNodes) {
                afterPriority = 0;
            }

            var isFolder = that.isNodeFolder(node_parent);
            // Get the real parent node
            var nodeParent = isFolder ? that.tree.jstree().get_node(node_parent.parent) : node_parent;
            var qname = nodeParent.data.qname;

            // Create
            $.ajax({
                dataType: "xml",
                url: Convertigo.createServiceUrl("studio.database_objects.Create"),
                data: {
                    qname: qname,
                    beanClass: node.id,
                    folderType: isFolder ? node_parent.data.folderType : null,
                    afterPriority: afterPriority
                },
                success: function (data, textStatus, jqXHR) {
                    var $adminXml = $(data).find("admin");
                    var $dboXml = $adminXml.find("dbo");
                    // Dbo created
                    if ($dboXml.length) {
                        // Store the id of the new node to show it at the end of the refresh
                        that.dnd.palette.newIdNodeCreated = that.computeNodeId($dboXml.attr("qname"));

                        // Store the opened nodes to re-open them at the end of the refresh
                        var projectNode = that.getProjectNode(node_parent);
                        that.dnd.palette.openedNodes = that.tree.jstree().get_json(projectNode, {
                            flat: true
                        })
                        // Only get opened nodes
                        .filter(function(node) {
                            return node.state.opened;
                        });

                        // Refresh the parent node to automaticaly generate the new node
                        that.tree.jstree().refresh_node(nodeParent.id);
                    }
                    else {
                        that.resetDndData();
                    }

                    // Show errors
                    $adminXml.find(">*[name='MessageBoxResponse']").reverse().each(function() {
                        var $msgBoxXml = $(this).find(">*");
                        ModalUtils.createMessageBox(
                            $msgBoxXml.find("title").text(),
                            $msgBoxXml.find("message").text()
                        );
                    });
                }
            });

            return false;
        }

        return true;
    };

    var handleDropSourcePicker = function (operation, node, node_parent, node_position, more) {
        if (operation === "copy_node") {
            if (that.dnd.sourcepicker.canCreate) {
                $.ajax({
                    dataType: "xml",
                    url: Convertigo.createServiceUrl("studio.database_objects.SetStepSourceDefinition"),
                    data: {
                        qname: node_parent.data.qname
                    },
                    success: function (data, textStatus, jqXHR) {
                        var $adminXml = $(data).find("admin");
                        if ($adminXml.find("step")) {
                            updatePropertiesView(node_parent);
                            /*
                             * No need to notify the properties view as the "updatePropertiesView"
                             * function has just updated the properties.
                             */ 
                            DatabaseObjectManager.notifySetProperty($adminXml, [that.propertiesView]);
                            that.resetDndData();
                        }
                    }
                });
            }
            return false;
        }

        return true;
    };

	// Initialize jstree
	$(that.tree)
		.jstree({
			core: {
			    dblclick_toggle: false,
				check_callback: function (operation, node, node_parent, node_position, more) {
					// operation can be 'create_node', 'rename_node', 'delete_node', 'move_node', 'copy_node' or 'edit'
				    // in case of 'rename_node' node_position is filled with the new node name
				    if (that.dnd.palette.started) {
				        return handleDropPalette(operation, node, node_parent, node_position, more);
				    }
				    else if (that.dnd.sourcepicker.started) {
				        return handleDropSourcePicker(operation, node, node_parent, node_position, more);
				    }

				    return true;
				},
			    force_text: true, // Prevent XSS vulnerabilities
				animation : 0,
				themes: {
					name: that.jstreeTheme,
					dots: false
				},
				// Create nodes
				data: function (node, cb) {
					var isRoot = node.id == "#";
					$.ajax({
					    dataType: "xml",
						url: Convertigo.createServiceUrl("studio.database_objects.GetChildren"),
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
				"contextmenu",
				"dnd",
				"grid",
				"types",
				"utils"
			],
			contextmenu: {
				show_at_node: false,
				items: function (node, buildContextMenu) {
					// Get all nodes
					var selectedNodes = that.tree.jstree().get_selected(true);

					// Get qnames and folderTypes to send to the server to get the context menu
					var qnames = [];
					var folderTypes = [];
					var refQnameFolder = null;
					for (var i = 0; i < selectedNodes.length; ++i) {
						var node = selectedNodes[i];
						if (VariableUtils.isDefined(node.data.qname)) {
							qnames.push(node.data.qname);
						}
						else {
							folderTypes.push(node.data.folderType);
						}
					}

					var items = {};
					if ((folderTypes.length === 1 && qnames.length === 0) ||
						(qnames.length > 0 && folderTypes.length === 0)) {
						/*
						 * If only one folder is selected and no node has been selected,
						 * we send the qname of the parent node of the folder
						 */
						if (qnames.length === 0) {
							var parentNode = that.tree.jstree().get_node(node.parent);
							refQnameFolder = parentNode.data.qname;
						}

						// Get menu
						$.ajax({
						    dataType: "xml",
						    url: Convertigo.createServiceUrl("studio.database_objects.GetMenu"),
							data: {
						    	qnames: qnames,
						    	folderTypes: folderTypes,
						    	refQnameFolder: refQnameFolder
						    },
							success: function (data, textStatus, jqXHR) {										
								// Create the menu if it has correctly been generated
								if ($(data).find("admin>response").attr("state") == "success") {
									that.createContextMenu(items, $(data).find("admin>menu"));
									buildContextMenu(items);
								}
							}
						});
					}
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
		    updatePropertiesView(data.node);

			/*
			 * If the condition is true, it means a dbo has been created.
			 * However when it is created, we select the new node automatically
			 * so the Palette will be updated with the new categories and we don't want
			 * to do that because it is not user friendly.
			 */
			if (!that.dnd.palette.started) {
				var qname = null;
				var folderType = null;
				// It is not a folder
				if (!that.isNodeFolder(data.node)) {
					qname = data.node.data.qname;
				}
				else {
					// Parent of the selected folder = the related dbo
					var parentNode = that.tree.jstree().get_node(data.node.parent);
					
					qname = parentNode.data.qname;
					folderType = data.node.data.folderType;
				}

				// Get Palette
				$.ajax({
				    dataType: "xml",
					url: Convertigo.createServiceUrl("studio.database_objects.GetPalette"),
					data: {
						qname: qname,
						folderType: folderType
					},
					success: function (data, textStatus, jqXHR) {
						var $adminXml =  $(data).find("admin");
						var $categoriesXml = $adminXml.find(">:first");
						// Update all Palette views
						for (var key in that.palettes) {
							that.palettes[key].update($categoriesXml);
						}

						// Show errors
						$adminXml.find(">*[name='MessageBoxResponse']").reverse().each(function () {
							var $msgBoxXml = $(this).find(">*");
							ModalUtils.createMessageBox(
								$msgBoxXml.find("title").text(),
								$msgBoxXml.find("message").text()
							);
						});
					}
				});
			}
		})
		.on("select_cell.jstree-grid", function (event, data) {
			var node = that.tree.jstree().get_node(data.node[0].id);
			if (!that.isNodeFolder(node)) {
				// Removes "// "
				var editComment = StringUtils.unescapeHTML(node.data.comment.substr(3));
	            that.editCell(
	            	node, {
                        value: data.sourceName
                    },
                    data.grid,
                    editComment
                );
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
		})
		.on("refresh_node.jstree", function (node, nodes) {
			// Case of drag and drop
			if (that.dnd.palette.started) {
				// Delete comments columns
				that.tree.jstree()._clean_grid();
				// ... and regenerate it (need to do that else comments are shifted, it is not handle by jstreegrid)
				that.tree.find("li[role='treeitem']").each(function () {
					that.tree.jstree()._prepare_grid($(this))
				});

				// Show the new created node and select it
				var idNodes = that.tree.jstree().getIdNodes(that.dnd.palette.newIdNodeCreated);
				that.tree.jstree()._open_to(idNodes[0]);
				that.tree.jstree().deselect_node(lastSelectedNodeId);
				that.tree.jstree().select_node(idNodes[0]);

				// Re-open opened nodes
				that.dnd.palette.openedNodes.forEach(function (node) {
				    that.tree.jstree().open_node(node);
				});

				that.resetDndData();
			}
		})
		.on("loaded.jstree", function () {
		    // Select the first node (=project) when the tree is loaded
		    var firstNodeId = that.tree.jstree().get_node("#").children[0];
		    that.tree.jstree().select_node(firstNodeId);
		})
		.on("dblclick.jstree", function (event, b) {
		    var target = $(event.target).closest("li");
		    var selectNode = that.tree.jstree().get_node(target);

		    if (!that.isNodeFolder(selectNode)) {
		        $.ajax({
                    dataType: "xml",
                    url: Convertigo.createServiceUrl("studio.database_objects.CallDblkAction"),
                    data: {
                        qname: selectNode.data.qname
                    },
                    success: function (data, textStatus, jqXHR) {
                        var $responses = $(data).find("admin");
                        $responses.find(">*").each(function () {
                            if ($(this).attr("state") === "success") {
                                ResponseActionManager.handleResponse(
                                    $(this).attr("name"),
                                    $(data),
                                    that,
                                    false
                                );
                            }
                            // Show error
                            else {
                                var $response = $responses.find("admin>*>*").first();
                                ModalUtils.createMessageBox(
                                    $response.find("title").text(),
                                    $response.find("message").text()
                                );
                            }
                        });
                    }
                });
		    }
		});

		var lastDistMarkerNode = null;

		var computeDistMarkerNode = function ($nodeElt) {
            return $("#jstree-marker").offset().left - $nodeElt.offset().left;
		};

		var isMarkerBetweenNodes = function (distMakerNode) {
            /*
             * This condition means that the arrow marker (icon ->) is between two nodes:
             * - if true (marker between nodes):
             *       ◢Parent1
             *           ◢Child1
             *           -> (marker is here)
             *           ◢Child2
             * - if false (marker on a node):
             *       ◢Parent2
             *         ->◢Child1 (marker is here)
             *           ◢Child2
             */
            return distMakerNode == -6;
		};
		
		var handleDndMovePalette = function ($targetElt, $allowIconElt, $doppableElement, data) {
            // Get the id of the current target
            var targetId = $doppableElement.attr("id");

            var distMakerNode = computeDistMarkerNode($targetElt);

            var targetChanged = that.dnd.palette.lastTargetNodeId !== targetId || lastDistMarkerNode !== distMakerNode;
            if (targetChanged) {
                that.dnd.palette.lastTargetNodeId = targetId;
                lastDistMarkerNode = distMakerNode;

                var targetNode = that.tree.jstree().get_node($targetElt);

                that.dnd.palette.markerBetweenNodes = isMarkerBetweenNodes(distMakerNode);
                if (that.dnd.palette.markerBetweenNodes) {
                    targetNode = that.tree.jstree().get_node(targetNode.parent);
                }

                // that.dnd.palette.canCreate = targetNode.id !== "#";
                if (targetNode.id !== "#") {
                    var qname = null;
                    var folderType = null;
                    // It is not a folder
                    if (!that.isNodeFolder(targetNode)) {
                        qname = targetNode.data.qname;
                    }
                    else {
                        var parentNode = that.tree.jstree().get_node(targetNode.parent);
                        if (parentNode.id === "#") {
                            qname = targetNode.data.qname;
                        }
                        else {
                            qname = parentNode.data.qname;
                            folderType = targetNode.data.folderType;
                        }
                    }

                    // Check if can create
                    $.ajax({
                        dataType: "xml",
                        url: Convertigo.createServiceUrl("studio.database_objects.CanCreate"),
                        data: {
                            qname: qname,
                            folderType: folderType,
                            beanClass: data.data.nodes[0].id
                        },
                        success: function (data, textStatus, jqXHR) {
                            // If can create
                            that.dnd.palette.canCreate = $(data).find("response").attr("state") === "true";
                            if (that.dnd.palette.canCreate) {
                                $allowIconElt.removeClass("forbidden").addClass("checkmark");
                            }
                            else {
                                $allowIconElt.removeClass("checkmark").addClass("forbidden");
                            }
                        }
                    });
                }
                else {
                    that.dnd.palette.canCreate = false;
                    $allowIconElt.removeClass("checkmark").addClass("forbidden");
                }
            }
		};

	    var handleDndMoveSourcePicker = function ($targetElt, $allowIconElt, $doppableElement, data) {
	        if (!isMarkerBetweenNodes(computeDistMarkerNode($targetElt))) {
	            // Get the id of the current target
	            var targetId = $doppableElement.attr("id");
	            if (that.dnd.sourcepicker.lastTargetNodeId !== targetId) {
	                that.dnd.sourcepicker.lastTargetNodeId = targetId;

	                var targetNode = that.tree.jstree().get_node($targetElt);
	                if (!that.isNodeFolder(targetNode)) {
                        var qname = targetNode.data.qname;

                        // Check if can create
                        $.ajax({
                            dataType: "xml",
                            url: Convertigo.createServiceUrl("studio.sourcepicker.CanCreateSource"),
                            data: {
                                qname: qname
                            },
                            success: function (data, textStatus, jqXHR) {
                                that.dnd.sourcepicker.canCreate = $(data).find("response").text() === "true";
                                if (that.dnd.sourcepicker.canCreate) {
                                    $allowIconElt.removeClass("forbidden").addClass("checkmark");
                                }
                                else {
                                    $allowIconElt.removeClass("checkmark").addClass("forbidden");
                                }
                            }
                        });
	                }
	                else {
	                    that.dnd.sourcepicker.canCreate = false;
	                    $allowIconElt.removeClass("checkmark").addClass("forbidden");
	                }
	            }
	        }
	        else {
	            that.dnd.sourcepicker.lastTargetNodeId = null;
                that.dnd.sourcepicker.canCreate = false;
                $allowIconElt.removeClass("checkmark").addClass("forbidden");
	        }
	    };

        $(document)
            .on("dnd_move.vakata", function (event, data) {
                // Target node
                var $targetElt = $(data.event.target);
                var $allowIconElt = data.helper.find(".allow-status");

                // If on the projects tree view
                if ($targetElt.closest(that.tree).length > 0) {
                    // If on a node
                    var $doppableElement = $targetElt.closest(".drop");
                    if ($doppableElement.length > 0) {
                        switch (data.data.transferData) {
                            case "palette":
                                handleDndMovePalette($targetElt, $allowIconElt, $doppableElement, data);
                                break;
                            case "sourcepicker":
                                handleDndMoveSourcePicker($targetElt, $allowIconElt, $doppableElement);
                                break;
                            default:
                                break;
                        }
                    }
                    else {
                        that.dnd.palette.lastTargetNodeId = null;
                        that.dnd.sourcepicker.lastTargetNodeId = null;
                    }
                }
                else {
                    that.dnd.palette.lastTargetNodeId = null;
                    that.dnd.sourcepicker.lastTargetNodeId = null;
                    $allowIconElt.removeClass("checkmark").addClass("forbidden");
                }
            })
            .on("dnd_stop.vakata", function (event, data) {
                // "Cancel" dnd
                var $targetElt = $(data.event.target);
                if (!$targetElt.closest(".drop").length) {
                    that.resetDndData();
                }
            });
}

ProjectsView.prototype = Object.create(TreeViewContainer.prototype);
ProjectsView.prototype.constructor = ProjectsView;

ProjectsView.prototype.addJstreeNodeType = function (classname) {
	var nodeType = StringUtils.replaceDotByMinus(classname);

	// Add the node type and specify the icon for these nodes
	this.tree.jstree().settings.types[nodeType] = {
		icon: nodeType
	}

	return nodeType;
};

ProjectsView.prototype.callServiceDblkAction = function (qname) {
    var that = this;
    $.ajax({
        dataType: "xml",
        url: Convertigo.createServiceUrl("studio.database_objects.CallDblkAction"),
        data: {
            qname: qname
        },
        success: function (data, textStatus, jqXHR) {
            var $responses = $(data).find("admin");
            $responses.find(">*").each(function () {
                if ($(this).attr("state") === "success") {
                    ResponseActionManager.handleResponse(
                        $(this).attr("name"),
                        $(data),
                        that,
                        false
                    );
                }
                // Show error
                else {
                    var $response = $responses.find("admin>*>*").first();
                    ModalUtils.createMessageBox(
                        $response.find("title").text(),
                        $response.find("message").text()
                    );
                }
            });
        }
    });
};

ProjectsView.prototype.callServiceCallAction = function (qnames, classAction, response) {
	var that = this;
	$.ajax({
	    dataType: "xml",
		url: Convertigo.createServiceUrl("studio.database_objects.CallAction"),
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
					ModalUtils.createMessageBox(
						$response.find("title").text(),
						$response.find("message").text()
					);
				}
			});
		}
	});
};

ProjectsView.prototype.createContextMenu = function (parent, $menu) {
	var that = this;
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
				iconClass += " grayscale";
			}

			(function () {
				var localLabel = label;

				// Get the action to call
				var classAction = $(children[i]).attr("class");
				parent[label.replace(/\s/g, "")] = {
					label: label,
					icon: iconClass,
					action: function (node) {
						var lastDotIndex = classAction.lastIndexOf(".");
						var simpleClassAction = classAction.substr(lastDotIndex + 1);
						// If it is a "Create action", show the Palette
						if (simpleClassAction.startsWith("Create")) {
							for (var key in that.palettes) {
								that.palettes[key].showCategory(localLabel);
							}
						}
						else {
							// Get the qnames
							var selectedNodes = that.tree.jstree().get_selected(true);
							var qnames = [];
							for (var i = 0; i < selectedNodes.length; ++i) {
								var node = selectedNodes[i];
								if (VariableUtils.isDefined(node.data.qname)) {
									qnames.push(node.data.qname);
								}
							}

							that.callServiceCallAction(qnames, classAction, null);
						}
					},
					_disabled: isDisabled,
					// menubarPath = category
					separator_after: 
						isValidIndex &&
						// The two entries belongs to the same category
						$(children[i]).attr("menubarPath") !== $(children[indexNextNode]).attr("menubarPath")
				}
			}());
		}
	}
};

ProjectsView.prototype.createChildNodes = function ($dbo, node) {
	var that = this;
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
						parentCategoryFound ? "Inherited" + categoryName : categoryName,
						$(this).attr("qname")
					);
					createCategory = true;
				}
				// Do not create category in those cases
				else if (categoryName == "MobileApplication") {
				}
				else if (categoryName == "UrlMapper") {
				}
				// If category not found, we create it
				else if (!parentCategoryFound) {
					var categoryNode = that.createNodeJsonFolder(
				        newCategoryName,
				        categoryName,
				        $(this).attr("qname")
					);
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

ProjectsView.prototype.computeCategoryName = function (category) {
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

	var newName = category.substring(0, 1).toUpperCase() + category.substring(1);	
	newName += category.substring(category.length - 1) == "s" ? "es" : "s";

	return newName;
};

ProjectsView.prototype.createNodeJsonDbo = function (dboElt) {
	var qname = $(dboElt).attr("qname");
	var comments = this.computeComment($(dboElt).attr("comment"));
	var enabled = this.computeEnabled($(dboElt).attr("isEnabled"));
	var inherited = this.computeInherited($(dboElt).attr("isInherited"));

	var nodeId = this.computeNodeId(qname);
	var liClasses = [
	        enabled.liClass,
	        inherited.liClass,
	        // Used for drag and drop from the Palette to the projects tree view
	        "drop"
	    ]
	    .join(" ");

	var nodeJsonDbo = {
		id: this.tree.jstree().generateId(nodeId),
		text: $(dboElt).attr("name"),
		// Add a new type for icons
		type: this.addJstreeNodeType($(dboElt).attr("icon")),
		li_attr: {
			"class": liClasses
		},
		children: $(dboElt).attr("hasChildren") === "true",
		data: {
			qname: qname,
			priority: $(dboElt).attr("priority"),
			comment: comments.comment,
			restOfComment: comments.restOfComment
		}
	};

	if (VariableUtils.isDefined(enabled.isEnabled)) {
		nodeJsonDbo.data.isEnabled = enabled.isEnabled;
	}

	return nodeJsonDbo;
};

ProjectsView.prototype.createNodeJsonFolder = function (text, categoryName, qname) {
    var nodeId = "fd-" + StringUtils.replaceDotByMinus(qname) + "-" + categoryName;
    return {
        id: this.tree.jstree().generateId(nodeId),
        text: text,
        children: [],
        li_attr: {
            "class": "drop"
        },
        data: {
            folderType: categoryName
        }
    };
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
		// The first line
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

	if (VariableUtils.isDefined(isEnabled)) {
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

	if (VariableUtils.isDefined(isInherited) && isInherited === "true") {
		inherited.liClass = "nodeIsInherited";
	}

	return inherited;
};

ProjectsView.prototype.computeNodeId = function (qname) {
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
	var that = this;

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

ProjectsView.prototype.resetDndData = function () {
	this.dnd = {
        palette: {
            lastTargetNodeId: null,
            started: false,
            // New node to show at the end of the refresh
            newIdNodeCreated: null,
            // Opened nodes to re-open at the end of the refresh
            openedNodes: null,
            markerBetweenNodes: null,
            canCreate: false 
        },
        sourcepicker: {
            lastTargetNodeId: null,
            started: false,
            canCreate: false
        }
	};
};

ProjectsView.prototype.isNodeFolder = function (node) {
	return node.type === "default";
};

ProjectsView.prototype.getProjectNode = function (node, asDom = false) {
    var currentNode = this.tree.jstree().get_node(node);
    return currentNode !== false ?
        // Return project node
        this.tree.jstree().get_node(
            currentNode.parent === "#" ?
            currentNode :
            currentNode.parents[currentNode.parents.length - 2],
            asDom
        ) :
        // No node
        null;
};
