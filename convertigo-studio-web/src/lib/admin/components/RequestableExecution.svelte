<script>
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import RequestableTestCases from '$lib/admin/components/RequestableTestCases.svelte';
	import RequestableVariables from '$lib/admin/components/RequestableVariables.svelte';
	import LightSvelte from '$lib/common/Light.svelte';
	import RequestableResponseEditor from '$lib/dashboard/RequestableResponseEditor.svelte';
	import { callRequestable } from '$lib/utils/service';
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
	let hasVariables = $derived((requestable?.variable?.length ?? 0) > 0);
	let hasTestcases = $derived((requestable?.testcase?.length ?? 0) > 0);
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
		updateResponse({ content: 'Loading ...', loading: true });
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

{#if requestable}
	<form class={['requestable-execution', cls]} onsubmit={run}>
		{#if kind === 'transaction'}
			<input type="hidden" name="__connector" value={connectorName} />
			<input type="hidden" name="__transaction" value={requestable.name} />
		{:else}
			<input type="hidden" name="__sequence" value={requestable.name} />
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
			<RequestableTestCases bind:requestable value={testcaseValue} showEdit={showTestcaseEdit} />
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
</style>
