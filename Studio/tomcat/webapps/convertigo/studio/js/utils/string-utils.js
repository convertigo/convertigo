var StringUtils = {
	addDoubleSlash: function (text) {
		return "// " + text;
	},
	// Source: https://stackoverflow.com/questions/2808368/decode-html-entities-in-javascript/2808386#2808386
    decodeHTML: function (text) {
        var entities = {
            "&amp;": "&",
            "&lt;": "<",
            "&gt;": ">"
        };

        for (var prop in entities) {
            if (entities.hasOwnProperty(prop)) {
                text = text.replace(new RegExp(prop, "g"), entities[prop]);
            }
        }
        return text;
    },
	escapeHTML: function (text) {
		var fakeDiv = $("<div/>", {
			text: text
		});

		return fakeDiv.html();
	},
	ellipsis: function (str, max, forHtml = false) {
	    var limit = max - 3;
	    return str.length > limit ? str.substring(0, limit) + (forHtml ? "&hellip;" : "...") : str; 
	},
	join: function (delimiter, ...text) {
		return $.grep(text, Boolean).join(delimiter);
	},
	replaceDotByMinus: function (text) {
		return text.replace(/\./g, "-");
	},
	replaceTextCRLFByRealCRLF: function (text) {
	    return text.replace(/\\r\\n/g, "\r\n");
	},
	unescapeHTML: function (text) {
		var unescapedHTML = $.parseHTML(text);
		return unescapedHTML ? unescapedHTML[0].nodeValue : "";
	}
};
