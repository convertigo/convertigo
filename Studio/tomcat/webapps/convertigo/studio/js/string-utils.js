var StringUtils = {
	escapeHTML: function (text) {
		var fakeDiv = $("<div/>", {
			text: text
		});
		
		return fakeDiv.html();
	},
	addDoubleSlash: function (text) {
		return "// " + text;
	},
	unescapeHTML: function (text) {
		var unescapedHTML = $.parseHTML(text);
		return unescapedHTML ? unescapedHTML[0].nodeValue : "";
	},
	replaceDotByMinus: function (text) {
		return text.replace(/\./g, "-");
	}
};
