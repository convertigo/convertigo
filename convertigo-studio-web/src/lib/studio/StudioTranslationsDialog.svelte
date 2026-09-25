<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call, toaster } from '$lib/utils/service';

	/**
	 * Creates or updates the translations files of an NGX application, as the Eclipse Studio does: the
	 * titles of its pages and its plain texts are the keys of the files of its source and target languages.
	 *
	 * @type {{ id: string, onDone?: (id: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { id, onDone, onClose } = $props();

	const LANGUAGES = [
		'ar',
		'bg',
		'ca',
		'cs',
		'da',
		'de',
		'el',
		'en',
		'es',
		'et',
		'fa',
		'fi',
		'fr',
		'he',
		'hi',
		'hr',
		'hu',
		'id',
		'it',
		'ja',
		'ko',
		'lt',
		'lv',
		'ms',
		'nb',
		'nl',
		'pl',
		'pt',
		'ro',
		'ru',
		'sk',
		'sl',
		'sr',
		'sv',
		'th',
		'tr',
		'uk',
		'vi',
		'zh'
	];
	const names = new Intl.DisplayNames(['en'], { type: 'language' });
	const languages = LANGUAGES.map((code) => ({ code, name: names.of(code) ?? code })).sort((a, b) =>
		a.name.localeCompare(b.name)
	);
	const browser = (typeof navigator === 'undefined' ? 'en' : navigator.language).slice(0, 2);

	let from = $state('en');
	let to = $state(LANGUAGES.includes(browser) && browser !== 'en' ? browser : 'fr');
	let busy = $state(false);
	let error = $state('');

	let name = $derived(id.split(/[.:]/).pop() ?? id);

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy) {
			return;
		}
		busy = true;
		error = '';
		try {
			const result = await call('studio.ngxbuilder.Translations', { id, from, to });
			if (result?.done) {
				toaster.success({
					description: `${result.texts} text${result.texts > 1 ? 's' : ''} written in ${result.files.join(', ')}.`
				});
				await onDone?.(id);
			} else {
				error = String(result?.error?.message ?? 'The translations files were not written.');
			}
		} finally {
			busy = false;
		}
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => !busy && onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-translations-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-translations-title">Translations files of {name}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy}>
				<p class="studio-dialog__hint">
					The titles of the pages and the plain texts of the application become the keys of the
					files. Existing translations are kept.
				</p>
				<label class="studio-dialog__field">
					<span>Language of the texts</span>
					<select class="select-common" bind:value={from}>
						{#each languages as language (language.code)}
							<option value={language.code}>{language.name} ({language.code})</option>
						{/each}
					</select>
				</label>
				<label class="studio-dialog__field">
					<span>Language to translate to</span>
					<select class="select-common" bind:value={to}>
						{#each languages as language (language.code)}
							<option value={language.code}>{language.name} ({language.code})</option>
						{/each}
					</select>
				</label>
				{#if error}
					<p class="studio-dialog__error" role="alert">{error}</p>
				{/if}
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Write the files
				</button>
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
		width: min(28rem, 100%);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__form {
		display: contents;
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

	.studio-dialog__footer button {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-dialog__body {
		min-width: 0;
		display: grid;
		gap: 0.7rem;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__hint {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.75rem;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__field select {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__error {
		margin: 0;
		color: var(--color-error-600-400);
		font-size: 0.75rem;
	}
</style>
