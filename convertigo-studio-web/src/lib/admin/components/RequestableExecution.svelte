<script module>
	import { SvelteMap } from 'svelte/reactivity';

	/**
	 * The last result of each requestable, which shows again once it is selected again.
	 * @type {SvelteMap<string, { content: string, language: string }>}
	 */
	const lastResults = new SvelteMap();
</script>

<script>
	import { Popover } from '@skeletonlabs/skeleton-svelte';
	import ActionBar from '#lib/admin/components/ActionBar.svelte';
	import Button from '#lib/admin/components/Button.svelte';
	import PropertyType from '#lib/admin/components/PropertyType.svelte';
	import RequestableTestCases from '#lib/admin/components/RequestableTestCases.svelte';
	import RequestableVariables from '#lib/admin/components/RequestableVariables.svelte';
	import LightSvelte from '#lib/common/Light.svelte.js';
	import RequestableResponseEditor from '#lib/dashboard/RequestableResponseEditor.svelte';
	import { settlePropertyApply } from '#lib/studio/propertyApply.svelte.js';
	import Ico from '#lib/utils/Ico.svelte';
	import { call, callRequestable, getUrl, toaster } from '#lib/utils/service.js';
	import { untrack } from 'svelte';
	import { fly } from 'svelte/transition';

	/**
	 * @typedef {Object} RequestableLike
	 * @property {string=} name
	 * @property {string=} comment
	 * @property {any[]=} variable
	 * @property {any[]=} testcase
	 * @property {string=} response
	 * @property {string=} language
	 * @property {boolean=} loading
	 * @property {any=} tc
	 */

	/**
	 * @type {{
	 *  projectName?: string,
	 *  requestable?: RequestableLike | null,
	 *  kind?: 'sequence' | 'transaction' | string,
	 *  connectorName?: string,
	 *  mode?: string,
	 *  modes?: string[],
	 *  showIntro?: boolean,
	 *  showComment?: boolean,
	 *  showTestcaseEdit?: boolean,
	 *  testcaseValue?: string,
	 *  stickyActions?: boolean,
	 *  freshContext?: boolean,
	 *  stubbable?: boolean,
	 *  stub?: boolean,
	 *  runTestcase?: string,
	 *  onRunTestcaseTaken?: () => void,
	 *  onChanged?: (id: string) => void,
	 *  onDebugStep?: (id: string) => void,
	 *  connectorData?: boolean,
	 *  disabled?: boolean,
	 *  class?: string
	 * }}
	 */
	let {
		projectName = '',
		requestable = $bindable(null),
		kind = 'sequence',
		connectorName = '',
		mode = $bindable('JSON'),
		modes = ['JSON', 'XML', 'BIN', 'CXML'],
		showIntro = false,
		showComment = false,
		showTestcaseEdit = true,
		testcaseValue = 'testcases',
		stickyActions = false,
		freshContext = false,
		stubbable = false,
		stub = $bindable(false),
		runTestcase = '',
		onRunTestcaseTaken,
		onChanged,
		onDebugStep,
		connectorData = false,
		disabled = false,
		class: cls = ''
	} = $props();
	const componentId = $props.id();
	const downloadTarget = componentId + '-download';

	let requestableKey = $derived(
		requestable
			? `${projectName}\u0000${kind}\u0000${connectorName}\u0000${requestable.name ?? ''}`
			: ''
	);
	let responseKey = $state('');
	let responseContent = $state('');
	let responseLanguage = $state('json');
	let responseLoading = $state(false);
	let responseRevision = $state(0);
	let copyAsSource = $state('');
	let hasVariables = $derived((requestable?.variable?.length ?? 0) > 0);
	let hasTestcases = $derived((requestable?.testcase?.length ?? 0) > 0);
	const copyFormats = [
		{ value: 'url', label: 'URL', icon: 'mdi:open-in-new-variant' },
		{ value: 'curl', label: 'cURL', icon: 'mdi:code-block-braces' },
		{ value: 'fetch', label: 'fetch', icon: 'mdi:code-tags' },
		{ value: 'body', label: 'POST body', icon: 'mdi:content-copy' }
	];
	let responseView = $derived.by(() => {
		if (responseKey === requestableKey) {
			return {
				content: responseContent,
				language: responseLanguage,
				loading: responseLoading
			};
		}
		// the last result of the requestable shows again, as its editor of the Eclipse Studio keeps it
		const kept = lastResults.get(requestableKey);
		return {
			content: requestable?.response || kept?.content || '',
			language: requestable?.response
				? (requestable?.language ?? 'json')
				: (kept?.language ?? 'json'),
			loading: requestable?.loading === true
		};
	});
	let hasResponse = $derived(responseView.content.length > 0 || responseView.loading);
	/**
	 * The data the connector of the transaction got, as the connector editors of the Eclipse Studio show it.
	 * @type {{ kind: string, text?: string, headers?: string[], rows?: string[][], total?: number } | null}
	 */
	let rawData = $state(null);
	let watchesConnector = $derived(
		connectorData && kind === 'transaction' && Boolean(connectorName)
	);
	/** the response shown was run in the XML mode, without the sheet of the requestable */
	let xmlResponse = $state(false);
	/** the sequence runs in the debug mode, stopping before each step as in the Eclipse Studio */
	let debug = $state(false);
	let debugToken = $state('');
	/** @type {{ active?: boolean, running?: boolean, paused?: boolean, stepByStep?: boolean, step?: { id: string, name: string } } | null} */
	let debugState = $state(null);
	let debuggable = $derived(stubbable && kind === 'sequence');
	/** the context of the running execution, which Stop aborts */
	let runningContext = $state('');
	/** the stubs recorded for the requestable, the default one first */
	let stubFiles = $state(/** @type {string[]} */ ([]));
	let defaultStub = $state('');
	let stubFile = $state('');
	let requestableId = $derived(
		!requestable?.name
			? ''
			: kind === 'transaction'
				? `${projectName}.cn:${connectorName}.tr:${requestable.name}`
				: `${projectName}.sq:${requestable.name}`
	);

	$effect(() => {
		// the stub files to execute from, as the "Execute from stub" menu of Eclipse
		const id = stubbable && stub ? requestableId : '';
		stubFiles = [];
		stubFile = '';
		if (id) {
			void call('studio.dbo.Stubs', { id }).then((result) => {
				if (id === requestableId && Array.isArray(result?.stubs)) {
					defaultStub = String(result.defaultStub ?? '');
					stubFiles = result.stubs.map(String);
					stubFile = stubFiles.includes(defaultStub) ? defaultStub : (stubFiles[0] ?? '');
				}
			});
		}
	});
	let responseTheme = $derived(LightSvelte.light ? '' : 'vs-dark');
	let responseEditorKey = $derived(`${requestableKey}\u0000${responseRevision}`);

	/**
	 * Saves the XML response as the default stub of the requestable, as Create stub from XML in Eclipse.
	 */
	async function saveStub() {
		if (!requestable?.name) {
			return;
		}
		const id = requestableId;
		let result = await call('studio.dbo.CreateStub', { id, xml: responseView.content });
		if (result?.exists && window.confirm(`The stub ${result.file} exists. Replace it?`)) {
			result = await call('studio.dbo.CreateStub', {
				id,
				xml: responseView.content,
				overwrite: 'true'
			});
		}
		if (result?.done) {
			toaster.success({ description: `The response is saved as the stub ${result.file}.` });
		}
	}

	/** what the validation of the last XML response against the schema of the project tells */
	let validation = $state(/** @type {{ valid: boolean, message: string } | null} */ (null));

	const AUTO_VALIDATE = 'studio.execution.autoValidate';
	/** each XML response is validated once it comes, as the auto validate of the Schema view of Eclipse */
	let autoValidate = $state(readAutoValidate());

	function readAutoValidate() {
		try {
			return localStorage.getItem(AUTO_VALIDATE) === 'true';
		} catch {
			return false;
		}
	}

	function toggleAutoValidate() {
		autoValidate = !autoValidate;
		try {
			localStorage.setItem(AUTO_VALIDATE, String(autoValidate));
		} catch {
			// no storage
		}
		if (autoValidate && xmlResponse && responseView.language === 'xml' && !responseView.loading) {
			void validateResponse();
		}
	}

	/**
	 * Validates the XML response against the schema of the project, as the auto validate of the Schema
	 * view of the Eclipse Studio.
	 */
	async function validateResponse() {
		if (!requestable?.name) {
			return;
		}
		const result = await call('studio.treeview.Schema', {
			id: requestableId,
			action: 'validate',
			requestable:
				kind === 'transaction' ? `${connectorName}__${requestable.name}` : requestable.name,
			xml: responseView.content
		});
		validation =
			typeof result?.valid === 'boolean'
				? { valid: result.valid, message: String(result.summary ?? '') }
				: null;
	}

	/**
	 * Writes the schema of the transaction again from the XML response, as "Update schema from current
	 * connector data" in Eclipse.
	 */
	async function updateSchema() {
		if (
			!requestable?.name ||
			!window.confirm(
				`Update the schema of ${requestable.name} from this response? The previous schema is replaced.`
			)
		) {
			return;
		}
		const id = requestableId;
		const result = await call('studio.dbo.UpdateSchema', { id, xml: responseView.content });
		if (result?.done) {
			onChanged?.(id);
		}
	}

	/**
	 * Follows the debug session of a sequence run: its current step and, while it stops, the output
	 * document built so far.
	 * @param {string} token
	 * @param {boolean} json
	 */
	async function followDebug(token, json) {
		let stepId = '';
		while (debugToken === token) {
			const result = await call('studio.debug.Steps', { token, json: String(json) });
			if (debugToken !== token) {
				return;
			}
			const state = result?.state;
			debugState = state?.active ? state : null;
			if (state?.step?.id && state.step.id !== stepId) {
				stepId = state.step.id;
				onDebugStep?.(stepId);
			}
			if (state?.paused && typeof state.output === 'string') {
				updateResponse({ content: state.output, language: json ? 'json' : 'xml' });
			}
			await new Promise((resolve) => setTimeout(resolve, state?.paused ? 700 : 300));
		}
	}

	/**
	 * Stops the running execution, as the Stop button of the sequence editor in Eclipse.
	 */
	async function abort() {
		if (runningContext) {
			await call('studio.debug.Abort', { context: runningContext });
		}
	}

	/**
	 * @param {'step' | 'run' | 'pause' | 'stop'} action
	 */
	async function debugAction(action) {
		const token = debugToken;
		if (!token) {
			return;
		}
		const result = await call('studio.debug.Steps', { token, action });
		if (debugToken === token) {
			debugState = result?.state?.active ? result.state : null;
		}
	}

	/**
	 * @param {{content?: string, language?: string, loading?: boolean}} next
	 */
	function updateResponse(next) {
		const previousContent = responseView.content;
		const previousLanguage = responseView.language;
		responseKey = requestableKey;
		responseContent = 'content' in next ? (next.content ?? '') : responseView.content;
		responseLanguage = 'language' in next ? (next.language ?? 'json') : responseView.language;
		responseLoading = 'loading' in next ? next.loading === true : responseView.loading;
		if (responseContent !== previousContent || responseLanguage !== previousLanguage) {
			responseRevision += 1;
		}
		if (requestable) {
			requestable.response = responseContent;
			requestable.language = responseLanguage;
			requestable.loading = responseLoading;
		}
		if (!responseLoading && requestableKey) {
			lastResults.set(requestableKey, { content: responseContent, language: responseLanguage });
		}
	}

	/**
	 * @param {{ send?: any }} variable
	 * @returns {boolean}
	 */
	function shouldSendVariable(variable) {
		return variable?.send === true || variable?.send == 'true';
	}

	/**
	 * @param {string} value
	 * @returns {string}
	 */
	function testcaseNameFromSource(value) {
		return value.startsWith('testcase:') ? value.slice('testcase:'.length) : '';
	}

	/**
	 * @param {any} value
	 * @returns {string}
	 */
	function parameterValue(value) {
		if (value == null) {
			return '';
		}
		if (typeof File != 'undefined' && value instanceof File) {
			return value.name;
		}
		return String(value);
	}

	/**
	 * @param {any} variable
	 * @returns {string[]}
	 */
	function variableValues(variable) {
		if (variable?.isMultivalued == 'true') {
			if (Array.isArray(variable.multipleValues)) {
				return variable.multipleValues.map(({ val }) => parameterValue(val));
			}
			try {
				const parsed = JSON.parse(variable.val ?? variable.value ?? '[]');
				return Array.isArray(parsed) ? parsed.map(parameterValue) : [];
			} catch {
				return [];
			}
		}
		return [parameterValue(variable?.val ?? variable?.value)];
	}

	/**
	 * @param {string} source
	 * @returns {[string, string][]}
	 */
	function requestEntries(source = 'current') {
		if (!requestable) {
			return [];
		}
		/** @type {[string, string][]} */
		const entries =
			kind === 'transaction'
				? [
						['__connector', connectorName],
						['__transaction', requestable.name ?? '']
					]
				: [['__sequence', requestable.name ?? '']];
		entries.push(['__nocache', 'true']);
		if (stub) {
			// the response is the stub recorded for the requestable, as Execute from stub in Eclipse
			entries.push(['__stub', 'true']);
			if (stubFile && stubFile !== defaultStub) {
				entries.push(['__stub_filename', stubFile]);
			}
		}
		if (freshContext) {
			entries.push(['__context', 'studio-web-execution-*']);
			entries.push(['__removeContext', 'true']);
		}
		const testcaseName = testcaseNameFromSource(source);
		if (testcaseName) {
			entries.push(['__testcase', testcaseName]);
			return entries;
		}
		for (const variable of requestable.variable ?? []) {
			if (!shouldSendVariable(variable)) {
				continue;
			}
			for (const value of variableValues(variable)) {
				entries.push([variable.name, value]);
			}
		}
		return entries;
	}

	/**
	 * @returns {string}
	 */
	function endpointUrl() {
		const path = getUrl(`projects/${projectName}/.${String(mode).toLowerCase()}`);
		if (typeof location == 'undefined') {
			return path;
		}
		return new URL(path, location.href).toString();
	}

	/**
	 * @param {string} source
	 * @returns {string}
	 */
	function urlPreset(source) {
		const base = endpointUrl();
		if (typeof location == 'undefined') {
			const query = new URLSearchParams(requestEntries(source)).toString();
			return query ? `${base}?${query}` : base;
		}
		const url = new URL(base);
		for (const [key, value] of requestEntries(source)) {
			url.searchParams.append(key, value);
		}
		return url.toString();
	}

	/**
	 * @param {string} value
	 * @returns {string}
	 */
	function shellQuote(value) {
		return `'${String(value).replaceAll("'", "'\\''")}'`;
	}

	/**
	 * @returns {string}
	 */
	function xsrfTokenExpression() {
		return 'localStorage.getItem("x-xsrf-token") ?? "Fetch"';
	}

	/**
	 * @param {string} source
	 * @returns {string}
	 */
	function bodyPreset(source) {
		return new URLSearchParams(requestEntries(source)).toString();
	}

	/**
	 * @param {string} source
	 * @returns {string}
	 */
	function curlPreset(source) {
		const lines = [
			`curl -X POST ${shellQuote(endpointUrl())}`,
			`  -H ${shellQuote('Content-Type: application/x-www-form-urlencoded')}`
		];
		const jsessionid = cookieValue('JSESSIONID');
		if (jsessionid) {
			lines.push(`  -H ${shellQuote(`Cookie: JSESSIONID=${jsessionid}`)}`);
		}
		lines.push(`  --data-raw ${shellQuote(bodyPreset(source))}`);
		return lines.join(' \\\n');
	}

	/**
	 * @param {string} name
	 * @returns {string}
	 */
	function cookieValue(name) {
		if (typeof document == 'undefined' || !document.cookie) {
			return '';
		}
		return (
			document.cookie
				.split(';')
				.map((part) => part.trim())
				.find((part) => part.startsWith(`${name}=`))
				?.slice(name.length + 1) ?? ''
		);
	}

	/**
	 * @param {string} source
	 * @returns {string}
	 */
	function fetchPreset(source) {
		const entries = JSON.stringify(requestEntries(source), null, 2);
		return `const response = await fetch(${JSON.stringify(endpointUrl())}, {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/x-www-form-urlencoded",
    "x-xsrf-token": ${xsrfTokenExpression()}
  },
  body: new URLSearchParams(${entries})
});

console.log(await response.text());`;
	}

	/**
	 * @param {string} format
	 * @param {string} source
	 * @returns {string}
	 */
	function buildCopyPreset(format, source = 'current') {
		if (!requestable || !projectName) {
			return '';
		}
		if (format === 'curl') {
			return curlPreset(source);
		}
		if (format === 'fetch') {
			return fetchPreset(source);
		}
		if (format === 'body') {
			return bodyPreset(source);
		}
		return urlPreset(source);
	}

	/**
	 * @param {string} format
	 */
	async function copyAs(format, source = 'current') {
		const content = buildCopyPreset(format, source);
		if (!content) {
			return;
		}
		try {
			await navigator.clipboard.writeText(content);
			toaster.success({
				description: `Copied ${copyFormats.find(({ value }) => value === format)?.label ?? 'preset'}`,
				duration: 2000
			});
			copyAsSource = '';
		} catch (err) {
			toaster.error({
				description: String(err instanceof Error ? err.message : err),
				duration: 4200
			});
		}
	}

	/**
	 * @param {{ open: boolean }} event
	 * @param {string} source
	 */
	function handleCopyOpenChange(event, source) {
		copyAsSource = event.open ? source : '';
	}

	/**
	 * Submits a binary request through the browser so the response can be streamed to disk.
	 *
	 * @param {HTMLFormElement} form
	 * @param {FormData} data
	 */
	function submitBinary(form, data) {
		const token = localStorage.getItem('x-xsrf-token');
		if (token && !data.has('__xsrfToken')) {
			data.append('__xsrfToken', token);
		}

		/** @param {FormDataEvent} event */
		const replaceFormData = (event) => {
			for (const key of new Set(event.formData.keys())) {
				event.formData.delete(key);
			}
			for (const [key, value] of data.entries()) {
				event.formData.append(key, value);
			}
		};

		const attributes = ['action', 'method', 'enctype', 'target'];
		const previous = Object.fromEntries(attributes.map((name) => [name, form.getAttribute(name)]));
		form.addEventListener('formdata', replaceFormData, { once: true });
		try {
			form.action = getUrl('projects/' + projectName + '/.' + mode.toLowerCase());
			form.method = 'POST';
			form.enctype = [...data.values()].some((value) => value instanceof File)
				? 'multipart/form-data'
				: 'application/x-www-form-urlencoded';
			form.target = downloadTarget;
			HTMLFormElement.prototype.submit.call(form);
		} finally {
			form.removeEventListener('formdata', replaceFormData);
			for (const name of attributes) {
				if (previous[name] == null) {
					form.removeAttribute(name);
				} else {
					form.setAttribute(name, previous[name]);
				}
			}
		}
	}

	/**
	 * @param {SubmitEvent & { currentTarget: HTMLFormElement }} event
	 */
	/** @type {HTMLFormElement | undefined} */
	let form = $state();

	$effect(() => {
		// a test case to run, asked from elsewhere, as the "Run" of a test case of the tree, or * for the
		// requestable itself, as F5 in the tree
		const name = runTestcase;
		if (!name || !form || !requestable) {
			return;
		}
		if (name !== '*' && !(requestable.testcase ?? []).some((testcase) => testcase?.name === name)) {
			return;
		}
		untrack(() => {
			onRunTestcaseTaken?.();
			if (form) {
				void execute(form, name === '*' ? '' : name);
			}
		});
	});

	async function run(event) {
		event.preventDefault();
		const submitter = /** @type {HTMLButtonElement | null} */ (event.submitter);
		if (submitter?.value === '__clear') {
			updateResponse({ content: '', loading: false });
			return;
		}
		await execute(event.currentTarget, submitter?.value ?? '');
	}

	/**
	 * @param {HTMLFormElement} target
	 * @param {string} testcase the test case whose variables the requestable runs with, or none
	 */
	async function execute(target, testcase) {
		if (!requestable || !projectName || disabled) {
			return;
		}
		// a property of the Studio committed as the button takes the focus is applied before the run
		await settlePropertyApply();
		const fd = new FormData(target);
		if (testcase) {
			fd.append('__testcase', testcase);
			for (const key of [...fd.keys()]) {
				if (!String(key).startsWith('__')) {
					fd.delete(key);
				}
			}
		} else {
			for (const variable of requestable.variable ?? []) {
				if (!shouldSendVariable(variable)) {
					fd.delete(variable.name);
				}
			}
		}
		if (mode.toUpperCase() === 'BIN') {
			updateResponse({ content: '', loading: false });
			submitBinary(target, fd);
			return;
		}
		updateResponse({ content: 'Loading ...', loading: true });
		validation = null;
		xmlResponse = mode.toUpperCase() === 'XML';
		rawData = null;
		if (watchesConnector) {
			await call('studio.dbo.ConnectorData', {
				action: 'watch',
				project: projectName,
				connector: connectorName
			});
		}
		let context = '';
		if (fd.get('__context') === 'studio-web-execution-*') {
			// a context of its own, which Stop aborts
			context = `studio-web-execution-${crypto.randomUUID()}`;
			fd.set('__context', context);
			runningContext = context;
		}
		let token = '';
		if (debug && debuggable) {
			token = crypto.randomUUID();
			await call('studio.debug.Steps', { action: 'start' });
			fd.append('__debug', token);
			debugToken = token;
			void followDebug(token, mode.toUpperCase() === 'JSON');
		}
		try {
			const data = await callRequestable(mode, projectName, fd).finally(() => {
				if (token && debugToken === token) {
					debugToken = '';
					debugState = null;
				}
				if (context && runningContext === context) {
					runningContext = '';
				}
			});
			updateResponse({
				content: await data.text(),
				language: data.headers.get('Content-Type')?.includes('json') ? 'json' : 'xml'
			});
			if (stubbable && autoValidate && xmlResponse && responseView.language === 'xml') {
				await validateResponse();
			}
			if (watchesConnector) {
				const result = await call('studio.dbo.ConnectorData', {
					project: projectName,
					connector: connectorName
				});
				rawData = result?.kind && result.kind !== 'none' ? result : null;
			}
		} catch (err) {
			updateResponse({
				content: String(err instanceof Error ? err.message : err),
				language: 'text'
			});
		} finally {
			updateResponse({ loading: false });
		}
	}
</script>

{#snippet copyAsButton(source)}
	<Popover
		open={copyAsSource === source}
		onOpenChange={(event) => handleCopyOpenChange(event, source)}
	>
		<Popover.Trigger
			type="button"
			class="button-secondary layout-x-low h-full min-h-fit text-wrap"
			disabled={disabled || !projectName}
			title="Copy request"
			aria-label="Copy request"
		>
			<span>Copy</span>
			<Ico icon="mdi:content-copy" size="btn" />
		</Popover.Trigger>
		<Popover.Positioner class="z-[160]" style="z-index: 160;">
			<Popover.Content class="border-none bg-transparent p-0 shadow-none">
				<div class="requestable-copy-as">
					<div class="layout-y-stretch-low">
						<div class="requestable-copy-as__formats">
							{#each copyFormats as format (format.value)}
								<Button
									label={format.label}
									full={false}
									class="button-primary"
									icon={format.icon}
									onclick={() => copyAs(format.value, source)}
								/>
							{/each}
						</div>
						<a
							class="requestable-copy-as__preview requestable-copy-as__preview--link"
							href={buildCopyPreset('url', source)}
							target="_blank"
							rel="noreferrer noopener">{buildCopyPreset('url', source)}</a
						>
					</div>
				</div>
				<Popover.Arrow class="fill-primary-100-900" />
			</Popover.Content>
		</Popover.Positioner>
	</Popover>
{/snippet}

{#if requestable}
	<form class={['requestable-execution', cls]} onsubmit={run} bind:this={form}>
		<iframe hidden name={downloadTarget} title="Binary download target"></iframe>
		{#if kind === 'transaction'}
			<input type="hidden" name="__connector" value={connectorName} />
			<input type="hidden" name="__transaction" value={requestable.name} />
		{:else}
			<input type="hidden" name="__sequence" value={requestable.name} />
		{/if}
		<input type="hidden" name="__nocache" value="true" />
		{#if stub}
			<input type="hidden" name="__stub" value="true" />
			{#if stubFile && stubFile !== defaultStub}
				<input type="hidden" name="__stub_filename" value={stubFile} />
			{/if}
		{/if}
		{#if freshContext}
			<input type="hidden" name="__context" value="studio-web-execution-*" />
			<input type="hidden" name="__removeContext" value="true" />
		{/if}

		{#if showIntro}
			<div class="requestable-execution__intro">
				<div>
					<strong>{requestable.name}</strong>
					{#if requestable.comment}
						<span>{requestable.comment}</span>
					{/if}
				</div>
				<PropertyType type="segment" bind:value={mode} item={modes} fit={true} />
			</div>
		{/if}

		{#if showComment && requestable.comment?.length}
			<p class="requestable-execution__comment">
				{requestable.comment}
			</p>
		{/if}

		{#if hasVariables}
			<RequestableVariables bind:requestable />
		{/if}

		{#if hasTestcases}
			<RequestableTestCases bind:requestable value={testcaseValue} showEdit={showTestcaseEdit}>
				{#snippet copyAs(testcase)}
					{@render copyAsButton(`testcase:${testcase.name}`)}
				{/snippet}
			</RequestableTestCases>
		{/if}

		<div
			class={[
				'requestable-execution__actions',
				stickyActions && 'requestable-execution__actions--sticky'
			]}
		>
			{#if !showIntro}
				<PropertyType type="segment" bind:value={mode} item={modes} fit={true} />
			{/if}
			<ActionBar wrap full={false}>
				<Button
					label="Execute"
					full={false}
					type="submit"
					class="button-primary"
					icon="mdi:play-circle-outline"
					disabled={disabled || responseView.loading}
				/>
				{@render copyAsButton('current')}
				{#if stubbable && hasResponse && responseView.language === 'xml' && !responseView.loading}
					<Button
						label="Save as stub"
						full={false}
						class="button-secondary"
						icon="mdi:content-save-outline"
						title="Save this response as the stub of the requestable"
						onclick={saveStub}
						{disabled}
					/>
				{/if}
				{#if stubbable && xmlResponse && responseKey === requestableKey && responseView.language === 'xml' && !responseView.loading}
					<Button
						label="Validate"
						full={false}
						class="button-secondary"
						icon="mdi:check"
						title="Validate this response against the schema of the project"
						onclick={validateResponse}
						{disabled}
					/>
				{/if}
				{#if stubbable && xmlResponse}
					<Button
						full={false}
						class={['button-secondary', autoValidate && 'preset-filled-primary-100-900']}
						icon={autoValidate ? 'mdi:check-decagram' : 'mdi:check-decagram-outline'}
						title={autoValidate
							? 'Each XML response is validated against the schema: stop it'
							: 'Validate each XML response against the schema of the project'}
						ariaLabel="Validate each response"
						aria-pressed={autoValidate}
						onclick={toggleAutoValidate}
					/>
				{/if}
				{#if onChanged && kind === 'transaction' && xmlResponse && responseKey === requestableKey && responseView.language === 'xml' && !responseView.loading}
					<Button
						label="Update schema"
						full={false}
						class="button-secondary"
						icon="mdi:file-tree-outline"
						title="Generate the schema of the transaction again from this response"
						onclick={updateSchema}
						{disabled}
					/>
				{/if}
				{#if stubbable && runningContext && responseView.loading}
					<Button
						label="Stop"
						full={false}
						class="button-secondary"
						icon="mdi:stop"
						title="Stop the execution"
						onclick={abort}
					/>
				{/if}
				{#if debuggable}
					<label
						class="requestable-execution__stub"
						title="Stop before each step of the sequence, as the Debug mode of the Eclipse Studio"
					>
						<input type="checkbox" bind:checked={debug} disabled={disabled || !!debugToken} />
						Debug
					</label>
				{/if}
				{#if stubbable}
					<label
						class="requestable-execution__stub"
						title="Answer with the stub recorded for this requestable"
					>
						<input type="checkbox" bind:checked={stub} {disabled} />
						From stub
					</label>
					{#if stub && stubFiles.length > 1}
						<select
							class="requestable-execution__stub-file input-common"
							aria-label="Stub file"
							bind:value={stubFile}
							{disabled}
						>
							{#each stubFiles as file (file)}
								<option value={file}>{file}</option>
							{/each}
						</select>
					{/if}
				{/if}
				{#if hasResponse}
					<Button
						label="Clear"
						full={false}
						type="submit"
						value="__clear"
						class="button-secondary"
						icon="mdi:broom"
						{disabled}
					/>
				{/if}
			</ActionBar>
		</div>

		{#if debugState}
			<div class="requestable-execution__debug" role="toolbar" aria-label="Sequence debug">
				<span class="requestable-execution__debug-step">
					<Ico icon={debugState.paused ? 'mdi:pause-circle-outline' : 'mdi:play'} size={4} />
					{#if debugState.step}
						{debugState.paused ? 'Stopped before' : 'Running'}
						<strong>{debugState.step.name}</strong>
					{:else}
						Waiting for the first step
					{/if}
				</span>
				<button
					type="button"
					class="button-secondary"
					title="Run the step and stop before the next one"
					disabled={!debugState.paused}
					onclick={() => debugAction('step')}
					><Ico icon="mdi:debug-step-over" size={4} /> Step</button
				>
				<button
					type="button"
					class="button-secondary"
					title="Run without stopping"
					disabled={!debugState.stepByStep}
					onclick={() => debugAction('run')}><Ico icon="mdi:play" size={4} /> Run</button
				>
				<button
					type="button"
					class="button-secondary"
					title="Stop before the next step"
					disabled={debugState.stepByStep}
					onclick={() => debugAction('pause')}><Ico icon="mdi:pause" size={4} /> Pause</button
				>
				<button
					type="button"
					class="button-secondary"
					title="Stop debugging, the sequence runs on"
					onclick={() => debugAction('stop')}
					><Ico icon="mdi:stop" size={4} /> Stop debugging</button
				>
			</div>
		{/if}

		{#if validation && !responseView.loading}
			<p
				class={[
					'requestable-execution__validation',
					validation.valid
						? 'requestable-execution__validation--valid'
						: 'requestable-execution__validation--invalid'
				]}
				role="status"
			>
				{validation.message}
			</p>
		{/if}
		{#if hasResponse}
			<div transition:fly={{ duration: 180, y: -24 }}>
				{#key responseEditorKey}
					<RequestableResponseEditor
						content={responseView.content}
						language={responseView.language}
						theme={responseTheme}
						loading={responseView.loading && !debugState?.paused}
					/>
				{/key}
			</div>
		{/if}
		{#if rawData && !responseView.loading}
			<details class="requestable-execution__raw">
				<summary>
					Connector data
					{#if rawData.kind === 'table'}
						<small
							>{rawData.total} row{rawData.total === 1 ? '' : 's'}{(rawData.rows?.length ?? 0) <
							(rawData.total ?? 0)
								? `, first ${rawData.rows?.length} shown`
								: ''}</small
						>
					{/if}
				</summary>
				{#if rawData.kind === 'table'}
					<div class="requestable-execution__raw-table">
						<table>
							<thead>
								<tr>
									{#each rawData.headers ?? [] as header, index (index)}
										<th>{header}</th>
									{/each}
								</tr>
							</thead>
							<tbody>
								{#each rawData.rows ?? [] as row, index (index)}
									<tr>
										{#each row as cell, column (column)}
											<td>{cell}</td>
										{/each}
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{:else}
					<pre>{rawData.text}</pre>
				{/if}
			</details>
		{/if}
	</form>
{/if}

<style>
	.requestable-execution__validation {
		margin: 0;
		font-size: 0.78rem;
		overflow-wrap: anywhere;
	}

	.requestable-execution__validation--valid {
		color: var(--color-success-600-400);
	}

	.requestable-execution__validation--invalid {
		color: var(--color-error-600-400);
	}

	.requestable-execution__raw {
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		font-size: 0.75rem;
	}

	.requestable-execution__raw summary {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		cursor: pointer;
		padding: 0.4rem 0.6rem;
		font-weight: 600;
	}

	.requestable-execution__raw summary small {
		opacity: 0.7;
		font-weight: 400;
	}

	.requestable-execution__raw pre {
		max-height: 24rem;
		overflow: auto;
		margin: 0;
		border-top: 1px solid var(--color-surface-200-800);
		padding: 0.6rem;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.requestable-execution__raw-table {
		max-height: 24rem;
		overflow: auto;
		border-top: 1px solid var(--color-surface-200-800);
	}

	.requestable-execution__raw-table table {
		border-collapse: collapse;
	}

	.requestable-execution__raw-table :is(th, td) {
		border-bottom: 1px solid var(--color-surface-200-800);
		padding: 0.2rem 0.5rem;
		text-align: left;
		white-space: nowrap;
	}

	.requestable-execution__raw-table th {
		position: sticky;
		top: 0;
		background: var(--color-surface-100-900);
	}

	.requestable-execution__stub-file {
		width: auto;
		max-width: 14rem;
		height: 1.9rem;
		padding-block: 0;
		font-size: 0.75rem;
	}

	.requestable-execution__debug {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.4rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		padding: 0.4rem 0.5rem;
		font-size: 0.78rem;
	}

	.requestable-execution__debug-step {
		display: inline-flex;
		flex: 1;
		align-items: center;
		gap: 0.35rem;
		min-width: 0;
	}

	.requestable-execution__debug button {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		height: 1.9rem;
		padding-inline: 0.55rem;
		font-size: 0.75rem;
	}

	.requestable-execution__stub {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		font-size: 0.78rem;
		white-space: nowrap;
	}

	.requestable-execution {
		display: grid;
		gap: 0.75rem;
		align-content: start;
	}

	.requestable-execution__intro {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 0.75rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 60%, transparent);
		padding: 0.65rem;
	}

	.requestable-execution__intro div {
		display: grid;
		min-width: 0;
		gap: 0.15rem;
	}

	.requestable-execution__intro strong,
	.requestable-execution__intro span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.requestable-execution__intro strong {
		font-size: 0.95rem;
	}

	.requestable-execution__intro span {
		color: var(--color-surface-600-400);
		font-size: 0.78rem;
	}

	.requestable-execution__comment {
		border: 1px dashed var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: color-mix(in oklab, var(--color-surface-50-950) 82%, transparent);
		padding: 0.6rem 0.75rem;
		color: var(--color-surface-600-400);
		font-size: 0.86rem;
	}

	.requestable-execution__actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		flex-wrap: wrap;
		border: 1px dashed var(--color-surface-200-800);
		border-radius: 0.55rem;
		background: color-mix(in oklab, var(--color-surface-50-950) 88%, transparent);
		padding: 0.75rem;
	}

	.requestable-execution__actions--sticky {
		position: sticky;
		bottom: 0.75rem;
		z-index: 10;
		box-shadow: 0 18px 36px -28px var(--color-surface-900);
		backdrop-filter: blur(8px);
	}

	.requestable-copy-as {
		width: min(34rem, calc(100vw - 2rem));
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.55rem;
		background: color-mix(in oklab, var(--color-surface-50-950) 94%, transparent);
		padding: 0.75rem;
		box-shadow: 0 18px 42px -24px var(--color-surface-900);
	}

	.requestable-copy-as__formats {
		display: flex;
		flex-wrap: wrap;
		gap: 0.45rem;
	}

	.requestable-copy-as__preview {
		max-height: 12rem;
		overflow: auto;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 72%, transparent);
		padding: 0.65rem;
		color: var(--color-surface-700-300);
		font-size: 0.72rem;
		line-height: 1.35;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.requestable-copy-as__preview--link {
		display: block;
		text-decoration: none;
	}

	.requestable-copy-as__preview--link:hover {
		color: var(--color-primary-600-400);
		text-decoration: underline;
	}
</style>
