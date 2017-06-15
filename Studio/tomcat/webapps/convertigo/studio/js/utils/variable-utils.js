var VariableUtils = {
	isDefined: function (variable) {
		return typeof variable !== "undefined";
	},
	isUndefinedOrNullOrEmpty: function (variable) {
	    return typeof variable === "undefined" || variable === null || variable === "";
	}
};
