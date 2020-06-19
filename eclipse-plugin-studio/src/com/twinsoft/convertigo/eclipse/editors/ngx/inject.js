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

function _c8o_display_menu(element, container) {
	menuFunctions = {
		addOption: function(ul, optionText, action) {
			li = ul.appendChild(document.createElement("li"))
			label = li.appendChild(document.createElement("label"))
			label.appendChild(document.createTextNode(optionText))
			label.setAttribute("style", "margin-left: 6px");
			
			li.setAttribute("style", "display: block;"
				  + "color: #000;"
				  + "padding: 4px 16px;"
			);
			
			li.setAttribute("onmouseover", "this.style.backgroundColor='#808080'");
			li.setAttribute("onmouseout",  "this.style.backgroundColor='#f1f1f1'");
			li.onclick =  function () { 
				var ml = [...document.getElementsByClassName('_c8o_menu')];
				if (ml.length != 0) 
					ml[0].remove();
			};
		},
		
		addSeparator(ul) {
			hr = ul.appendChild(document.createElement("hr"))
			hr.setAttribute("style", "border-width: 1px");
		}
	};
	
	/* remove previous menus */
	var ml = [...document.getElementsByClassName("_c8o_menu")];
	if (ml.length != 0)
		ml[0].remove();
	
	/* Create a Menu */
	menu = document.createElement("div");
	menu.setAttribute("style",
			"position: absolute;"
			+ "z-index: 999999;"
			+ "background-color: #f1f1f1;"
			+ "top: 5px;"
			+ "left: 5px;"
			+ "width: 200px;"
			+ "border: solid black  1px;"
			+ "overflow-y: scroll;"
			+ "height: 500px;"
			+ "margin-top: 2px;"
			+ "margin-left: 1px;"
	);
	
	menu.setAttribute("class", "_c8o_menu");

	ul = menu.appendChild(document.createElement("ul"))
	ul.setAttribute("style", 
		"list-style-type: none;" +
		"margin: 0;" +
		"padding: 2px;" 
	);

	menuFunctions.addOption(ul, "Text Align Left", function() {});
	menuFunctions.addOption(ul, "Text Align Right", function() {});
	menuFunctions.addOption(ul, "Text Align Start", function() {});
	menuFunctions.addOption(ul, "Text Align End", function() {});
	menuFunctions.addOption(ul, "Text Align Center", function() {});
	menuFunctions.addOption(ul, "Text Justify", function() {});
	menuFunctions.addSeparator(ul);
	
	menuFunctions.addOption(ul, "Text Wrap", function() {});
	menuFunctions.addOption(ul, "Text No Wrap", function() {});
	menuFunctions.addSeparator(ul);
	
	menuFunctions.addOption(ul, "Text Capitalize", function() {});
	menuFunctions.addOption(ul, "Text Lower case", function() {});
	menuFunctions.addOption(ul, "Text Upper case", function() {});
	menuFunctions.addSeparator(ul);
	
	menuFunctions.addOption(ul, "Margin Vertical", function() {});
	menuFunctions.addOption(ul, "Margin Horizontal", function() {});
	menuFunctions.addOption(ul, "Margin Top", function() {});
	menuFunctions.addOption(ul, "Margin Bottom", function() {});
	menuFunctions.addOption(ul, "Margin start", function() {});
	menuFunctions.addOption(ul, "Margin end", function() {});
	menuFunctions.addOption(ul, "Margin none", function() {});
	menuFunctions.addSeparator(ul);
	
	menuFunctions.addOption(ul, "Padding Vertical", function() {});
	menuFunctions.addOption(ul, "Padding Horizontal", function() {});
	menuFunctions.addOption(ul, "Padding Top", function() {});
	menuFunctions.addOption(ul, "Padding Bottom", function() {});
	menuFunctions.addOption(ul, "Padding start", function() {});
	menuFunctions.addOption(ul, "Padding end", function() {});
	menuFunctions.addOption(ul, "Padding none", function() {});
	menuFunctions.addSeparator(ul);
	
	menuFunctions.addOption(ul, "Hidden", function() {});
	menuFunctions.addOption(ul, "Visible", function() {});
	
	container.appendChild(menu);
}


function _c8o_handle_context_menu(e) {
	_c8o_display_menu(
			window._c8o_currentElement,
			window._c8o_currentContainer
	);
	e.preventDefault();
}

function _c8o_showGrids(bShow) {
	//console.log("**** showGrids:" + bShow)
	class styleUtils  {
		constructor() {
			this.final_style = document.createElement('style');
			this.final_style.type = 'text/css';
		}

		addNewStyle(selector, style, id){
			this.final_style.innerHTML += selector + '{ ' + style + ' } \n';
			this.final_style.id =  id;
		}

		submitNewStyle() {
			document.getElementsByTagName('head')[0].appendChild(this.final_style);
			this.final_style = document.createElement('style');
			this.final_style.type = 'text/css';
		}
		
		removeStyle(selector) {
		    var elem = document.getElementById(selector);
		    if (elem)
		    	return elem.parentNode.removeChild(elem);
		}
	}
	
	su = new styleUtils();
	//console.log("**** showGrids: su : " + su);
	
	if (bShow) {
		su.addNewStyle("ion-grid", "border: solid 1px red",  '_c8o_style_');
		su.addNewStyle("ion-row",  "border: solid 1px blue", '_c8o_style_');
		su.addNewStyle("ion-col",  "border: solid 1px green",'_c8o_style_');
		su.submitNewStyle();
	} else {
		su.removeStyle("_c8o_style_");
	}
	
	//console.log("**** showGrids: Finished" + bShow)
}

_c8o_highlight_class_previous = null;
_c8o_OverlayOriginalPosition = [];

function _c8o_compute_move(ol) {
	moveBy = {
			x: Math.round(ol.style.left.replace("px", "") * 1 - window._c8o_OverlayOriginalPosition[i].left.replace("px", "") * 1),
			y: Math.round(ol.style.top.replace("px", "") * 1 -  window._c8o_OverlayOriginalPosition[i].top.replace("px", "") * 1)
	}
	return(moveBy)
}

function _c8o_keyEventListener(e) {
	ol = [...document.getElementsByClassName("_c8o_overlay")]
	for (i in ol) {
		
		//console.log("i                 : " + i)
		//console.log("ol[i].style.left  : " + ol[i].style.left)
		//console.log("ol[i].style.top   : " + ol[i].style.top)
		
		switch(e.keyCode) {
			case 27: // Escape , reset original position
				ol[i].style.left = window._c8o_OverlayOriginalPosition[i].left; 
				ol[i].style.top  = window._c8o_OverlayOriginalPosition[i].top;
				e.preventDefault();
				break;
			case 39: // Key right
				ol[i].style.left = ol[i].style.left.replace("px", "") * 1 + 1 + "px";
				e.preventDefault();
				break;
			case 37: // Key Left
				ol[i].style.left = ol[i].style.left.replace("px", "") * 1 - 1 + "px";
				e.preventDefault();
				break;
			case 38: // Key UP
				ol[i].style.top =  ol[i].style.top.replace("px", "") * 1 - 1 + "px";
				e.preventDefault();
				break;
			case 40: // Key Down
				ol[i].style.top =  ol[i].style.top.replace("px", "") * 1 + 1 + "px";
				e.preventDefault();
				break;
			case 13: // Key ENTER
				moveBy = _c8o_compute_move(ol[i])
				if (moveBy.x != 0 && moveBy.y != 0) {
					console.log("Moving the component class:" + ol[i].getAttribute("targetClass") + " by  x:" + moveBy.x + ",y:" + moveBy.y);
					_c8o_toast("Moving the component x:" + moveBy.x + ",y:" + moveBy.y);
				}
				e.preventDefault();
				break;
		}
	}
}

function _c8o_highlight_class(classname) {
	var i, nl;
	var ol = [...document.getElementsByClassName("_c8o_overlay")];
	
	if (_c8o_highlight_class_previous != classname) {
		_c8o_highlight_class_previous = classname;
		nl = document.getElementsByClassName(classname);
		document.addEventListener("keydown", _c8o_keyEventListener); 	
	} else {
		_c8o_highlight_class_previous = null;
		nl = [];
		document.removeEventListener("keydown", _c8o_keyEventListener);
	}
	
	for (i = 0; i < nl.length; i++) {
		var overlay = ol[i];
		var rect = nl[i].getBoundingClientRect();
		var container = nl[i].parentNode;
		var element   = nl[i];
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
					+ "transition: 0.2s;"
					+ "top: " + (rect.top - cRect.top + container.scrollTop + rect.height / 2) + "px;"
					+ "left: " + (rect.left - cRect.left + rect.width / 2) + "px;"
					+ "width: 1px;"
					+ "height: 1px;"
					+ "border: red dotted 3px;");
			
			overlay.setAttribute("class", "_c8o_overlay");
			
			window._c8o_dragOverLay = false;
			
			overlay.addEventListener("mousedown", function(e) {
				// console.log("Mouse Down : " + e.x + " " + e.y);
				window._c8o_dragOverLay = {
						event: e
				};
				
				
				document.addEventListener("mousemove", function(e) {
					if (window._c8o_dragOverLay != false) {
						window._c8o_dragOverLay.event.target.style.left 	=  e.x + "px";
						window._c8o_dragOverLay.event.target.style.top  	=  e.y + "px";
					}
					e.preventDefault();
				});
				
				document.addEventListener("mouseup", function(e) {
					if (window._c8o_dragOverLay != false) {
						window._c8o_dragOverLay = false;
						e.preventDefault();
					}
				});
				
				e.preventDefault();
			});

			container.appendChild(overlay);
			_c8o_toast("You can drag the component using the mouse or arrow keys, use ESC to reset to original position or ENTER to set new component position");
		}

		
		
		window._c8o_currentElement = element;
		window._c8o_currentContainer = container;
		overlay.removeEventListener("contextmenu", _c8o_handle_context_menu);
		overlay.addEventListener("contextmenu",    _c8o_handle_context_menu);
		overlay.setAttribute("targetClass", classname);
		
		overlay.style.top = (rect.top - cRect.top + container.scrollTop) + "px";
		overlay.style.left = (rect.left - cRect.left) + "px";
		overlay.style.width = rect.width + "px";
		overlay.style.height = rect.height + "px";
		
		window._c8o_OverlayOriginalPosition[i]  = {
				top: overlay.style.top,
				left: overlay.style.left,
		};
		// console.log(JSON.stringify(window._c8o_OverlayOriginalPosition));
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
}, false);

window.addEventListener("dragover", function (e) {
	try {
		e.preventDefault();
		e.dataTransfer.dropEffect = "move";
		window.java.onDragOver(e);
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