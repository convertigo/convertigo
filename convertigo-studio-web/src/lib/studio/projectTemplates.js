/**
 * The templates of the new projects, as the new project wizards of the Eclipse Studio declare them:
 * each template is a project archive imported under the new name, whose default connector takes the
 * settings of its fields (see the studio.project.Create service).
 *
 * @typedef {{ name: string, label: string, type?: 'text' | 'number' | 'password' | 'checkbox', value?: string | number | boolean, placeholder?: string, required?: boolean }} ProjectTemplateField
 * @typedef {{ id: string, label: string, description: string, icon: string, url: string, fields: ProjectTemplateField[], settings?: Record<string, any>, legacy?: boolean }} ProjectTemplate
 */

const JAVELIN =
	'template_javelin=https://github.com/convertigo/c8oprj-template-javelin/archive/7.8.0.zip';
const JAVELIN_DKU =
	'template_javelinDKU=https://github.com/convertigo/c8oprj-template-javelindku/archive/7.6.0.zip';
const JAVELIN_INTEGRATION =
	'template_javelinIntegration=https://github.com/convertigo/c8oprj-template-javelin-integration/archive/7.6.0.zip';

/**
 * The fields of a screen connector: its service code is "connection parameter,DIR|host:port" ("TCP" for
 * DKU), as the new project wizards of the Eclipse Studio build it.
 * @param {string} parameterHint
 * @returns {ProjectTemplateField[]}
 */
function screenFields(parameterHint) {
	return [
		{ name: 'connectorName', label: 'Connector name', placeholder: 'default name' },
		{ name: 'host', label: 'Host name', value: 'localhost', required: true },
		{ name: 'port', label: 'Host port', type: 'number', value: 23 },
		{ name: 'connectionParameter', label: 'Connection parameter', placeholder: parameterHint }
	];
}

/**
 * @param {string} id
 * @param {string} label
 * @param {string} description
 * @param {string} url
 * @param {'IBM3270' | 'IBM5250' | 'BullDKU7107' | 'UnixVT220'} emulator
 * @param {string} parameterHint
 * @returns {ProjectTemplate}
 */
function screenTemplate(id, label, description, url, emulator, parameterHint) {
	return {
		id,
		label,
		description,
		icon: 'mdi:monitor',
		url,
		fields: screenFields(parameterHint),
		settings: { emulator },
		legacy: true
	};
}

/** @type {ProjectTemplateField} */
const connectorName = {
	name: 'connectorName',
	label: 'Connector name',
	placeholder: 'default name'
};

/** @type {ProjectTemplate[]} */
export const PROJECT_TEMPLATES = [
	{
		id: 'fullstack',
		label: 'Low Code FullStack application',
		description:
			'A Web, desktop or mobile application built with the NGX Builder, on Ionic and Angular.',
		icon: 'mdi:cellphone-link',
		url: 'template_ngxBuilderIonic=https://github.com/convertigo/c8oprj-template-ngx-builder/archive/8.4.0.zip',
		fields: []
	},
	{
		id: 'backend',
		label: 'Low Code Back-End',
		description: 'Sequences orchestrating connectors, exposed as REST and SOAP web services.',
		icon: 'mdi:source-branch',
		url: 'template_sequence=https://github.com/convertigo/c8oprj-template-sequence/archive/8.3.0.zip',
		fields: []
	},
	{
		id: 'rest',
		label: 'REST web service',
		description: 'An HTTP connector calling a remote REST web service.',
		icon: 'mdi:web',
		url: 'template_HTTP=https://github.com/convertigo/c8oprj-template-http/archive/7.9.0.zip',
		fields: [
			connectorName,
			{ name: 'server', label: 'Server', placeholder: 'api.example.com', required: true },
			{ name: 'port', label: 'Port', type: 'number', value: 443 },
			{ name: 'https', label: 'HTTPS', type: 'checkbox', value: true }
		]
	},
	{
		id: 'sql',
		label: 'SQL database',
		description: 'A SQL connector running queries on a database through JDBC.',
		icon: 'mdi:database',
		url: 'template_SQL=https://github.com/convertigo/c8oprj-template-sql/archive/7.6.0.zip',
		fields: [
			connectorName,
			{
				name: 'jdbcDriver',
				label: 'JDBC driver',
				placeholder: 'org.postgresql.Driver',
				required: true
			},
			{
				name: 'jdbcUrl',
				label: 'JDBC URL',
				placeholder: 'jdbc:postgresql://localhost:5432/database',
				required: true
			},
			{ name: 'user', label: 'User' },
			{ name: 'password', label: 'Password', type: 'password' }
		]
	},
	{
		id: 'sap',
		label: 'SAP BAPI (JCo)',
		description: 'A SAP JCo connector calling the BAPIs of a SAP system.',
		icon: 'mdi:server-network',
		url: 'template_SAP=https://github.com/convertigo/c8oprj-template-sap/archive/7.6.0.zip',
		fields: [
			connectorName,
			{ name: 'asHost', label: 'Application server host', required: true },
			{ name: 'systemNumber', label: 'System number', value: '00' },
			{ name: 'client', label: 'Client', value: '000' },
			{ name: 'user', label: 'User' },
			{ name: 'password', label: 'Password', type: 'password' },
			{ name: 'language', label: 'Language', value: 'EN' }
		]
	},
	{
		id: 'siteclipper',
		label: 'Site Clipper',
		description: 'A Site Clipper connector serving a remote Web site through Convertigo.',
		icon: 'mdi:application-outline',
		url: 'template_siteClipper=https://github.com/convertigo/c8oprj-template-site-clipper/archive/7.6.0.zip',
		fields: [
			connectorName,
			{
				name: 'targetUrl',
				label: 'Site URL',
				placeholder: 'https://www.example.com',
				required: true
			},
			{
				name: 'trustAllServerCertificates',
				label: 'Trust all server certificates',
				type: 'checkbox',
				value: false
			}
		]
	},
	screenTemplate(
		'3270web',
		'3270 Web Style',
		'An IBM 3270 Web style webization project.',
		JAVELIN,
		'IBM3270',
		'TN3270 device name'
	),
	screenTemplate(
		'5250web',
		'5250 Web Style',
		'An IBM 5250 Web style webization project.',
		JAVELIN,
		'IBM5250',
		'TN5250 device name'
	),
	screenTemplate(
		'dkuweb',
		'DKU7xxx Web Style',
		'A Bull DKU7xxx Web style webization project.',
		JAVELIN_DKU,
		'BullDKU7107',
		'MAILBOX'
	),
	screenTemplate(
		'3270screen',
		'3270 Screen',
		'An IBM 3270 screen based web service connector project.',
		JAVELIN_INTEGRATION,
		'IBM3270',
		'TN3270 device name'
	),
	screenTemplate(
		'5250screen',
		'5250 Screen',
		'An IBM 5250 screen based web service connector project.',
		JAVELIN_INTEGRATION,
		'IBM5250',
		'TN5250 device name'
	),
	screenTemplate(
		'dkuscreen',
		'Bull DKUxxx Screen',
		'A Bull DKU7xxx screen based web service connector project.',
		JAVELIN_INTEGRATION,
		'BullDKU7107',
		'MAILBOX'
	),
	screenTemplate(
		'vt220screen',
		'Unix VTxxx Screen',
		'A Unix VTxxx screen based web service connector project.',
		JAVELIN_INTEGRATION,
		'UnixVT220',
		''
	),
	{
		id: 'cics',
		label: 'CICS COMMAREA',
		description: 'An IBM CICS COMMAREA based web service connector project.',
		icon: 'mdi:server-network',
		url: 'template_CICS=https://github.com/convertigo/c8oprj-template-cics/archive/7.6.0.zip',
		fields: [
			connectorName,
			{ name: 'ctgName', label: 'CTG configuration name', required: true },
			{ name: 'ctgServer', label: 'CTG server address', required: true },
			{ name: 'ctgPort', label: 'CTG server port', type: 'number', value: 2006 }
		],
		legacy: true
	}
];

/**
 * @param {ProjectTemplate} template
 * @param {Record<string, any>} values
 * @returns {Record<string, any>} the settings of the connector, typed as the service reads them
 */
export function projectTemplateSettings(template, values) {
	/** @type {Record<string, any>} */
	const settings = { ...template.settings };
	for (const field of template.fields) {
		const value = values[field.name];
		if (field.type === 'checkbox') {
			settings[field.name] = Boolean(value);
		} else if (field.type === 'number') {
			if (value !== '' && value !== undefined && value !== null) {
				settings[field.name] = Number(value);
			}
		} else if (String(value ?? '').trim()) {
			settings[field.name] = String(value).trim();
		}
	}
	return settings;
}

/**
 * @param {string} name
 * @returns {string} why a project cannot take this name, or an empty string
 */
export function projectNameError(name) {
	if (!name.trim()) {
		return 'The project needs a name.';
	}
	if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(name.trim())) {
		return 'A project name starts with a letter and uses letters, digits and underscores only.';
	}
	return '';
}
