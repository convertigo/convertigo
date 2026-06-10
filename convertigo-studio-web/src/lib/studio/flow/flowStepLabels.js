/**
 * @param {string | undefined} label
 * @returns {string}
 */
function branchLabel(label) {
	const normalized = (label ?? '').trim().toLowerCase();
	if (normalized === 'true' || normalized === 'then' || normalized === 'yes') {
		return 'Then';
	}
	if (normalized === 'false' || normalized === 'else' || normalized === 'no') {
		return 'Else';
	}
	if (normalized === 'next' || normalized === 'continue' || normalized === 'suite') {
		return 'Next';
	}
	if (normalized === 'loop') {
		return 'Loop';
	}
	if (normalized === 'done') {
		return 'Done';
	}
	return label ?? '';
}

/**
 * @param {string | undefined} label
 * @param {{ type?: string, classname?: string, hasElseBranch?: boolean }=} step
 * @returns {string}
 */
function branchLabelForStep(label, step = {}) {
	const normalized = branchLabel(label);
	if (normalized === 'Else' && isSimpleIfStep(step) && !step.hasElseBranch) {
		return 'Next';
	}
	return normalized;
}

/**
 * @param {string | undefined} label
 * @returns {'then' | 'else' | 'next' | 'loop' | 'done' | ''}
 */
function branchTone(label) {
	const normalized = branchLabel(label).toLowerCase();
	return ['then', 'else', 'next', 'loop', 'done'].includes(normalized)
		? /** @type {'then' | 'else' | 'next' | 'loop' | 'done'} */ (normalized)
		: '';
}

/**
 * @param {{ type?: string, classname?: string }} data
 * @returns {boolean}
 */
function isIfStep(data) {
	const type = `${data.type ?? ''} ${data.classname ?? ''}`.toLowerCase();
	return type.includes('ifstep') || type.includes('thenelsestep');
}

/**
 * @param {{ type?: string, classname?: string }} data
 * @returns {boolean}
 */
function isThenElseStep(data) {
	const type = `${data.type ?? ''} ${data.classname ?? ''}`.toLowerCase();
	return type.includes('thenelsestep');
}

/**
 * @param {{ type?: string, classname?: string }} data
 * @returns {boolean}
 */
function isSimpleIfStep(data) {
	return isIfStep(data) && !isThenElseStep(data);
}

/**
 * @param {{
 *  denied?: boolean,
 *  position?: 'inside' | 'before' | 'after',
 *  branch?: string,
 *  host?: string,
 *  isIf?: boolean,
 *  isThenElse?: boolean,
 *  step?: { type?: string, classname?: string, hasElseBranch?: boolean }
 * }} drop
 * @returns {string}
 */
function dropLabel(drop) {
	if (drop.denied) {
		return 'Not allowed';
	}
	const branch = branchLabelForStep(drop.branch, drop.step);
	const host = drop.host ?? '';
	if (drop.position === 'before') {
		return placementLabel('Before', branch, host);
	}
	const isIf = drop.isIf || (drop.step ? isIfStep(drop.step) : false);
	const isThenElse = drop.isThenElse || (drop.step ? isThenElseStep(drop.step) : false);
	if (drop.position === 'after') {
		if (isIf) {
			return isThenElse ? 'Drop in ELSE branch' : 'Drop on NEXT path';
		}
		return placementLabel('After', branch, host);
	}
	if (branch) {
		return host ? `${branch} branch of ${host}` : `${branch} branch`;
	}
	if (isIf) {
		return 'Drop in THEN branch';
	}
	return host ? `Inside ${host}` : 'Inside';
}

/**
 * @param {{
 *  zone: 'before' | 'inside' | 'after',
 *  branch?: string,
 *  isIf?: boolean,
 *  isThenElse?: boolean,
 *  step?: { type?: string, classname?: string, hasElseBranch?: boolean }
 * }} drop
 * @returns {string}
 */
function dropZoneLabel(drop) {
	const isIf = drop.isIf || (drop.step ? isIfStep(drop.step) : false);
	const isThenElse = drop.isThenElse || (drop.step ? isThenElseStep(drop.step) : false);
	if (drop.zone === 'inside') {
		const branch = branchLabelForStep(drop.branch, drop.step);
		if (branch) {
			return branch.toUpperCase();
		}
		if (isIf) {
			return 'THEN';
		}
		return 'INSIDE';
	}
	if (drop.zone === 'after' && isIf) {
		return isThenElse ? 'ELSE' : 'NEXT';
	}
	return drop.zone === 'before' ? 'BEFORE' : 'AFTER';
}

/**
 * @param {{
 *  zone: 'before' | 'inside' | 'after',
 *  branch?: string,
 *  isIf?: boolean,
 *  isThenElse?: boolean,
 *  step?: { type?: string, classname?: string, hasElseBranch?: boolean }
 * }} drop
 * @returns {'then' | 'else' | 'next' | 'loop' | 'done' | ''}
 */
function dropZoneTone(drop) {
	const isIf = drop.isIf || (drop.step ? isIfStep(drop.step) : false);
	const isThenElse = drop.isThenElse || (drop.step ? isThenElseStep(drop.step) : false);
	if (drop.zone === 'inside' && isIf) {
		const branch = branchTone(branchLabelForStep(drop.branch, drop.step));
		if (branch) {
			return branch;
		}
		return 'then';
	}
	if (drop.zone === 'after' && isIf) {
		return isThenElse ? 'else' : 'next';
	}
	return branchTone(branchLabelForStep(drop.branch, drop.step));
}

/**
 * @param {'Before' | 'After'} placement
 * @param {string} branch
 * @param {string} host
 * @returns {string}
 */
function placementLabel(placement, branch, host) {
	if (branch) {
		return host
			? `${placement} in ${branch} branch of ${host}`
			: `${placement} in ${branch} branch`;
	}
	if (host) {
		return `${placement} in ${host}`;
	}
	return placement;
}

export {
	branchLabel,
	branchLabelForStep,
	branchTone,
	dropLabel,
	dropZoneLabel,
	dropZoneTone,
	isIfStep,
	isSimpleIfStep,
	isThenElseStep
};
