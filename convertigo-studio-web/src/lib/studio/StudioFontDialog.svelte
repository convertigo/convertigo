<script>
	import { call } from '$lib/utils/service';

	/**
	 * Chooses the font of an NGX application, as the font editor of the Eclipse Studio: a font of the
	 * Fontsource catalog with its style, weight and subset, previewed, or none to inherit the font.
	 *
	 * @type {{
	 *  label?: string,
	 *  value?: string,
	 *  onApply?: (value: string) => void,
	 *  onClose?: () => void
	 * }}
	 */
	let { label = 'Font', value = '{}', onApply, onClose } = $props();

	/** @typedef {{ id: string, family: string, category?: string, type?: string, weights?: number[], styles?: string[], subsets?: string[], defSubset?: string }} Font */

	/** @type {Font[]} */
	let fonts = $state([]);
	let filter = $state('');
	let fontId = $state('');
	let weight = $state('');
	let style = $state('');
	let subset = $state('');
	/** @type {any} */
	let details = $state(null);

	$effect.pre(() => {
		let current = {};
		try {
			current = JSON.parse(value || '{}');
		} catch {
			// no font
		}
		fontId = String(current.fontId ?? '');
		weight = String(current.fontWeight ?? '');
		style = String(current.fontStyle ?? '');
		subset = String(current.fontSubset ?? '');
	});

	$effect(() => {
		void call('studio.ngxbuilder.Fonts', {}).then((result) => {
			fonts = Array.isArray(result?.fonts) ? result.fonts : [];
		});
	});

	let font = $derived(fonts.find((candidate) => candidate.id === fontId));
	let shown = $derived.by(() => {
		const text = filter.trim().toLowerCase();
		return (
			text ? fonts.filter((candidate) => candidate.family.toLowerCase().includes(text)) : fonts
		).slice(0, 300);
	});

	$effect(() => {
		// the files of the font, for its preview
		const id = fontId;
		details = null;
		if (id) {
			void call('studio.ngxbuilder.Fonts', { font: id }).then((result) => {
				if (id === fontId) {
					details = result?.font ?? null;
				}
			});
		}
	});

	let fontFace = $derived.by(() => {
		const urls = details?.variants?.[weight]?.[style]?.[subset]?.url;
		if (!urls || !font) {
			return '';
		}
		const src = Object.entries(urls)
			.map(([format, url]) => `url('${url}') format('${format}')`)
			.join(', ');
		const range = details.unicodeRange?.[subset];
		return `@font-face { font-family: 'studio-preview-${font.id}'; font-style: ${style}; font-weight: ${weight}; font-display: block; src: ${src};${range ? ` unicode-range: ${range};` : ''} }`;
	});

	/**
	 * @param {Font} next
	 */
	function choose(next) {
		fontId = next.id;
		const weights = (next.weights ?? []).map(String);
		const styles = next.styles ?? [];
		const subsets = next.subsets ?? [];
		weight = weights.includes(weight)
			? weight
			: weights.includes('400')
				? '400'
				: (weights[0] ?? '');
		style = styles.includes(style)
			? style
			: styles.includes('normal')
				? 'normal'
				: (styles[0] ?? '');
		subset = subsets.includes(subset) ? subset : (next.defSubset ?? subsets[0] ?? '');
	}

	function apply() {
		onApply?.(
			font
				? JSON.stringify({
						fontId: font.id,
						fontFamily: font.family,
						fontWeight: weight,
						fontStyle: style,
						fontSubset: subset
					})
				: '{}'
		);
	}
</script>

<svelte:head>
	{#if fontFace}
		{@html `<style>${fontFace}</style>`}
	{/if}
</svelte:head>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-font-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-font-title">{label}</strong>
		</header>
		<form
			class="studio-dialog__form"
			onsubmit={(event) => {
				event.preventDefault();
				apply();
			}}
		>
			<div class="studio-dialog__body">
				<input
					class="input-common"
					type="search"
					placeholder="Search a font"
					aria-label="Search a font"
					bind:value={filter}
				/>
				<div class="studio-font__list" role="listbox" aria-label="Fonts">
					{#each shown as candidate (candidate.id)}
						<button
							type="button"
							role="option"
							aria-selected={candidate.id === fontId}
							class={[
								'studio-font__item',
								candidate.id === fontId && 'studio-font__item--selected'
							]}
							onclick={() => choose(candidate)}
						>
							<span>{candidate.family}</span>
							<small>{candidate.category ?? ''}</small>
						</button>
					{:else}
						<p class="studio-font__empty">No font found.</p>
					{/each}
				</div>
				{#if font}
					<div class="studio-font__variant">
						<label>
							<span>Style</span>
							<select class="input-common" bind:value={style}>
								{#each font.styles ?? [] as option (option)}
									<option value={option}>{option}</option>
								{/each}
							</select>
						</label>
						<label>
							<span>Weight</span>
							<select class="input-common" bind:value={weight}>
								{#each (font.weights ?? []).map(String) as option (option)}
									<option value={option}>{option}</option>
								{/each}
							</select>
						</label>
						<label>
							<span>Subset</span>
							<select class="input-common" bind:value={subset}>
								{#each font.subsets ?? [] as option (option)}
									<option value={option}>{option}</option>
								{/each}
							</select>
						</label>
					</div>
					<p
						class="studio-font__preview"
						style:font-family={`'studio-preview-${font.id}', sans-serif`}
						style:font-style={style}
						style:font-weight={weight}
					>
						Sphinx of black quartz, judge my vow.
					</p>
				{:else}
					<p class="studio-font__empty">The application inherits its font.</p>
				{/if}
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={() => (fontId = '')}
					>Inherit the font</button
				>
				<span class="studio-font__spacer"></span>
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary">Apply</button>
			</footer>
		</form>
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
		width: min(34rem, 100%);
		max-height: min(42rem, 100%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__form {
		display: grid;
		min-height: 0;
		grid-template-rows: minmax(0, 1fr) auto;
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
		grid-template-rows: auto minmax(8rem, 1fr) auto auto;
		gap: 0.6rem;
		overflow: hidden;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__body input,
	.studio-dialog__body select {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-font__list {
		min-height: 0;
		overflow-y: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		padding: 0.25rem;
	}

	.studio-font__item {
		display: flex;
		width: 100%;
		align-items: baseline;
		justify-content: space-between;
		gap: 0.5rem;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.25rem 0.5rem;
		text-align: left;
	}

	.studio-font__item:hover {
		background: var(--studio-hover-bg);
	}

	.studio-font__item--selected {
		background: var(--studio-selection-bg);
		color: var(--studio-text-strong);
	}

	.studio-font__item small,
	.studio-font__empty {
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-font__empty {
		margin: 0;
	}

	.studio-font__variant {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 0.5rem;
	}

	.studio-font__variant label {
		display: grid;
		gap: 0.25rem;
	}

	.studio-font__variant span {
		font-weight: 600;
	}

	.studio-font__preview {
		margin: 0;
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		padding: 0.6rem;
		font-size: 1.4rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-font__spacer {
		flex: 1;
	}
</style>
