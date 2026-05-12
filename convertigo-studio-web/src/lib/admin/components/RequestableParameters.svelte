<script>
	import { checkArray } from '$lib/utils/service';
	import Button from './Button.svelte';
	import PropertyType from './PropertyType.svelte';

	let { requestable, savedParameters = {}, class: className = '' } = $props();

	let selectedTestcase = $state('0');
	let variables = $state([]);
	let appliedKey = '';
	let testcases = $derived(checkArray(requestable?.testcase).filter(({ name }) => name != null));
	let testcaseItems = $derived([
		{ value: '0', text: 'Custom parameters' },
		...testcases.map(({ name }) => ({ value: name, text: name }))
	]);
	let testcase = $derived(testcases.find(({ name }) => name == selectedTestcase));

	function firstValue(values, fallback = '') {
		return String(checkArray(values)[0] ?? fallback ?? '');
	}

	function parseMultipleValues(value) {
		try {
			return checkArray(JSON.parse(value)).map((val) => ({ val: String(val ?? '') }));
		} catch {
			return [];
		}
	}

	function initialMultipleValues(variable, values) {
		const multipleValues = values.length
			? values.map((val) => ({ val: String(val ?? '') }))
			: parseMultipleValues(variable.value);
		return multipleValues.length ? multipleValues : [{ val: '' }];
	}

	function restoreMultipleValues(variable) {
		variable.send = true;
		variable.multipleValues = initialMultipleValues(variable, []);
	}

	function addMultipleValue(variable) {
		variable.send = true;
		variable.multipleValues ??= [];
		variable.multipleValues.push({ val: '' });
	}

	function parameterName(variable) {
		return variable.send ? `requestable_parameter_${variable.name}` : undefined;
	}

	$effect(() => {
		const key = JSON.stringify({
			requestable: requestable?.name,
			variables: checkArray(requestable?.variable)
				.filter(({ name }) => name != null)
				.map(({ name, value }) => [name, value]),
			savedParameters
		});
		if (key == appliedKey) {
			return;
		}

		appliedKey = key;
		selectedTestcase = firstValue(savedParameters.__testcase, '0') || '0';
		variables = checkArray(requestable?.variable)
			.filter(({ name }) => name != null)
			.map((variable) => {
				const values = checkArray(savedParameters[variable.name]);
				return {
					...variable,
					send: values.length > 0,
					val: firstValue(values, variable.value),
					multipleValues:
						variable.isMultivalued == 'true' ? initialMultipleValues(variable, values) : undefined
				};
			});
	});
</script>

{#if requestable}
	<div class="layout-y-stretch {className}">
		<PropertyType
			type="combo"
			name="parameters"
			description="Parameters"
			bind:value={selectedTestcase}
			item={testcaseItems}
			multiple={false}
		/>

		{#if selectedTestcase == '0'}
			{#if variables.length > 0}
				<div
					class="layout-y-stretch rounded-md border border-surface-200-800/50 bg-surface-50-950/40 p-3"
				>
					{#each variables as variable (variable.name)}
						<div
							class="grid grid-cols-[minmax(9rem,auto)_minmax(14rem,1fr)] items-end gap-low max-sm:grid-cols-1"
						>
							<PropertyType
								type="check"
								fit={true}
								label={variable.name}
								checked={variable.send}
								tooltip={variable.description || variable.comment}
								onCheckedChange={(event) => {
									variable.send = event.checked;
								}}
							/>
							{#if variable.isMultivalued == 'true'}
								<div class="layout-y-low">
									<div class="layout-x-low">
										<Button
											full={false}
											type="button"
											icon="mdi:plus"
											title="Add value"
											class="button-ico-primary h-7 w-7 min-w-7 justify-center p-0!"
											onclick={() => addMultipleValue(variable)}
										/>
										<Button
											full={false}
											type="button"
											icon="mdi:backup-restore"
											title="Restore list"
											class="button-ico-primary h-7 w-7 min-w-7 justify-center p-0!"
											onclick={() => restoreMultipleValues(variable)}
										/>
									</div>
									{#each variable.multipleValues ?? [] as value, i (value)}
										<div class="layout-x-low">
											<PropertyType
												type={variable.isMasked == 'true' ? 'password' : 'text'}
												name={parameterName(variable)}
												bind:value={() => value.val ?? '', (val) => (value.val = val)}
												tooltip={variable.description || variable.comment}
												onfocus={() => {
													variable.send = true;
												}}
											/>
											<Button
												full={false}
												type="button"
												icon="mdi:delete-outline"
												title="Delete value"
												class="button-ico-primary h-9 w-9 min-w-9 justify-center p-0!"
												onclick={() => variable.multipleValues?.splice(i, 1)}
											/>
										</div>
									{/each}
								</div>
							{:else}
								<PropertyType
									type={variable.isMasked == 'true' ? 'password' : 'text'}
									name={parameterName(variable)}
									bind:value={() => variable.val ?? '', (value) => (variable.val = value)}
									tooltip={variable.description || variable.comment}
									onfocus={() => {
										variable.send = true;
									}}
								/>
							{/if}
						</div>
					{/each}
				</div>
			{/if}
		{:else if checkArray(testcase?.variable).length > 0}
			<div
				class="layout-y-stretch rounded-md border border-surface-200-800/50 bg-surface-50-950/40 p-3"
			>
				{#each checkArray(testcase.variable) as variable (variable.name)}
					<div
						class="grid grid-cols-[minmax(9rem,auto)_minmax(14rem,1fr)] gap-low max-sm:grid-cols-1"
					>
						<p class="text-sm font-medium text-strong">{variable.name}</p>
						<p class="text-sm break-words whitespace-pre-wrap text-surface-600-400">
							{variable.value}
						</p>
					</div>
				{/each}
			</div>
		{/if}
	</div>
{/if}
