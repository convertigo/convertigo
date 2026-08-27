/**
 * @param {Record<string, any>} properties
 * @param {string} displayName
 * @param {string=} technicalName
 * @returns {string}
 */
export function objectPropertyValue(properties, displayName, technicalName = '') {
	const direct = properties?.[displayName];
	const property =
		direct ??
		Object.values(properties ?? {}).find(
			(candidate) =>
				candidate?.displayName === displayName ||
				(Boolean(technicalName) && candidate?.name === technicalName)
		);
	return String(property?.value ?? '').trim();
}

/**
 * Convert the stable provider type into the same human form used by the
 * frontend palette while its exact descriptor is being resolved.
 * @param {unknown} value
 * @returns {string}
 */
export function flowTypeDisplayName(value) {
	const words = String(value ?? '')
		.trim()
		.replace(/^.*[.:/]/, '')
		.replace(/([a-z\d])([A-Z])/g, '$1 $2')
		.replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
		.replace(/[_-]+/g, ' ')
		.split(/\s+/)
		.filter(Boolean);
	if (!words.length) {
		return '';
	}
	return words
		.map((word, index) => {
			if (/^[A-Z\d]{2,}$/.test(word)) {
				return word;
			}
			const lower = word.toLowerCase();
			return index === 0 ? `${lower.charAt(0).toUpperCase()}${lower.slice(1)}` : lower;
		})
		.join(' ');
}

/**
 * @param {unknown} value
 * @returns {string}
 */
function normalizedDefinitionType(value) {
	return String(value ?? '')
		.replace(/^frontendblock:/i, '')
		.replace(/^.*[.:/]/, '')
		.replace(/[^a-z\d]/gi, '')
		.toLowerCase();
}

/**
 * Match the public provider descriptor by its stable Flow type. Java class is
 * only a fallback for regular DBOs: all frontend widgets intentionally share
 * FlowVirtualObject and must not collapse onto that technical class.
 * @param {{ name?: string, items?: any[] }[]} categories
 * @param {{ flowType?: string, javaClass?: string }} identity
 * @returns {any | null}
 */
export function findBlockDefinition(categories, { flowType = '', javaClass = '' } = {}) {
	const normalizedFlowType = normalizedDefinitionType(flowType);
	for (const category of categories ?? []) {
		for (const item of category?.items ?? []) {
			if (
				normalizedFlowType &&
				[item?.block, item?.classname, item?.id, item?.name].some(
					(candidate) => normalizedDefinitionType(candidate) === normalizedFlowType
				)
			) {
				return item;
			}
		}
	}
	if (!javaClass || javaClass.endsWith('.FlowVirtualObject')) {
		return null;
	}
	for (const category of categories ?? []) {
		const item = (category?.items ?? []).find((candidate) => candidate?.classname === javaClass);
		if (item) {
			return item;
		}
	}
	return null;
}

/**
 * Keep the palette definition as the public identity and attach the selected
 * instance name as secondary context.
 * @param {any} item
 * @param {string} id
 * @param {Record<string, any>} properties
 * @returns {any}
 */
export function blockDefinitionForInstance(item, id, properties) {
	const itemPropertyDocumentation = Array.isArray(item?.propertyDocumentation)
		? item.propertyDocumentation
		: [];
	const propertyDocumentation = itemPropertyDocumentation.length
		? itemPropertyDocumentation
		: String(item?.propertiesDescriptionHtml ?? '').trim()
			? []
			: propertyDocumentationFromProperties(properties);
	return {
		...item,
		instanceId: id,
		instanceName:
			objectPropertyValue(properties, 'Summary', 'summary') ||
			objectPropertyValue(properties, 'Name', 'P_Name') ||
			String(id ?? '')
				.split('.')
				.at(-1) ||
			'',
		flowType: objectPropertyValue(properties, 'Flow type', 'virtualType'),
		propertyDocumentation,
		isBlockDefinition: true
	};
}

/**
 * Keep property documentation available when the public palette descriptor is
 * intentionally compact. Instance properties remain the authoritative source
 * for labels and descriptions exposed by Studio.
 * @param {Record<string, any>} properties
 * @returns {{ label: string, description: string }[]}
 */
export function propertyDocumentationFromProperties(properties) {
	return Object.entries(properties ?? {})
		.filter(([, property]) => property?.category !== 'Information' && property?.shortDescription)
		.map(([label, property]) => ({
			label: String(property?.displayName || label),
			description: String(property.shortDescription).trim()
		}))
		.filter((property) => property.description);
}

/**
 * Read the public property contract embedded in a lightweight Library block
 * definition. This avoids rebuilding the palette merely to render its docs.
 * @param {Record<string, any>} properties
 * @returns {{ label: string, description: string }[]}
 */
export function propertyDocumentationFromDefinition(properties) {
	const encoded = Object.values(properties ?? {}).find(
		(property) => property?.name === 'properties' || property?.displayName === 'properties'
	)?.value;
	let definitions;
	try {
		definitions = typeof encoded === 'string' ? JSON.parse(encoded) : encoded;
	} catch {
		return [];
	}
	if (!definitions || typeof definitions !== 'object' || Array.isArray(definitions)) {
		return [];
	}
	return Object.entries(definitions)
		.filter(([, definition]) => !definition?.hidden && String(definition?.description ?? '').trim())
		.map(([name, definition]) => ({
			label: String(definition?.label || name),
			description: String(definition.description).trim()
		}));
}

/**
 * @param {unknown} value
 * @returns {boolean}
 */
export function isFrontendBlockDefinitionSourceId(value) {
	const id = String(value ?? '');
	return id.includes('.frontends.') && /\.catalog\..*\.block_[^.]+$/.test(id);
}

/**
 * Resolve the source-backed block definition into the provider project's
 * lightweight Library catalog. The catalog exposes implementation metadata
 * without adding every implementation AST to consumer project trees.
 * @param {any} item
 * @returns {string}
 */
export function blockDefinitionSourceId(item) {
	const project = String(item?.sourceProject ?? item?.provider ?? '').trim();
	const definitionPath = String(item?.sourceDefinitionPath ?? item?.definitionPath ?? '').trim();
	if (
		!/^[A-Za-z0-9_.-]+$/.test(project) ||
		!definitionPath ||
		!/^[A-Za-z0-9_.-]+$/.test(definitionPath) ||
		definitionPath.includes('..')
	) {
		return '';
	}
	return `${project}.FlowEngine.${definitionPath}`;
}
