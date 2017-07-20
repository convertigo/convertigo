function Toolbar(container, classAction) {
    this.toolbar = $(container);
    this.classAction = classAction;
}

Toolbar.prototype.createAction = function (id, srcImg, tooltip) {
    // Create new action
    var $newAction = $("<li/>");

    // DOM data
    $newAction.attr("id", this.generateId(id));
    $newAction.attr("title", tooltip);
    $newAction.addClass("img-action");
    $newAction.addClass(this.classAction);

    // Create image
    var $newActionImg = $("<img/>", {
        src: srcImg
    });
    $newAction.append($newActionImg);

    return $newAction;
}

Toolbar.prototype.addAction = function (id, srcImg, tooltip, func) {
    var $newAction = this.createAction(
        id,
        srcImg,
        tooltip
    );
    $newAction.click(func);
    $(this.toolbar).prepend($newAction);
};

Toolbar.prototype.addActionToggable = function (id, srcImg, tooltip, func, toggleByDefault) {
    // Create action
    var $newAction = this.createAction(
        id,
        srcImg,
        tooltip
    );
    var coreFunc = func;
    var actionEnabled = "action-enabled";
    func = function () {
        // Toogle CSS and call function
        $newAction.toggleClass(actionEnabled);
        coreFunc();
    }
    $newAction.click(func);

    // Toggle by default if needed
    if (toggleByDefault) {
        $newAction.addClass(actionEnabled);
    }

    $(this.toolbar).prepend($newAction);
};

Toolbar.prototype.getClassAction = function () {
    return this.classAction;
}

Toolbar.prototype.generateId = function (id) {
    var i = 1;
    var resId = id;
    while ($("#" + resId).length) {
        resId = id + i.toString();
        ++i;
    }

    return resId;
}
