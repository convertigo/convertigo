<script>
	import { call } from '#lib/utils/service.js';
	import { asset } from '$app/paths';
	import { fromAction } from 'svelte/attachments';
	import { changesInZones, findZones, generatedRuns, zoneAt } from './editableZones.js';

	/**
	 * breakpoints: the lines holding a breakpoint, shown in a margin whose clicks call onBreakpointToggle;
	 * currentLine: the line where the debugger stopped; revealLine: a line to show and select, once for
	 * each revealSerial, as a line a search found; path: the path of the file the editor shows, and
	 * typesProject: the project whose packages give their types to its TypeScript, as the TypeScript
	 * editor of the Eclipse Studio knows the packages of the project; editableZones: the code is a
	 * generated class whose zones between its Begin_c8o and End_c8o comments only are editable, and
	 * zonesFocus folds its generated code; zoneReveal: a zone to show and write in, once for each serial
	 * @type {{content?: string, language?: string, theme?: string, readOnly?: boolean, contentHeight?: number, scrollBeyondLastLine?: boolean, breakpoints?: number[] | null, onBreakpointToggle?: (line: number) => void, currentLine?: number, revealLine?: number, revealSerial?: number, path?: string, typesProject?: string, onSave?: () => void, editableZones?: boolean, zonesFocus?: boolean, zoneReveal?: { name: string, serial: number }}}
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
		onSave = undefined,
		editableZones = false,
		zonesFocus = false,
		zoneReveal = { name: '', serial: 0 }
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
		editableZones,
		zonesFocus,
		zoneReveal,
		onContentChange: onEditorContentChange,
		onContentHeightChange: onEditorContentHeightChange
	}));

	const monacoBase = (
		import.meta.env.VITE_MONACO_BASE ?? asset('monaco/vs/loader.js').replace(/\/loader\.js$/, '')
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
			editableZones: value?.editableZones === true,
			zonesFocus: value?.zonesFocus === true,
			zoneReveal: {
				name: String(value?.zoneReveal?.name ?? ''),
				serial: Number(value?.zoneReveal?.serial) || 0
			},
			onContentChange:
				typeof value?.onContentChange == 'function' ? value.onContentChange : undefined,
			onContentHeightChange:
				typeof value?.onContentHeightChange == 'function' ? value.onContentHeightChange : undefined
		};
	}

	/**
	 * @param {HTMLDivElement} node
	 * @param {{content?: string, language?: string, theme?: string, readOnly?: boolean, scrollBeyondLastLine?: boolean, breakpoints?: number[] | null, onBreakpointToggle?: (line: number) => void, currentLine?: number, revealLine?: number, revealSerial?: number, path?: string, typesProject?: string, onSave?: () => void, editableZones?: boolean, zonesFocus?: boolean, zoneReveal?: { name: string, serial: number }, onContentChange?: (nextContent: string) => void, onContentHeightChange?: (nextContentHeight: number) => void}} value
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
		/** @type {any} the zones of a generated class, their markers and the generated code around them */
		let zoneDecorations;
		/** @type {import('./editableZones.js').EditableZone[]} */
		let zones = [];
		/** the text the zones were found in, which a change outside them gives back */
		let zoneText = '';
		let zoneMode = '';
		let zoneReadOnly = false;
		let revealedZoneSerial = 0;
		let reverting = false;
		/** @type {any[]} the selections before a change, given back with the text */
		let zoneSelections = [];
		/** @type {string[]} the view zones that sum up the generated code folded */
		let foldZoneIds = [];
		/** @type {Map<string, string>} the run of each of them */
		const foldZoneRuns = new Map();
		let foldSignature = '';
		/** the runs of generated code unfolded by a click on their summary */
		const unfoldedRuns = new Set();
		/** @type {{ dispose: () => void }[]} */
		const zoneSubscriptions = [];

		/**
		 * Finds the zones of the code again, and shows them: the generated code dimmed, the markers as
		 * labels, a hint in the empty zones, marks in the scrollbar, the generated code folded in focus.
		 * @param {boolean} [force] even when the code and the mode did not change
		 */
		function refreshZones(force = false) {
			const Monaco = globalThis.monaco;
			const model = editor?.getModel();
			if (!model || !zoneDecorations) return;
			const text = model.getValue();
			const mode = `${pending.editableZones}:${pending.zonesFocus}:${pending.readOnly}`;
			if (!force && text === zoneText && mode === zoneMode) return;
			if (mode !== zoneMode && pending.zonesFocus) {
				unfoldedRuns.clear();
			}
			zoneMode = mode;
			zoneText = text;
			zones = pending.editableZones ? findZones(text) : [];
			/** @param {number} from @param {number} to */
			const range = (from, to) => {
				const start = model.getPositionAt(from);
				const end = model.getPositionAt(to);
				return new Monaco.Range(start.lineNumber, start.column, end.lineNumber, end.column);
			};
			const decorations = [];
			let generatedFrom = 0;
			for (const zone of zones) {
				if (zone.beginOffset > generatedFrom) {
					decorations.push({
						range: range(generatedFrom, zone.beginOffset),
						options: { inlineClassName: 'studio-zone-generated' }
					});
				}
				generatedFrom = zone.endOffset;
				decorations.push({
					range: new Monaco.Range(zone.beginLine, 1, zone.endLine, 1),
					options: {
						isWholeLine: true,
						className: 'studio-zone-line',
						linesDecorationsClassName: 'studio-zone-gutter',
						overviewRuler: {
							color: 'rgba(14, 165, 233, 0.7)',
							position: Monaco.editor.OverviewRulerLane.Left
						},
						minimap: {
							color: 'rgba(14, 165, 233, 0.45)',
							position: Monaco.editor.MinimapPosition.Inline
						}
					}
				});
				const nameStart = zone.beginOffset + '/*Begin_c8o_'.length;
				const nameEnd = zone.contentStart - 2;
				decorations.push(
					{
						range: range(zone.beginOffset, nameStart),
						options: { inlineClassName: 'studio-zone-marker' }
					},
					{
						range: range(nameStart, nameEnd),
						options: {
							inlineClassName: 'studio-zone-name',
							before: { content: '✎ ', inlineClassName: 'studio-zone-name' },
							hoverMessage: {
								value: `**${zone.label}**${zone.hint ? `: ${zone.hint}` : ''}. The code written between the Begin_c8o and End_c8o comments is kept in the component.`
							}
						}
					},
					{
						range: range(nameEnd, zone.contentStart),
						options: { inlineClassName: 'studio-zone-marker' }
					},
					{
						range: range(zone.contentEnd, zone.endOffset),
						options: { inlineClassName: 'studio-zone-marker' }
					}
				);
				if (zone.empty && !pending.readOnly) {
					decorations.push({
						range: range(zone.contentStart, zone.contentStart),
						options: {
							showIfCollapsed: true,
							after: {
								content: 'Write your code here',
								inlineClassName: 'studio-zone-placeholder',
								cursorStops: Monaco.editor.InjectedTextCursorStops.None
							}
						}
					});
				}
			}
			if (zones.length && text.length > generatedFrom) {
				decorations.push({
					range: range(generatedFrom, text.length),
					options: { inlineClassName: 'studio-zone-generated' }
				});
			}
			zoneDecorations.set(decorations);
			foldGeneratedCode();
			updateZoneReadOnly();
		}

		/**
		 * In focus, hides the runs of generated code and shows a line that sums up each, which a click
		 * unfolds; the folding of the code itself is not changed.
		 */
		function foldGeneratedCode() {
			const Monaco = globalThis.monaco;
			const model = editor.getModel();
			const runs =
				pending.editableZones && pending.zonesFocus && zones.length
					? generatedRuns(zones, model.getLinesContent()).filter(
							(run) => !unfoldedRuns.has(run.key)
						)
					: [];
			const signature = runs.map((run) => `${run.key}:${run.startLine}:${run.endLine}`).join('|');
			if (signature === foldSignature) return;
			foldSignature = signature;
			editor.setHiddenAreas?.(
				runs.map((run) => new Monaco.Range(run.startLine, 1, run.endLine, 1)),
				'c8o-zones'
			);
			editor.changeViewZones((/** @type {any} */ accessor) => {
				for (const id of foldZoneIds) accessor.removeZone(id);
				foldZoneIds = [];
				foldZoneRuns.clear();
				for (const run of runs) {
					const domNode = document.createElement('div');
					domNode.className = 'studio-zone-fold';
					const count = run.endLine - run.startLine + 1;
					const label = document.createElement('span');
					label.className = 'studio-zone-fold__label';
					label.textContent = `⋯ ${count} generated lines`;
					const summary = document.createElement('span');
					summary.className = 'studio-zone-fold__summary';
					summary.textContent = run.summary;
					domNode.append(label, summary);
					domNode.title = 'Show these generated lines';
					const id = accessor.addZone({
						afterLineNumber: run.startLine - 1,
						heightInLines: 1,
						// the summary takes the place of the lines it hides
						showInHiddenAreas: true,
						domNode
					});
					foldZoneIds.push(id);
					foldZoneRuns.set(id, run.key);
				}
			});
		}

		/**
		 * The code outside the zones is read-only: the editor is while a selection is outside them, and
		 * tells why when something is typed there.
		 */
		function updateZoneReadOnly() {
			const model = editor.getModel();
			let readOnly = pending.readOnly;
			if (!readOnly && pending.editableZones && zones.length) {
				readOnly = !editor
					.getSelections()
					.every((/** @type {any} */ selection) =>
						zoneAt(
							zones,
							model.getOffsetAt(selection.getStartPosition()),
							model.getOffsetAt(selection.getEndPosition())
						)
					);
			}
			if (readOnly !== zoneReadOnly) {
				zoneReadOnly = readOnly;
				editor.updateOptions({
					readOnly,
					readOnlyMessage: pending.readOnly
						? undefined
						: {
								value:
									'This code is generated by Convertigo: write yours in a ✎ zone, between its Begin_c8o and End_c8o comments.'
							}
				});
			}
		}

		/**
		 * Shows a zone and puts the cursor in it, at its code or where to write it.
		 * @param {string} name
		 */
		function revealZone(name) {
			const model = editor.getModel();
			const zone = zones.find((candidate) => candidate.name === name);
			if (!zone || !model) return;
			const code = model.getValue().slice(zone.contentStart, zone.contentEnd);
			const offset = zone.empty
				? zone.contentStart
				: zone.contentStart + (code.length - code.replace(/^\s+/, '').length);
			const position = model.getPositionAt(offset);
			editor.setPosition(position);
			editor.revealLineInCenter(position.lineNumber);
			editor.focus();
		}

		/**
		 * A click on the hint of an empty zone opens a line to write in.
		 * @param {any} event
		 */
		function openEmptyZone(event) {
			if (!event.target?.element?.classList?.contains('studio-zone-placeholder')) return;
			const model = editor.getModel();
			const position = event.target.position;
			if (pending.readOnly || !position) return;
			const clicked = zones.find(
				(candidate) =>
					candidate.empty &&
					model.getPositionAt(candidate.contentStart).lineNumber === position.lineNumber
			);
			if (!clicked) return;
			const at = model.getPositionAt(clicked.contentStart);
			const indent = /^\s*/.exec(model.getLineContent(at.lineNumber))?.[0] ?? '';
			editor.executeEdits('c8o-zones', [
				{
					range: new globalThis.monaco.Range(at.lineNumber, at.column, at.lineNumber, at.column),
					text: `\n${indent}`
				}
			]);
			editor.setPosition({ lineNumber: at.lineNumber + 1, column: indent.length + 1 });
			editor.focus();
		}

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
			zoneReadOnly = pending.readOnly;
			refreshZones(true);
			if (pending.zoneReveal.name && pending.zoneReveal.serial !== revealedZoneSerial) {
				revealedZoneSerial = pending.zoneReveal.serial;
				revealZone(pending.zoneReveal.name);
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
				changeSubscription = editor.onDidChangeModelContent((/** @type {any} */ event) => {
					if (applyingContent || reverting) return;
					if (
						pending.editableZones &&
						zones.length &&
						!event.isUndoing &&
						!event.isRedoing &&
						!changesInZones(zones, event.changes)
					) {
						// a change of the generated code, as a replacement or a drop, is given back
						const model = editor.getModel();
						const selections = zoneSelections;
						reverting = true;
						model.pushEditOperations(
							editor.getSelections(),
							[{ range: model.getFullModelRange(), text: zoneText }],
							() => selections
						);
						reverting = false;
						return;
					}
					refreshZones();
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
				zoneDecorations = editor.createDecorationsCollection();
				zoneSubscriptions.push(
					editor.onDidChangeCursorSelection(() => {
						if (reverting) return;
						zoneSelections = editor.getSelections();
						if (pending.editableZones) updateZoneReadOnly();
					}),
					// Backspace at the start of a zone and Delete at its end would remove its markers
					editor.onKeyDown((/** @type {any} */ event) => {
						if (!pending.editableZones || !zones.length || zoneReadOnly) return;
						const backspace = event.keyCode === Monaco.KeyCode.Backspace;
						if (!backspace && event.keyCode !== Monaco.KeyCode.Delete) return;
						const model = editor.getModel();
						const blocked = editor.getSelections().some((/** @type {any} */ selection) => {
							if (!selection.isEmpty()) return false;
							const offset = model.getOffsetAt(selection.getStartPosition());
							return zones.some((zone) =>
								backspace ? zone.contentStart === offset : zone.contentEnd === offset
							);
						});
						if (blocked) {
							event.preventDefault();
							event.stopPropagation();
						}
					}),
					editor.onMouseUp((/** @type {any} */ event) => {
						if (pending.editableZones) openEmptyZone(event);
					})
				);
				mouseDownSubscription = editor.onMouseDown((event) => {
					const run =
						event.target?.type === Monaco.editor.MouseTargetType.CONTENT_VIEW_ZONE
							? foldZoneRuns.get(event.target.detail?.viewZoneId)
							: undefined;
					if (run) {
						unfoldedRuns.add(run);
						foldGeneratedCode();
						return;
					}
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
				for (const subscription of zoneSubscriptions) subscription.dispose();
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

	/* the zones of a generated class: its code at full contrast, the generated code dimmed around it */
	:global(.studio-zone-line) {
		background: color-mix(in oklab, var(--color-primary-500) 7%, transparent);
	}

	:global(.studio-zone-gutter) {
		width: 3px !important;
		margin-left: 3px;
		background: var(--color-primary-500);
	}

	:global(.studio-zone-generated) {
		opacity: 0.5;
	}

	:global(.studio-zone-marker) {
		opacity: 0.45;
		font-style: italic;
	}

	:global(.studio-zone-name) {
		background: color-mix(in oklab, var(--color-primary-500) 20%, transparent);
		color: var(--color-primary-700-300) !important;
		font-style: normal;
	}

	:global(.studio-zone-placeholder) {
		margin-left: 1.5ch;
		color: var(--color-surface-500) !important;
		font-style: italic;
		cursor: pointer;
	}

	:global(.studio-zone-fold) {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		height: 100%;
		overflow: hidden;
		color: var(--color-surface-600-400);
		font-family: var(--studio-font-sans, inherit);
		font-size: 0.72rem;
		white-space: nowrap;
		cursor: pointer;
	}

	:global(.studio-zone-fold__label) {
		flex: none;
		border: 1px solid var(--color-surface-300-700);
		border-radius: 0.3rem;
		background: var(--color-surface-100-900);
		padding: 0 0.45rem;
		line-height: 1.2rem;
	}

	:global(.studio-zone-fold:hover .studio-zone-fold__label) {
		border-color: var(--color-primary-500);
		color: var(--color-primary-600-400);
	}

	:global(.studio-zone-fold__summary) {
		overflow: hidden;
		font-family: var(--monaco-monospace-font, monospace);
		opacity: 0.7;
		text-overflow: ellipsis;
	}

	:global(.studio-editor-breakpoint)::before {
		width: 0.55rem;
		height: 0.55rem;
		border-radius: 999px;
		background: var(--color-error-500);
		content: '';
	}
</style>
