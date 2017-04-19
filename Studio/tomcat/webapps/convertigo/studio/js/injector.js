var Injector = {
	// Inject a CSS
	injectLinkStyle: function (cssUrl, callback) {
		var linkStyleTag = document.createElement("link");
		if (callback) {
			linkStyleTag.addEventListener("load", callback);
		}
		linkStyleTag.rel = "stylesheet";
		linkStyleTag.href = cssUrl;
		document.head.appendChild(linkStyleTag);
	}
};
