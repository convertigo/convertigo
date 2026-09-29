<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';

	/**
	 * The pictures of an application, as the Capture Manager of the Eclipse Studio: the capture of the
	 * preview, or a picture chosen, is saved as the thumbnail of the application, for the dashboard and the
	 * Marketplace, or as a screen of its Marketplace detail.
	 *
	 * @type {{
	 *  projectName: string,
	 *  capture?: string,
	 *  error?: string,
	 *  onCapture?: () => void | Promise<void>,
	 *  onClose?: () => void
	 * }}
	 */
	let { projectName, capture: captured = '', error = '', onCapture, onClose } = $props();

	const LABELS = {
		thumbnail: 'Thumbnail',
		screen1: 'Screen 1',
		screen2: 'Screen 2',
		screen3: 'Screen 3'
	};

	/** @type {{ slot: keyof typeof LABELS, file: string, data?: string }[]} */
	let captures = $state([]);
	let picture = $state('');
	let busy = $state(false);
	/** a file chosen that is not a picture */
	let chooseError = $state('');

	$effect.pre(() => {
		picture = captured;
	});

	$effect(() => {
		void run({});
	});

	/**
	 * @param {Record<string, string>} params
	 */
	async function run(params) {
		busy = true;
		try {
			const result = await call('studio.ngxbuilder.Captures', { project: projectName, ...params });
			if (Array.isArray(result?.captures)) {
				captures = result.captures;
			}
		} finally {
			busy = false;
		}
	}

	/**
	 * @param {Event & { currentTarget: HTMLInputElement }} event
	 */
	async function choose(event) {
		const file = event.currentTarget.files?.[0];
		if (!file) {
			return;
		}
		try {
			picture = await fitPicture(file);
			chooseError = '';
		} catch {
			picture = '';
			chooseError = `${file.name} is not a picture.`;
		}
	}

	/** the longest side of a picture chosen, which keeps it under the 2 MB a request of the Engine takes */
	const MAX_SIDE = 1600;
	const MAX_DATA = 1_500_000;

	/**
	 * Reads a picture chosen, reduced to fit the request that saves it: a photo would exceed it.
	 * @param {File} file
	 * @returns {Promise<string>} the data URL of the picture
	 */
	async function fitPicture(file) {
		const bitmap = await createImageBitmap(file);
		const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
		const canvas = document.createElement('canvas');
		canvas.width = Math.round(bitmap.width * scale);
		canvas.height = Math.round(bitmap.height * scale);
		canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
		bitmap.close();
		// a PNG keeps its transparency when it fits, the others become JPEG
		const png = file.type === 'image/png' ? canvas.toDataURL('image/png') : '';
		if (png && png.length <= MAX_DATA) {
			return png;
		}
		for (const quality of [0.9, 0.75, 0.6]) {
			const jpeg = canvas.toDataURL('image/jpeg', quality);
			if (jpeg.length <= MAX_DATA || quality === 0.6) {
				return jpeg;
			}
		}
		return '';
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => !busy && onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-capture-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-capture-title">Pictures of {projectName}</strong>
		</header>
		<div class="studio-dialog__body">
			<p class="studio-dialog__hint">
				Save the capture as the thumbnail of the application, for the dashboard and the Marketplace,
				or as a screen of its Marketplace detail.
			</p>
			<section class="studio-capture__new" aria-label="New picture">
				{#if picture}
					<img src={picture} alt="Capture of the application" />
				{:else}
					<div class="studio-capture__empty">
						<Ico icon="mdi:image-outline" size={8} />
						<span>{chooseError || error || 'No capture yet.'}</span>
					</div>
				{/if}
				<div class="studio-capture__sources">
					{#if onCapture}
						<button
							type="button"
							class="button-secondary"
							disabled={busy}
							onclick={() => onCapture()}
							><Ico icon="mdi:camera-outline" size={4} /> Capture the preview</button
						>
					{/if}
					<label class="studio-capture__file button-secondary">
						<Ico icon="mdi:image-outline" size={4} /> Choose a picture
						<input type="file" accept="image/*" onchange={choose} />
					</label>
				</div>
			</section>
			<div class="studio-capture__slots">
				{#each captures as capture (capture.slot)}
					<section class="studio-capture__slot" aria-label={LABELS[capture.slot]}>
						<strong>{LABELS[capture.slot]}</strong>
						{#if capture.data}
							<img src={capture.data} alt={`${LABELS[capture.slot]} of the application`} />
						{:else}
							<div class="studio-capture__empty"><span>Empty</span></div>
						{/if}
						<small class="studio-ellipsis" title={capture.file}>{capture.file}</small>
						<button
							type="button"
							class="button-primary"
							disabled={busy || !picture}
							onclick={() => run({ action: 'save', slot: capture.slot, data: picture })}
							>Save as {LABELS[capture.slot].toLowerCase()}</button
						>
						<button
							type="button"
							class="button-secondary"
							disabled={busy || !capture.data}
							onclick={() => run({ action: 'delete', slot: capture.slot })}
							><Ico icon="mdi:delete-outline" size={4} /> Delete</button
						>
					</section>
				{/each}
			</div>
		</div>
		<footer class="studio-dialog__footer">
			<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
				>Close</button
			>
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
		width: min(52rem, 100%);
		max-height: 100%;
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
		justify-content: flex-start;
		border-bottom: 1px solid var(--studio-line);
	}

	.studio-dialog__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-dialog__footer {
		border-top: 1px solid var(--studio-line);
	}

	.studio-dialog__body {
		display: grid;
		min-height: 0;
		gap: 0.9rem;
		overflow-y: auto;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__hint {
		margin: 0;
		color: var(--studio-text-idle);
	}

	.studio-capture__new {
		display: grid;
		justify-items: center;
		gap: 0.6rem;
	}

	.studio-capture__new img {
		max-width: 100%;
		max-height: 16rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
	}

	.studio-capture__sources {
		display: flex;
		flex-wrap: wrap;
		justify-content: center;
		gap: 0.5rem;
	}

	.studio-capture__sources button,
	.studio-capture__file,
	.studio-capture__slot button {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		gap: 0.35rem;
	}

	.studio-capture__file {
		position: relative;
		cursor: pointer;
	}

	.studio-capture__file input {
		position: absolute;
		inset: 0;
		opacity: 0;
		cursor: pointer;
	}

	.studio-capture__empty {
		display: grid;
		place-items: center;
		gap: 0.3rem;
		min-height: 6rem;
		width: 100%;
		border: 1px dashed var(--studio-line);
		border-radius: 0.35rem;
		color: var(--studio-text-idle);
		padding: 0.5rem;
		text-align: center;
	}

	.studio-capture__slots {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr));
		gap: 0.75rem;
	}

	.studio-capture__slot {
		display: grid;
		align-content: start;
		gap: 0.4rem;
		min-width: 0;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
		padding: 0.5rem;
	}

	.studio-capture__slot img {
		width: 100%;
		height: 8rem;
		object-fit: contain;
		background: var(--studio-hover-bg);
		border-radius: 0.3rem;
	}

	.studio-capture__slot .studio-capture__empty {
		height: 8rem;
	}

	.studio-capture__slot small {
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}
</style>
