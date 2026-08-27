import { describe, expect, it } from 'vitest';
import {
	blockDefinitionForInstance,
	blockDefinitionSourceId,
	findBlockDefinition,
	flowTypeDisplayName,
	isFrontendBlockDefinitionSourceId,
	objectPropertyValue,
	propertyDocumentationFromDefinition,
	propertyDocumentationFromProperties
} from './blockDefinition';

const properties = {
	Summary: { name: 'summary', value: '@local.copy.details.datetime.startDate' },
	'Flow type': { name: 'virtualType', value: 'DatePicker' },
	'Java class': {
		name: 'P_JavaClass',
		value: 'com.twinsoft.convertigo.beans.flow.FlowVirtualObject'
	}
};

describe('Flow block definition identity', () => {
	it('matches the public palette definition from a frontend Flow type', () => {
		const datePicker = {
			id: 'frontendblock:svelte.datePicker',
			name: 'Date picker',
			classname: 'svelte.datePicker'
		};
		const text = { id: 'frontendblock:svelte.text', name: 'Text', classname: 'svelte.text' };
		expect(
			findBlockDefinition([{ name: 'Forms', items: [text, datePicker] }], {
				flowType: objectPropertyValue(properties, 'Flow type', 'virtualType'),
				javaClass: objectPropertyValue(properties, 'Java class', 'P_JavaClass')
			})
		).toBe(datePicker);
	});

	it('does not confuse similarly prefixed widget types', () => {
		const text = { id: 'frontendblock:svelte.text', name: 'Text', classname: 'svelte.text' };
		const textTrim = {
			id: 'frontendblock:svelte.textTrim',
			name: 'Text trim',
			classname: 'svelte.textTrim'
		};
		expect(findBlockDefinition([{ items: [text, textTrim] }], { flowType: 'TextTrim' })).toBe(
			textTrim
		);
	});

	it('keeps the palette name primary and the instance summary secondary', () => {
		const documentedProperties = {
			...properties,
			Value: {
				displayName: 'Value',
				category: 'Base properties',
				shortDescription: 'Current date selected by the user.'
			},
			Path: {
				displayName: 'Path',
				category: 'Information',
				shortDescription: 'Technical source path.'
			}
		};
		const identity = blockDefinitionForInstance(
			{ id: 'frontendblock:svelte.datePicker', name: 'Date picker' },
			'sample.FlowEngine.frontends.authoring_startDatePicker',
			documentedProperties
		);
		expect(identity.name).toBe('Date picker');
		expect(identity.instanceName).toBe('@local.copy.details.datetime.startDate');
		expect(identity.flowType).toBe('DatePicker');
		expect(identity.propertyDocumentation).toEqual([
			{ label: 'Value', description: 'Current date selected by the user.' }
		]);
		expect(identity.isBlockDefinition).toBe(true);
	});

	it('keeps descriptor documentation instead of duplicating instance properties', () => {
		const item = {
			id: 'frontendblock:svelte.datePicker',
			name: 'Date picker',
			propertiesDescriptionHtml: '<ul><li>Value</li></ul>'
		};
		const identity = blockDefinitionForInstance(item, 'sample.DatePicker', {
			Value: { shortDescription: 'Current date selected by the user.' }
		});
		expect(identity.propertyDocumentation).toEqual([]);
	});

	it('projects documented public properties while excluding technical information', () => {
		expect(
			propertyDocumentationFromProperties({
				Value: { shortDescription: 'Selected value.' },
				Path: { category: 'Information', shortDescription: 'Technical path.' }
			})
		).toEqual([{ label: 'Value', description: 'Selected value.' }]);
	});

	it('restores public docs directly from a lightweight Library definition', () => {
		expect(
			propertyDocumentationFromDefinition({
				properties: {
					name: 'properties',
					value: JSON.stringify({
						value: { label: 'Value', description: 'Selected date.' },
						id: { label: 'Id', description: 'Technical id.', hidden: true }
					})
				}
			})
		).toEqual([{ label: 'Value', description: 'Selected date.' }]);
		expect(
			isFrontendBlockDefinitionSourceId(
				'library.FlowEngine.frontends.builder.catalog.provider_library.namespace_svelte.uiBlocks.block_svelte_datePicker'
			)
		).toBe(true);
		expect(
			isFrontendBlockDefinitionSourceId('sample.FlowEngine.frontends.builder.routes.home')
		).toBe(false);
	});

	it('resolves a safe provider definition into the Studio library tree', () => {
		expect(
			blockDefinitionSourceId({
				sourceProject: 'lib_flow_frontbuilder_svelte',
				sourceDefinitionPath:
					'frontends.builder_svelte.catalog.provider_lib_flow_frontbuilder_svelte.namespace_svelte.uiBlocks.block_svelte_datePicker'
			})
		).toBe(
			'lib_flow_frontbuilder_svelte.FlowEngine.frontends.builder_svelte.catalog.provider_lib_flow_frontbuilder_svelte.namespace_svelte.uiBlocks.block_svelte_datePicker'
		);
		expect(
			blockDefinitionSourceId({ sourceProject: 'library', sourceDefinitionPath: '../secret' })
		).toBe('');
	});

	it('humanizes stable provider types while the descriptor is loading', () => {
		expect(flowTypeDisplayName('DatePicker')).toBe('Date picker');
		expect(flowTypeDisplayName('OTPInput')).toBe('OTP input');
		expect(flowTypeDisplayName('HugeRTE')).toBe('Huge RTE');
	});
});
