<script>
	import { FileDropzone, getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { candidates, certificatesList } from '../stores/certificatesStore';
	import { onMount } from 'svelte';
	import { call } from '$lib/utils/service';
	import Icon from '@iconify/svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta ?? {};

	let isLoading = false;

	onMount(() => {
		certificatesList();
	});

	async function removeCertificates(e) {
		e.preventDefault();
		const fd = new FormData(e.target);
		try {
			const res = await call('certificates.Remove', fd);
			console.log('remove', res);
			await certificatesList();
		} catch (error) {
			console.error('Error removing certificate:', error);
		} finally {
			modalStore.close();
		}
	}

	async function installNewCertificates(e) {
		isLoading = true;
		const file = e.target.files[0];
		if (file) {
			const formData = new FormData();
			formData.append('userfile', file);

			try {
				const res = await call('certificates.Install', formData);
			} catch (err) {
				console.error('Error installing certificate:', err);
			} finally {
				isLoading = false;
				modalStore.close();
				await certificatesList();
			}
		}
	}
</script>

{#if mode === 'Remove'}
	{#if $modalStore[0]}
		<Card title="Remove Certificate">
			<form on:submit|preventDefault={removeCertificates}>
				<label for="certificateSelect" class="label-common">Select Certificate:</label>
				{#if $candidates.length > 0}
					<select id="certificateSelect" class="input-common mb-5" name="certificateName">
						{#each $candidates as candidate}
							<option value={candidate['@_name']}>{candidate['@_name']}</option>
						{/each}
					</select>
				{:else}
					No certificate
				{/if}
				<div class="flex flex-wrap gap-5 mt-5">
					<div class="flex-1">
						<button
							type="button"
							class="btn cancel-button w-full font-light"
							on:click={() => modalStore.close()}>Cancel</button
						>
					</div>
					<div class="flex-1">
						<button type="submit" class="bg-primary-400-500-token">Confirm</button>
					</div>
				</div>
			</form>
		</Card>
	{/if}
{:else if mode === 'Install'}
	<Card title="Install a New Certificate">
		{#if isLoading}
			<Icon icon="eos-icons:three-dots-loading" class="w-10 h-10" />
		{:else}
			<FileDropzone
				name="fileinput"
				id="fileinput"
				accept=".p12,.pfx,.cer"
				on:change={installNewCertificates}
			>
				<svelte:fragment slot="message"
					><div class="flex flex-col items-center">
						<Icon icon="icon-park:application-one" class="w-10 h-10" />Upload your project or drag
						and drop
					</div></svelte:fragment
				>
				<svelte:fragment slot="meta">.p12 .pfx .cer files</svelte:fragment>
			</FileDropzone>
			<button class="mt-5 btn cancel-button w-full font-light" on:click={() => modalStore.close()}
				>Cancel</button
			>
		{/if}
	</Card>
{/if}
