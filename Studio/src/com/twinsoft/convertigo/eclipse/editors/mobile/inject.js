function _c8o_remove_all_overlay() {
	var ol = [...document.getElementsByClassName("_c8o_overlay")];
	for (i in ol) {
		_c8o_remove_overlay(ol[i]);
	}
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

function _c8o_highlight_class(classname) {
	var i;
	var ol = [...document.getElementsByClassName("_c8o_overlay")];
	
	var nl = document.getElementsByClassName(classname);
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
	
	nl = document.getElementsByClassName("_c8o_overlay");
	if (nl.length) {
		var visible = false;
		for (i = 0; !visible && i < nl.length; i++) {
			visible = !nl[i].hasAttribute("isRemoving") && nl[i].getBoundingClientRect().top >= 0;
		}
		if (!visible) {
			nl[0].scrollIntoViewIfNeeded();
		}
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
}, false);
