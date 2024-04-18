<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import { onMount } from 'svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Icon from '@iconify/svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import {
		certificatesList,
		candidates,
		certificates,
		anonymousBinding,
		cariocaBinding
	} from '$lib/admin/stores/certificatesStore';
	import { projectsStore, projectsCheck } from '$lib/admin/stores/projectsStore';

	const modalStore = getModalStore();

	let selectedProjectId;
	let selectedCertificateId;
	let virtualServerValue;
	let authorizationValue;
	let userValue;

	onMount(async () => {
		await certificatesList();
		await projectsCheck();
		selectedProjectId = $projectsStore[0]['@_name'];
	});

	async function UpdateCertificate(e) {
		const fd = new FormData(e.target);
		try {
			const res = await call('certificates.Configure', fd);
			if (res) {
				await certificatesList();
			}
			console.log('add certificate', res);
		} catch (err) {
			console.error(err);
		}
	}

	function openInstallNewCertificates() {
		modalStore.trigger({
			type: 'component',
			component: 'modalCertificates',
			meta: { mode: 'Install' },
			response: (confirmed) => {
				if (confirmed) {
					removeCertificates();
				}
			}
		});
	}

	async function removeCertificates(certificateName) {
		const fd = new FormData();
		fd.append('certificateName_1', certificateName);
		try {
			const res = call('certificates.Delete', fd);
			await certificatesList();
			console.log('remove', res);
		} catch (err) {
			console.error(err);
		}
	}

	async function mappingAnonymous() {
		try {
			await call('certificates.mappings.Configure', {
				targettedObject_0: 'projects',
				cert_0: selectedCertificateId,
				convProject_0: selectedProjectId
			});
			await certificatesList();
		} catch (err) {
			console.error(err);
		}
	}

	async function mappingCarioca() {
		try {
			await call('certificates.mappings.Configure', {
				targettedObject_0: 'tas',
				cert_0: selectedCertificateId,
				virtualServer_0: virtualServerValue,
				group_0: authorizationValue,
				user_0: userValue,
				project_0: selectedProjectId
			});
			await certificatesList();
		} catch (err) {
			console.error(err);
		}
	}

	async function deleteAnonymousCertificates(row) {
		// Need to configure row Type in link_1 format. type can be server or client.
		// actually it is set to server value
		const link_1 = `projects.${row['@_projectName']}.server.store`;

		try {
			const response = await call('certificates.mappings.Delete', { link_1 });
			console.log('Deleted:', response);
			await certificatesList();
			console.log(row['@_type'], 'row type log');
		} catch (err) {
			console.error('Error deleting:', link_1, err);
		}
	}

	async function deleteCariocaCertificates(row) {
		const link_1 = `tas.${row['@_virtualServerName']}.${row['@_imputationGroup']}.${row['@_userName']}.projects.${row['@_projectName']}.server.store`;

		try {
			const response = await call('certificates.mappings.Delete', { link_1 });
			await certificatesList();
			console.log(row, 'row type log');
		} catch (err) {
			console.error('Error deleting:', link_1, err);
		}
	}

	function openRemoveCertificates() {
		modalStore.trigger({
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
	<p class="font-bold text-surface-300">For each authentication certificate, you can :</p>
	<ul class="p-3 font-bold text-surface-300">
		<li>Install the certificate file</li>
		<li>Define its type (client or server), its password and eventually its group</li>
		<li>Define its mappings</li>
	</ul>
</Card>

<Card title="Certificate configuration" class="mt-5">
	<div slot="cornerOption" class="flex justify-end">
		<form class="flex flex-wrap gap-1 justify-end">
			<div class="flex-1">
				<button class="bg-primary-400-500-token min-w-60" on:click={openInstallNewCertificates}
					>Installer de nouveaux certificats</button
				>
			</div>
			<div class="flex-1">
				<button class="bg-error-400-500-token min-w-60" on:click={openRemoveCertificates}
					>Supprimer un certificat</button
				>
			</div>
		</form>
	</div>

	<p class="mt-5 font-bold text-surface-300">
		Configure here the certificates used by Convertigo. The certificates can be individual
		certificates files (*.pfx, *.p12 or *.cer) or certificates store files (*.store). Usually,
		individual certificates authenticate clients, certificates stores authenticate servers.
	</p>

	<form on:submit|preventDefault={UpdateCertificate}>
		{#if $certificates.length > 0}
			<TableAutoCard
				title="Installed Certificates"
				class="mt-10"
				definition={[
					{ name: 'Certificate / Store', custom: true },
					{ name: 'Type', custom: true },
					{ name: 'Password', custom: true },
					{ name: 'Group', custom: true },
					{ name: 'Update', custom: true },
					{ name: 'Delete', custom: true }
				]}
				data={$certificates}
				let:def
				let:row
			>
				{#if $certificates.length > 0}
					{#if def.name === 'Certificate / Store'}
						{#if row == 'new'}
							<select class="input-common" name="name_0">
								{#each $candidates as candidates}
									<option value={candidates['@_name']}>{candidates['@_name']}</option>
								{/each}
							</select>
						{:else}
							{row['@_name']}
						{/if}
					{:else if def.name === 'Type'}
						<select class="input-common" name="type_0">
							<option value="server" selected={row['@_type'] === 'server'}>Server</option>
							<option value="client" selected={row['@_type'] === 'client'}>Client</option>
						</select>
					{:else if def.name === 'Password'}
						<input
							type="password"
							placeholder="Enter certificate password ..."
							class="input-common"
							name="pwd_0"
							value={row['@_password'] || ''}
						/>
					{:else if def.name === 'Group'}
						<input
							type="text"
							placeholder="Enter group value ..."
							class="input-common"
							name="group_0"
							value={row['@_group'] || ''}
						/>
					{:else if def.name === 'Update'}
						<button
							type="submit"
							class="shadow-md p-1 px-2 ring-outline-token bg-secondary-400-500-token"
						>
							<Icon icon="material-symbols-light:update" class="h-7 w-7" />
						</button>
					{:else if def.name === 'Delete'}
						{#if row != 'new'}
							<button
								class="shadow-md p-1 px-2 ring-outline-token bg-error-400-500-token"
								type="button"
								on:click={() => removeCertificates(row['@_name'])}
							>
								<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7" />
							</button>
						{/if}
					{/if}
				{/if}
			</TableAutoCard>
		{/if}
	</form>
</Card>

<Card title="Mappings configuration" class="mt-5">
	{#if $anonymousBinding.length > 0}
		<TableAutoCard
			title="Mappings for anonymous users"
			definition={[
				{ name: 'Project Name', custom: true },
				{ name: 'Certificate / Store', custom: true },
				{ name: 'Update', custom: true },
				{ name: 'Delete', custom: true }
			]}
			data={$anonymousBinding}
			let:def
			let:row
		>
			{#if def.name === 'Project Name'}
				{#if row == 'new'}
					<select class="input-common" bind:value={selectedProjectId}>
						{#each $projectsStore as project}
							<option value={project['@_name']}>{project['@_name']}</option>
						{/each}
					</select>
				{:else}
					{row['@_projectName']}
				{/if}
			{:else if def.name === 'Certificate / Store'}
				{#if row == 'new'}
					<select class="input-common" bind:value={selectedCertificateId}>
						{#each $certificates as certificate}
							{#if certificate != 'new'}
								<option value={certificate['@_name']}>
									{certificate['@_name']}
								</option>
							{/if}
						{/each}
					</select>
				{:else}
					{row['@_certificateName']}
				{/if}
			{:else if def.name === 'Update'}
				<button
					type="submit"
					class="shadow-md p-1 px-2 ring-outline-token bg-secondary-400-500-token"
					on:click={() => mappingAnonymous()}
				>
					<Icon icon="material-symbols-light:update" class="h-7 w-7" />
				</button>
			{:else if def.name === 'Delete'}
				{#if row !== 'new'}
					<button
						class="shadow-md p-1 px-2 ring-outline-token bg-error-400-500-token"
						on:click={() => deleteAnonymousCertificates(row)}
					>
						<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7" />
					</button>
				{/if}
			{/if}
		</TableAutoCard>
	{/if}

	{#if $cariocaBinding.length > 0}
		<TableAutoCard
			title="Mappings for carioca users"
			class="mt-10"
			definition={[
				{ name: 'Project Name', custom: true },
				{ name: 'Virtual Server', custom: true },
				{ name: 'Authorization Group', custom: true },
				{ name: 'User', custom: true },
				{ name: 'Certificate / Store', custom: true },
				{ name: 'Update', custom: true },
				{ name: 'Delete', custom: true }
			]}
			data={$cariocaBinding}
			let:def
			let:row
		>
			{#if def.name === 'Project Name'}
				{#if row == 'new'}
					<select class="input-common" name="project_0">
						{#each $projectsStore as project}
							<option value={project['@_name']}>{project['@_name']}</option>
						{/each}
					</select>
				{:else}
					{row['@_projectName']}
				{/if}
			{:else if def.name === 'Virtual Server'}
				{#if row == 'new'}
					<input
						class="input-common"
						name="virtualServer_0"
						bind:value={virtualServerValue}
						placeholder="Enter Value ..."
					/>
				{:else}
					<input
						class="input-common"
						name="virtualServer_0"
						value={row['@_virtualServerName']}
						placeholder="Enter Value ..."
					/>
				{/if}
			{:else if def.name === 'Authorization Group'}
				{#if row == 'new'}
					<input
						class="input-common"
						name="group_0"
						bind:value={authorizationValue}
						placeholder="Enter Value ..."
					/>
				{:else}
					<input
						class="input-common"
						name="virtualServer_0"
						value={row['@_imputationGroup']}
						placeholder="Enter Value ..."
					/>
				{/if}
			{:else if def.name === 'User'}
				{#if row == 'new'}
					<input
						class="input-common"
						name="user_0"
						bind:value={userValue}
						placeholder="Enter Value ..."
					/>
				{:else}
					<input
						class="input-common"
						name="virtualServer_0"
						value={row['@_userName']}
						placeholder="Enter Value ..."
					/>
				{/if}
			{:else if def.name === 'Certificate / Store'}
				<select class="input-common" name="cert_0">
					{#each $certificates as certificate}
						<option value={certificate['@_name']}>{certificate['@_name']}</option>
					{/each}
				</select>
			{:else if def.name === 'Update'}
				<button
					type="submit"
					class="shadow-md p-1 px-2 ring-outline-token bg-secondary-400-500-token"
					on:click={() => mappingCarioca()}
				>
					<Icon icon="material-symbols-light:update" class="h-7 w-7" />
				</button>
			{:else if def.name === 'Delete'}
				{#if row !== 'new'}
					<button
						class="shadow-md p-1 px-2 ring-outline-token bg-error-400-500-token"
						on:click={() => deleteCariocaCertificates(row)}
					>
						<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7" />
					</button>
				{/if}
			{/if}
		</TableAutoCard>
	{/if}
</Card>
