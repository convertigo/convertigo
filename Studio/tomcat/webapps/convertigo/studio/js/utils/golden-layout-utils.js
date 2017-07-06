var GoldenLayoutUtils = {
    activeTabByTitle: function (layout, title) {
        var allContentItems = layout._getAllContentItems();
        for (var i = 0; i < allContentItems.length; ++i) {
            if (allContentItems[i].config.title === title) {
                allContentItems[i].parent.setActiveContentItem(allContentItems[i]);
                return;
            }
        }
    }
};
