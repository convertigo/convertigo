var VariableUtils = {
    createObject: function (key, value) {
        var obj = {};
        obj[key] = value;
        return obj;
    },
	isDefined: function (variable) {
		return typeof variable !== "undefined";
	},
	isUndefinedOrNullOrEmpty: function (variable) {
	    return typeof variable === "undefined" || variable === null || variable === "";
	}
};
