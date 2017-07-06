function Tab(id, label) {
	this.id = id;
	this.label = label;
	this.mainDiv = $("<div/>");
	$(this.mainDiv).attr("id", id);
}

Tab.prototype.getId = function () {
	return this.id;
};

Tab.prototype.getDiv = function () {
	return this.mainDiv;
};

Tab.prototype.update = function (data) {
};

Tab.prototype.focus = function (layout) {
    GoldenLayoutUtils.activeTabByTitle(layout, this.label);
};
