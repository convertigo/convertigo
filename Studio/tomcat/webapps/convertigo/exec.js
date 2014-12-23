$(function () {
	$(document).on("click", "#stopProcess", function () {
		if (confirm("Do you really want to STOP the build?")) { // OK
			$.ajax({
				type:'POST',
				url: 'stop.jsp'
			});
		}
	});
	
	if (window.location.search) {
		alert(window.location.search.substring(1));
	}
	
	var params = {};
	(function () {
		var vars = window.location.hash.split("&"), i, id, key, value;
		for (i = 0; i < vars.length; i += 1) {
			if (vars[i].length) {
				id = vars[i].indexOf("=");
				key = (id > 0) ? vars[i].substring(0, id) : vars[i];
				value = "";
				if (id > 0) {
					value = vars[i].substring(id + 1);
					if (value.length) {
						value = value.replace(new RegExp("\\+", "g"), " ");
						try {
							value = decodeURIComponent(value);
						} catch (err1) {
							try {
								value = unescape(value);
							} catch (err2) {}
						}
					}
				}
				if (!$.isArray(value)) {
					value = [value];
				}
				for (var i in value) {
					if (C8O.isUndefined(data[key])) {
						data[key] = value[i];
					} else if ($.isArray(data[key])) {
						data[key].push(value[i]);
					} else {
						data[key] = [data[key], value[i]];
					}
				}
			}
		}
	})();
	_parseQuery: function (params, query) {
		var data = C8O.isUndefined(params) ? {} : params,
			vars = (query ? query : C8O._getQuery()).split("&"),
			i, id, key, value;
		for (i = 0; i < vars.length; i += 1) {
			if (vars[i].length > 0) {
				id = vars[i].indexOf("=");
				key = (id > 0) ? vars[i].substring(0, id) : vars[i];
				value = "";
				if (id > 0) {
					value = vars[i].substring(id + 1);
					if (value.length) {
						value = value.replace(C8O._define.re_plus, " ");
						try {
							value = decodeURIComponent(value);
						} catch (err1) {
							try {
								value = unescape(value);
							} catch (err2) {}
						}
					}
				}
				C8O.appendValue(data, key, value);
				if (!$.isArray(value)) {
					value = [value];
				}
				for (var i in value) {
					if (C8O.isUndefined(data[key])) {
						data[key] = value[i];
					} else if ($.isArray(data[key])) {
						data[key].push(value[i]);
					} else {
						data[key] = [data[key], value[i]];
					}
				}
			}
		}
		return data;
	},
//	CommandArgs
//	CommandExecDir
//	CommandName
//	CommandComment
});
