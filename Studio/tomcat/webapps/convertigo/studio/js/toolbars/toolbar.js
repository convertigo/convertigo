function Toolbar(panelSelector) {
    this.toolbar = $(panelSelector).parents("div[role='part']").find("div[role='toolbar-header']");
}

Toolbar.prototype.addAction = function (id, srcImg, tootltip, action) {
    // Get all actions (=icons) of the toolbar
    var actions = $(this.toolbar).find(">div").get().filter(function (elt) {
        var rightValue = elt.style.right;
        return rightValue !== "0px" && rightValue !== "";
    });
    
    // Create new action
    var $newAction = $(actions[0]).clone();
    
    // Compute right offset
    var rightValue = Toolbar.spaceFactor * Toolbar.iconSize + parseInt(actions[actions.length - 1].style.right.replace("px", ""));
    $newAction.css("right", rightValue + "px");
    $newAction.attr("id", id);
    $newAction.attr("title", tootltip);
    $newAction.click(action);
    
    // Create image
    var $newActionImg = $("<img/>", {
        src: srcImg
    });
    $newAction.find("*:first").replaceWith($newActionImg);
    
    // Add new icon action
    $(this.toolbar).append($newAction);
};

Toolbar.spaceFactor = 1.5;
Toolbar.iconSize = 16;
