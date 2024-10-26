const plugin = require('tailwindcss/plugin');

exports.convertigoPlugin = plugin(({ addUtilities, matchUtilities, theme }) => {
	const dash = (value) => (value.length ? `-${value}` : '');
	const merge = (target, source) => {
		if (typeof source !== 'object' || source === null) {
			return source;
		}
		const output = { ...target };
		for (const key of Object.keys(source)) {
			if (key in target && typeof target[key] === 'object' && typeof source[key] === 'object') {
				output[key] = merge(target[key], source[key]);
			} else {
				output[key] = source[key];
			}
		}
		return output;
	};

	const versions = {
		'': { base: theme('spacing.2'), md: theme('spacing.5') },
		low: { base: theme('spacing.1'), md: theme('spacing.2') }
	};

	const props = {
		gap: (v) => ({ gap: v }),
		px: (v) => ({ paddingLeft: v, paddingRight: v }),
		py: (v) => ({ paddingTop: v, paddingBottom: v }),
		mx: (v) => ({ marginLeft: v, marginRight: v }),
		my: (v) => ({ marginTop: v, marginBottom: v })
	};

	const getVersion = (property, value) => {
		if (!versions[value]) {
			return;
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
						getVersion(prop + 'x', version)
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
		matchUtilities({
			[`layout-grid${dash(version)}`]: rule
		});
		['m', 'p'].forEach((edge) => {
			matchUtilities({
				[`layout-grid${edge}${dash(version)}`]: (minWidth) =>
					merge(
						merge(rule(minWidth), getVersion(edge + 'x', version)),
						getVersion(edge + 'y', version)
					)
			});
		});
	});
});
