<script>
	import { base } from '$app/paths';
	import { fromAction } from 'svelte/attachments';

	/** @type {{content?: string, language?: string, theme?: string, readOnly?: boolean}} */
	let {
		content = $bindable('/* Loading... */'),
		language = 'json',
		theme = 'vs-dark',
		readOnly = true
	} = $props();

	function onEditorContentChange(nextContent) {
		if (content === nextContent) return;
		content = nextContent;
	}

	const editorOptions = $derived.by(() => ({
		content,
		language,
		theme,
		readOnly,
		onContentChange: onEditorContentChange
	}));

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
			readOnly: value?.readOnly ?? true,
			onContentChange:
				typeof value?.onContentChange == 'function' ? value.onContentChange : undefined
		};
	}

	/**
	 * @param {HTMLDivElement} node
	 * @param {{content?: string, language?: string, theme?: string, readOnly?: boolean, onContentChange?: (nextContent: string) => void}} value
	 */
	function mountMonaco(node, value) {
		/** @type {any} */
		let editor;
		/** @type {ResizeObserver | undefined} */
		let resizeObserver;
		/** @type {{ dispose: () => void } | undefined} */
		let changeSubscription;
		let disposed = false;
		let pending = normalizeOptions(value);
		let applyingContent = false;

		function layout() {
			if (!editor) return;
			const rect = node.getBoundingClientRect();
			editor.layout({ width: rect.width, height: rect.height });
		}

		function apply(nextValue) {
			pending = normalizeOptions(nextValue);
			if (!editor) return;
			editor.updateOptions({ readOnly: pending.readOnly });
			globalThis.monaco?.editor?.setTheme(pending.theme || 'vs');
			if (editor.getValue() !== pending.content) {
				applyingContent = true;
				editor.setValue(pending.content);
				applyingContent = false;
			}
			const model = editor.getModel();
			if (model && model.getLanguageId() !== pending.language) {
				globalThis.monaco?.editor?.setModelLanguage(model, pending.language);
			}
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
				changeSubscription = editor.onDidChangeModelContent(() => {
					if (applyingContent) return;
					const nextContent = editor.getValue();
					if (pending.content === nextContent) return;
					pending = { ...pending, content: nextContent };
					pending.onContentChange?.(nextContent);
				});

				resizeObserver = new ResizeObserver(() => layout());
				resizeObserver.observe(node);
				apply(pending);
				// Monaco can render with a stale tiny viewport when mounted during route/layout transitions.
				// Trigger a few deferred layouts to stabilize height/width in dynamic containers.
				layout();
				requestAnimationFrame(() => layout());
				setTimeout(() => layout(), 0);
				setTimeout(() => layout(), 120);
			})
			.catch(() => {});

		return {
			update(next) {
				apply(next);
			},
			destroy() {
				disposed = true;
				resizeObserver?.disconnect();
				changeSubscription?.dispose();
				editor?.dispose();
			}
		};
	}

	const attachEditor = $derived(fromAction(mountMonaco, () => editorOptions));
</script>

<div class="h-full w-full" {@attach attachEditor}></div>
