<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call, getUrl } from '#lib/utils/service.js';
	import { onMount } from 'svelte';
	import { studioActivity } from './studioActivity.svelte.js';

	/**
	 * The Studio tutorials of the Convertigo site, as the Tutorial view of the Eclipse Studio: each step
	 * declares the controls the Studio checks, as an object created or a property set, before the tutorial
	 * goes on. The engine serves the pages sandboxed by their Content-Security-Policy, in an origin of their
	 * own away from the session of the Studio, and they talk to it with messages.
	 *
	 * @type {{ onClose?: () => void }}
	 */
	let { onClose } = $props();

	/** @type {HTMLIFrameElement | undefined} */
	let frame = $state();
	let src = `${getUrl()}studio.tutorial.Page?path=${encodeURIComponent('/studio-tutorials')}`;
	/** @type {any[]} */
	let controls = $state([]);
	let passed = $state(false);
	/** @type {{ url: string, video: boolean } | null} */
	let media = $state(null);
	/** @type {ReturnType<typeof setTimeout> | undefined} */
	let timer;

	/**
	 * @returns {{ previewProject: string, previewUrl: string }} the application previewed now
	 */
	function preview() {
		try {
			const previewFrame = /** @type {HTMLIFrameElement | null} */ (
				document.querySelector('iframe.studio-preview__frame')
			);
			const url = String(previewFrame?.contentWindow?.location.href ?? '');
			const project = /\/projects\/([^/]+)\//.exec(url)?.[1] ?? '';
			if (url && project) {
				return { previewProject: project, previewUrl: url };
			}
		} catch {
			// the preview shows another site
		}
		return {
			previewProject: studioActivity.previewProject,
			previewUrl: studioActivity.previewUrl
		};
	}

	async function check() {
		clearTimeout(timer);
		const current = controls;
		if (!current.length || passed) {
			return;
		}
		const result = await call('studio.tutorial.Check', {
			controls: JSON.stringify(current),
			lastDeployment: studioActivity.lastDeployment,
			lastLink: studioActivity.lastLink,
			...preview()
		});
		if (current !== controls) {
			return;
		}
		if (result?.ok) {
			passed = true;
			frame?.contentWindow?.postMessage({ c8oTutorial: 'next' }, '*');
		} else {
			timer = setTimeout(check, 1000);
		}
	}

	/**
	 * @param {MessageEvent} event
	 */
	function receive(event) {
		if (!frame || event.source !== frame.contentWindow || !event.data?.c8oTutorial) {
			return;
		}
		const message = event.data.c8oTutorial;
		if (message.type === 'control') {
			try {
				const next = JSON.parse(String(message.json ?? '[]'));
				controls = Array.isArray(next) ? next : [];
			} catch {
				controls = [];
			}
			passed = false;
			void check();
		} else if (message.type === 'imgEnter' && typeof message.url === 'string') {
			media = { url: message.url, video: /\.(mp4|webm)(\?|$)/i.test(message.url) };
		}
	}

	onMount(() => {
		window.addEventListener('message', receive);
		return () => {
			window.removeEventListener('message', receive);
			clearTimeout(timer);
		};
	});
</script>

<div
	class="studio-tutorials"
	role="dialog"
	aria-modal="true"
	aria-labelledby="studio-tutorials-title"
>
	<header class="studio-tutorials__header">
		<strong id="studio-tutorials-title">Tutorials</strong>
		<span class="studio-tutorials__status" role="status">
			{#if controls.length}
				{#if passed}
					<Ico icon="mdi:check" size={4} /> Step done, go on
				{:else}
					<Ico icon="mdi:sync" size={4} /> Waiting for the step to be done in the Studio
				{/if}
			{/if}
		</span>
		<button type="button" class="button-secondary" onclick={() => onClose?.()}>
			<Ico icon="mdi:close" size={4} /> Close
		</button>
	</header>
	<iframe bind:this={frame} title="Convertigo Studio tutorials" {src}></iframe>
	{#if media}
		<div class="studio-tutorials__media" role="presentation" onclick={() => (media = null)}>
			{#if media.video}
				<!-- svelte-ignore a11y_media_has_caption -->
				<video src={media.url} controls autoplay></video>
			{:else}
				<img src={media.url} alt="Tutorial illustration" />
			{/if}
		</div>
	{/if}
</div>

<style>
	.studio-tutorials {
		position: fixed;
		top: 3%;
		right: 2%;
		bottom: 3%;
		z-index: 80;
		display: grid;
		width: min(46rem, 96%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-tutorials__header {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.6rem 0.8rem;
	}

	.studio-tutorials__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
	}

	.studio-tutorials__header button {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
	}

	.studio-tutorials__status {
		display: inline-flex;
		flex: 1;
		align-items: center;
		gap: 0.3rem;
		color: var(--studio-text-idle);
		font-size: 0.75rem;
	}

	iframe {
		width: 100%;
		height: 100%;
		border: 0;
		background: white;
	}

	.studio-tutorials__media {
		position: absolute;
		inset: 0;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 70%, transparent);
		padding: 1rem;
	}

	.studio-tutorials__media video,
	.studio-tutorials__media img {
		max-width: 100%;
		max-height: 100%;
	}
</style>
