<script>
	import { base } from '$app/paths';
	import { call } from '$lib/utils/service';
	import { fromAction } from 'svelte/attachments';

	/**
	 * breakpoints: the lines holding a breakpoint, shown in a margin whose clicks call onBreakpointToggle;
	 * currentLine: the line where the debugger stopped; revealLine: a line to show and select, once for
	 * each revealSerial, as a line a search found; path: the path of the file the editor shows, and
	 * typesProject: the project whose packages give their types to its TypeScript, as the TypeScript
	 * editor of the Eclipse Studio knows the packages of the project
	 * @type {{content?: string, language?: string, theme?: string, readOnly?: boolean, contentHeight?: number, scrollBeyondLastLine?: boolean, breakpoints?: number[] | null, onBreakpointToggle?: (line: number) => void, currentLine?: number, revealLine?: number, revealSerial?: number, path?: string, typesProject?: string, onSave?: () => void}}
	 */
	let {
		content = $bindable('/* Loading... */'),
		language = 'json',
		theme = 'vs-dark',
		readOnly = true,
		contentHeight = $bindable(0),
		scrollBeyondLastLine = true,
		breakpoints = null,
		onBreakpointToggle,
		currentLine = 0,
		revealLine = 0,
		revealSerial = 0,
		path = '',
		typesProject = '',
		onSave = undefined
	} = $props();

	function onEditorContentChange(nextContent) {
		if (content === nextContent) return;
		content = nextContent;
	}

	function onEditorContentHeightChange(nextContentHeight) {
		if (contentHeight === nextContentHeight) return;
		contentHeight = nextContentHeight;
	}

	const editorOptions = $derived.by(() => ({
		content,
		language,
		theme,
		readOnly,
		scrollBeyondLastLine,
		breakpoints,
		onBreakpointToggle,
		currentLine,
		revealLine,
		revealSerial,
		path,
		typesProject,
		onSave,
		onContentChange: onEditorContentChange,
		onContentHeightChange: onEditorContentHeightChange
	}));

	const monacoBase = (
		import.meta.env.VITE_MONACO_BASE ?? `${base.replace(/\/$/, '')}/monaco/vs`
	).replace(/\/$/, '');

	/** @type {Promise<any> | null} */
	let monacoLoader = null;
	/** the projects whose types the TypeScript checker knows */
	const loadedTypes = new Set();

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
			scrollBeyondLastLine: value?.scrollBeyondLastLine ?? true,
			breakpoints: Array.isArray(value?.breakpoints) ? value.breakpoints : null,
			onBreakpointToggle:
				typeof value?.onBreakpointToggle == 'function' ? value.onBreakpointToggle : undefined,
			currentLine: Number(value?.currentLine) || 0,
			revealLine: Number(value?.revealLine) || 0,
			revealSerial: Number(value?.revealSerial) || 0,
			path: String(value?.path ?? ''),
			typesProject: String(value?.typesProject ?? ''),
			onSave: typeof value?.onSave == 'function' ? value.onSave : undefined,
			onContentChange:
				typeof value?.onContentChange == 'function' ? value.onContentChange : undefined,
			onContentHeightChange:
				typeof value?.onContentHeightChange == 'function' ? value.onContentHeightChange : undefined
		};
	}

	/**
	 * @param {HTMLDivElement} node
	 * @param {{content?: string, language?: string, theme?: string, readOnly?: boolean, scrollBeyondLastLine?: boolean, breakpoints?: number[] | null, onBreakpointToggle?: (line: number) => void, currentLine?: number, revealLine?: number, revealSerial?: number, path?: string, typesProject?: string, onSave?: () => void, onContentChange?: (nextContent: string) => void, onContentHeightChange?: (nextContentHeight: number) => void}} value
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
		/** @type {{ dispose: () => void } | undefined} */
		let contentSizeSubscription;
		/** @type {{ dispose: () => void } | undefined} */
		let mouseDownSubscription;
		/** @type {any} */
		let breakpointDecorations;
		/** @type {any} */
		let currentLineDecorations;
		let shownLine = 0;
		let revealedSerial = 0;
		/** @type {Set<any>} the models the editor made, disposed with it */
		const ownModels = new Set();
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

		/**
		 * Shows the file of the path in its own model, whose path lets the TypeScript checker find the
		 * packages of the project, or an anonymous model without path.
		 */
		function applyModel() {
			const Monaco = globalThis.monaco;
			const current = editor.getModel();
			if (pending.path) {
				const uri = Monaco.Uri.parse(`file:///${pending.path}`);
				if (current?.uri.toString() === uri.toString()) {
					return;
				}
				let model = Monaco.editor.getModel(uri);
				if (!model) {
					model = Monaco.editor.createModel(pending.content, pending.language, uri);
					ownModels.add(model);
				}
				applyingContent = true;
				editor.setModel(model);
				applyingContent = false;
			} else if (current?.uri.scheme === 'file') {
				const model = Monaco.editor.createModel(pending.content, pending.language);
				ownModels.add(model);
				applyingContent = true;
				editor.setModel(model);
				applyingContent = false;
			}
			if (pending.typesProject) {
				loadTypes(pending.typesProject);
			}
		}

		function apply(nextValue) {
			pending = normalizeOptions(nextValue);
			if (!editor) return;
			applyModel();
			editor.updateOptions({
				readOnly: pending.readOnly,
				domReadOnly: pending.readOnly,
				scrollBeyondLastLine: pending.scrollBeyondLastLine,
				glyphMargin: pending.breakpoints !== null
			});
			breakpointDecorations?.set(
				(pending.breakpoints ?? []).map((line) => ({
					range: new globalThis.monaco.Range(line, 1, line, 1),
					options: {
						glyphMarginClassName: 'studio-editor-breakpoint',
						glyphMarginHoverMessage: { value: 'Breakpoint' },
						stickiness: 1
					}
				}))
			);
			const line = pending.currentLine;
			currentLineDecorations?.set(
				line
					? [
							{
								range: new globalThis.monaco.Range(line, 1, line, 1),
								options: { isWholeLine: true, className: 'studio-editor-current-line' }
							}
						]
					: []
			);
			if (line && line !== shownLine) {
				editor.revealLineInCenterIfOutsideViewport(line);
			}
			shownLine = line;
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
			if (
				pending.revealLine &&
				pending.revealSerial !== revealedSerial &&
				model &&
				pending.revealLine <= model.getLineCount()
			) {
				// the line found, selected once its content is shown
				revealedSerial = pending.revealSerial;
				const line = pending.revealLine;
				editor.setSelection(
					new globalThis.monaco.Range(line, 1, line, model.getLineMaxColumn(line))
				);
				editor.revealLineInCenter(line);
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
					domReadOnly: pending.readOnly,
					scrollBeyondLastLine: pending.scrollBeyondLastLine,
					automaticLayout: false
				});
				changeSubscription = editor.onDidChangeModelContent(() => {
					if (applyingContent) return;
					const nextContent = editor.getValue();
					if (pending.content === nextContent) return;
					pending = { ...pending, content: nextContent };
					pending.onContentChange?.(nextContent);
				});
				contentSizeSubscription = editor.onDidContentSizeChange((event) => {
					pending.onContentHeightChange?.(Math.ceil(event.contentHeight));
				});
				// Ctrl or ⌘ with S saves the file, as in the editors of the Eclipse Studio
				editor.addCommand(Monaco.KeyMod.CtrlCmd | Monaco.KeyCode.KeyS, () => pending.onSave?.());
				breakpointDecorations = editor.createDecorationsCollection();
				currentLineDecorations = editor.createDecorationsCollection();
				mouseDownSubscription = editor.onMouseDown((event) => {
					const line = event.target?.position?.lineNumber;
					if (
						pending.breakpoints !== null &&
						line &&
						event.target.type === Monaco.editor.MouseTargetType.GUTTER_GLYPH_MARGIN
					) {
						pending.onBreakpointToggle?.(line);
					}
				});
				pending.onContentHeightChange?.(Math.ceil(editor.getContentHeight()));

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
				contentSizeSubscription?.dispose();
				mouseDownSubscription?.dispose();
				editor?.dispose();
				for (const model of ownModels) {
					model.dispose();
				}
			}
		};
	}

	const attachEditor = $derived(fromAction(mountMonaco, () => editorOptions));

	/**
	 * Gives the type definitions of the packages of a project to the TypeScript checker, once, for the
	 * completion and the hovers of its files; the files of the application are not all known, their
	 * semantic errors are not shown.
	 * @param {string} project
	 */
	function loadTypes(project) {
		const Monaco = globalThis.monaco;
		const typescript = Monaco?.languages?.typescript?.typescriptDefaults;
		if (!typescript || loadedTypes.has(project)) {
			return;
		}
		loadedTypes.add(project);
		if (loadedTypes.size === 1) {
			const ts = Monaco.languages.typescript;
			typescript.setCompilerOptions({
				target: ts.ScriptTarget.ES2020,
				module: ts.ModuleKind.ESNext,
				moduleResolution: ts.ModuleResolutionKind.NodeJs,
				experimentalDecorators: true,
				emitDecoratorMetadata: true,
				allowNonTsExtensions: true,
				allowSyntheticDefaultImports: true,
				esModuleInterop: true,
				skipLibCheck: true,
				lib: ['es2020', 'dom']
			});
			typescript.setDiagnosticsOptions({ noSemanticValidation: true, noSyntaxValidation: false });
		}
		void call('studio.source.Types', { project }).then((result) => {
			for (const [file, content] of Object.entries(result?.files ?? {})) {
				typescript.addExtraLib(String(content), `file:///${project}/_private/ionic/${file}`);
			}
		});
	}
</script>

<div class="h-full w-full" {@attach attachEditor}></div>

<style>
	/* the breakpoints, drawn as in the debug panel */
	:global(.studio-editor-breakpoint) {
		display: grid;
		place-items: center;
		cursor: pointer;
	}

	:global(.studio-editor-current-line) {
		background: color-mix(in oklab, var(--color-warning-500) 28%, transparent);
	}

	:global(.studio-editor-breakpoint)::before {
		width: 0.55rem;
		height: 0.55rem;
		border-radius: 999px;
		background: var(--color-error-500);
		content: '';
	}
</style>
