const plugin = require('tailwindcss/plugin');

exports.convertigoPlugin = plugin(({ addUtilities, matchUtilities, theme }) => {
	const dash = (value) => (value.length ? `-${value}` : '');

	/** @return {import('tailwindcss/types/config').CSSRuleObject} */
	const merge = (...sources) => {
		/** @type {import('tailwindcss/types/config').CSSRuleObject} */
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
	};

	const versions = {
		'': { base: theme('spacing.2'), md: theme('spacing.4') },
		low: { base: theme('spacing.1'), md: theme('spacing.2') },
		none: { base: theme('spacing.0'), md: theme('spacing.0') }
	};

	const props = {
		gap: (v) => ({ gap: v }),
		px: (v) => ({ paddingLeft: v, paddingRight: v }),
		py: (v) => ({ paddingTop: v, paddingBottom: v }),
		p: (v) => ({ paddingLeft: v, paddingRight: v, paddingTop: v, paddingBottom: v }),
		mx: (v) => ({ marginLeft: v, marginRight: v }),
		my: (v) => ({ marginTop: v, marginBottom: v }),
		m: (v) => ({ marginLeft: v, marginRight: v, marginTop: v, marginBottom: v })
	};

	const getVersion = (property, value) => {
		if (!versions[value]) {
			return {};
		}
		return {
			...props[property](versions[value].base),
			'@screen md': {
				...props[property](versions[value].md)
			}
		};
	};
	Object.keys(props).forEach((property) => {
		Object.keys(versions).forEach((version) =>
			addUtilities({
				[`.${property}${dash(version)}`]: getVersion(property, version)
			})
		);
	});

	['x', 'y'].forEach((axis) => {
		Object.keys(versions).forEach((version) => {
			const rule = {
				[`.layout-${axis}${dash(version)}`]: {
					display: 'flex',
					'flex-direction': axis == 'y' ? 'column' : 'row',
					'align-items': 'center',
					...getVersion('gap', version)
				}
			};
			addUtilities(rule);
			['m', 'p'].forEach((prop) => {
				addUtilities({
					[`.layout-${axis}-${prop}${dash(version)}`]: merge(
						rule[`.layout-${axis}${dash(version)}`],
						getVersion(prop, version)
					)
				});
			});
		});
	});

	Object.keys(versions).forEach((version) => {
		const rule = (minWidth) => ({
			display: 'grid',
			gridTemplateColumns: `repeat(auto-fit, minmax(${minWidth}, 1fr))`,
			'> *': { minWidth },
			...getVersion('gap', version)
		});
		matchUtilities(
			{
				[`layout-grid${dash(version)}`]: rule
			},
			{ values: theme('spacing') }
		);
		['m', 'p'].forEach((edge) => {
			matchUtilities(
				{
					[`layout-grid${edge}${dash(version)}`]: (minWidth) =>
						merge(rule(minWidth), getVersion(edge + 'x', version), getVersion(edge + 'y', version))
				},
				{ values: theme('spacing') }
			);
		});
	});
});
