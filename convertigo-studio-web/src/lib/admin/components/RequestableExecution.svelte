<script>
	import { Popover } from '@skeletonlabs/skeleton-svelte';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import RequestableTestCases from '$lib/admin/components/RequestableTestCases.svelte';
	import RequestableVariables from '$lib/admin/components/RequestableVariables.svelte';
	import LightSvelte from '$lib/common/Light.svelte';
	import RequestableResponseEditor from '$lib/dashboard/RequestableResponseEditor.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { callRequestable, getUrl, toaster } from '$lib/utils/service';
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
		return {
			content: requestable?.response ?? '',
			language: requestable?.language ?? 'json',
			loading: requestable?.loading === true
		};
	});
	let hasResponse = $derived(responseView.content.length > 0 || responseView.loading);
	let responseTheme = $derived(LightSvelte.light ? '' : 'vs-dark');
	let responseEditorKey = $derived(`${requestableKey}\u0000${responseRevision}`);

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
	async function run(event) {
		event.preventDefault();
		if (!requestable || !projectName || disabled) {
			return;
		}
		const submitter = /** @type {HTMLButtonElement | null} */ (event.submitter);
		if (submitter?.value === '__clear') {
			updateResponse({ content: '', loading: false });
			return;
		}
		const fd = new FormData(event.currentTarget);
		if (submitter?.value) {
			fd.append('__testcase', submitter.value);
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
			submitBinary(event.currentTarget, fd);
			return;
		}
		updateResponse({ content: 'Loading ...', loading: true });
		try {
			const data = await callRequestable(mode, projectName, fd);
			updateResponse({
				content: await data.text(),
				language: data.headers.get('Content-Type')?.includes('json') ? 'json' : 'xml'
			});
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
	<form class={['requestable-execution', cls]} onsubmit={run}>
		<iframe hidden name={downloadTarget} title="Binary download target"></iframe>
		{#if kind === 'transaction'}
			<input type="hidden" name="__connector" value={connectorName} />
			<input type="hidden" name="__transaction" value={requestable.name} />
		{:else}
			<input type="hidden" name="__sequence" value={requestable.name} />
		{/if}
		<input type="hidden" name="__nocache" value="true" />

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

		{#if hasResponse}
			<div transition:fly={{ duration: 180, y: -24 }}>
				{#key responseEditorKey}
					<RequestableResponseEditor
						content={responseView.content}
						language={responseView.language}
						theme={responseTheme}
						loading={responseView.loading}
					/>
				{/key}
			</div>
		{/if}
	</form>
{/if}

<style>
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
