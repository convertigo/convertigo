function TreeViewContainer (treeClassName) {
    this.tree = $("<div/>", {
        "class": treeClassName
    });
}

TreeViewContainer.prototype.getDivTree = function () {
    return $(this.tree);
};

TreeViewContainer.prototype.getDivWrapperTree = function () {
    /*
     * As we use the plugin jstreegrid (to show comments), the main div of jstree
     * and the comments colums is wrapped in a div.
     * So we find this wrapper div.
     */
    return $(this.tree).parents().last();
};
