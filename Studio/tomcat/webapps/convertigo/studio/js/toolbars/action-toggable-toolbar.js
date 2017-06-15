function ActionToggabeToolbar(id, srcImg, tooltip, func, toggleByDefault) {
    ActionToolbar.call(this, id, srcImg, tooltip, func);

    this.toggleByDefault = toggleByDefault;

    var coreFunc = this.func;
    var jqueryElemId = "#" + this.id;
    this.func = function () {
        // Toogle CSS and call function
        $(jqueryElemId).toggleClass("action-enabled");
        coreFunc();
    }
}

ActionToggabeToolbar.prototype = Object.create(ActionToolbar.prototype);
ActionToggabeToolbar.prototype.constructor = ActionToolbar;
