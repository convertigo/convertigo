import fs from 'fs';
import path from 'path';

const versions = {
	'': { base: 2, md: 4 },
	low: { base: 1, md: 2 },
	none: { base: 0, md: 0 }
};

const props = {
	gap: (v) => ({ gap: v }),
	pl: (v) => ({ 'padding-left': v }),
	pr: (v) => ({ 'padding-right': v }),
	pt: (v) => ({ 'padding-top': v }),
	pb: (v) => ({ 'padding-bottom': v }),
	px: (v) => ({ 'padding-left': v, 'padding-right': v }),
	py: (v) => ({ 'padding-top': v, 'padding-bottom': v }),
	p: (v) => ({ 'padding-left': v, 'padding-right': v, 'padding-top': v, 'padding-bottom': v }),
	ml: (v) => ({ 'margin-left': v }),
	mr: (v) => ({ 'margin-right': v }),
	mt: (v) => ({ 'margin-top': v }),
	mb: (v) => ({ 'margin-bottom': v }),
	mx: (v) => ({ 'margin-left': v, 'margin-right': v }),
	my: (v) => ({ 'margin-top': v, 'margin-bottom': v }),
	m: (v) => ({ 'margin-left': v, 'margin-right': v, 'margin-top': v, 'margin-bottom': v })
};

function getVersion(property, version, prefix = '', suffix = '') {
	if (!versions[version]) {
		return {};
	}
	return {
		...props[property](`${prefix}--spacing(${versions[version].base})${suffix}`),
		'@media (width >= var(--breakpoint-md))': {
			...props[property](`${prefix}--spacing(${versions[version].md})${suffix}`)
		}
	};
}

function dash(value) {
	return value.length ? `-${value}` : '';
}

function merge(...sources) {
	const output = {};

	for (const source of sources) {
		for (const key of Object.keys(source)) {
			if (key in output && typeof output[key] == 'object' && typeof source[key] == 'object') {
				output[key] = merge(output[key], source[key]);
			} else {
				output[key] = source[key];
			}
		}
	}
	for (const key of Object.keys(output).filter((k) => k.startsWith('@'))) {
		const o = output[key];
		delete output[key];
		output[key] = o;
	}
	return output;
}

function toCSS(value, indent = '') {
	if (typeof value == 'object') {
		return `{
${Object.entries(value)
	.map(([key, value]) => `${indent}    ${key.trim()}: ${toCSS(value, `${indent}    `)}`)
	.join('\n')}
${indent}}`;
	}
	return `${value};`;
}

function addUtilities(utilities) {
	for (const [selector, rules] of Object.entries(utilities)) {
		css += `
@utility ${selector} ${toCSS(rules)}
`;
	}
}

function matchUtilities(utilities, options) {
	console.log(utilities, options);
}

let css;

function generateLayoutCss() {
	css = '';

	Object.keys(props).forEach((property) => {
		Object.keys(versions).forEach((version) => {
			addUtilities({
				[`${property}${dash(version)}`]: getVersion(property, version),
				[`-${property}${dash(version)}`]: getVersion(property, version, 'calc(-1 * ', ')')
			});
		});
	});

	['x', 'y'].forEach((axis) => {
		['', 'start', 'end', 'center', 'baseline', 'stretch'].forEach((align) => {
			Object.keys(versions).forEach((version) => {
				const rule = {
					[`layout-${axis}${dash(align)}${dash(version)}`]: {
						display: 'flex',
						'flex-direction': axis == 'y' ? 'column' : 'row',
						'align-items': align == '' ? 'center' : align,
						...getVersion('gap', version)
					}
				};
				addUtilities(rule);
				['m', 'p'].forEach((prop) => {
					addUtilities({
						[`layout-${axis}-${prop}${dash(align)}${dash(version)}`]: merge(
							rule[`layout-${axis}${dash(align)}${dash(version)}`],
							getVersion(prop, version)
						)
					});
				});
			});
		});
	});

	Object.keys(versions).forEach((version) => {
		const rule = {
			display: 'grid',
			'grid-template-columns': `repeat(auto-fit, minmax(calc(--value(integer) * var(--spacing)), 1fr))`,
			'grid-template-columns ': `repeat(auto-fit, minmax(--value([*]), 1fr))`,
			'> *': {
				'min-width': 'calc(--value(integer) * var(--spacing))',
				'min-width ': '--value([*])'
			},
			...getVersion('gap', version)
		};
		addUtilities({
			[`layout-grid${dash(version)}-*`]: rule
		});
		['m', 'p'].forEach((edge) => {
			addUtilities({
				[`layout-grid-${edge}${dash(version)}`]: merge(
					rule,
					getVersion(edge + 'x', version),
					getVersion(edge + 'y', version)
				)
			});
		});
	});
	return css;
}

export default function GenerateLayoutCssPlugin(options = {}) {
	return {
		name: 'convertigo-utilities-css-plugin',
		buildStart() {
			const css = generateLayoutCss();
			const outPath = path.resolve(process.cwd(), 'src/convertigo.utilities.css');
			fs.writeFileSync(outPath, css, 'utf-8');
			console.log(`Generated Convertigo CSS utilities: ${outPath}`);
		}
	};
}
