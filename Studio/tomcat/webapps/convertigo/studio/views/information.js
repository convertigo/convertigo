var PropertiesView = {
	// Variables
	refNodeProjectsView: null,
	tree: null,
	
	// Functions
	init: function (jstreeId) {
		$(PropertiesView).on("set_property.database-object-manager", function (event, qname, property, value, data) {
			var idNode = PropertiesView.tree.jstree().getIdNodes("pr-" + property.replace(/\s/g, "-"))[0];
	    	var node = PropertiesView.tree.jstree().get_node(idNode);
	    	node.data.value = StringUtils.escapeHTML(value.toString());
	    	PropertiesView.tree.jstree().redraw_node(node.id);
		});
		
		PropertiesView.tree = $(jstreeId);
		
		PropertiesView.tree
			.jstree({
				core: {
					check_callback: true,
					force_text: true,
					animation : 0,
					themes: {
						name: "default-dark",
						dots: false,
						icons: false
					},
					data: function (node, cb) {
						/*
						 * 'Hack CSS' part 1: we create an empty node so that jstree-grid will generate
						 * the CSS to place the rows of the Value column at the right position.
						 * If we don't create this empty node, the CSS will generate with 
						 * 'line-weight: nullpx' and 'height: nullpx' so the position of the rows
						 * will be invalid. Then we will delete this node when the tree is loaded.
						 */ 
						cb.call(this, [{
						    text: ""
						}]);
					}
				},
				plugins: [
					"grid",
					"sort",
					"utils"
				],
				grid: {
				    columns: [{
				    	header: "Property"
				    }, {
				    	header: "Value",
				    	width: "100%",
				    	value: "value"
				    }], // Property - Value
				    resizable: true
				}
			})
			.one("loaded.jstree", function (event, data) {				
				// 'Hack CSS' part 2: Delete the useless empty node which allowed to generate the right CSS
				PropertiesView.removeTreeData();
			})
			.one("loaded_grid.jstree", function (event, data) {
				/* 
				* If on the same page we have several jstrees using the jstree grid plugin and that one of them
				* does not show headers, the headers of all others jstrees will not be displayed. So we have to
				* add this special CSS class property.
				*/
				PropertiesView.tree
					.parents(".jstree-grid-wrapper")
					.find(".jstree-grid-header-regular")
					.addClass("jstree-header");
			})
			.on("select_cell.jstree-grid", function (event, data) {
				var node = PropertiesView.tree.jstree().get_node(data.node[0].id);
				var parent = PropertiesView.tree.jstree().get_node(node.parent);
				/* 
				 * Check if the node has a value : if it does not have a value, it is a folder.
				 * We also need to check if the property is editable by checking the parent category.
				 * Information properties are not editable.
			     */
				if (typeof node.data.value !== "undefined" && parent.data.isEditable) {
					var editComment = StringUtils.unescapeHTML(node.data.value);
					PropertiesView.editCell(node, {
					    value: data.sourceName
					}, data.grid, editComment);
				}
				
		        event.preventDefault();
			})
			.on("update_cell.jstree-grid", function (event, data) {
				DatabaseObjectManager.setProperty(PropertiesView.refNodeProjectsView.data.qname, data.node.data.name, data.value);
			});
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
		var rtl = PropertiesView.tree.jstree()._data.core.rtl,
		    w = PropertiesView.tree.jstree().element.width(),
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
		            "height": (PropertiesView.tree.jstree()._data.core.li_height) + "px",
		            "lineHeight": (PropertiesView.tree.jstree()._data.core.li_height) + "px",
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
		                obj.data[col.value] = v.length ? StringUtils.escapeHTML(v) : v;
		                PropertiesView.tree.jstree().element.trigger('update_cell.jstree-grid', {
		                    node: obj,
		                    col: col.value,
		                    value: v,
		                    old: t
		                });
		                PropertiesView.tree.jstree()._prepare_grid(this.get_node(obj, true));
		            }
		            h2.remove();
		            element.show();
		        }, PropertiesView.tree.jstree()),
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
	createNodeJsonPropertyCategory: function (textNode, editable) {
		return {
			text: textNode,
			state: {
				opened: true // Expand the node by default
			},
			data: {
				isEditable: editable
			},
			children: [] // Properties - Values
		};
	},
	removeTreeData: function () {
		PropertiesView.updateTreeData([]);
	},
	refresh: function (refNodeProjectsView) {
		if (refNodeProjectsView.type !== "default") {
			// Get properties of the object
			$.ajax({
				url: ProjectsView.createConvertigoServiceUrl("studio.database_objects.Get"),
				data: {
					qname: refNodeProjectsView.data.qname
				},
				success: function (data, textStatus, jqXHR) {
					PropertiesView.refNodeProjectsView = refNodeProjectsView;
					PropertiesView.removeTreeData();
					PropertiesView.updateProperties($(data).find("admin>*").first());
				}
			});
		}
		else {
			PropertiesView.removeTreeData();
		}
	},
	updateProperties: function ($dboElt) {	
		// Different categories (Base properties, Expert, etc.)
		var propertyCategories = {};
		var isExtractionRule = $dboElt.attr("isExtractionRule") == "true";

		// Add property to the right category
		$dboElt.find("property[isHidden!=true]").each(function () {
			var key = $(this).attr("isExpert");
			// Create the category if it does not exist yet
			if (!propertyCategories[key]) {
				propertyCategories[key] = key == "true" ?
					    PropertiesView.createNodeJsonPropertyCategory(isExtractionRule ? "Selection" : "Expert", true) :
					    PropertiesView.createNodeJsonPropertyCategory(isExtractionRule ? "Configuration" : "Base properties", true);
			}
			
			var propertyName = $(this).attr("name");
			// Add the property to the category
			propertyCategories[key].children.push({
				id: PropertiesView.tree.jstree().generateId("pr-" + propertyName.replace(/\s/g, "-")),
				text: $(this).attr("displayName"),
				data: {
					value: StringUtils.escapeHTML($(this).find("[value]").attr("value")),
					name: propertyName
				}
			});
		});

		var propertyViewTreeNodes = [];
		// Add the categories if they have properties
		for (var key in propertyCategories) {
			// Do they have properties ?
			if (propertyCategories[key].children.length) {
				propertyViewTreeNodes.push(propertyCategories[key]);
			}
		}
		
		// Create information category
		var informationCategory = PropertiesView.createNodeJsonPropertyCategory("Information", false);
		informationCategory.children.push({
			text: "Depth",
			data: {
				value: StringUtils.escapeHTML($dboElt.attr("depth"))
			}
		});
		informationCategory.children.push({
			text: "Exported",
			data: {
				value: StringUtils.escapeHTML($dboElt.attr("exported"))
			}
		});
		informationCategory.children.push({
			text: "Java class",
			data: {
				value: StringUtils.escapeHTML($dboElt.attr("java_class"))
			}
		});
		informationCategory.children.push({
			text: "Name",
			data: {
				value: StringUtils.escapeHTML($dboElt.find("property[name=name]").first().find("[value]").attr("value"))
			}
		});
		informationCategory.children.push({
			text: "Priority",
			data: {
				value: StringUtils.escapeHTML($dboElt.attr("priority"))
			}
		});
		informationCategory.children.push({
			text: "QName",
			data: {
				value: StringUtils.escapeHTML($dboElt.attr("qname"))
			}
		});
		informationCategory.children.push({
			text: "Type",
			data: {
				value: StringUtils.escapeHTML($dboElt.attr("displayName"))
			}
		});
		propertyViewTreeNodes.push(informationCategory);
		
		// Update the properties view with the new data
		PropertiesView.updateTreeData(propertyViewTreeNodes);
	},
	updateTreeData: function (data) {		
		PropertiesView.tree.jstree().settings.core.data = data;
		PropertiesView.tree.jstree().refresh(true);
	}
};