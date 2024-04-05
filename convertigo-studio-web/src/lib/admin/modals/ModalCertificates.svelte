<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { candidatesList, candidates } from '../stores/certificatesStore';
	import { onMount } from 'svelte';
	import { call } from '$lib/utils/service';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta ?? {};

	onMount(() => {
		candidatesList();
	});

	async function removeCertificates(e) {
		e.preventDefault();
		const fd = new FormData(e.target.form);
		try {
			const res = await call('certificates.Remove', fd);
			console.log('remove', res);
		} catch (error) {
			console.error('Error removing certificate:', error);
		}
	}
</script>

{#if mode === 'Remove'}
	{#if $modalStore[0]}
		<Card title="Remove Certificate">
			<form on:submit|preventDefault={removeCertificates}>
				<label for="certificateSelect" class="label-common">Select Certificate:</label>
				<select id="certificateSelect" class="input-common mb-5" name="certificateName">
					{#each $candidates as candidate}
						<option value={candidate['@_name']}>{candidate['@_name']}</option>
					{/each}
				</select>
				<button type="submit" class="bg-primary-400-500-token">Confirm</button>
			</form>
		</Card>
	{/if}
{/if}
