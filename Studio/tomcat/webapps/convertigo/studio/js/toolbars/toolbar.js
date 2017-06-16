function Toolbar(panelSelector) {
    this.toolbar = 
        $(panelSelector)
            .parents("div[role='part']")
            .find("div[role='toolbar-header']");
}

Toolbar.prototype.createAction = function (id, srcImg, tooltip) {
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

Toolbar.prototype.addAction = function (id, srcImg, tooltip, func) {
    var $newAction = this.createAction(
        id,
        srcImg,
        tooltip
    );
    $newAction.click(func);
    $(this.toolbar).append($newAction);
};

Toolbar.prototype.addActionToggable = function (id, srcImg, tooltip, func, toggleByDefault) {
    // Create action
    var $newAction = this.createAction(
        id,
        srcImg,
        tooltip
    );
    var coreFunc = func;
    func = function () {
        // Toogle CSS and call function
        $newAction.toggleClass("action-enabled");
        coreFunc();
    }
    $newAction.click(func);

    // Toggle by default if needed
    if (toggleByDefault) {
        $newAction.addClass("action-enabled");
    }

    $(this.toolbar).append($newAction);
};

Toolbar.spaceFactor = 1.5;
Toolbar.iconSize = 16;
