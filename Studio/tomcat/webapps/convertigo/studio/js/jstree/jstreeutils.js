(function($, undefined) {
    "use strict";
    $.jstree.defaults.utils = function () {
        return true;
    };
    $.jstree.plugins.utils = function (options, parent) {
    	this._computeNameId = function (id, num) {
    		return id + "-" + num;
    	},
    	this.generateId = function (id) {
    		var num = 1;
    		var newId = this._computeNameId(id, num);
    		while (typeof this._model.data[newId] !== "undefined") {
    			newId = this._computeNameId(id, num++);
    		}

			return newId;
    	},
    	this.getIdNodes = function (id) {
    		var num = 1;
    		var newId = this._computeNameId(id, num);
    		if (typeof this._model.data[newId] === "undefined") {
    			return false;
    		}
    		
    		var ids = [];
       		do {
       			newId = this._computeNameId(id, num)
       			ids.push(newId);
       			++num;
       			newId = this._computeNameId(id, num)
    		} while (typeof this._model.data[newId] !== "undefined")
    		
    		return ids;
    	}
    };
})(jQuery);