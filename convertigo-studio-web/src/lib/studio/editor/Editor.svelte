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

	/**
	 * @param {string} src
	 * @returns {Promise<void>}
	 */
	function loadScript(src) {
		return new Promise((resolve, reject) => {
			const existing = document.querySelector(`script[data-monaco="${src}"]`);
			if (existing) {
				existing.addEventListener('load', () => resolve(), { once: true });
				existing.addEventListener('error', reject, { once: true });
				if (existing instanceof HTMLScriptElement && existing.dataset.loaded === 'true') {
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

	/**
	 * @returns {Promise<any>}
	 */
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
					// @ts-ignore Monaco's AMD loader extends the browser require global at runtime.
					require.config({ paths: { vs: monacoBase } });
					return new Promise((resolve, reject) => {
						// @ts-ignore Monaco's AMD loader accepts dependency arrays.
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
		/** @type {IntersectionObserver | undefined} */
		let intersectionObserver;
		/** @type {MutationObserver | undefined} */
		let visibilityObserver;
		/** @type {{ dispose: () => void } | undefined} */
		let changeSubscription;
		let disposed = false;
		let pending = normalizeOptions(value);
		let applyingContent = false;
		let layoutFrame = 0;
		/** @type {number[]} */
		let layoutTimers = [];

		function layout() {
			if (!editor) return;
			const rect = node.getBoundingClientRect();
			editor.layout({ width: rect.width, height: rect.height });
		}

		function clearScheduledLayout() {
			if (layoutFrame) {
				cancelAnimationFrame(layoutFrame);
				layoutFrame = 0;
			}
			for (const timer of layoutTimers) clearTimeout(timer);
			layoutTimers = [];
		}

		function scheduleLayout() {
			if (disposed) return;
			layout();
			clearScheduledLayout();
			layoutFrame = requestAnimationFrame(() => {
				layoutFrame = 0;
				layout();
			});
			for (const delay of [0, 120]) {
				layoutTimers.push(window.setTimeout(() => layout(), delay));
			}
		}

		function watchVisibilityChanges() {
			intersectionObserver = new IntersectionObserver(() => scheduleLayout());
			intersectionObserver.observe(node);
			visibilityObserver = new MutationObserver(() => scheduleLayout());
			let current = /** @type {HTMLElement | null} */ (node);
			while (current) {
				visibilityObserver.observe(current, {
					attributeFilter: ['class', 'hidden', 'style'],
					attributes: true
				});
				current = current.parentElement;
			}
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
			scheduleLayout();
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
				watchVisibilityChanges();
				apply(pending);
				// Monaco can render with a stale tiny viewport when mounted during route/layout transitions.
				// Trigger a few deferred layouts to stabilize height/width in dynamic containers.
				scheduleLayout();
			})
			.catch(() => {});

		return {
			update(next) {
				apply(next);
			},
			destroy() {
				disposed = true;
				clearScheduledLayout();
				resizeObserver?.disconnect();
				intersectionObserver?.disconnect();
				visibilityObserver?.disconnect();
				changeSubscription?.dispose();
				editor?.dispose();
			}
		};
	}

	const attachEditor = $derived(fromAction(mountMonaco, () => editorOptions));
</script>

<div class="h-full w-full" {@attach attachEditor}></div>
