<script>
	import { base } from '$app/paths';
	import { onMount } from 'svelte';

	/** @type {{content?: string, language?: string, theme?: string, readOnly?: boolean}} */
	let {
		content = '/* Loading... */',
		language = 'json',
		theme = 'vs-dark',
		readOnly = true
	} = $props();

	let divEl = $state();
	let editor = $state();

	$effect.pre(() => {
		if (editor) {
			editor.updateOptions({
				theme,
				readOnly
			});
			editor.setValue(content);
			globalThis.monaco?.editor?.setModelLanguage(editor.getModel(), language);
		}
	});

	const monacoBase = (
		import.meta.env.VITE_MONACO_BASE ?? `${base.replace(/\/$/, '')}/monaco/vs`
	).replace(/\/$/, '');

	/** @type {Promise<any> | null} */
	let monacoLoader = null;

	function loadScript(src) {
		return new Promise((resolve, reject) => {
			const existing = document.querySelector(`script[data-monaco="${src}"]`);
			if (existing) {
				existing.addEventListener('load', resolve, { once: true });
				existing.addEventListener('error', reject, { once: true });
				if (existing.dataset.loaded === 'true') {
					resolve();
				}
				return;
			}
			const script = document.createElement('script');
			script.src = src;
			script.async = true;
			script.dataset.monaco = src;
			script.addEventListener('load', () => {
				script.dataset.loaded = 'true';
				resolve();
			});
			script.addEventListener('error', reject);
			document.head.appendChild(script);
		});
	}

	function loadMonaco() {
		if (globalThis.monaco) {
			return Promise.resolve(globalThis.monaco);
		}
		if (!monacoLoader) {
			monacoLoader = loadScript(`${monacoBase}/loader.js`)
				.then(() => {
					const require = globalThis.require;
					if (!require) {
						throw new Error('Monaco loader not available');
					}
					require.config({ paths: { vs: monacoBase } });
					return new Promise((resolve, reject) => {
						require(['vs/editor/editor.main'], () => {
							if (globalThis.monaco) {
								resolve(globalThis.monaco);
							} else {
								reject(new Error('Monaco failed to initialize'));
							}
						});
					});
				})
				.catch((error) => {
					monacoLoader = null;
					throw error;
				});
		}
		return monacoLoader;
	}

	onMount(() => {
		loadMonaco().then((Monaco) => {
			globalThis.monaco = Monaco;
			editor = Monaco.editor.create(divEl, {
				value: content,
				language: language,
				theme: theme,
				readOnly: readOnly
			});
			resize();
		});

		return () => {
			editor?.dispose();
		};
	});

	function resize() {
		if (!editor) return;
		editor.layout({ width: 0, height: 0 });
		window.requestAnimationFrame(() => {
			const rect = divEl.parentElement.getBoundingClientRect();
			editor.layout(rect);
		});
	}
</script>

<div bind:this={divEl} style:width="100%"></div>

<svelte:window onresize={resize} />
