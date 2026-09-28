/**
 * The property changes being applied: a save of the project or an execution waits for them, as the
 * Eclipse Studio sets a property before its next action, when the field that changed loses the focus.
 */
let pending = Promise.resolve();

/**
 * @template T
 * @param {Promise<T>} apply
 * @returns {Promise<T>}
 */
export function trackPropertyApply(apply) {
	pending = pending.then(
		() => apply.then(() => {}),
		() => apply.then(() => {})
	);
	pending = pending.catch(() => {});
	return apply;
}

/**
 * @returns {Promise<void>} once the property changes asked are applied
 */
export function settlePropertyApply() {
	return pending;
}
