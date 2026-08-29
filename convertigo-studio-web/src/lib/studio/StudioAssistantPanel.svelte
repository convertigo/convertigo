<script>
	import { browser } from '$app/environment';
	import Light from '$lib/common/Light.svelte';
	import { getFrontendUrl } from '$lib/utils/service';

	/** @type {{ projectName?: string, agentProfile?: string }} */
	let { projectName = '', agentProfile = 'generalist' } = $props();

	const assistantProject = 'lib_ConvertigoAssistant';
	const assistantContextType = `${assistantProject}.context`;
	const assistantContextRequestType = `${assistantContextType}.request`;
	const assistantBaseUrl = getFrontendUrl(assistantProject).replace(/index\.html$/, '');
	const assistantAgentUrl = `${assistantBaseUrl}path-to-xfirst/:threadid`;
	let iframe = $state();
	let iframeReady = $state(false);
	let assistantUrl = $derived.by(() => {
		const query = new URLSearchParams({
			agentBridge: '1',
			serverAgent: '1',
			assistantMode: 'agent',
			assistantSurface: 'studio',
			assistantContext: 'studio',
			agentProfile,
			skillProfile: agentProfile,
			userId: 'studio',
			'dark-theme': String(Light.dark)
		});
		if (projectName) {
			query.set('targetProject', projectName);
		}
		return `${assistantAgentUrl}?${query}`;
	});
	let assistantContext = $derived({
		assistantSurface: 'studio',
		assistantContext: 'studio',
		agentProfile,
		skillProfile: agentProfile,
		userId: 'studio',
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
		if (projectName) {
			postAssistantMessage({
				type: 'select',
				projectName,
				...context
			});
		}
	}

	function onAssistantLoad() {
		iframeReady = true;
		sendAssistantContext();
		postAssistantMessage({ type: 'init' });
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
	 * @param {Record<string, string>} context
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
			if (context.defaultProject) {
				node.contentWindow.postMessage(
					{ type: 'select', projectName: context.defaultProject, ...context },
					origin
				);
			}
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
