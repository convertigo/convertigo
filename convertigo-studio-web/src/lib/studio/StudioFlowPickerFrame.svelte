<script>
	import Button from '$lib/admin/components/Button.svelte';
	import Light from '$lib/common/Light.svelte.js';
	import { call } from '$lib/utils/service';
	import StudioEmptyState from './StudioEmptyState.svelte';

	const FLOW_PICKER_RETRY_DELAY_MS = 160;

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
	 *  onChange?: (value: any) => void
	 * }}
	 */
	let { active = true, pickerTarget = null, onChange = () => {} } = $props();

	let frame = $state(/** @type {HTMLIFrameElement | null} */ (null));
	let payload = $state(/** @type {any} */ (null));
	let loading = $state(false);
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

	$effect(() => {
		const theme = Light.mode;
		const targetFrame = frame;
		const id = bridgeId;
		if (!targetFrame?.contentWindow || !id || !payload?.html) {
			return;
		}
		targetFrame.contentWindow.postMessage(
			{ channel: 'convertigo-flow-picker', bridgeId: id, type: 'theme', theme },
			'*'
		);
	});

	/**
	 * @param {PickerTarget} target
	 * @param {number} serial
	 */
	async function loadEditor(target, serial, attempt = 0) {
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
			payload = {
				...response,
				state: {
					...response.state,
					mode: 'property',
					embedded: true,
					value: editorValue(target.value),
					theme: Light.mode
				}
			};
		} catch (err) {
			if (
				attempt === 0 &&
				serial === loadSerial &&
				active &&
				pickerTarget?.id === target.id &&
				pickerTarget?.propertyName === target.propertyName
			) {
				await new Promise((resolve) => setTimeout(resolve, FLOW_PICKER_RETRY_DELAY_MS));
				if (
					serial === loadSerial &&
					active &&
					pickerTarget?.id === target.id &&
					pickerTarget?.propertyName === target.propertyName
				) {
					await loadEditor(target, serial, attempt + 1);
					return;
				}
			}
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

	function retryPicker() {
		const target = pickerTarget;
		if (!active || !target?.id || !target?.propertyName || loading) {
			return;
		}
		void loadEditor(target, ++loadSerial);
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
			if (event.type === 'value' && event.valid !== false) {
				onChange(String(event.value ?? ''));
			}
		} catch (err) {
			error = String(err instanceof Error ? err.message : err);
		}
	}

	/** @param {any} value */
	function editorValue(value) {
		if (value == null) {
			return '';
		}
		return typeof value === 'string' ? value : JSON.stringify(value);
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
			window.addEventListener('message', function (event) {
				var message = event.data || {};
				if (event.source !== window.parent || message.channel !== 'convertigo-flow-picker' || message.bridgeId !== data.bridgeId || message.type !== 'theme') return;
				data.state.theme = message.theme;
				if (typeof window.flowSetTheme === 'function') window.flowSetTheme(message.theme);
			});
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
		<StudioEmptyState>
			<div class="studio-flow-picker__error" role="alert">
				<span>{error}</span>
				<Button
					label="Retry"
					icon="mdi:refresh"
					class="button-secondary"
					full={false}
					onclick={retryPicker}
				/>
			</div>
		</StudioEmptyState>
	{:else if payload?.html}
		<iframe
			bind:this={frame}
			class="studio-flow-picker__frame"
			title={`Flow picker for ${pickerTarget?.displayName || pickerTarget?.propertyName || 'property'}`}
			sandbox="allow-scripts"
			srcdoc={frameDocument(payload, bridgeId)}
		></iframe>
		{#if error}
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
		background: light-dark(var(--color-surface-50), var(--color-surface-950));
	}

	.studio-flow-picker__frame {
		width: 100%;
		height: 100%;
		min-height: 18rem;
		border: 0;
		background: light-dark(var(--color-surface-50), var(--color-surface-950));
	}

	.studio-flow-picker__error {
		display: grid;
		justify-items: center;
		gap: 0.75rem;
		max-width: 28rem;
		text-align: center;
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
