<script>
	import LightSvelte from '$lib/common/Light.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import Editor from './editor/Editor.svelte';

	/**
	 * The rows of a view of a design document of a CouchDB connector, as the Execute actions of a view in
	 * the Eclipse Studio run it: reduced, or mapped on its first 50 rows.
	 *
	 * @type {{ id: string, reduce?: boolean, onClose?: () => void }}
	 */
	let { id, reduce = false, onClose } = $props();

	let content = $state('');
	let title = $state('');
	let summary = $state('');
	let loading = $state(true);
	let theme = $derived(LightSvelte.light ? '' : 'vs-dark');

	async function run() {
		loading = true;
		try {
			const result = await call('studio.dbo.CouchView', { id, reduce: String(reduce) });
			if (result?.result) {
				title = `${result.view} in ${result.database}`;
				const rows = Array.isArray(result.result.rows) ? result.result.rows.length : 0;
				summary = result.reduce
					? `Reduced to ${rows} row${rows === 1 ? '' : 's'}`
					: `${rows} of ${result.result.total_rows ?? rows} rows`;
				content = JSON.stringify(result.result, null, 2);
			} else {
				title = id.split(/[.:]/).pop() ?? id;
				summary = 'The view did not run, see the message.';
				content = '';
			}
		} finally {
			loading = false;
		}
	}

	$effect(() => {
		void run();
	});
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-couch-view-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-couch-view-title">{title || 'View'}</strong>
			<span role="status">{loading ? 'Running…' : summary}</span>
		</header>
		<div class="studio-dialog__body">
			{#if content}
				<Editor {content} language="json" {theme} readOnly={true} />
			{/if}
		</div>
		<footer class="studio-dialog__footer">
			<button type="button" class="button-secondary" disabled={loading} onclick={() => void run()}>
				<Ico icon="mdi:reload" size={4} /> Run again
			</button>
			<button type="button" class="button-primary" onclick={() => onClose?.()}>Close</button>
		</footer>
	</div>
</div>

<style>
	.studio-dialog {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-dialog__box {
		display: grid;
		width: min(48rem, 100%);
		height: min(36rem, 100%);
		grid-template-rows: auto minmax(0, 1fr) auto;
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__header,
	.studio-dialog__footer {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
	}

	.studio-dialog__header {
		justify-content: space-between;
		border-bottom: 1px solid var(--studio-line);
	}

	.studio-dialog__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-dialog__header span {
		color: var(--studio-text-idle);
		font-size: 0.75rem;
	}

	.studio-dialog__footer {
		border-top: 1px solid var(--studio-line);
	}

	.studio-dialog__footer button {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-dialog__body {
		min-height: 0;
	}
</style>
