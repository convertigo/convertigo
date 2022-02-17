function _c8o_toast(msg) {
	var toast = document.createElement("div");
	toast.textContent = msg;
	toast.setAttribute("style",
		"position: fixed;" +
		"z-index: 99999999;" +
		"top: 0px;" +
		"margin: 2% 5%;" +
		"background-color: black;" +
		"color: white;" +
		"font-size: 1.2em;" +
		"font-family: sans-serif" +
		"padding: 5px;" +
		"width: 90%;" +
		"border-radius: 10px;" +
		"text-align: center;" +
		"transition: 1s;"
	);
	
	document.body.appendChild(toast);
	
	window.setTimeout(function () {
		toast.style.opacity = 0;
	}, 2000);
	window.setTimeout(function () {
		toast.remove();
	}, 3000);
}

function _c8o_remove_all_overlay() {
	var ol = [...document.getElementsByClassName("_c8o_overlay")];
	for (i in ol) {
		_c8o_remove_overlay(ol[i]);
	}
	_c8o_highlight_class_previous = null;
}

function _c8o_remove_overlay(overlay) {
	if (overlay) {
		overlay.setAttribute("isRemoving", "true");
		var top = overlay.style.top.replace("px", "") * 1;
		var left = overlay.style.left.replace("px", "") * 1;
		var width = overlay.style.width.replace("px", "") * 1;
		var height = overlay.style.height.replace("px", "") * 1;
		overlay.style.top = (top + height / 2) + "px";
		overlay.style.left = (left + width / 2) + "px";
		overlay.style.width = "1px";
		overlay.style.height = "1px";
		window.setTimeout(function () {overlay.remove();}, 200);
	}
}

class StyleUtils  {
	constructor() {
		this.final_style = document.createElement("style");
		this.final_style.type = "text/css";
	}

	addNewStyle(selector, style, id) {
		this.final_style.innerHTML += selector + "{ " + style + " } \n";
		this.final_style.id =  id;
	}

	submitNewStyle() {
		document.getElementsByTagName("head")[0].appendChild(this.final_style);
		this.final_style = document.createElement("style");
		this.final_style.type = "text/css";
	}
	
	removeStyle(selector) {
		var elem = document.getElementById(selector);
		if (elem) {
			return elem.parentNode.removeChild(elem);
		}
	}
}

function _c8o_showGrids(bShow) {
	var su = new StyleUtils();
	
	if (bShow) {
		su.addNewStyle("ion-grid", "border: solid 1px red",  "_c8o_style_");
		su.addNewStyle("ion-row",  "border: solid 1px blue", "_c8o_style_");
		su.addNewStyle("ion-col",  "border: solid 1px green","_c8o_style_");
		su.submitNewStyle();
	} else {
		su.removeStyle("_c8o_style_");
	}
}

_c8o_highlight_class_previous = null;
function _c8o_highlight_class(classname) {
	var i, nl;
	var ol = [...document.getElementsByClassName("_c8o_overlay")];
	if (_c8o_highlight_class_previous != classname) {
		_c8o_highlight_class_previous = classname;
		nl = document.getElementsByClassName(classname);
	} else {
		_c8o_highlight_class_previous = null;
		nl = [];
	}
	for (i = 0; i < nl.length; i++) {
		var overlay = ol[i];
		var rect = nl[i].getBoundingClientRect();
		var container = nl[i].parentNode;
		while (!container.classList.contains("scroll-content") && container != document.body) {
			container = container.parentNode;
		}
		
		var cRect = container.getBoundingClientRect();
		
		
		if (!overlay || overlay.parentNode != container) {
			_c8o_remove_overlay(overlay);
			overlay = document.createElement("div");
			overlay.setAttribute("style",
					"position: absolute;"
					+ "z-index: 999999;"
					+ "background: rgba(0, 100, 255, 0.3);"
					+ "pointer-events: none;"
					+ "transition: 0.2s;"
					+ "top: " + (rect.top - cRect.top + container.scrollTop + rect.height / 2) + "px;"
					+ "left: " + (rect.left - cRect.left + rect.width / 2) + "px;"
					+ "width: 1px;"
					+ "height: 1px;"
					+ "border: red dotted 3px;");
			overlay.setAttribute("class", "_c8o_overlay");
			container.appendChild(overlay);
		}
		
		overlay.style.top = (rect.top - cRect.top + container.scrollTop) + "px";
		overlay.style.left = (rect.left - cRect.left) + "px";
		overlay.style.width = rect.width + "px";
		overlay.style.height = rect.height + "px";
	}
	
	while (i < ol.length) {
		_c8o_remove_overlay(ol[i]);
		i++;
	}
	
	window.setTimeout(function () {
		var nl = document.getElementsByClassName("_c8o_overlay");
		if (nl.length) {
			nl[0].scrollIntoViewIfNeeded();
		}
	}, 200);
}

function _c8o_getShadowHost(element) {
	try {
		return element.getRootNode().host;
	} catch (e) {
		return null;
	}
}

document.addEventListener("DOMContentLoaded", function () {
	var scrollStyle = document.createElement("style");
	scrollStyle.textContent =
		".scroll-content { overflow-y: overlay; }\n"
		+ "::-webkit-scrollbar { width: 8px; }\n"
		+ "::-webkit-scrollbar-thumb {"
			+ "background-color: rgba(0,0,0,0.3);"
			+ "border-radius: 4px;"
		+ "}";
	document.head.appendChild(scrollStyle);
	
	console.defaultLog = console.log.bind(console);
	console.log = function () {
		console.defaultLog.apply(console, arguments);
		try {
			if (arguments[0].indexOf("[HMR] App ") == 0) {
				_c8o_remove_all_overlay();
			}
		} catch(e) {}
	};
}, false);

window.addEventListener("dragover", function (e) {
	try {
		if (e.dataTransfer.items.length == 0) {
			e.preventDefault();
			e.dataTransfer.dropEffect = "move";
			window.java.onDragOver(e);
		}
	} catch (ex) {
		console.log("dragover: " + ex);
	}
});

window.addEventListener("drop", function (e) {
	try {
		// bug from jxbrowser 7.0, onDrag
		//window.java.onDragOver(e);
		window.java.onDrop(e);
	} catch (ex) {
		console.log("drop: " + ex);
	}
});