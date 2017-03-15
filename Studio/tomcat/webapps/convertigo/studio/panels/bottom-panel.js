// Property View
var PV = {
	// Variables
	tree: null,
	
	// Functions
	init: function (jstreeId) {
		PV.tree = $(jstreeId);
		
		PV.tree
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
			.one("loaded.jstree", function () {				
				// 'Hack CSS' part 2: Delete the useless empty node which allowed to generate the right CSS
				PV.removeTreeData();
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
		PV.updateTreeData([]);
	},
	update: function ($dboElt) {
		var isExtractionRule = $dboElt.attr("isExtractionRule") == "true";
		
		// Create the different categories (Base properties, Expert, ...)
		var propertyCategories = {};
		propertyCategories["false"] = PV.createNodeJsonPropertyCategory(isExtractionRule ? "Configuration" : "Base properties");
		propertyCategories["true"] = PV.createNodeJsonPropertyCategory(isExtractionRule ? "Selection" : "Expert");

		// Add property to the right category
		$dboElt.find("property[isHidden!=true]").each(function () {
			var key = $(this).attr("isExpert");
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
		var informationCategory = PV.createNodeJsonPropertyCategory("Information");
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
		PV.updateTreeData(propertyViewTreeNodes);
	},
	updateTreeData: function (data) {
		PV.tree.jstree().settings.core.data = data;
		PV.tree.jstree().refresh(true);
	}
};