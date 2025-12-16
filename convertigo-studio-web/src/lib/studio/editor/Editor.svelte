<script>
	import { base } from '$app/paths';
	import { fromAction } from 'svelte/attachments';

	/** @type {{content?: string, language?: string, theme?: string, readOnly?: boolean}} */
	let {
		content = '/* Loading... */',
		language = 'json',
		theme = 'vs-dark',
		readOnly = true
	} = $props();

	const editorOptions = $derived.by(() => ({ content, language, theme, readOnly }));

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

	function normalizeOptions(value) {
		return {
			content: value?.content ?? '/* Loading... */',
			language: value?.language ?? 'json',
			theme: value?.theme ?? 'vs-dark',
			readOnly: value?.readOnly ?? true
		};
	}

	/**
	 * @param {HTMLDivElement} node
	 * @param {{content?: string, language?: string, theme?: string, readOnly?: boolean}} value
	 */
	function mountMonaco(node, value) {
		/** @type {any} */
		let editor;
		/** @type {ResizeObserver | undefined} */
		let resizeObserver;
		let disposed = false;
		let pending = normalizeOptions(value);

		function layout() {
			if (!editor) return;
			const rect = node.getBoundingClientRect();
			editor.layout({ width: rect.width, height: rect.height });
		}

		function apply(nextValue) {
			pending = normalizeOptions(nextValue);
			if (!editor) return;
			editor.updateOptions({ theme: pending.theme, readOnly: pending.readOnly });
			if (editor.getValue() !== pending.content) {
				editor.setValue(pending.content);
			}
			globalThis.monaco?.editor?.setModelLanguage(editor.getModel(), pending.language);
			layout();
		}

		loadMonaco()
			.then((Monaco) => {
				if (disposed) return;
				globalThis.monaco = Monaco;
				editor = Monaco.editor.create(node, {
					value: pending.content,
					language: pending.language,
					theme: pending.theme,
					readOnly: pending.readOnly,
					automaticLayout: false
				});

				resizeObserver = new ResizeObserver(() => layout());
				resizeObserver.observe(node);
				apply(pending);
			})
			.catch(() => {});

		return {
			update(next) {
				apply(next);
			},
			destroy() {
				disposed = true;
				resizeObserver?.disconnect();
				editor?.dispose();
			}
		};
	}

	const attachEditor = $derived(fromAction(mountMonaco, () => editorOptions));
</script>

<div class="h-full w-full" {@attach attachEditor}></div>
