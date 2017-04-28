function StudioTabs() {
	// Div that will contains all tabs content
	this.mainDiv = $("<div/>");
	this.mainList = $("<ul/>");
	
	// All tabs
	this.tabs = {}
	
	this.mainDiv.append(this.mainList);
}

StudioTabs.prototype.addTab = function (tab) {
	// Create  its title
	this.mainList.append($('<li><a href="#' + tab.id + '">' + tab.label + '</a></li>'));
	
	// Create its content
	this.mainDiv.append(tab.getDiv());
	
	// Store it
	this.tabs[tab.getId()] = tab;
};

StudioTabs.prototype.getDiv = function (tab) {
	return this.mainDiv;
};

StudioTabs.prototype.renderTabs = function (active = 0) {
	// Call jqueryui tabs function to render the tabs 
	this.mainDiv.tabs({
		active: active
	});
};
