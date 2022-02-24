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
	var i;
	if (_c8o_highlight_class_previous != null) {
		var nl = document.getElementsByClassName(_c8o_highlight_class_previous);
		for (i = 0; i < nl.length; i++) {
			nl[i].removeAttribute("draggable");
			if (nl[i].hasAttribute("data-save-draggable")) {
				nl[i].setAttribute("draggable", nl[i].getAttribute("data-save-draggable"));
				nl[i].removeAttribute("data-save-draggable");
			}
		}
	}
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

var _c8o_highlight_class_previous = null;
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
		if (nl[i].hasAttribute("draggable")) {
			nl[i].setAttribute("data-save-draggable", nl[i].getAttribute("draggable"));
		}
		nl[i].setAttribute("draggable", "true");
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
	
	var back = document.createElement("div");
	document.body.appendChild(back);
	back.setAttribute("style", "background-color: #25252575; position: absolute; width: 100%; height: 100%");
	back.style.display = "none";
	
	var prg = document.createElement("div");
	prg.setAttribute("style", "position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);");
	var canvas = document.createElement("canvas");
	prg.appendChild(canvas);
	canvas.setAttribute("width", "100");
	canvas.setAttribute("height", "100");
	var msg = document.createElement("div");
	prg.appendChild(msg);
	msg.setAttribute("style", "position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);"
		+ "font-weight: bolder; font-family: monospace; font-size: xx-large;");
	document.body.appendChild(prg);
	
	var _init_doProgress = false;
	window.doProgress = (progress) => {
		_c8o_remove_all_overlay();
		back.style["display"] = "block";
		var context = canvas.getContext("2d");
		if (!_init_doProgress) {
			context.clearRect(0, 0, canvas.width, canvas.height);
			context.lineWidth = 15;
			context.strokeStyle = "#333333";
			
			context.beginPath();
			context.arc(50, 50, 40, 0, 2 * Math.PI, false);
			context.stroke();
			
			context.strokeStyle = "#33b5e5";
			_init_doProgress = true;
		}
		context.beginPath();
		context.arc(50, 50, 40, 1.5 * Math.PI, (2 * progress / 100 + 1.5) * Math.PI, false);
		context.stroke();
		if (progress == 100) {
			back.style["display"] = prg.style["display"] = "none";
			msg.textContent = "";
			_init_doProgress = false;
		} else {
			msg.textContent = progress + "%";
			prg.style["display"] = "block";
		}
	};
	
	var dropRoot = document.createElement("div");
	document.body.appendChild(dropRoot);
	dropRoot.setAttribute("style", "position: absolute; display: none; z-index: 1000000; width: 100%; height: 100%");
	dropRoot.onclick = (e) => {
		dropRoot.style["display"] = "none";
		window.java.onDrag({msg: "cancel"});
	};
	
	var dropOptions = document.createElement("div");
	dropRoot.appendChild(dropOptions);
	dropOptions.setAttribute("style", "position: absolute; z-index: 1000001; box-shadow: rgb(50 50 50) 4px 4px 4px 2px;");
	
	var dropEvent, i;
	var dropClip = (e) => {
		dropEvent["dropOption"] = e.target.textContent;
		dropRoot.style["display"] = "none";
		window.java.onDrop(dropEvent);
	};
	
	["before", "inside", "after"].forEach((opt) => {
		var dropOption = document.createElement("button");
		dropOptions.appendChild(dropOption);
		dropOption.textContent = opt;
		dropOption.style["display"] = "block";
		dropOption.style["width"] = "100%";
		dropOption.style["padding"] = "10px";
		dropOption.style["border"] = "solid 1px black";
		dropOption.onclick = dropClip;
	})
	
	window.addEventListener("drop", function (e) {
		try {
			// bug from jxbrowser 7.0, onDrag
			//window.java.onDragOver(e);
			dropRoot.style["display"] = "block";
			dropOptions.style["left"] = (e.x - dropOptions.offsetWidth / 2) + "px";
			dropOptions.style["top"] = (e.y - dropOptions.offsetHeight / 2) + "px";
			dropEvent = e;
//			window.java.onDrop(e);
		} catch (ex) {
			console.log("drop: " + ex);
		}
	});
	
	console.log("inject.js initialized!");
}, false);

var _c8o_drag_start_dataTransfer = null;
window.addEventListener("drag", function (e) {
	try {
		if (e.dataTransfer != _c8o_drag_start_dataTransfer) {
			_c8o_drag_start_dataTransfer = e.dataTransfer;
			window.java.onDrag(e);
		}
	} catch (ex) {
		console.log("drag: " + ex);
	}
});

window.addEventListener("dragover", function (e) {
	try {
//		if (e.dataTransfer.items.length == 0) {
			e.preventDefault();
			e.dataTransfer.dropEffect = "move";
			window.java.onDragOver(e);
//		}
	} catch (ex) {
		console.log("dragover: " + ex);
	}
});