/**
 * The templates of the new projects, as the new project wizards of the Eclipse Studio declare them:
 * each template is a project archive imported under the new name, whose default connector takes the
 * settings of its fields (see the studio.project.Create service).
 *
 * @typedef {{ name: string, label: string, type?: 'text' | 'number' | 'password' | 'checkbox', value?: string | number | boolean, placeholder?: string, required?: boolean }} ProjectTemplateField
 * @typedef {{ id: string, label: string, description: string, icon: string, url: string, fields: ProjectTemplateField[] }} ProjectTemplate
 */

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
	}
];

/**
 * @param {ProjectTemplate} template
 * @param {Record<string, any>} values
 * @returns {Record<string, any>} the settings of the connector, typed as the service reads them
 */
export function projectTemplateSettings(template, values) {
	/** @type {Record<string, any>} */
	const settings = {};
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
