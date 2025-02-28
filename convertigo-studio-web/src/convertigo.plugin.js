import plugin from 'tailwindcss/plugin'

export default plugin(({ addUtilities, matchUtilities, theme }) => {
	const dash = (value) => (value.length ? `-${value}` : '');

	const merge = (...sources) => {
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
		pl: (v) => ({ paddingLeft: v }),
		pr: (v) => ({ paddingRight: v }),
		pt: (v) => ({ paddingTop: v }),
		pb: (v) => ({ paddingBottom: v }),
		px: (v) => ({ paddingLeft: v, paddingRight: v }),
		py: (v) => ({ paddingTop: v, paddingBottom: v }),
		p: (v) => ({ paddingLeft: v, paddingRight: v, paddingTop: v, paddingBottom: v }),
		ml: (v) => ({ marginLeft: v }),
		mr: (v) => ({ marginRight: v }),
		mt: (v) => ({ marginTop: v }),
		mb: (v) => ({ marginBottom: v }),
		mx: (v) => ({ marginLeft: v, marginRight: v }),
		my: (v) => ({ marginTop: v, marginBottom: v }),
		m: (v) => ({ marginLeft: v, marginRight: v, marginTop: v, marginBottom: v })
	};

	const getVersion = (property, version, prefix = '', suffix = '') => {
		if (!versions[version]) {
			return {};
		}
		return {
			...props[property](`${prefix}${versions[version].base}${suffix}`),
			'@screen md': {
				...props[property](`${prefix}${versions[version].md}${suffix}`)
			}
		};
	};
	Object.keys(props).forEach((property) => {
		Object.keys(versions).forEach((version) => {
			addUtilities({
				[`.${property}${dash(version)}`]: getVersion(property, version)
			});
			// addUtilities({
			// 	[`.-${property}${dash(version)}`]: getVersion(property, version, 'calc(-1 * ', ')')
			// });
		});
	});

	['x', 'y'].forEach((axis) => {
		['', 'start', 'end', 'center', 'baseline', 'stretch'].forEach((align) => {
			Object.keys(versions).forEach((version) => {
				const rule = {
					[`.layout-${axis}${dash(align)}${dash(version)}`]: {
						display: 'flex',
						'flex-direction': axis == 'y' ? 'column' : 'row',
						'align-items': align == '' ? 'center' : align,
						...getVersion('gap', version)
					}
				};
				addUtilities(rule);
				['m', 'p'].forEach((prop) => {
					addUtilities({
						[`.layout-${axis}-${prop}${dash(align)}${dash(version)}`]: merge(
							rule[`.layout-${axis}${dash(align)}${dash(version)}`],
							getVersion(prop, version)
						)
					});
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
