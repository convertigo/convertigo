function References(id) {
	++References.nbInstances;
	Tab.call(this, "references" + References.nbInstances, "References");
    $(this.mainDiv).text("References");
}

References.prototype = Object.create(Tab.prototype);
References.prototype.constructor = References;

//Used to generate the div id
References.nbInstances = 0;

References.prototype.update = function (data) {
};
