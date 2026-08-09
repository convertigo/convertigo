<script>
	import { call } from '$lib/utils/service';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/**
	 * @typedef {{
	 *  id: string,
	 *  propertyName?: string,
	 *  displayName?: string,
	 *  value?: any,
	 *  serial?: number
	 * }} PickerTarget
	 */
	/**
	 * @type {{
	 *  active?: boolean,
	 *  pickerTarget?: PickerTarget | null,
	 *  onApply?: (id: string, value?: any) => void | Promise<void>
	 * }}
	 */
	let { active = true, pickerTarget = null, onApply = () => {} } = $props();

	let frame = $state(/** @type {HTMLIFrameElement | null} */ (null));
	let payload = $state(/** @type {any} */ (null));
	let loading = $state(false);
	let applying = $state(false);
	let error = $state('');
	let bridgeId = $state('');
	let loadSerial = 0;

	$effect(() => {
		function onMessage(event) {
			if (
				event.source !== frame?.contentWindow ||
				event.data?.channel !== 'convertigo-flow-picker' ||
				event.data?.bridgeId !== bridgeId
			) {
				return;
			}
			void receive(String(event.data.message ?? ''));
		}
		window.addEventListener('message', onMessage);
		return () => window.removeEventListener('message', onMessage);
	});

	$effect(() => {
		const target = pickerTarget;
		if (!active || !target?.id || !target?.propertyName) {
			payload = null;
			return;
		}
		const serial = ++loadSerial;
		void loadEditor(target, serial);
	});

	/**
	 * @param {PickerTarget} target
	 * @param {number} serial
	 */
	async function loadEditor(target, serial) {
		loading = true;
		error = '';
		try {
			const response = await call('studio.flowpicker.Get', {
				id: target.id,
				propertyName: target.propertyName,
				value: editorValue(target.value)
			});
			if (serial !== loadSerial) {
				return;
			}
			if (!response?.html || !response?.state) {
				throw new Error(response?.message || 'Flow picker is not available');
			}
			bridgeId = crypto.randomUUID();
			payload = response;
		} catch (err) {
			if (serial === loadSerial) {
				payload = null;
				error = String(err instanceof Error ? err.message : err);
			}
		} finally {
			if (serial === loadSerial) {
				loading = false;
			}
		}
	}

	/**
	 * @param {string} message
	 */
	async function receive(message) {
		try {
			const event = JSON.parse(message || '{}');
			if (event.type === 'copy') {
				await navigator.clipboard?.writeText(String(event.value ?? ''));
				return;
			}
			if (event.type === 'openExternal' && /^https?:\/\//i.test(String(event.url ?? ''))) {
				window.open(String(event.url), '_blank', 'noopener,noreferrer');
				return;
			}
			if (event.type === 'setProperty') {
				await applyProperty(String(event.property ?? ''), String(event.value ?? ''));
			}
		} catch (err) {
			error = String(err instanceof Error ? err.message : err);
		}
	}

	/**
	 * @param {string} property
	 * @param {string} value
	 */
	async function applyProperty(property, value) {
		if (applying || !pickerTarget?.id || !property) {
			return;
		}
		applying = true;
		error = '';
		try {
			const originalValue = editorValue(payload?.state?.definition?.[property]);
			const response = await call('studio.properties.Set', {
				id: pickerTarget.id,
				props: JSON.stringify([{ name: property, value, originalValue }]),
				save: 'true'
			});
			if (!response?.done) {
				throw new Error(response?.message || 'Unable to apply Flow property');
			}
			payload = {
				...payload,
				state: {
					...payload.state,
					applied: { property, value }
				}
			};
			payload.state.definition[property] = parseEditorValue(value);
			await onApply?.(response.id || pickerTarget.id, value);
		} catch (err) {
			error = String(err instanceof Error ? err.message : err);
		} finally {
			applying = false;
		}
	}

	/** @param {any} value */
	function editorValue(value) {
		if (value == null) {
			return '';
		}
		return typeof value === 'string' ? value : JSON.stringify(value);
	}

	/** @param {string} value */
	function parseEditorValue(value) {
		try {
			return JSON.parse(value);
		} catch {
			return value;
		}
	}

	/**
	 * @param {any} editorPayload
	 * @param {string} id
	 */
	function frameDocument(editorPayload, id) {
		if (!editorPayload?.html || !editorPayload?.state || !id) {
			return '';
		}
		const data = safeScriptJson({
			bridgeId: id,
			state: editorPayload.state,
			requests: editorPayload.requests ?? {}
		});
		const bootstrap = `<script>(function () {
			var data = ${data};
			function reply(message) {
				window.parent.postMessage({ channel: 'convertigo-flow-picker', bridgeId: data.bridgeId, message: message }, '*');
			}
			window.flowEditor = {
				receive: reply,
				request: function (message) {
					try {
						var request = JSON.parse(message || '{}');
						var name = String(request.name || '');
						if (data.requests[name]) return JSON.stringify(data.requests[name]);
						if (name === 'bindingSources') {
							var property = request.payload && request.payload.property || data.state.property || '';
							var definitions = data.state.info && data.state.info.propertyDefinitions || {};
							var sources = definitions[property] && definitions[property].bindingSources || [];
							return JSON.stringify({ ok: true, bindingSources: sources });
						}
						return JSON.stringify({ ok: false, error: 'Unsupported Flow picker request: ' + name });
					} catch (error) {
						return JSON.stringify({ ok: false, error: String(error) });
					}
				}
			};
			if (typeof window.receiveFromJava === 'function') window.receiveFromJava(data.state);
		}());<\/script>`;
		return editorPayload.html.includes('</body>')
			? editorPayload.html.replace('</body>', `${bootstrap}</body>`)
			: `${editorPayload.html}${bootstrap}`;
	}

	/** @param {any} value */
	function safeScriptJson(value) {
		return JSON.stringify(value)
			.replaceAll('<', '\\u003c')
			.replaceAll('\u2028', '\\u2028')
			.replaceAll('\u2029', '\\u2029');
	}
</script>

<div class="studio-flow-picker">
	{#if loading}
		<StudioEmptyState message="Loading Flow picker" loading />
	{:else if error && !payload}
		<StudioEmptyState message={error} icon="mdi:alert-circle-outline" />
	{:else if payload?.html}
		<iframe
			bind:this={frame}
			class="studio-flow-picker__frame"
			title={`Flow picker for ${pickerTarget?.displayName || pickerTarget?.propertyName || 'property'}`}
			sandbox="allow-scripts"
			srcdoc={frameDocument(payload, bridgeId)}
		></iframe>
		{#if applying}
			<div class="studio-flow-picker__status" aria-live="polite">Applying…</div>
		{:else if error}
			<div class="studio-flow-picker__status studio-flow-picker__status--error" role="alert">
				{error}
			</div>
		{/if}
	{:else}
		<StudioEmptyState message="No Flow property selected" />
	{/if}
</div>

<style>
	.studio-flow-picker {
		position: relative;
		display: grid;
		height: 100%;
		min-height: 0;
		background: var(--color-surface-950-50);
	}

	.studio-flow-picker__frame {
		width: 100%;
		height: 100%;
		min-height: 18rem;
		border: 0;
		background: #1f2327;
	}

	.studio-flow-picker__status {
		position: absolute;
		right: 0.6rem;
		bottom: 0.6rem;
		max-width: calc(100% - 1.2rem);
		border: 1px solid color-mix(in oklab, var(--color-primary-500) 48%, transparent);
		border-radius: 0.45rem;
		background: color-mix(in oklab, var(--color-surface-950) 92%, transparent);
		color: var(--color-surface-50);
		padding: 0.45rem 0.65rem;
		font-size: 0.72rem;
		box-shadow: 0 0.5rem 1.4rem color-mix(in oklab, black 24%, transparent);
	}

	.studio-flow-picker__status--error {
		border-color: color-mix(in oklab, var(--color-error-500) 62%, transparent);
		color: var(--color-error-200);
	}
</style>
