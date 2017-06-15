function Toolbar(panelSelector) {
    this.toolbar = 
        $(panelSelector)
            .parents("div[role='part']")
            .find("div[role='toolbar-header']");
}

Toolbar.prototype.createAction = function (id, srcImg, tooltip, func) {
    // Get all actions (=icons) of the toolbar
    var actions = $(this.toolbar).find(">div").get().filter(function (elt) {
        var rightValue = elt.style.right;
        return rightValue !== "0px" && rightValue !== "";
    });

    // Create new action
    var $newAction = $(actions[0]).clone();

    // Compute right offset
    var rightValue =
        Toolbar.spaceFactor * Toolbar.iconSize +
        parseInt(actions[actions.length - 1].style.right.replace("px", ""));

    // DOM data
    $newAction.css("right", rightValue + "px");
    $newAction.attr("id", id);
    $newAction.attr("title", tooltip);
    $newAction.click(func);
    $newAction.addClass("img-action");

    // Create image
    var $newActionImg = $("<img/>", {
        src: srcImg
    });
    $newAction
        .find("*:first")
        .replaceWith($newActionImg);

    return $newAction;
}

Toolbar.prototype.addAction = function (actionToolbar) {
    $(this.toolbar).append(this.createAction(
        actionToolbar.id,
        actionToolbar.srcImg,
        actionToolbar.tooltip,
        actionToolbar.func
    ));
};

Toolbar.prototype.addActionToggable = function (actionToggableToolbar) {
    // Create action
    var $newAction = this.createAction(
        actionToggableToolbar.id,
        actionToggableToolbar.srcImg,
        actionToggableToolbar.tooltip,
        actionToggableToolbar.func
    );

    // Toggle by default if needed
    if (actionToggableToolbar.toggleByDefault) {
        $newAction.addClass("action-enabled");
    }

    $(this.toolbar).append($newAction);
};

Toolbar.spaceFactor = 1.5;
Toolbar.iconSize = 16;
