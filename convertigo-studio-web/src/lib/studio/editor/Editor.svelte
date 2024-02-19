<script>
	import { onMount } from 'svelte';
	import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
	import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
	import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker';
	import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker';
	import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';

	let subscriptions = [];
	export let content = '/* Loading... */';
	export let language = 'java';
	export let theme = 'vs-dark';
	export let readOnly = true;

	let divEl;
	let editor;
	let Monaco;

	$: if (editor) {
		editor.updateOptions({
			language,
			theme,
			readOnly
		});
	}
	onMount(async () => {
		self.MonacoEnvironment = {
			getWorker: function (_moduleId, label) {
				if (label === 'json') {
					return new jsonWorker();
				}
				if (label === 'css' || label === 'scss' || label === 'less') {
					return new cssWorker();
				}
				if (label === 'html' || label === 'handlebars' || label === 'razor') {
					return new htmlWorker();
				}
				if (label === 'typescript' || label === 'javascript') {
					return new tsWorker();
				}
				return new editorWorker();
			}
		};

		Monaco = await import('monaco-editor');
		let initialContent = content;
		editor = Monaco.editor.create(divEl, {
			value: initialContent,
			language: language,
			theme: theme,
			readOnly: readOnly
		});
		editor.onDidChangeModelContent(() => {
			const text = editor.getValue();
			subscriptions.forEach((sub) => sub(text));
		});
		content = {
			subscribe(func) {
				subscriptions.push(func);
				return () => {
					subscriptions = subscriptions.filter((sub) => sub != func);
				};
			},
			set(val) {
				editor.setValue(val);
			}
		};
		return () => {
			editor.dispose();
		};
	});
</script>

<div bind:this={divEl} style:width="100%" />

<svelte:window
	on:resize={() => {
		editor.layout({ width: 0, height: 0 });
		window.requestAnimationFrame(() => {
			const rect = divEl.parentElement.getBoundingClientRect();
			editor.layout({ width: rect.width, height: rect.height });
		});
	}}
/>
