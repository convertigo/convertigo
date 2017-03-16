var PropertiesView = {
	// Variables
	tree: null,
	
	// Functions
	init: function (jstreeId) {
		PropertiesView.tree = $(jstreeId);
		
		PropertiesView.tree
			.jstree({
				core: {
					check_callback: true,
					force_text: true,
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
					"sort"
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
			.on("loaded_grid.jstree", function (event, data) {
				/* 
				* If on the same page we have several jstrees using the jstree grid plugin and that one of them
				* does not show headers, the headers of all others jstrees will not be displayed. So we have to
				* add this special CSS class property.
				*/
				PropertiesView.tree
					.parents(".jstree-grid-wrapper")
					.find(".jstree-grid-header-regular")
					.addClass("jstree-header");
			});
	},
	createNodeJsonPropertyCategory: function (textNode) {
		return {
			text: textNode,
			state: {
				opened: true // Expand the node by default
			},
			children: [] // Properties - Values
		};
	},
	removeTreeData: function () {
		PropertiesView.updateTreeData([]);
	},
	update: function ($dboElt) {
		// Different categories (Base properties, Expert, etc.)
		var propertyCategories = {};
		var isExtractionRule = $dboElt.attr("isExtractionRule") == "true";

		// Add property to the right category
		$dboElt.find("property[isHidden!=true]").each(function () {
			var key = $(this).attr("isExpert");
			// Create the category if it does not exist yet
			if (!propertyCategories[key]) {
				propertyCategories[key] = key == "true" ?
					    PropertiesView.createNodeJsonPropertyCategory(isExtractionRule ? "Selection" : "Expert") :
					    PropertiesView.createNodeJsonPropertyCategory(isExtractionRule ? "Configuration" : "Base properties");
			}
			
			// Add the property to the category
			propertyCategories[key].children.push({
				text: $(this).attr("displayName"),
				data: {
					value: StringUtils.escapeHTML($(this).find("[value]").attr("value"))
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
		var informationCategory = PropertiesView.createNodeJsonPropertyCategory("Information");
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
		
		// Update the property view with the new data
		PropertiesView.updateTreeData(propertyViewTreeNodes);
	},
	updateTreeData: function (data) {
		PropertiesView.tree.jstree().settings.core.data = data;
		PropertiesView.tree.jstree().refresh(true);
	}
};