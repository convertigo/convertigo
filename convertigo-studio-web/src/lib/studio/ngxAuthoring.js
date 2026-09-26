/**
 * The authoring of an NGX application in the preview, as the application editor of the Eclipse Studio
 * with its inject.js: the preview has the origin of the Studio, so its document is reached directly.
 * Each element of a component carries the class "class" + the priority of the component.
 */

const PRIORITY_CLASS = /(?:^|\s)class(\d+)(?:\s|$)/;
const OVERLAY_ATTRIBUTE = 'data-c8o-authoring';

/**
 * @param {EventTarget | null | undefined} target
 * @param {Event} [event]
 * @returns {{ priority: string, element: Element } | null} the component element of an event, crossing
 * the shadow roots
 */
export function componentOf(target, event) {
	const path = event?.composedPath?.() ?? [];
	const candidates = path.length ? path : [];
	if (!candidates.length) {
		/** @type {any} */
		let node = target;
		while (node) {
			candidates.push(node);
			node = node.parentNode ?? node.host ?? null;
		}
	}
	for (const node of candidates) {
		const element = /** @type {any} */ (node);
		if (element?.getAttribute?.(OVERLAY_ATTRIBUTE) != null) {
			continue;
		}
		const match =
			typeof element?.className === 'string' ? PRIORITY_CLASS.exec(element.className) : null;
		if (match) {
			return { priority: match[1], element };
		}
	}
	return null;
}

/**
 * @param {Document} doc
 * @param {string[]} classes the classes of a component then of its parents
 * @returns {Element[]} the elements of the first class the document shows
 */
export function elementsOf(doc, classes) {
	for (const name of classes ?? []) {
		const elements = [...doc.getElementsByClassName(name)].filter(
			(element) => element.getAttribute(OVERLAY_ATTRIBUTE) == null
		);
		if (elements.length) {
			return elements;
		}
	}
	return [];
}

/**
 * @typedef {{
 *  onSelect?: (priority: string) => void,
 *  onDrop?: (request: { priority: string, position: 'before' | 'inside' | 'after' }) => void,
 *  canDrop?: () => boolean
 * }} NgxAuthoringHandlers
 */

/**
 * Follows the document of the preview: the highlight of the selected component, the hover, the selection
 * on click in select mode and the drops of the palette with their position.
 * @param {Document} doc
 * @param {NgxAuthoringHandlers} handlers
 */
export function attachNgxAuthoring(doc, handlers) {
	const win = doc.defaultView;
	/** @type {Element[]} */
	let selected = [];
	/** @type {string[]} the classes of the selected component then of its parents */
	let selectedClasses = [];
	/** @type {Element | null} */
	let hovered = null;
	let selecting = false;
	/** @type {HTMLElement[]} */
	let selectedOverlays = [];
	/** @type {HTMLElement | null} */
	let hoverOverlay = null;
	/** @type {HTMLElement | null} */
	let chooser = null;
	let frame = 0;

	/**
	 * @param {string} color
	 */
	function overlay(color) {
		const div = doc.createElement('div');
		div.setAttribute(OVERLAY_ATTRIBUTE, '');
		div.style.cssText = [
			'position: fixed',
			'z-index: 2147483646',
			'pointer-events: none',
			'box-sizing: border-box',
			`border: 2px solid ${color}`,
			`background: ${color}22`,
			'border-radius: 2px',
			'transition: all 0.12s ease'
		].join(';');
		doc.body.appendChild(div);
		return div;
	}

	/**
	 * @param {HTMLElement} div
	 * @param {Element} element
	 */
	function place(div, element) {
		const rect = element.getBoundingClientRect();
		div.style.display = rect.width || rect.height ? 'block' : 'none';
		div.style.top = `${rect.top}px`;
		div.style.left = `${rect.left}px`;
		div.style.width = `${rect.width}px`;
		div.style.height = `${rect.height}px`;
	}

	function render() {
		frame = 0;
		if (!doc.body) {
			return;
		}
		if (!selected.length || selected.some((element) => !element.isConnected)) {
			// the application shows its elements late or replaces them, as when a page opens or reloads
			selected = selectedClasses.length ? elementsOf(doc, selectedClasses) : [];
		}
		while (selectedOverlays.length < selected.length) {
			selectedOverlays.push(overlay('#e0443e'));
		}
		selectedOverlays.splice(selected.length).forEach((div) => div.remove());
		selected.forEach((element, index) => place(selectedOverlays[index], element));
		if (hovered?.isConnected && !selected.includes(hovered)) {
			hoverOverlay ??= overlay('#3a7bd5');
			place(hoverOverlay, hovered);
		} else {
			hoverOverlay?.remove();
			hoverOverlay = null;
		}
	}

	function schedule() {
		if (!frame && win) {
			frame = win.requestAnimationFrame(render);
		}
	}

	function closeChooser() {
		chooser?.remove();
		chooser = null;
	}

	/**
	 * @param {number} x
	 * @param {number} y
	 * @param {string} priority
	 */
	function choosePosition(x, y, priority) {
		closeChooser();
		const box = doc.createElement('div');
		box.setAttribute(OVERLAY_ATTRIBUTE, '');
		box.style.cssText = [
			'position: fixed',
			'z-index: 2147483647',
			'display: grid',
			'min-width: 7rem',
			'overflow: hidden',
			'border: 1px solid #555',
			'border-radius: 6px',
			'background: #1f1f1f',
			'box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35)',
			'font: 13px sans-serif'
		].join(';');
		for (const position of /** @type {const} */ (['before', 'inside', 'after'])) {
			const button = doc.createElement('button');
			button.setAttribute(OVERLAY_ATTRIBUTE, '');
			button.type = 'button';
			button.textContent = position;
			button.style.cssText =
				'border: 0; padding: 8px 14px; background: transparent; color: #eee; text-align: left; cursor: pointer';
			button.onmouseenter = () => (button.style.background = '#3a7bd5');
			button.onmouseleave = () => (button.style.background = 'transparent');
			button.onclick = (event) => {
				event.stopPropagation();
				closeChooser();
				hovered = null;
				schedule();
				handlers.onDrop?.({ priority, position });
			};
			box.appendChild(button);
		}
		doc.body.appendChild(box);
		const width = box.offsetWidth;
		const height = box.offsetHeight;
		const viewWidth = win?.innerWidth ?? width;
		const viewHeight = win?.innerHeight ?? height;
		box.style.left = `${Math.max(0, Math.min(viewWidth - width, x - width / 2))}px`;
		box.style.top = `${Math.max(0, Math.min(viewHeight - height, y - height / 2))}px`;
		chooser = box;
	}

	/** @param {MouseEvent} event */
	function onMove(event) {
		if (!selecting) {
			return;
		}
		const component = componentOf(event.target, event);
		if (component?.element !== hovered) {
			hovered = component?.element ?? null;
			schedule();
		}
	}

	/** @param {MouseEvent} event */
	function onClick(event) {
		if (chooser) {
			if (chooser.contains(/** @type {Node} */ (event.target))) {
				// a position of the drop, chosen in the chooser
				return;
			}
			closeChooser();
			hovered = null;
			schedule();
		}
		if (!selecting) {
			return;
		}
		event.preventDefault();
		event.stopPropagation();
		const component = componentOf(event.target, event);
		if (component) {
			handlers.onSelect?.(component.priority);
		}
	}

	/** @param {DragEvent} event */
	function onDragOver(event) {
		if (handlers.canDrop && !handlers.canDrop()) {
			return;
		}
		event.preventDefault();
		if (event.dataTransfer) {
			event.dataTransfer.dropEffect = 'copy';
		}
		const component = componentOf(event.target, event);
		if (component?.element !== hovered) {
			hovered = component?.element ?? null;
			schedule();
		}
	}

	/** @param {DragEvent} event */
	function onDrop(event) {
		if (handlers.canDrop && !handlers.canDrop()) {
			return;
		}
		event.preventDefault();
		event.stopPropagation();
		const component = componentOf(event.target, event);
		if (component) {
			hovered = component.element;
			schedule();
			choosePosition(event.clientX, event.clientY, component.priority);
		}
	}

	function onDragLeave(/** @type {DragEvent} */ event) {
		if (!event.relatedTarget) {
			hovered = null;
			schedule();
		}
	}

	const options = { capture: true };
	doc.addEventListener('mousemove', onMove, options);
	doc.addEventListener('click', onClick, options);
	doc.addEventListener('dragover', onDragOver, options);
	doc.addEventListener('drop', onDrop, options);
	doc.addEventListener('dragleave', onDragLeave, options);
	doc.addEventListener('scroll', schedule, options);
	win?.addEventListener('resize', schedule);
	// the application moves its elements on its own, as when a page opens
	const timer = win?.setInterval(schedule, 400);

	return {
		/**
		 * @param {string[]} classes the classes of the selected component then of its parents
		 * @returns {boolean} whether the preview shows the component
		 */
		highlight(classes) {
			selectedClasses = classes ?? [];
			selected = doc.body ? elementsOf(doc, selectedClasses) : [];
			const first = selected[0];
			if (first) {
				const rect = first.getBoundingClientRect();
				const height = win?.innerHeight ?? 0;
				if (rect.bottom < 0 || rect.top > height) {
					first.scrollIntoView({ block: 'center', behavior: 'smooth' });
				}
			}
			schedule();
			return selected.length > 0;
		},
		/**
		 * @param {boolean} value
		 */
		setSelecting(value) {
			selecting = value;
			doc.documentElement.style.cursor = value ? 'crosshair' : '';
			if (!value) {
				hovered = null;
				schedule();
			}
		},
		destroy() {
			doc.removeEventListener('mousemove', onMove, options);
			doc.removeEventListener('click', onClick, options);
			doc.removeEventListener('dragover', onDragOver, options);
			doc.removeEventListener('drop', onDrop, options);
			doc.removeEventListener('dragleave', onDragLeave, options);
			doc.removeEventListener('scroll', schedule, options);
			win?.removeEventListener('resize', schedule);
			if (timer) {
				win?.clearInterval(timer);
			}
			if (frame) {
				win?.cancelAnimationFrame(frame);
			}
			closeChooser();
			selectedOverlays.forEach((div) => div.remove());
			hoverOverlay?.remove();
			doc.documentElement.style.cursor = '';
		}
	};
}
