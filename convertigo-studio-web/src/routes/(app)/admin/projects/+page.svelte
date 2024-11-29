<script>
	import { onMount } from 'svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import { base } from '$app/paths';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import { call, getUrl } from '$lib/utils/service';
	import { FileUpload } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';

	onMount(() => {
		Projects.refresh();
	});

	let exportChoices = $state({});

	let modalDelete = $state();
	let modalExport = $state();
	let modalDeployUpload = $state();
	let modalDeployURL = $state();
	let modalSymbols;

	/** @type {any} */
	let bag = $state({});
</script>

<ModalYesNo bind:this={modalDelete} />
<ModalDynamic bind:this={modalExport}>
	{#snippet children({ close, params: { options, project } })}
		<Card title="Exporting {project}" class="!items-stretch">
			{#each options as { name, display }}
				<CheckState {name} bind:value={exportChoices[name]}>{display}</CheckState>
			{/each}
			<ResponsiveButtons
				class="w-full"
				buttons={[
					{
						icon: 'bytesize:export',
						label: 'Export',
						cls: 'green-button',
						onclick: () => {
							location.href = `${getUrl()}projects.Export?__xsrfToken=${encodeURIComponent(localStorage.getItem('x-xsrf') ?? '')}&projectName=${encodeURIComponent(project)}&exportOptions=${encodeURIComponent(JSON.stringify(exportChoices))}`;
							close();
						}
					},
					{
						icon: 'material-symbols-light:cancel-outline',
						label: 'Cancel',
						cls: 'cancel-button',
						onclick: close
					}
				]}
			/>
		</Card>
	{/snippet}
</ModalDynamic>
<ModalDynamic bind:this={modalDeployUpload}>
	<Card title="Drop or choose a .car/.zip file and Deploy">
		<form
			onsubmit={async (event) => {
				event.preventDefault();
				bag.uploading = true;
				// @ts-ignore
				await call('projects.Deploy', new FormData(event?.target));
				bag.uploading = false;
				Projects.refresh();
				modalDeployUpload.close();
			}}
		>
			<fieldset disabled={bag.uploading}>
				<FileUpload
					name="file"
					accept={{ 'application/zip': ['.car', '.zip'] }}
					maxFiles={1}
					subtext="then press Deploy"
					classes="w-full"
					required
					allowDrop
				>
					{#snippet iconInterface()}<Ico icon="carbon:application" size="8" />{/snippet}
					{#snippet iconFile()}<Ico icon="mdi:briefcase-upload-outline" size="8" />{/snippet}
					{#snippet iconFileRemove()}<Ico
							icon="material-symbols-light:delete-outline"
							size="8"
						/>{/snippet}
				</FileUpload>
				<CheckState name="bAssembleXsl" value="false"
					>Assemble XSL files included in style sheets when deploying</CheckState
				>
				<div class="w-full layout-x justify-end">
					<button type="submit" class="basic-button"
						><span><Ico icon="carbon:application" size="btn" /></span><span>Deploy</span></button
					>
					<button type="button" onclick={modalDeployUpload.close} class="cancel-button"
						><span><Ico icon="material-symbols-light:cancel-outline" size="btn" /></span><span
							>Cancel</span
						></button
					>
				</div>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>
<ModalDynamic bind:this={modalDeployURL}>
	<Card title="Import from a Remote Project URL">
		<form
			onsubmit={async (event) => {
				event.preventDefault();
				bag.uploading = true;
				// @ts-ignore
				await call('projects.ImportURL', new FormData(event?.target));
				bag.uploading = false;
				Projects.refresh();
				modalDeployURL.close();
			}}
		>
			<fieldset disabled={bag.uploading} class="layout-y !items-start">
				<p>Import a project from url like:</p>
				<p class="font-bold">
					{'<project name>=<git or http URL>[:path=<optional subpath>][:branch=<optional branch>]'}
				</p>
				<p>Or a Convertigo Archive HTTP(S) URL.</p>
				<input name="url" type="text" class="input w-full" required />
				<div class="w-full layout-x justify-end">
					<button type="submit" class="basic-button"
						><span><Ico icon="bytesize:import" size="btn" /></span><span>Import</span></button
					>
					<button type="button" onclick={modalDeployURL.close} class="cancel-button"
						><span><Ico icon="material-symbols-light:cancel-outline" size="btn" /></span><span
							>Cancel</span
						></button
					>
				</div>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>
<ModalDynamic bind:this={modalSymbols} class="w-[800px]">
	{#snippet children({ close, params: { symbols, project } })}
		<Card title="Undefined Global Symbols" class="!items-start w-full">
			<p>Find here the undefined Global Symbols for the project <b>{project}</b>:</p>
			<div class="layout-x flex-wrap w-full">
				{#each symbols as symbol}
					<div class="rounded preset-filled-secondary-200-800 px-low">{symbol}</div>
				{/each}
			</div>
			<div class="w-full layout-x justify-end">
				<button onclick={() => close(true)} class="basic-button"
					><span><Ico icon="et:tools-2" size="btn" /></span><span>Create symbols</span></button
				>
				<button onclick={close} class="cancel-button"
					><span><Ico icon="material-symbols-light:cancel-outline" size="btn" /></span><span
						>Cancel</span
					></button
				>
			</div>
		</Card>
	{/snippet}
</ModalDynamic>
<Card title="Projects">
	{#snippet cornerOption()}
		<ResponsiveButtons
			buttons={[
				{
					icon: 'carbon:application',
					value: 'deploy',
					cls: 'basic-button',
					label: 'Deploy project',
					onclick: modalDeployUpload?.open
				},
				{
					icon: 'bytesize:import',
					value: 'export',
					cls: 'basic-button',
					label: 'Import a Remote Project URL',
					onclick: modalDeployURL?.open
				}
			]}
			class="max-w-4xl"
		/>
	{/snippet}
	<TableAutoCard
		definition={[
			{ name: 'Actions', custom: true },
			{ name: 'Name', key: 'name', class: 'font-medium' },
			{ name: 'Comment', key: 'comment' },
			{ name: 'Version', key: 'version', class: 'break-words opacity-80 min-w-32' },
			{ name: 'Exported', key: 'exported', class: 'text-sm min-w-32' },
			{ name: 'Deployment', key: 'deployDate', class: 'text-sm min-w-32' }
		]}
		data={Projects.projects}
		class="rounded"
	>
		{#snippet children({ row: { name, undefined_symbols }, def })}
			{@const project = name ? name : '_'}
			{#if def?.name == 'Actions'}
				<ResponsiveButtons
					buttons={[
						{
							icon: 'mingcute:delete-line',
							cls: 'delete-button',
							onclick: async (event) => {
								if (
									await modalDelete.open({
										event,
										title: 'Delete project',
										message: `${project}?`
									})
								) {
									Projects.remove(project);
								}
							}
						},
						{
							icon: 'simple-line-icons:reload',
							cls: 'green-button',
							onclick: () => {
								Projects.reload(project);
							}
						},
						{
							icon: 'bytesize:export',
							cls: 'basic-button',
							onclick: async (event) => {
								event.currentTarget?.blur();
								const options = await Projects.exportOptions(project);
								exportChoices = options.reduce((acc, option) => {
									acc[option.name] = 'true';
									return acc;
								}, {});
								modalExport.open({ project, options });
							}
						},
						{
							icon: 'file-icons:test-ruby',
							cls: 'yellow-button',
							href: `${base}/dashboard/${project}/backend/`
						},
						{
							icon: 'mdi:warning-outline',
							cls: 'button preset-tonal-warning',
							hidden: !undefined_symbols,
							onclick: async (event) => {
								event.currentTarget?.blur();
								const symbols = await Projects.undefinedSymbols(project);
								if (await modalSymbols.open({ project, symbols })) {
									await Projects.createSymbols(project);
									Projects.refresh();
								}
							}
						}
					]}
					size="4"
					class="min-w-32 w-full"
				/>
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>
