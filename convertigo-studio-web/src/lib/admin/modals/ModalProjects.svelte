<script>
	import { call } from '$lib/utils/service';
	import { SlideToggle, getModalStore } from '@skeletonlabs/skeleton';
	import CacheInput from '../components/CacheInput.svelte';
	import Card from '../components/Card.svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;

	/**
	 * @param {Event} e
	 */
	async function deployProject(e) {
		try {
			// @ts-ignore
			await call('projects.Deploy', new FormData(e.target.form));
			modalStore.close();
		} catch (err) {
			console.error(err);
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
		<form class="p-5 rounded-xl flex flex-col">
			<h1 class="text-xl mb-5 text-center">Choose .car file and Deploy</h1>
			<SlideToggle
				name="bAssembleXsl"
				value="true"
				active="bg-success-500"
				background="bg-error-500"
				>Assemble XSL files included in style sheets when deploying</SlideToggle
			>
			<input
				type="file"
				name="userfile"
				id="deployProject"
				accept=".car,.zip"
				class="hidden"
				on:change={deployProject}
			/>
			<label for="deployProject" class="btn variant-filled mt-5">Deploy</label>
			<button class="mt-5 btn bg-white text-black font-light" on:click={() => modalStore.close()}
				>Cancel</button
			>
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
					<button class="btn w-full">Import</button>
				</div>
				<div class="flex-1">
					<button class="w-full variant-filled-error" on:click={() => modalStore.close()}
						>Cancel</button
					>
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
