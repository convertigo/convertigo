function SourcePicker(id) {
	++SourcePicker.nbInstances;
	Tab.call(this, "sourcePicker" + SourcePicker.nbInstances, "Source Picker");

    var $top = $("<div/>");

    var $mid = $("<div/>");
    var $bottom = $("<div/>");

    $top.css("height", "500px");
    $mid.css("height", "500px");
    $bottom.css("height", "500px");

//    $top.css("display", "inline-block");
//    $mid.css("display", "inline-block");
//    $bottom.css("display", "inline-block");
    
    $(this.mainDiv)
        .append($top)
        .append($("<hr>"))
        .append($mid)
        .append($("<hr>"))
        .append($bottom);

    /*
     * TOP
     */
    // Infos
    var $infos = $("<div/>", {
        "class": "infos"
    });
    $top.append($infos);

    // Tag
    var $tagContainer = $('<div></div>');
    this.tagInput = $("<input/>", {
        type: "text",
        disabled: "disabled"
    });
    $tagContainer
        .append($("<label>Tag :</label>"))
        .append(this.tagInput);
    $infos.append($tagContainer);

    // Type
    var $typeContainer = $('<div></div>');
    this.typeInput = $("<input/>", {
        type: "text",
        disabled: "disabled"
    });
    $typeContainer
        .append($("<label>Type :</label>"))
        .append(this.typeInput);
    $infos.append($typeContainer);

    // Name
    var $nameContainer = $('<div></div>');
    this.nameInput = $("<input/>", {
        type: "text",
        disabled: "disabled"
    });
    $nameContainer
        .append($("<label>Name :</label>"))
        .append(this.nameInput);
    $infos.append($nameContainer);

    // Comment
    var $commentContainer = $('<div></div>');
    this.commentInput = $("<textarea/>", {
        disabled: "disabled"
    });
    this.commentInput.css("vertical-align", "middle");
    $commentContainer
        .append($("<label>Comment :</label>"))
        .append(this.commentInput);
    $infos.append($commentContainer);

    var $buttonsContainer = $("<div/>");
    
    
    this.showBtn = $("<button/>", {
        type: "button",
        text: "Show step\'s source",
        disabled: "disabled"
    });
    this.showBtn.css("margin-right", "8px");
    this.removeBtn = $("<button/>", {
        type: "button",
        text: "Remove source",
        disabled: "disabled"
    });
    $buttonsContainer
        .append($("<br>"))
        .append(this.showBtn )
        .append(this.removeBtn);

    $infos.append($buttonsContainer);    

    // Help
    var $help = $("<div/>", {
        html: "Note:<br>Drag items to a step<br>in the Projects view<br>to link the source.",
        "class": "help"
    });
    $top.append($help);

    var that = this;
    this.triggerSelectNodeEvent = false;
    /*
     * MID
     */
    this.domTree = $("<div/>");
    this.domTree.jstree({
        core: {
            themes: {
                dots: false
            },
            check_callback: true,
            force_text: true,
            animation: 0
//            data: [{
//                text: 'Simple root node',
//                type: "default"
//            }, {
//                text: 'Root node 2',
//                type: "attrib",
//                state: {
//                    opened: true,
//                    selected: true
//                },
//                children: [{
//                    text: "",
//                    type: "text"
//                }]
//            }]
        },
        plugins: [
            "state",
            "types"
        ],
        types: {
            "default": {
                icon: Convertigo.getBaseConvertigoStudioUrl("img/node.png")
            },
            attrib: {
                icon: Convertigo.getBaseConvertigoStudioUrl("img/attrib.png")
            },
            text: {
                icon: Convertigo.getBaseConvertigoStudioUrl("img/text.png")
            }
        }
    })
    .on("select_node.jstree", function (event, data) {
        if (data.node.data.isRealNode && that.triggerSelectNodeEvent) {
            $.ajax({
                dataType: "xml",
                url: Convertigo.createServiceUrl("studio.sourcepicker.GetXpathTree"),
                data: {
                    nodeId: data.node.id
                },
                success: function (data, textStatus, jqXHR) {
                    that.removeTree(that.leftTree);
                    that.addNodeInDomTree(that.leftTree, $(data).find("xpath_tree>"));
                    that.setXpathText($(data).find("xpath").text());
                    that.xpath.highlightTextarea("destroy")
                    that.xpath.highlightTextarea({
                        words: [
                            "^" + $(data).find("anchor").text()
                        ]
                    });
                }
            });
        }
    });
    $mid.append(this.domTree);

    /*
     * BOTTOM
     */
    var $xpathBtns = $("<div/>");
    var $evaluateXpathBtn = $("<button/>", {
        text: "Evaluate xPath",
        type: "button",
        click: function () {
            console.log("Evaluate");
        }
    });
    $xpathBtns.append($evaluateXpathBtn);
    $bottom.append($xpathBtns);

    var $containerLabelXpath = $("<div/>");
    var $label = $("<label/>", {
        "for": "xpath-txtarea",
        text: "xPath"
    });
    this.xpath = $("<textarea/>", {
        id: "xpath-txtarea",
        "class": "xpath",
        rows: 4
    });
    $containerLabelXpath
        .append($label)
        .append(this.xpath);    

    $bottom.append($containerLabelXpath);

    var $result = $("<div/>", {
        id: "result"
    });
    this.leftTree = $("<div/>", {
        id: "left"
    });
    this.leftTree.jstree({
        core: {
            themes: {
                dots: false
            },
            check_callback: true,
            force_text: true,
            animation: 0
        },
        plugins: [
            "state",
            "types"
        ],
        types: {
            "default": {
                icon: Convertigo.getBaseConvertigoStudioUrl("img/node.png")
            },
            attrib: {
                icon: Convertigo.getBaseConvertigoStudioUrl("img/attrib.png")
            },
            text: {
                icon: Convertigo.getBaseConvertigoStudioUrl("img/text.png")
            }
        }
    });
    
    var $rightRender = $('<div id="right"><textarea>TEST<textarea/></div>');
    $result
        .append(this.leftTree)
        .append($rightRender);
    $bottom.append($result);
}

SourcePicker.prototype = Object.create(Tab.prototype);
SourcePicker.prototype.constructor = SourcePicker;

// Used to generate the div id
SourcePicker.nbInstances = 0;

SourcePicker.prototype.update = function (data) {
};

SourcePicker.prototype.fillHelpContent = function (tag, type, name, comment, textBtn, enableBtn) {
    this.tagInput.val(tag);
    this.typeInput.val(type);
    this.nameInput.val(name);
    this.commentInput.val(comment);
    this.showBtn.text(textBtn);
    if (enableBtn) {
        this.showBtn.removeAttr("disabled");
        this.removeBtn.removeAttr("disabled");
    }
    else {
        this.showBtn.attr("disabled", "disabled");
        this.removeBtn.attr("disabled", "disabled");
    }
};

SourcePicker.prototype.fillDomTree = function ($domTree) {
    this.addNodeInDomTree(this.domTree, $domTree.find(">"));
    
    this.triggerSelectNodeEvent = false;

    var childNodes = this.domTree.jstree().get_node("#").children_d;

    // Select the two first nodes
    this.domTree.jstree().select_node(childNodes[0]);
    this.domTree.jstree().select_node(childNodes[1]);

    this.triggerSelectNodeEvent = true;

    // ... and expand the 1st one
    this.domTree.jstree().open_node(childNodes[0]);
};

SourcePicker.prototype.removeTree = function (tree) {
    var rootNode = tree.jstree().get_node("#").children[0];
    tree.jstree().delete_node(rootNode);
};

SourcePicker.prototype.removeAll = function () {
    this.removeTree(this.domTree);
};

SourcePicker.prototype.addNodeInDomTree = function (tree, $node, parentId = "#") {
    var that = this;

    var newNode = {
        id: $node.attr("id"),
        text: $node.attr("text"),
        type: that.getNodeType($node.attr("type")),
        state: {
            opened: parentId === "#"
        },
        data: {
            isRealNode: $node.attr("is_real") === "true"
        }
    };

    var newId = tree.jstree().create_node(parentId, newNode);

    $node.children().each(function () {
        that.addNodeInDomTree(tree, $(this), newId);
    });
};

SourcePicker.prototype.getNodeType = function (nodeType) {
    if (nodeType === "2") {
        return "attrib";
    }

    if (nodeType === "3") {
        return "text";
    }

    return "default";
};

SourcePicker.prototype.setXpathText = function (xpath) {
    this.xpath.text(xpath);
};
