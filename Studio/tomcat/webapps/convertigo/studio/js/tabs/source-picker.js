function SourcePicker(projectsView, jstreeTheme = "default") {
	++SourcePicker.nbInstances;
	Tab.call(this, "sourcepicker" + SourcePicker.nbInstances.toString(), "Source Picker");

    this.triggerSelectNodeEvent = false;
	this.inputTags = {};
	this.jstreeTheme = jstreeTheme;

	this.createTopContent();
	this.createMidContent();
	this.createBottomContent();

	var that = this;
    $(document)
        .on("mousedown touchstart", ".dom-tree a", function (event) {
            projectsView.dnd.sourcepicker.started = true;
            projectsView.dnd.sourcepicker.lastTargetNodeId = null;

            that.domTree.jstree().deselect_all();
            that.domTree.jstree().select_node(event.target);

            // Create the floating div
            return $.vakata.dnd.start(
                event, {
                    jstree: true,
                    obj: $(this),
                    transferData: "sourcepicker",
                    nodes: [{
                       id: "dummy-id"
                    }]
                },
                '<div id="dnd-node-dom-tree">' +
                    // Allow status
                    '<i class="draggable allow-status forbidden"></i>' +
                "</div>"
            );
        });
}

SourcePicker.prototype = Object.create(Tab.prototype);
SourcePicker.prototype.constructor = SourcePicker;

// Used to generate the div id
SourcePicker.nbInstances = 0;

SourcePicker.prototype.update = function (data) {
};

SourcePicker.prototype.createTopContent = function () {
    var $top = $("<div/>", {
        "class": "infos-help-container"
    });

    // Infos
    var $infos = $("<div/>", {
        "class": "infos"
    });
    $top.append($infos);

    var that = this;
    var createLabelInput = function (labelValue, isTextArea = false) {
        // Label + Input container
        var $labelInputContainer = $("<div/>", {
            "class": "label-input-container"
        });

        // Label
        var $labelTag = $("<label/>", {
            text: labelValue + " :",
            title: labelValue
        });

        // Input
        var $inputTag = !isTextArea ?
                $("<input/>", {
                    type: "text"
                }) :
                $("<textarea/>");
        $inputTag.attr("disabled", "disabled");

        // Store it to update help content
        that.inputTags[labelValue] = $inputTag;

        $labelInputContainer
            .append($labelTag)
            .append($inputTag);
        return $labelInputContainer;
    };

    // Tag
    $infos.append(createLabelInput("Tag"));

    // Type
    $infos.append(createLabelInput("Type"));

    // Name
    $infos.append(createLabelInput("Name"));

    // Comment
    $infos.append(createLabelInput("Comment", true));

    var $buttonsContainer = $("<div/>", {
        "class": "buttons-container"
    });

    var createBtn = function (text, func) {
        var $btn = $("<button/>", {
            type: "button",
            text: text,
            title: text,
            click: func
        });
        $btn.attr("disabled", "disabled");

        return $btn;
    }

    this.showBtn = createBtn(
        "Show step's source",
        function () {
        }
    );

    this.removeBtn = createBtn(
        "Remove source",
        function () {
        }
    );

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
    $(this.mainDiv).append($top)
};

SourcePicker.prototype.createMidContent = function () {
    var $mid = $("<div/>");

    var that = this;

    // Dom tree
    this.domTree = $("<div/>", {
        "class": "dom-tree"
    });
    this.domTree.jstree({
        core: {
            themes: {
                name: that.jstreeTheme,
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
                    that.createXpathTree($(data).find("xpath_tree>"));
                    that.setXpathText($(data).find("xpath").text(), $(data).find("anchor").text());
                }
            });
        }
    });

    // Make the tree scrollable
    var $scrollableDiv = $("<div/>", {
        "class": "scrollable-container"
    });
    $scrollableDiv.append(this.domTree);
    $mid.append($scrollableDiv);

    $(this.mainDiv).append($mid)
};

SourcePicker.prototype.createBottomContent = function () {
    var that = this;
    var $bottom = $("<div/>");

    // Buttons
    var $xpathBtns = $("<div/>", {
        "class": "xpath-btn"
    });

    var createBtn = function (title, func, imgUrl) {
        var $btn = $("<button/>", {
            type: "button",
            title: title,
            click: func
        });
        $btn.css("background-image", "url(" + imgUrl + ")");
        $btn.attr("disabled", "disabled");

        return $btn;
    }

    // Evaluate XPath
    this.evaluateXpathBtn = createBtn(
        "Evaluate XPath",
        function () {
            $.ajax({
                dataType: "xml",
                url: Convertigo.createServiceUrl("studio.sourcepicker.EvaluateXpath"),
                data: {
                    xpath: that.xpath.val().trim()
                },
                success: function (data, textStatus, jqXHR) {
                    that.createXpathTree($(data).find("xpath_tree>"));
                }
            });
        },
        Convertigo.getBaseConvertigoStudioUrl("img/editors/calc_xpath.png")
    );

    // Backward XPath history
    var $backwardXpath = createBtn(
        "Backward XPath history",
        function () {
        },
        Convertigo.getBaseConvertigoStudioUrl("img/editors/backward_history.png")
    );

    // Forward XPath history
    var $forwardXpath = createBtn(
        "Forward XPath history",
        function () {
        },
        Convertigo.getBaseConvertigoStudioUrl("img/editors/forward_history.png")
    );

    $xpathBtns
        .append(this.evaluateXpathBtn)
        .append($("<br>"))
        .append($backwardXpath)
        .append($("<br>"))
        .append($forwardXpath);

    // Textarea
    var xpathTxtAreaId = "xpath-txtarea" + SourcePicker.nbInstances.toString();
    var $containerLabelXpath = $("<div/>", {
        "class": "xpath-container"
    });
    var $label = $("<label/>", {
        "for": xpathTxtAreaId,
        text: "xPath"
    });
    this.xpath = $("<textarea/>", {
        id: xpathTxtAreaId,
        spellcheck: "false",
        "class": "xpath"
    });

    $(document)
        // Enter key when focus is on textarea = Evaluate XPath
        .on("keypress", this.xpath.selector, function (e) {
            if (e.which == 13) {
                if (that.xpath.val().length) {
                    that.evaluateXpathBtn.click();
                }
                e.preventDefault();
            }
        })
        // Content in textarea = enable button
        .on("keyup", this.xpath.selector, function (e) {
            $.ajax({
                dataType: "xml",
                url: Convertigo.createServiceUrl("studio.sourcepicker.ModifyXPathText"),
                data: {
                    xpath: that.xpath.val().trim()
                }
            });
            that.updateStateEvaluateXpathBtn();
        });

    $containerLabelXpath
        .append($label)
        .append(this.xpath);

    var $mainContainer = $("<div/>", {
        "class": "main-container"
    });
    $mainContainer
        .append($xpathBtns)
        .append($containerLabelXpath);
    $bottom.append($mainContainer);

    // Xoath tree result
    this.xpathTree = $("<div/>", {
        "class": "xpath-tree"
    });
    this.xpathTree.jstree({
        core: {
            themes: {
                name: that.jstreeTheme,
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

    $bottom.append(this.xpathTree);
    $(this.mainDiv).append($bottom);
};

SourcePicker.prototype.fillHelpContent = function (tag, type, name, comment, textBtn, enableBtn) {
    this.inputTags["Tag"].val(tag);
    this.inputTags["Type"].val(type);
    this.inputTags["Name"].val(name);
    this.inputTags["Comment"].val(comment);
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

SourcePicker.prototype.setXpathText = function (xpath, anchor) {
    this.xpath.val(xpath);

    this.updateStateEvaluateXpathBtn();

    this.removeAnchor();
    if (anchor) {
        this.xpath.highlightTextarea({
            words: [
                "^" + anchor
            ]
        });
    }
};

SourcePicker.prototype.removeAnchor = function () {
    this.xpath.highlightTextarea("destroy");
};

SourcePicker.prototype.createXpathTree = function ($rootNode) {
    this.removeTree(this.xpathTree);    
    if ($rootNode.length) {
        this.addNodeInDomTree(this.xpathTree, $rootNode);
    }
};

SourcePicker.prototype.updateStateEvaluateXpathBtn = function () {
    if (this.xpath.val().length) {
        this.evaluateXpathBtn.removeAttr("disabled");
    }
    else {
        this.evaluateXpathBtn.attr("disabled", "disabled");
    }
};
