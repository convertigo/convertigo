var StyleUtils = {
    changeCssByStylesheetId: function (styleTagId, cssClass, element, value) {
        var cssRules;
        var added = false;

        for (var sIndex = 0; sIndex < document.styleSheets.length; sIndex++) {
            var stylesheet = document.styleSheets[sIndex];
            if (stylesheet.ownerNode.id === styleTagId) {
                if (stylesheet["rules"]) {
                    cssRules = stylesheet["rules"];
                }
                else if (stylesheet["cssRules"]) {
                    cssRules = stylesheet["cssRules"];
                }
                else {
                    // no rules found... browser unknown
                    return "";
                }

                for (var rIndex = 0; rIndex < cssRules.length; ++rIndex) {
                    var cssRule = cssRules[rIndex];
                    if (cssRule.selectorText == cssClass) {
                        if (cssRule.style[element]) {
                            var previousValue = cssRule.style[element];
                            cssRule.style[element] = value;
                            added = true;
                            return previousValue;
                        }
                    }
                }

                if (!added) {
                    try {
                        stylesheet.insertRule(cssClass + " { " + element + ": " + value + "; }", cssRules.length);
                    }
                    catch (err) {
                        try {
                            stylesheet.addRule(cssClass, element + ": " + value + ";");
                        }
                        catch (err) {
                        }
                    }
                }
                break;
            }
        }

        return "";
    },
	// Inject a CSS
	injectLinkStyle: function (cssUrl, callback) {
		var linkStyleTag = document.createElement("link");
		if (callback) {
			linkStyleTag.addEventListener("load", callback, false);
		}
		linkStyleTag.rel = "stylesheet";
		linkStyleTag.href = cssUrl;
		document.head.appendChild(linkStyleTag);
	}
};
