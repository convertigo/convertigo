function PropertiesView(jstreeTheme = "default") {
    TreeViewContainer.call(this, "propertiesTreeView");
    this.refNodeProjectsView = null;
    
    var that = this;
    
    $(that).on("database_object_delete.dbo-manager", function (event, qnamesDbosToDelete) {
        if (qnamesDbosToDelete.length) {
            that.removeTreeData();
        }
    });
    
    $(that).on("set_property.dbo-manager", function (event, qnames, property, value, data) {
        var idNode = that.tree.jstree().getIdNodes("pr-" + property.replace(/\s/g, "-"))[0];
        var node = that.tree.jstree().get_node(idNode);
        
        var $nodeData = $(data).find(">*[qname='" + that.refNodeProjectsView.data.qname + "']").children();
        var newValue = StringUtils.escapeHTML($nodeData.find("[value]").attr("value").toString());
        
        node.data.value = newValue;
        that.tree.jstree().redraw_node(node.id);
    });
    
    that.tree
        .jstree({
            core: {
                check_callback: true,
                force_text: true,
                animation : 0,
                themes: {
                    name: jstreeTheme,
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
            that.removeTreeData();
        })
        .one("loaded_grid.jstree", function (event, data) {
            /* 
            * If on the same page we have several jstrees using the jstree grid plugin and that one of them
            * does not show headers, the headers of all others jstrees will not be displayed. So we have to
            * add this special CSS class property.
            */
            that.tree
                .parents(".jstree-grid-wrapper")
                .find(".jstree-grid-header-regular")
                .addClass("jstree-header");
        })
        .on("select_cell.jstree-grid", function (event, data) {
            var node = that.tree.jstree().get_node(data.node[0].id);
            var parent = that.tree.jstree().get_node(node.parent);
            /* 
             * Check if the node has a value : if it does not have a value, it is a folder.
             * We also need to check if the property is editable by checking the parent category.
             * Information properties are not editable.
             */
            if (typeof node.data.value !== "undefined" && parent.data.isEditable) {
                var editComment = StringUtils.unescapeHTML(node.data.value);
                that.editCell(node, {
                    value: data.sourceName
                }, data.grid, editComment);
            }
            
            event.preventDefault();
        })
        .on("update_cell.jstree-grid", function (event, data) {
            DatabaseObjectManager.setProperty([that.refNodeProjectsView.data.qname], data.node.data.name, data.value);
        });
}

PropertiesView.prototype = Object.create(TreeViewContainer.prototype);
PropertiesView.prototype.constructor = PropertiesView;

/*
 * Function highly inspired from the _edit(...) function of jstree-grid plugin.
 * 
 * NOTE : If you use another version of the jstree-grid plugin, you migh have
 *        to update this function as it uses functions defined in the jstree-grid
 *        plugin.
*/
PropertiesView.prototype.editCell = function (obj, col, element, editText) {
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
                    obj.data[col.value] = v.length ? StringUtils.escapeHTML(v) : v;
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

PropertiesView.prototype.createNodeJsonPropertyCategory = function (textNode, editable) {
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
};

PropertiesView.prototype.removeTreeData = function () {
    this.updateTreeData([]);
};

PropertiesView.prototype.refresh = function (refNodeProjectsView) {
    var that = this;
    if (refNodeProjectsView.type !== "default") {
        // Get properties of the object
        $.ajax({
            url: Convertigo.createServiceUrl("studio.database_objects.Get"),
            data: {
                qname: refNodeProjectsView.data.qname
            },
            success: function (data, textStatus, jqXHR) {
                that.refNodeProjectsView = refNodeProjectsView;
                that.removeTreeData();
                that.updateProperties($(data).find("admin>*").first());
            }
        });
    }
    else {
        that.removeTreeData();
    }
};


PropertiesView.prototype.updateProperties = function ($dboElt) {  
    var that = this;

    // Different categories (Base properties, Expert, etc.)
    var propertyCategories = {};
    var isExtractionRule = $dboElt.attr("isExtractionRule") == "true";

    // Add property to the right category
    $dboElt.find("property[isHidden!=true]").each(function () {
        var key = $(this).attr("isExpert");
        // Create the category if it does not exist yet
        if (!propertyCategories[key]) {
            propertyCategories[key] = key == "true" ?
                that.createNodeJsonPropertyCategory(isExtractionRule ? "Selection" : "Expert", true) :
                that.createNodeJsonPropertyCategory(isExtractionRule ? "Configuration" : "Base properties", true);
        }
        
        var propertyName = $(this).attr("name");
        // Add the property to the category
        propertyCategories[key].children.push({
            id: that.tree.jstree().generateId("pr-" + propertyName.replace(/\s/g, "-")),
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
    var informationCategory = that.createNodeJsonPropertyCategory("Information", false);
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
    that.updateTreeData(propertyViewTreeNodes);
};

PropertiesView.prototype.updateTreeData = function (data) {       
    this.tree.jstree().settings.core.data = data;
    this.tree.jstree().refresh(true);
};
