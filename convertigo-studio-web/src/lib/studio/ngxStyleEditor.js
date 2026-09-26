/**
 * The style editor of an NGX application in the preview, as the application editor of the Eclipse
 * Studio runs it with its inject.js: GrapesJS, served by the engine, edits the application in place, then
 * gives the styles of the components by their "class<priority>", their texts and their moves.
 */

/**
 * @param {string} projectUrl the URL of the project, as /convertigo/projects/<name>
 * @returns {string} the script the preview runs, taken from the inject.js of the Eclipse Studio
 */
function editorScript(projectUrl) {
	return `
window._c8o_remove_all_overlay = window._c8o_remove_all_overlay || function () {};
window.java = window.java || { onEditorEvent() {} };

function _c8o_initEditor(device) {
	let app = document.getElementsByTagName('ion-app')[0];
	if (!app || app.getAttribute('id') == 'gjs') {
		return;
	}
	app.setAttribute('id', 'gjs');
	let textChanged = {};
	let eltMoved = [];
	let exStyle = '';
	for (let s of [...document.head.getElementsByTagName('style')]) {
		if (s.textContent.includes('.class')) {
			exStyle += s.textContent.replaceAll(/\\[_ng.*?]/g, '') + '\\n';
		}
	}
	var docs = {};
	fetch('${projectUrl}/_private/ionic/node_modules/@ionic/core/dist/docs.json').then(async (r) => {
		docs = await r.json();
	}).catch(() => {});

	const webComponentsPlugin = (editor) => {
		editor.Components.addType('web-component', {
			isComponent: (el) => el.tagName?.includes('-') && { name: el.tagName.toLowerCase(), type: 'web-component' },
			view: {
				preinitialize(opt) {
					this.opts = opt;
				},
				_createElement(tagName) {
					const frameDoc = this.frameView?.getDoc();
					const doc = frameDoc || document;
					return doc.createElement(tagName);
				}
			}
		});
	};
	const editor = window.grapesjs.init({
		container: '#gjs',
		fromElement: true,
		width: 'auto',
		height: 'auto',
		storageManager: false,
		plugins: [webComponentsPlugin],
		canvas: {
			scripts: [
				{ src: '${projectUrl}/_private/ionic/node_modules/@ionic/core/dist/ionic/ionic.esm.js', type: 'module' },
				{ src: '${projectUrl}/_private/ionic/node_modules/@ionic/core/dist/ionic/ionic.js' }
			],
			styles: ['${projectUrl}/_private/ionic/node_modules/@ionic/core/css/ionic.bundle.css']
		},
		richTextEditor: { actions: null, custom: true },
		deviceManager: { default: device ?? 'desktop' }
	});

	editor.on('load', () => {
		let doc = window.document.getElementsByTagName('iframe')[0].contentDocument;
		let link = doc.createElement('link');
		link.setAttribute('rel', 'stylesheet');
		link.setAttribute('href', 'styles.css');
		doc.getElementsByTagName('head')[0].appendChild(link);
		let gjsdiv = doc.querySelector('div[data-gjs-highlightable="true"]');
		if (gjsdiv) {
			gjsdiv.style.display = 'flex';
			gjsdiv.style['flex-direction'] = 'column';
			gjsdiv.style['justify-content'] = 'space-between';
		}
		document.querySelectorAll('#gjs-clm-tags-field,#gjs-clm-field,.gjs-clm-sels-info,.gjs-pn-views,.gjs-pn-btn.fa-code,.gjs-pn-btn.fa-arrows-all').forEach((elt) => (elt.hidden = true));
		const views = document.querySelector('.gjs-pn-views-container');
		if (views) {
			views.style.minWidth = '200px';
		}
	});

	editor.on('style:target', () => {
		const sm = editor.StyleManager;
		if (sm.getSector('sector-ionic')) {
			sm.removeSector('sector-ionic');
		}
		const attrs = editor.getSelected()?.attributes;
		if (attrs?.type == 'web-component') {
			const tagName = attrs.tagName;
			var styles = docs?.components?.find((c) => c.tag == tagName)?.styles ?? [];
			if (styles.length > 0) {
				sm.addSector('sector-ionic', {
					name: tagName,
					open: false,
					properties: styles.map(({ name }) => {
						const prop = { property: name, full: true };
						if (name.includes('border-style')) {
							prop.extend = 'border-style';
						} else if (name.includes('opacity')) {
							prop.extend = 'opacity';
							prop.type = 'slider';
						} else if (name.includes('color') || name.includes('background')) {
							prop.type = 'color';
						} else if (name.includes('transition') || name.includes('box-shadow')) {
							prop.type = 'text';
						} else {
							prop.type = 'number';
							prop.units = ['px', '%', 'em', 'rem', 'vh', 'vw'];
						}
						return prop;
					})
				});
			}
		}
	});

	editor.on('component:drag:end', (info) => {
		const target = /class(\\d+)/.exec(info?.target?.view?.el?.className)?.[1];
		const parent = /class(\\d+)/.exec(info?.parent?.view?.el?.className)?.[1];
		eltMoved.push({ target, parent, index: info?.index });
	});

	editor.on('rte:disable', (event) => {
		const priority = /class(\\d+)/.exec(event?.el?.className)?.[1];
		if (priority) {
			textChanged[priority] = event.el.textContent;
		}
	});

	window.getEditorChanges = () =>
		JSON.stringify({ text: textChanged, move: eltMoved, scss: _c8o_toSCSS(editor.getCss({ json: true })) });

	editor.addStyle(exStyle);
	window.gjseditor = editor;
}

function _c8o_toSCSS(rules) {
	const ruleToString = (rule) => {
		let style = '';
		let indent = '    ';
		if (rule.getAtRule()) {
			style += indent + rule.getAtRule() + ' {\\n';
			indent += '    ';
		}
		if (rule.getState()) {
			style += indent + '&:' + rule.getState().getName() + ' {\\n';
			indent += '    ';
		}
		style += Object.entries(rule.getStyle()).map((s) => indent + s[0] + ': ' + s[1] + ';').join('\\n');
		if (rule.getState()) {
			indent = indent.substring(4);
			style += '\\n' + indent + '}';
		}
		if (rule.getAtRule()) {
			style += '\\n' + indent.substring(4) + '}\\n';
		}
		return style;
	};
	const cls = {};
	for (let r of rules) {
		let selector = r.getSelectors().models.find((s) => s.getFullName().match(/^\\.class\\d+$/));
		if (selector) {
			var k = selector.getFullName().substring(6);
			cls[k] = cls[k] ?? [];
			cls[k].push(r);
		}
	}
	for (let k in cls) {
		cls[k] = cls[k].map((r) => ruleToString(r)).join('\\n') + '\\n';
	}
	return cls;
}

window.initGrapesJS = function (device) {
	if (document.getElementsByTagName('ion-app').length == 0) {
		return;
	}
	if ('grapesjs' in window) {
		_c8o_initEditor(device);
		return;
	}
	let elt = document.createElement('script');
	elt.setAttribute('src', '${projectUrl}/../../scripts/grapes.min.js');
	elt.onload = () => _c8o_initEditor(device);
	document.head.appendChild(elt);
	elt = document.createElement('link');
	elt.setAttribute('rel', 'stylesheet');
	elt.setAttribute('href', '${projectUrl}/../../css/grapes.min.css');
	document.head.appendChild(elt);
};
`;
}

/**
 * Starts the style editor in the document of the preview.
 * @param {Window} win
 * @param {string} projectUrl the URL of the project, as /convertigo/projects/<name>
 * @param {'desktop' | 'mobilePortrait' | 'mobileLandscape'} [device]
 */
export function startStyleEditor(win, projectUrl, device = 'desktop') {
	const anyWin = /** @type {any} */ (win);
	if (typeof anyWin.initGrapesJS !== 'function') {
		const script = win.document.createElement('script');
		script.textContent = editorScript(projectUrl);
		win.document.head.appendChild(script);
	}
	anyWin.initGrapesJS(device);
}

/**
 * @param {Window} win
 * @returns {{ scss?: Record<string, string>, text?: Record<string, string>, move?: any[] } | null} the
 * changes the style editor made, null when it did not run
 */
export function styleEditorChanges(win) {
	const anyWin = /** @type {any} */ (win);
	if (typeof anyWin.getEditorChanges !== 'function') {
		return null;
	}
	try {
		return JSON.parse(anyWin.getEditorChanges());
	} catch {
		return null;
	}
}
