<script>
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';

	/**
	 * @type {{
	 *  requestable?: any,
	 *  value?: string,
	 *  showEdit?: boolean,
	 *  copyAs?: import('svelte').Snippet<[any]>
	 * }}
	 */
	let { requestable = $bindable(), value = 'testcases', showEdit = true, copyAs } = $props();

	let testcases = $derived(requestable?.testcase ?? []);

	/**
	 * @param {any} testcase
	 */
	function applyTestcase(testcase) {
		if (requestable) {
			requestable = {
				...requestable,
				tc: {
					...testcase,
					variable: testcase.variable?.map((variable) => ({ ...variable })) ?? []
				}
			};
		}
	}
</script>

{#if testcases.length > 0}
	<AccordionGroup
		collapsible
		value={requestable?.testCasesOpened ? [value] : []}
		onValueChange={({ value: openValues }) => {
			requestable.testCasesOpened = (openValues ?? []).includes(value);
		}}
		class="rounded-md border border-surface-200-800/40 bg-surface-50-950/45"
	>
		<AccordionSection
			{value}
			class="rounded-md"
			title={`Test cases (${testcases.length})`}
			triggerClass="group w-full rounded-md px-2.5 py-2"
			titleClass="text-sm font-semibold"
			panelClass="bg-transparent px-2.5 pb-2 pt-0"
		>
			{#snippet panel()}
				<div class="requestable-testcases__grid">
					{#each testcases as testcase (testcase.name)}
						{@const testcaseValuesOpen = testcase.valuesOpened ?? false}
						<Card title={testcase.name} bg="bg-surface-50-950/70" class="gap-2 p-low">
							{#snippet cornerOption()}
								<div class="requestable-testcases__actions">
									<Button
										label="Execute"
										type="submit"
										value={testcase.name}
										class="button-primary"
										icon="mdi:play-circle-outline"
										full={false}
									/>
									{@render copyAs?.(testcase)}
									{#if showEdit}
										<Button
											label="Edit"
											class="button-secondary"
											icon="mdi:edit-outline"
											onclick={() => applyTestcase(testcase)}
											full={false}
										/>
									{/if}
								</div>
							{/snippet}
							<AccordionGroup
								collapsible
								value={testcaseValuesOpen ? ['values'] : []}
								onValueChange={({ value: openValues }) => {
									testcase.valuesOpened = (openValues ?? []).includes('values');
								}}
								class="rounded-md border border-surface-200-800/40 bg-surface-100-900/25"
							>
								<AccordionSection
									value="values"
									class="rounded-md"
									title={`Preset values (${testcase.variable?.length ?? 0})`}
									triggerClass="w-full rounded-md px-2.5 py-2"
									titleClass="text-sm font-semibold text-strong"
									panelClass="bg-transparent px-2 pb-2 pt-0"
								>
									{#snippet panel()}
										<div class="requestable-testcases__values">
											{#each testcase.variable ?? [] as variable (variable.name)}
												<div class="requestable-testcases__value">
													<div class="requestable-testcases__name">{variable.name}</div>
													<div class="requestable-testcases__content">
														{variable.value}
													</div>
												</div>
											{/each}
										</div>
									{/snippet}
								</AccordionSection>
							</AccordionGroup>
						</Card>
					{/each}
				</div>
			{/snippet}
		</AccordionSection>
	</AccordionGroup>
{/if}

<style>
	.requestable-testcases__grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(min(100%, 16rem), 1fr));
		gap: 0.5rem;
	}

	.requestable-testcases__actions {
		display: flex;
		flex-wrap: wrap;
		justify-content: flex-end;
		gap: 0.45rem;
	}

	.requestable-testcases__values {
		display: grid;
		gap: 0.45rem;
	}

	.requestable-testcases__value {
		border-bottom: 1px solid var(--color-surface-200-800);
		padding-bottom: 0.45rem;
	}

	.requestable-testcases__value:last-child {
		border-bottom: 0;
		padding-bottom: 0;
	}

	.requestable-testcases__name {
		color: var(--color-surface-900-100);
		font-size: 0.78rem;
		font-weight: 650;
	}

	.requestable-testcases__content {
		color: var(--color-surface-600-400);
		font-size: 0.72rem;
		overflow-wrap: anywhere;
		white-space: pre-wrap;
	}
</style>
