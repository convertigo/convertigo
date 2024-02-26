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

var _c8o_showGrids_state = false;
var _c8o_showGrids_previous = null;
function _c8o_showGrids(bShow) {
	_c8o_showGrids_state = bShow;
	_c8o_showGridsApply();
}

function _c8o_showGridsApply() {
	var su = new StyleUtils();
	
	su.removeStyle("_c8o_style_");
	if (_c8o_showGrids_state) {
		var nl = document.getElementsByClassName(_c8o_highlight_class_previous);
		if (nl.length && nl.item(0).tagName.toLowerCase() == "ion-grid") {
			var c = "." + _c8o_highlight_class_previous;
			su.addNewStyle("ion-grid" + c, "border: solid 1px red",  "_c8o_style_");
			su.addNewStyle("ion-grid" + c + ">ion-row",  "border: dotted 1px blue", "_c8o_style_");
			su.addNewStyle("ion-grid" + c + ">ion-row>ion-col",  "border: solid 1px green","_c8o_style_");
		} else {
			su.addNewStyle("ion-grid", "border: solid 1px red",  "_c8o_style_");
			su.addNewStyle("ion-row",  "border: dotted 1px blue", "_c8o_style_");
			su.addNewStyle("ion-col",  "border: solid 1px green","_c8o_style_");
		}
		su.submitNewStyle();
		_c8o_showGrids_previous = _c8o_highlight_class_previous;
		_c8o_remove_all_overlay();
	} else if (_c8o_showGrids_previous != null) {
		var cls = _c8o_showGrids_previous;
		_c8o_showGrids_previous = null;
		_c8o_highlight_class(cls);
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
	if (_c8o_showGrids_state) {
		_c8o_showGridsApply();
		return;
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

function _c8o_error(str) {
	
}

document.addEventListener("DOMContentLoaded", function () {
	if (document.title == "Error") {
		var pre = document.createElement("pre");
		document.body.appendChild(pre);
		window._c8o_error = (str) => {
			if (str == null) {
				window.location.reload();
			} else {
				pre.textContent = str;
			}
		};
		_c8o_error("Compilation failed, please fix this error and reload the page.");
		return;
	}
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
			var pix = Math.max(0, e.x - dropOptions.offsetWidth / 2);
			pix = Math.min(window.innerWidth - dropOptions.offsetWidth, pix);
			dropOptions.style["left"] = pix + "px";
			pix = Math.max(0, e.y - dropOptions.offsetHeight / 2);
			pix = Math.min(window.innerHeight - dropOptions.offsetHeight, pix);
			dropOptions.style["top"] = pix + "px";
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

function initEditor(device) {
	_c8o_remove_all_overlay();
	let app = document.getElementsByTagName('ion-app')[0];
	if (app.getAttribute["id"] == "gjs") {
		return;
	}
	app.setAttribute("id", "gjs");
	let textChanged = {};
	let eltMoved = [];
	let exStyle = '';
	for (let s of [...document.head.getElementsByTagName('style')]) {
	    if (s.textContent.includes('.class')) {
	        exStyle += s.textContent.replaceAll(/\[_ng.*?]/g,'') + '\n';
	    }
	}
	var docs = {};
	fetch('${projectUrl}/_private/ionic/node_modules/@ionic/core/dist/docs.json').then(async (r) => {
		docs = await r.json();
	});
	
	const webComponentsPlugin = (editor) => {
	  editor.Components.addType('web-component', {
	    isComponent: (el) =>
	      el.tagName?.includes('-') && {
	        name: el.tagName.toLowerCase(),
	        type: 'web-component',
	      },
	    view: {
	      preinitialize(opt) {
	        this.opts = opt;
	      },
	      _createElement(tagName) {
	        const frameDoc = this.frameView?.getDoc();
	        const doc = frameDoc || document;
	        return doc.createElement(tagName);
	      },
	    }
	  });
	};
	const editor = window.grapesjs.init({
		// Indicate where to init the editor. You can also pass an HTMLElement
		container: '#gjs',
		// Get the content for the canvas directly from the element
		// As an alternative we could use: `components: '<h1>Hello World Component!</h1>'`,
		fromElement: true,
		// Size of the editor
		width: 'auto',
		height: 'auto',
		// Disable the storage manager for the moment
		storageManager: false,
		// Avoid any default panel
		plugins: [webComponentsPlugin],
		canvas: {
			scripts: [
				{
					src: '${projectUrl}/_private/ionic/node_modules/@ionic/core/dist/ionic/ionic.esm.js',
					type: 'module',
				},
				{
					src: '${projectUrl}/_private/ionic/node_modules/@ionic/core/dist/ionic/ionic.js',
				},
			],
			styles: ['${projectUrl}/_private/ionic/node_modules/@ionic/core/css/ionic.bundle.css'],
		},
		richTextEditor: {
			actions: null,
			custom: true
		},
		deviceManager: {
			default: device ?? 'desktop'
		}
	});
	
	editor.on('load', () => {
		let doc = window.document.getElementsByTagName("iframe")[0].contentDocument;
		
		/* copy Link */
		let head = doc.getElementsByTagName('head')[0];
		let link = doc.createElement('link');
		link.setAttribute("rel", "stylesheet");
		link.setAttribute("href", "styles.css");
		head.appendChild(link);
		
		/* Set GrapesJS DIV wrapper to be flex */
		let gjsdiv = doc.querySelector('div[data-gjs-highlightable="true"]');
		gjsdiv['style']['display'] = 'flex';
		gjsdiv['style']['flex-direction'] = 'column';
		gjsdiv['style']['justify-content'] = 'space-between';
		
		document.querySelectorAll('#gjs-clm-tags-field,#gjs-clm-field,.gjs-clm-sels-info,.gjs-pn-views,.gjs-pn-btn.fa-code,.gjs-pn-btn.fa-arrows-all').forEach(elt => elt.hidden = true);
		document.querySelector('.gjs-pn-views-container').style.minWidth = '200px';
	});
	
	/* will be called each time a user mofifies a style property */
	editor.on('style:property:update', (props, obj) => {
		//console.log("Property is : " +JSON.stringify(props))
	});
	
	/* will be called each time a user selects on aother item to style*/
	editor.on('style:target', (target) => {
		window.java.onEditorEvent(JSON.stringify({
			event: 'style:target',
			target
		}));
		const sm = editor.StyleManager;
		if (sm.getSector('sector-ionic')) {
			sm.removeSector('sector-ionic');
		}
		const attrs = editor.getSelected().attributes;
		if (attrs?.type == 'web-component') {
			const tagName = attrs.tagName;
			var styles = docs?.components?.find(c => c.tag == tagName).styles ?? [];
			if (styles.length > 0) {
				sm.addSector('sector-ionic', {
					name: tagName,
					open: false,
					properties: styles.map(({name}) => {
						const prop = { property: name, full: true };
						if (name.includes('border-style')) {
							prop.extend = 'border-style';
						} else if (name.includes('opacity')) {
							prop.extend = "opacity";
							prop.type = "slider";
						} else if (name.includes('color') || name.includes('background')) {
							prop.type = "color";
						} else if (name.includes('transition') || name.includes('box-shadow')) {
							prop.type = "text";
						} else {
							prop.type = "number";
							prop.units = ['px', '%', 'em', 'rem', 'vh', 'vw'];
						}
						return prop;
					})
				});
			}
		}
	});
	
	editor.on('component:drag:end', (info) => {
		console.log("Drag end", info);
		
		const target = /class(\d+)/.exec(info?.target?.view?.el?.className)?.[1];
		const parent = /class(\d+)/.exec(info?.parent?.view?.el?.className)?.[1];
		eltMoved.push({target, parent, index: info?.index});
	});
	
	editor.on('rte:disable', (event) => {
		console.log("rte:disable ", event);
		
		const priority = /class(\d+)/.exec(event?.el?.className)?.[1]
		if (priority) {
			textChanged[priority] = event.el.textContent;
		}
	});
	window.getEditorChanges = () => {
		return JSON.stringify({
			text: textChanged,
			move: eltMoved,
			scss: toSCSS(editor.getCss({json: true}))
		});
	};
	
	console.log('add current style:\n' + exStyle);
	editor.addStyle(exStyle);
	
	window.gjseditor = editor;
}

function initGrapesJS(device) {
	const doc = document;
	if (doc.getElementsByTagName('ion-app').length == 0) {
		return;
	} else if ('grapesjs' in window) {
		console.log("GrapesJS already init");
		initEditor(device);
	} else {
		console.log("init GrapesJS");
		try {
			let elt = doc.createElement('script');
			elt.setAttribute('src', '${projectUrl}/../../scripts/grapes.min.js');
			elt.onload = () => {
				initEditor(device);
			};
			doc.head.appendChild(elt);
			elt = doc.createElement('link');
			elt.setAttribute('rel', 'stylesheet');
			elt.setAttribute('href', '${projectUrl}/../../css/grapes.min.css');
			doc.head.appendChild(elt);
		} catch(e) {
			console.log('grapejs init failed', e);
		}
	}
}

function toSCSS(rules) {
    const ruleToString = (rule) => {
		console.log(rule);
        let style = '';
        let indent = '    ';
        if (rule.getAtRule()) {
            style += `${indent}${rule.getAtRule()} {\n`;
            indent += '    ';
        }
        if (rule.getState()) {
            style += `${indent}&:${rule.getState().getName()} {\n`;
            indent += '    ';
        }
        style += Object.entries(rule.getStyle()).map(s => `${indent}${s[0]}: ${s[1]};`).join('\n');

        if (rule.getState()) {
            indent = indent.substring(4);
            style += `\n${indent}}`;
        }
        if (rule.getAtRule()) {
            style += `\n${indent.substring(4)}}\n`;
        }
        return style;
    };
    const cls = {};
    for (let r of rules) {
        let selector = r.getSelectors().models.find(s => s.getFullName().match(/^\.class\d+$/));
        if (selector) {
            var k = selector.getFullName().substring(6);
            cls[k] = cls[k] ?? [];
            cls[k].push(r);
        }
    }
    for (let k in cls) {
        cls[k] = cls[k].map(r => ruleToString(r)).join('\n') + '\n';
    };
    return cls;
}