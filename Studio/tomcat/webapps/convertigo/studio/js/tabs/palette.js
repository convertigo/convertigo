function Palette(id) {
	++Palette.nbInstances;
	Tab.call(this, "palette" + Palette.nbInstances, "Palette");

	// Div that contains the categories (or the sub categories)
	this.divContainer = $("<div/>");
	
	// Visual separator bewteen the sub categories and the description
	this.separator = $("<hr/>");
	
	// Div for the description of the selected element
	this.divDescription = $("<div/>");
	
	// Data received from the service GetNewBeans
	this.data = null;

	this.currentCategory = null;
	
	/*
	 * this.categoriesToSubCategoriesXml = {}
	 * Map to keep the sub categories of each category
	 */
	this.emptyCategoriesToSubCategoriesXml();
	
	$(this.mainDiv).attr("class", "tab-palette");
	$(this.mainDiv).append(this.divContainer);
}

Palette.prototype = Object.create(Tab.prototype);
Palette.prototype.constructor = Palette;

// Used to generate the div id
Palette.nbInstances = 0;

Palette.prototype.update = function (data) {
	//Object.getPrototypeOf(this.constructor.prototype).update.call(this);
	
	var that = this;
	
	that.currentCategory = null;

	// Keep data for the "back" button when we go from the sub categories to the categories page
	that.data = data;
	that.emptyCategoriesToSubCategoriesXml();
	
	// Remove all contents
	$(that.divContainer).empty();
	$(that.divDescription).empty();
	$(that.separator).remove();
	$(that.divDescription).remove();
	
	var $categories = $(data).find(">category");
	
	// Title of the categories page
	if ($categories.length) {
		$(that.divContainer).append($("<h2/>", {
			text: "Please select a category."
		}));
	}

	// Create all div categories
	$categories.each(function () {
		// Keep the sub categories of the current category
		var categoryName = $(this).attr("name");
		var id = $(this).attr("id");

		that.categoriesToSubCategoriesXml[categoryName] = $(this).children();
		
		// Divs icon + text
		var $divCategoryIcon = $("<i/>", {
			"class": $(this).attr("icon")
		});
		var $divCategoryName = $("<span/>", {
			text: $(this).attr("name")
		});
		
		// Create the div category
		var $divCategory = $("<div/>", {
		        "class": "category",
		        click: function () {
		        	that.currentCategory =  id;
		            that.createSubCategories(categoryName);
		        }
		    })
		    .append($divCategoryIcon)
		    .append($divCategoryName);
		
		// Add the current category
		$(that.divContainer).append($divCategory);
	});
};

Palette.prototype.getCurrentCategory = function () {
	return this.currentCategory;
};

Palette.prototype.emptyCategoriesToSubCategoriesXml = function () {
	this.categoriesToSubCategoriesXml = {};
};

Palette.prototype.createCategories = function () {
	this.update(this.data);
};

Palette.prototype.showCategory = function (categoryName) {
	// Recreate categories
	this.createCategories();
	this.getDiv().find("div:contains('" + categoryName + "')").click();
	this.focus();
};

Palette.prototype.createSubCategories = function (categoryName) {
	var that = this;

	// Remove div categories
	$(that.divContainer).empty();

	// Create back button
	var $backBtn = $("<button/>", {
		type: "button",
		text: "Back",
		click: function () {
			// Recreate categories
			that.createCategories();
		}
	});
	
	// Add the separator and the description
	$(that.mainDiv)
		.append(that.separator)
		.append(that.divDescription);
	
	$(that.divContainer)
	 	// Instructions
		.append($("<h2/>", {
			text: "New " + categoryName
		}))
		.append($("<h4/>", {
			text: "Drag and drop an object in your project tree view..."
		}))
		// Back button
		.append($backBtn)
		.append($("<br/>"))
		.append($("<br/>"));

	// Get the sub categories of the current category
	var $subCategoriesXml = that.categoriesToSubCategoriesXml[categoryName];
	var $defaultBeanToSelect = null;
	var lastSelectedBean = null;
	
	// Create sub categories
	$subCategoriesXml.each(function () {
		// Div of the sub category
		var $divSubCategory = $("<div/>", {
			"class": "sub-category"
		});
		
		// Accordion button of the sub category
		$(that.divContainer).append($("<button/>", {
			type: "button",
			"class": "accordion active",
			text: $(this).attr("name")
		}));
		
		// Create beans of each sub category
		$(this).children().each(function (index) {				
			var bean = this;
			
			// Divs icon + text
			var $divBeanIcon = $("<i/>", {
				"class": $(this).attr("icon")
			});
			var $divBeanText = $("<span/>", {
				text: $(this).attr("displayName")
			});
			
			// Create the div bean
			var $divBean = $("<div/>", {
					"data-beanclass": $(this).attr("classname"),
			        click: function() {
			        	// Update only if we select a different bean
			        	if (lastSelectedBean !== this) {
				        	// Disable selection of the last selected bean
				            $(lastSelectedBean).removeClass("gray-selection");

				            // Now the current bean is the one which is selected
				            lastSelectedBean = this;
				            $(this).addClass("gray-selection");
				            
				            // Update its description
				            $(that.divDescription).html($(bean).attr("description"));
			        	}
			        }
			    })
			    .append($divBeanIcon)
			    .append($divBeanText);
			
			// Add the bean to the sub category
			$divSubCategory.append($divBean);
			
			if ($(this).attr("selectedByDefault") === "true") {
				$defaultBeanToSelect = $divBean;
			}
		});

		$(that.divContainer).append($divSubCategory);
	});

	// "Accordionify" the sub categories
	accordionify();
	
	// Trigger click event on the default bean
	$defaultBeanToSelect.click();
};
