/**
 * Shows an element over the whole window, out of the view whose panel would contain it (a fixed position
 * inside a panel of the dock is clipped): in the body of the document of the view, the Studio or a window
 * the view was moved to.
 * @param {HTMLElement} node
 */
export function portal(node) {
	node.ownerDocument.body.appendChild(node);
	return {
		destroy() {
			node.remove();
		}
	};
}
