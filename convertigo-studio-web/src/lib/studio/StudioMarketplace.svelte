<script>
	import Projects from '$lib/common/Projects.svelte.js';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';

	/**
	 * The Convertigo Marketplace, as the Marketplace view of the Eclipse Studio: the site shows the
	 * libraries and starters, and asks the Studio to install them, which imports their project. The path of
	 * a project the site copies can also be imported here.
	 *
	 * @type {{ onInstalled?: (projectName: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { onInstalled, onClose } = $props();

	const MARKETPLACE_URL = 'https://marketplace.convertigo.com/';
	const origin = new URL(MARKETPLACE_URL).origin;

	/** @type {HTMLIFrameElement | undefined} */
	let frame = $state();
	let src = $state(MARKETPLACE_URL);
	let installing = $state('');
	let projectPath = $state('');

	/**
	 * @param {Record<string, any>} message
	 */
	function post(message) {
		frame?.contentWindow?.postMessage(message, origin);
	}

	/**
	 * @param {string} name
	 */
	function installed(name) {
		return (Projects.projects ?? []).find((project) => project?.name === name) ?? null;
	}

	/**
	 * @param {string} url the project to import, as "Name=git or archive URL"
	 */
	async function install(url) {
		installing = url.split('=')[0];
		try {
			const result = await call('projects.ImportURL', { url });
			const project = String(result?.admin?.success ?? '').match(/project '([^']+)'/)?.[1];
			if (project) {
				await Projects.refresh();
			}
			post({
				type: 'postInstall',
				installed: Boolean(project),
				project,
				version: project ? (installed(project)?.version ?? '') : ''
			});
			if (project) {
				await onInstalled?.(project);
			}
		} finally {
			installing = '';
		}
	}

	onMount(() => {
		/**
		 * @param {MessageEvent} event
		 */
		function onMessage(event) {
			if (event.origin !== origin || event.source !== frame?.contentWindow) {
				return;
			}
			const data = event.data;
			if (data?.type === 'install' && typeof data.url === 'string') {
				void install(data.url);
			} else if (data?.type === 'get' && typeof data.project === 'string') {
				const project = installed(data.project);
				post({
					type: 'postGet',
					project: data.project,
					installed: Boolean(project),
					version: project?.version ?? ''
				});
			}
		}
		window.addEventListener('message', onMessage);
		return () => window.removeEventListener('message', onMessage);
	});
</script>

<div class="studio-marketplace" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-marketplace__box"
		role="dialog"
		aria-modal="true"
		aria-label="Marketplace"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-marketplace__header">
			<Ico icon="mdi:store-outline" size={4} />
			<strong>Marketplace</strong>
			<form
				class="studio-marketplace__import"
				onsubmit={async (event) => {
					event.preventDefault();
					if (projectPath.trim() && !installing) {
						await install(projectPath.trim());
						projectPath = '';
					}
				}}
			>
				<input
					class="input-common"
					aria-label="Project path"
					placeholder="Paste the project path copied from the marketplace"
					bind:value={projectPath}
				/>
				<button
					type="submit"
					class="button-secondary"
					disabled={!projectPath.trim() || Boolean(installing)}>Import</button
				>
			</form>
			{#if installing}
				<span class="studio-marketplace__status"
					><Ico icon="mdi:sync" size={4} /> Installing {installing}…</span
				>
			{/if}
			<button
				type="button"
				class="studio-marketplace__icon"
				title="Home"
				aria-label="Home"
				onclick={() => (src = `${MARKETPLACE_URL}?home=${Date.now()}`)}
				><Ico icon="mdi:home-outline" size={4} /></button
			>
			<a
				class="studio-marketplace__icon"
				href={MARKETPLACE_URL}
				target="_blank"
				rel="noopener"
				title="Open in a new tab"
				aria-label="Open in a new tab"><Ico icon="mdi:open-in-new" size={4} /></a
			>
			<button
				type="button"
				class="studio-marketplace__icon"
				title="Close"
				aria-label="Close"
				onclick={() => onClose?.()}><Ico icon="mdi:close" size={4} /></button
			>
		</header>
		<!-- the site talks to its parent with messages; the "init" of the Eclipse Studio would make it call
		     window.java instead, which a frame has not -->
		<iframe bind:this={frame} {src} title="Convertigo Marketplace" allow="clipboard-write"></iframe>
	</div>
</div>

<style>
	.studio-marketplace {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1.5rem;
	}

	.studio-marketplace__box {
		display: grid;
		width: min(78rem, 100%);
		height: min(52rem, 100%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-marketplace__header {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.5rem 0.6rem 0.5rem 1rem;
	}

	.studio-marketplace__import {
		display: flex;
		min-width: 0;
		flex: 1;
		justify-content: flex-end;
		gap: 0.4rem;
		margin: 0 0.4rem;
	}

	.studio-marketplace__import input {
		width: min(26rem, 100%);
		min-width: 0;
		height: 1.8rem;
		padding-block: 0;
		padding-inline: 0.55rem;
		font-size: 0.75rem;
	}

	.studio-marketplace__import button {
		height: 1.8rem;
		padding-block: 0;
		font-size: 0.75rem;
	}

	.studio-marketplace__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-marketplace__status {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		color: var(--studio-text-idle);
		font-size: 0.75rem;
	}

	.studio-marketplace__icon {
		display: inline-grid;
		width: 1.8rem;
		height: 1.8rem;
		place-items: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0;
	}

	.studio-marketplace__icon:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	iframe {
		width: 100%;
		height: 100%;
		border: 0;
		background: white;
	}
</style>
