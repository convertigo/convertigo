<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { writable } from 'svelte/store';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import { onMount } from 'svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Icon from '@iconify/svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import {
		candidatesList,
		certificatesList,
		candidates,
		certificates
	} from '$lib/admin/stores/certificatesStore';
	import { projectsStore, projectsCheck } from '$lib/admin/stores/projectsStore';

	const certificateModalStore = getModalStore();

	onMount(() => {
		candidatesList();
		certificatesList();
		projectsCheck();
	});

	async function UpdateCertificate(e) {
		e.preventDefault();
		const fd = new FormData(e.target.form);
		try {
			const res = await call('certificates.Configure', fd);
			console.log('add certificate', res);
			console.log('data entries', Array.from(fd.entries()));
		} catch (err) {
			console.error(err);
		}
	}

	async function installNewCertificates(e) {
		const file = e.target.files[0];
		if (file) {
			const formData = new FormData();
			formData.append('userfile', file);

			try {
				const res = await call('certificates.Install', formData);

				await certificatesList();
			} catch (err) {
				console.error('Error installing certificate:', err);
			}
		}
	}

	async function removeCertificates(e) {
		const fd = new FormData(e.target.form);
		const res = call('certificates.Remove');
		console.log('remove', res);
	}

	function openRemoveCertificates() {
		certificateModalStore.trigger({
			type: 'component',
			component: 'modalCertificates',
			meta: { mode: 'Remove' },
			response: (confirmed) => {
				if (confirmed) {
					removeCertificates();
				}
			}
		});
	}
</script>

<Card title="Certificates">
	<p class="font-normal">For each authentication certificate, you can :</p>
	<ul class="p-3">
		<li>Install the certificate file</li>
		<li>Define its type (client or server), its password and eventually its group</li>
		<li>Define its mappings</li>
	</ul>
</Card>

<Card title="Certificate configuration" class="mt-5">
	<div slot="cornerOption" class="gap-5 flex">
		<form class="flex flex-wrap gap-5">
			<div class="flex-1">
				<input type="file" id="fileInput" class="hidden" on:change={installNewCertificates} />
				<label for="fileInput" class="btn confirm-button cursor-pointer"
					>Install New Certificates</label
				>
			</div>

			<button class="bg-error-400-500-token" on:click={openRemoveCertificates}
				>Remove a New Certificate</button
			>
		</form>
	</div>

	<p class="mt-10">
		Configure here the certificates used by Convertigo. The certificates can be individual
		certificates files (*.pfx, *.p12 or *.cer) or certificates store files (*.store). Usually,
		individual certificates authenticate clients, certificates stores authenticate servers.
	</p>
	<form on:submit|preventDefault={UpdateCertificate}>
		{#if $candidates.length > 0}
			<TableAutoCard
				class="mt-10"
				definition={[
					{ name: 'Certificate / Store', custom: true },
					{ name: 'Type', custom: true },
					{ name: 'Password', custom: true },
					{ name: 'Group', custom: true },
					{ name: 'Update', custom: true },
					{ name: 'Delete', custom: true }
				]}
				data={$candidates}
				let:def
			>
				{#if def.name === 'Certificate / Store'}
					<select class="input-common" name="name_0">
						{#each $candidates as candidate}
							<option value={candidate['@_name']}>{candidate['@_name']}</option>
						{/each}
					</select>
				{:else if def.name === 'Type'}
					<select class="input-common" name="type_0">
						<option value="server">Server</option>
						<option value="client">Client</option>
					</select>
				{:else if def.name === 'Password'}
					<input
						type="password"
						placeholder="Enter certificate password ..."
						class="input-common"
						name="pwd_0"
					/>
				{:else if def.name === 'Group'}
					<input
						type="text"
						placeholder="Enter group value ..."
						class="input-common"
						name="group_0"
					/>
				{:else if def.name === 'Update'}
					<button
						type="submit"
						class="shadow-md p-1 px-2 ring-outline-token bg-secondary-400-500-token"
					>
						<Icon icon="material-symbols-light:update" class="h-7 w-7" />
					</button>
				{:else if def.name === 'Delete'}
					<button class="shadow-md p-1 px-2 ring-outline-token bg-error-400-500-token">
						<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7" />
					</button>
				{/if}
			</TableAutoCard>
		{/if}
	</form>
</Card>

<Card title="Mappings configuration" class="mt-5">
	<TableAutoCard
		definition={[
			{ name: 'Project Name', custom: true },
			{ name: 'Certificate / Store', custom: true },
			{ name: 'Update', custom: true },
			{ name: 'Delete', custom: true }
		]}
		data={$candidates}
		let:def
	>
		{#if def.name === 'Project Name'}
			<select class="input-common" name="name_0">
				{#each $projectsStore as project}
					<option value={project['@_name']}>{project['@_name']}</option>
				{/each}
			</select>
		{:else if def.name === 'Certificate / Store'}
			<select class="input-common" name="name_0">
				{#each $candidates as candidate}
					<option value={candidate['@_name']}>{candidate['@_name']}</option>
				{/each}
			</select>
		{:else if def.name === 'Update'}
			<button
				type="submit"
				class="shadow-md p-1 px-2 ring-outline-token bg-secondary-400-500-token"
			>
				<Icon icon="material-symbols-light:update" class="h-7 w-7" />
			</button>
		{:else if def.name === 'Delete'}
			<button class="shadow-md p-1 px-2 ring-outline-token bg-error-400-500-token">
				<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7" />
			</button>
		{/if}
	</TableAutoCard>
</Card>
