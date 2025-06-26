<script>
	import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
	import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker';
	import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker';
	import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
	import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
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
	onMount(() => {
		self.MonacoEnvironment = {
			getWorker: function (_moduleId, label) {
				if (label === 'json') {
					return new jsonWorker();
				}
				if (label === 'css' || label === 'scss' || label === 'less') {
					return new cssWorker();
				}
				if (label === 'html' || label === 'handlebars' || label === 'razor' || label === 'xml') {
					return new htmlWorker();
				}
				if (label === 'typescript' || label === 'javascript') {
					return new tsWorker();
				}
				return new editorWorker();
			}
		};

		import('monaco-editor').then((Monaco) => {
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
			editor.dispose();
		};
	});

	function resize() {
		editor.layout({ width: 0, height: 0 });
		window.requestAnimationFrame(() => {
			const rect = divEl.parentElement.getBoundingClientRect();
			editor.layout(rect);
		});
	}
</script>

<div bind:this={divEl} style:width="100%"></div>

<svelte:window onresize={resize} />
