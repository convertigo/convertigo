import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import { describe, expect, it } from 'vitest';

const source = readFileSync(
	new URL(
		'../../../../eclipse-plugin-studio/tomcat/webapps/convertigo/swagger/patch/swagger-initializer.js',
		import.meta.url
	),
	'utf8'
);

function configuration() {
	let config;
	const bundle = (value) => {
		config = value;
		return {};
	};
	bundle.presets = { apis: 'apis' };
	bundle.plugins = { DownloadUrl: 'download' };
	const window = {};
	runInNewContext(source, {
		window,
		SwaggerUIBundle: bundle,
		SwaggerUIStandalonePreset: 'standalone'
	});
	window.onload();
	return config;
}

function selector(options = {}) {
	const responses = options.responses ?? {
		200: { content: options.content ?? { 'application/json': {}, 'application/xml': {} } }
	};
	const calls = [];
	const original = (...args) => {
		calls.push(args);
		return options.selected ?? null;
	};
	const system = {
		specSelectors: {
			isOAS3: () => options.oas3 !== false,
			specJsonWithResolvedSubtrees: () => ({
				getIn: (path) => {
					if (
						options.missing ||
						path[0] !== 'paths' ||
						path[1] !== '/sample/{id}' ||
						path[2] !== 'post' ||
						path[3] !== 'responses'
					) {
						return undefined;
					}
					return { toJS: () => responses };
				}
			}),
			currentProducesFor: () => options.displayed ?? 'application/json'
		},
		oas3Selectors: { responseCodeContentType: () => options.perCode }
	};
	const plugin = configuration().plugins[1]();
	const wrapped = plugin.statePlugins.oas3.wrapSelectors.responseContentType(original, system);
	return {
		run: (state = {}, path = '/sample/{id}', method = 'post') => wrapped(state, path, method),
		calls
	};
}

describe('Swagger response media type fallback', () => {
	it('registers the plugin without changing the other console settings', () => {
		const config = configuration();
		expect(config.plugins[0]).toBe('download');
		expect(config.presets).toEqual(['apis', 'standalone']);
		expect(config.queryConfigEnabled).toBe(true);
		expect(config.requestInterceptor).toBeUndefined();
	});

	it('uses the displayed JSON default before any change event', () => {
		expect(selector().run()).toBe('application/json');
	});

	it.each(['application/json', 'application/xml', '*/*', 'application/*', 'text/plain;q=0.8'])(
		'preserves an explicit selection: %s',
		(selected) => expect(selector({ selected }).run()).toBe(selected)
	);

	it('forwards the state and operation to the original selector', () => {
		const state = {};
		const tested = selector();
		tested.run(state);
		expect(tested.calls).toEqual([[state, '/sample/{id}', 'post']]);
	});

	it('leaves Swagger 2 untouched', () => expect(selector({ oas3: false }).run()).toBeNull());
	it('leaves an unknown operation untouched', () =>
		expect(selector({ missing: true }).run()).toBeNull());
	it('does not confuse methods', () =>
		expect(selector().run({}, '/sample/{id}', 'get')).toBeNull());
	it('does not confuse operation paths', () =>
		expect(selector().run({}, '/other', 'post')).toBeNull());
	it('supports an absent response map', () => expect(selector({ missing: true }).run()).toBeNull());
	it('supports an empty response map', () => expect(selector({ responses: {} }).run()).toBeNull());
	it('supports a response without a value', () =>
		expect(selector({ responses: { 200: null } }).run()).toBeNull());
	it('does not infer content for 204-only responses', () =>
		expect(selector({ responses: { 204: {} } }).run()).toBeNull());
	it('does not use an error response as the default', () => {
		expect(
			selector({ responses: { 400: { content: { 'application/json': {} } } } }).run()
		).toBeNull();
	});
	it('uses the documented default response if no successful response declares content', () => {
		expect(
			selector({ responses: { 204: {}, default: { content: { 'text/plain': {} } } } }).run()
		).toBe('text/plain');
	});
	it('prefers a successful response to the documented default', () => {
		expect(
			selector({
				responses: {
					200: { content: { 'application/xml': {} } },
					default: { content: { 'text/plain': {} } }
				}
			}).run()
		).toBe('application/xml');
	});
	it.each(['application/xml', 'application/octet-stream', 'application/vnd.demo+json'])(
		'does not force JSON for a response declaring only %s',
		(type) => expect(selector({ content: { [type]: {} } }).run()).toBe(type)
	);
	it('uses the first declared type if the displayed value is not available', () => {
		expect(selector({ content: { 'text/plain': {}, 'application/xml': {} } }).run()).toBe(
			'text/plain'
		);
	});
	it('uses the displayed JSON choice even when XML is declared first', () => {
		expect(selector({ content: { 'application/xml': {}, 'application/json': {} } }).run()).toBe(
			'application/json'
		);
	});
	it('honors a valid per-response selection before the displayed default', () => {
		expect(selector({ perCode: 'application/xml' }).run()).toBe('application/xml');
	});
	it('ignores a stale per-response value outside the declared media types', () => {
		expect(selector({ perCode: 'text/plain' }).run()).toBe('application/json');
	});
	it('does not modify the response declaration', () => {
		const responses = { 200: { content: { 'application/json': {}, 'application/xml': {} } } };
		const before = structuredClone(responses);
		selector({ responses }).run();
		expect(responses).toEqual(before);
	});
});
