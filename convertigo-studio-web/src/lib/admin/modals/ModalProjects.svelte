<script>
	import { call } from '$lib/utils/service';
	import { FileDropzone, getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { onMount } from 'svelte';
	import { projectsCheck } from '../stores/projectsStore';
	import Icon from '@iconify/svelte';
	import CheckState from '../components/CheckState.svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;

	let isLoading = false;

	const exportOptions = [
		{ name: 'Include Test Case' },
		{ name: 'Include MobileApp' },
		{ name: 'Include Mobile App Assets' },
		{ name: 'Include Mobile Platforms Assets' }
	];

	onMount(() => {});

	/**
	 * @param {Event} e
	 */
	async function deployProject(e) {
		isLoading = true;
		try {
			// @ts-ignore
			const res = await call('projects.Deploy', new FormData(e.target.form));
			await projectsCheck(true);
			console.log('deploy res', res);
		} catch (err) {
			console.error(err);
		} finally {
			isLoading = false; // Stop loading regardless of success or failure
			modalStore.close();
		}
	}

	/**
	 * @param {Event} e
	 */
	async function importProject(e) {
		try {
			// @ts-ignore
			await call('projects.ImportURL', new FormData(e.target));
			modalStore.close();
		} catch (err) {
			console.error(err);
		}
	}
</script>

{#if mode == 'deploy'}
	<Card>
		<form class="p-5 rounded-xl flex flex-col items-center">
			{#if isLoading}
				<Icon icon="eos-icons:three-dots-loading" class="w-10 h-10" />
			{:else}
				<h1 class="text-xl mb-5 text-center">Choose .car file and Deploy</h1>
				<CheckState name="bAssembleXsl" checked={false}
					>Assemble XSL files included in style sheets when deploying</CheckState
				>
				<FileDropzone
					class="mt-5"
					name="userfile"
					id="deployProject"
					accept=".car,.zip"
					on:change={deployProject}
				>
					<svelte:fragment slot="message"
						><div class="flex flex-col items-center">
							<Icon icon="icon-park:application-one" class="w-10 h-10" />Upload your project or drag
							and drop
						</div></svelte:fragment
					>
					<svelte:fragment slot="meta">.car file</svelte:fragment>
				</FileDropzone>

				<div class="flex flex-wrap gap-5 mt-5">
					<div class="flex-1">
						<button
							class="mt-5 btn cancel-button w-full font-light"
							on:click={() => modalStore.close()}>Cancel</button
						>
					</div>
					<div class="flex-1"></div>
				</div>
			{/if}
		</form>
	</Card>
{:else if mode == 'Export'}
	<Card>
		<form class="p-5 rounded-xl gap-5 flex flex-col">
			<h1 class="text-2xl font-bold mb-5">Export Project</h1>
			{#each exportOptions as exportOpt}
				<CheckState name={exportOpt.name}>
					{exportOpt.name}
				</CheckState>
			{/each}

			<div class="flex flex-wrap gap-5 mt-5">
				<div class="flex-1">
					<button class="w-full cancel-button" on:click={() => modalStore.close()}>Cancel</button>
				</div>
				<div class="flex-1">
					<button class="btn w-full confirm-button">Export</button>
				</div>
			</div>
		</form>
	</Card>
{:else}
	<Card>
		<form on:submit={importProject} class="p-5 rounded-xl flex flex-col">
			<h1 class="text-xl mb-5 text-center">Import from a Remote Project URL</h1>
			<p>
				Import a project from url like:<br /><b
					>&lt;project name&gt;=&lt;git or http URL&gt;[:path=&lt;optional
					subpath&gt;][:branch=&lt;optional branch&gt;]</b
				>
			</p>
			<p>Or a Convertigo Archive HTTP(S) URL.</p>
			<input type="text" class="input" name="url" size="70" />

			<div class="flex flex-wrap gap-5 mt-5">
				<div class="flex-1">
					<button class="w-full cancel-button" on:click={() => modalStore.close()}>Cancel</button>
				</div>
				<div class="flex-1">
					<button class="btn w-full confirm-button">Import</button>
				</div>
			</div>
		</form>
	</Card>
{/if}

<style lang="postcss">
	input {
		margin: 10px 0;
		padding: 10px;
		background: rgba(255, 255, 255, 1);
		border: none;
		border-radius: 5px;
	}
</style>
