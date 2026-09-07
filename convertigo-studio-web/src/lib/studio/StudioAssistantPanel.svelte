<script>
	import { browser } from '$app/environment';
	import Light from '$lib/common/Light.svelte';
	import { getFrontendUrl } from '$lib/utils/service';
	import { untrack } from 'svelte';

	/** @type {{ projectName?: string, agentProfile?: string }} */
	let { projectName = '', agentProfile = 'generalist' } = $props();

	const assistantProject = 'lib_ConvertigoAssistant';
	const assistantContextType = `${assistantProject}.context`;
	const assistantContextRequestType = `${assistantContextType}.request`;
	const assistantBaseUrl = getFrontendUrl(assistantProject).replace(/index\.html$/, '');
	const assistantAgentUrl = `${assistantBaseUrl}path-to-xfirst/:threadid`;
	let iframe = $state();
	let iframeReady = $state(false);
	// Bootstrap only: selection/profile updates must never navigate the iframe.
	// Keep the initial theme for Assistants that only read it at startup.
	const assistantUrl = (() => {
		const query = new URLSearchParams({
			agentBridge: '1',
			serverAgent: '1',
			assistantMode: 'agent',
			assistantSurface: 'studio',
			assistantContext: 'studio',
			userId: 'studio',
			'dark-theme': String(untrack(() => Light.dark))
		});
		return `${assistantAgentUrl}?${query}`;
	})();
	let assistantContext = $derived({
		assistantSurface: 'studio',
		assistantContext: 'studio',
		assistantRuntime: 'server',
		agentBridgeAvailable: true,
		localAgentBridgeAvailable: false,
		agentProfile,
		skillProfile: agentProfile,
		userId: 'studio',
		darkTheme: Light.dark,
		theme: Light.dark ? 'dark' : 'light',
		projectContext: projectName,
		defaultProject: projectName,
		projectScope: projectName ? 'selected' : '',
		currentUrl: browser ? window.location.href : '',
		currentRoute: browser ? window.location.pathname : ''
	});

	/**
	 * @param {Record<string, unknown>} message
	 */
	function postAssistantMessage(message) {
		if (!browser || !iframe?.contentWindow) {
			return;
		}
		iframe.contentWindow.postMessage(message, new URL(assistantUrl, window.location.href).origin);
	}

	function sendAssistantContext() {
		const context = assistantContext;
		postAssistantMessage({ type: assistantContextType, payload: context });
		postAssistantMessage({
			type: 'select',
			projectName,
			...context
		});
	}

	function onAssistantLoad() {
		iframeReady = true;
		postAssistantMessage({ type: 'init' });
		sendAssistantContext();
	}

	/**
	 * @param {HTMLIFrameElement} node
	 */
	function assistantFrame(node) {
		iframe = node;
		return () => {
			if (iframe === node) {
				iframe = undefined;
				iframeReady = false;
			}
		};
	}

	/**
	 * @param {Record<string, string | boolean>} context
	 * @param {boolean} ready
	 * @param {string} url
	 * @returns {import('svelte/attachments').Attachment<HTMLIFrameElement>}
	 */
	function synchronizeAssistant(context, ready, url) {
		return (node) => {
			if (!browser || !ready || !node.contentWindow) {
				return;
			}
			const origin = new URL(url, window.location.href).origin;
			node.contentWindow.postMessage({ type: assistantContextType, payload: context }, origin);
			// An empty selection is an update too: clear the previous ambient project.
			node.contentWindow.postMessage(
				{ type: 'select', projectName: context.defaultProject, ...context },
				origin
			);
		};
	}

	/**
	 * @param {MessageEvent} event
	 */
	function onAssistantMessage(event) {
		if (!browser || event.source !== iframe?.contentWindow) {
			return;
		}
		const expectedOrigin = new URL(assistantUrl, window.location.href).origin;
		if (event.origin !== expectedOrigin) {
			return;
		}
		if (event.data?.type === assistantContextRequestType) {
			sendAssistantContext();
		}
	}
</script>

<svelte:window onmessage={onAssistantMessage} />

<iframe
	{@attach assistantFrame}
	{@attach synchronizeAssistant(assistantContext, iframeReady, assistantUrl)}
	src={assistantUrl}
	title="Convertigo Assistant"
	class="studio-assistant"
	referrerpolicy="strict-origin-when-cross-origin"
	onload={onAssistantLoad}
></iframe>

<style>
	.studio-assistant {
		display: block;
		width: 100%;
		height: 100%;
		min-height: 0;
		border: 0;
		background: var(--color-surface-50-950);
	}
</style>
