<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { call, createFormDataFromParent } from '$lib/utils/service';
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
	const custom = true;

	onMount(async () => {
		await projectsCheck();
		await certificatesList();
	});

	function openModalCertificates(mode) {
		modalStore.trigger({
			type: 'component',
			component: 'modalCertificates',
			meta: { mode }
		});
	}

	async function removeCertificates(certificateName_1) {
		await call('certificates.Delete', { certificateName_1 });
		await certificatesList();
	}

	async function updateCertificate(e) {
		const tr = e.target.closest('tr');
		const fd = createFormDataFromParent(tr);
		await call('certificates.Configure', fd);
		await certificatesList();
	}

	async function updateMapping(e) {
		const tr = e.target.closest('tr');
		const fd = createFormDataFromParent(tr);
		if (fd.has('link')) {
			await call('certificates.mappings.Delete', { link_1: fd.get('link') });
			fd.delete('link');
		}
		if (fd.has('virtualServer_0')) {
			fd.append('targettedObject_0', 'tas');
			fd.append('project_0', fd.get('convProject_0') ?? '');
			fd.delete('convProject_0');
		} else {
			fd.append('targettedObject_0', 'projects');
		}
		await call('certificates.mappings.Configure', fd);
		await certificatesList();
	}

	async function deleteMapping(link_1) {
		await call('certificates.mappings.Delete', { link_1 });
		await certificatesList();
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
	<div slot="cornerOption" class="flex flex-wrap gap-1 justify-end">
		<button
			class="bg-primary-400-500-token min-w-60"
			on:click={() => openModalCertificates('Install')}>Install a new certificate</button
		>
		<button class="bg-error-400-500-token min-w-60" on:click={() => openModalCertificates('Remove')}
			>Remove a certificate</button
		>
	</div>

	<p class="mt-5 font-bold text-surface-300">
		Configure here the certificates used by Convertigo.<br />
		The certificates can be <strong>individual certificates files</strong> (*.pfx, *.p12 or *.cer)
		or <strong>certificates store files</strong> (*.store).<br />
		Usually, individual certificates authenticate clients, certificates stores authenticate servers.
	</p>

	<TableAutoCard
		title="Installed Certificates"
		class="mt-10"
		definition={[
			{ name: 'Certificate / Store', custom },
			{ name: 'Type', custom },
			{ name: 'Password', custom },
			{ name: 'Group', custom },
			{ name: 'Update', custom },
			{ name: 'Delete', custom }
		]}
		data={$candidates.length ? [...$certificates, 'new'] : $certificates}
		let:def
		let:row
	>
		{#if def.name === 'Certificate / Store'}
			{#if row == 'new'}
				<select class="input-common" name="name_0">
					{#each $candidates as candidates}
						<option value={candidates['@_name']}>{candidates['@_name']}</option>
					{/each}
				</select>
			{:else}
				<input type="hidden" name="name_0" value={row['@_name']} />
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
				value={row['@_password'] ?? ''}
			/>
		{:else if def.name === 'Group'}
			<input
				type="text"
				placeholder="Enter group value ..."
				class="input-common"
				name="group_0"
				value={row['@_group'] ?? ''}
			/>
		{:else if def.name === 'Update'}
			<button
				class="shadow-md p-1 px-2 ring-outline-token bg-secondary-400-500-token"
				on:click={updateCertificate}
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
	</TableAutoCard>
</Card>

<Card title="Mappings configuration" class="mt-5">
	<p class="mt-5 font-bold text-surface-300">
		Configure here the mappings between the authentication paths and the corresponding certificates.<br
		/>
		The mappings could refer to users either anonymous or authenticated by the Carioca/Vic portal.<br
		/><br />
		In the case of the identification of anonymous users, you will have to choose the correct Convertigo
		project.<br />
		The 'default' project allows the identification on all projects.<br /><br />
		Otherwise, you will have to choose the virtual server, the authorization group and the related Carioca/Vic
		user.<br />
		An empty 'virtual server' field selects all servers, all groups and all users.<br />
		An empty 'authorization group' field selects all groups and all users of the specified virtual server.<br
		/>
		An empty 'user' field selects all users of the specified group.
	</p>
	{#if true}
		{@const conf = [
			{
				title: 'Mappings for anonymous users',
				definition: [
					{ name: 'Project Name', custom },
					{ name: 'Certificate / Store', custom },
					{ name: 'Update', custom },
					{ name: 'Delete', custom }
				],
				store: $anonymousBinding
			},
			{
				title: 'Mappings for carioca users',
				definition: [
					{ name: 'Project Name', custom },
					{ name: 'Virtual Server', custom },
					{ name: 'Authorization Group', custom },
					{ name: 'User', custom },
					{ name: 'Certificate / Store', custom },
					{ name: 'Update', custom },
					{ name: 'Delete', custom }
				],
				store: $cariocaBinding
			}
		]}
		{#each conf as { title, definition, store }}
			<TableAutoCard
				{title}
				{definition}
				data={$certificates.length ? [...store, 'new'] : []}
				let:def
				let:row
			>
				{#if def.name === 'Project Name'}
					{#if row == 'new'}
						<select class="input-common" name="convProject_0">
							{#each $projectsStore as project}
								<option value={project['@_name']}>{project['@_name']}</option>
							{/each}
						</select>
					{:else}
						<input type="hidden" name="convProject_0" value={row['@_projectName']} />
						<input type="hidden" name="link" value={row['@_link']} />
						{row['@_projectName']}
					{/if}
				{:else if def.name === 'Virtual Server'}
					<input
						class="input-common"
						name="virtualServer_0"
						value={row['@_virtualServerName'] ?? ''}
						placeholder="Enter Value ..."
					/>
				{:else if def.name === 'Authorization Group'}
					<input
						class="input-common"
						name="group_0"
						value={row['@_imputationGroup'] ?? ''}
						placeholder="Enter Value ..."
					/>
				{:else if def.name === 'User'}
					<input
						class="input-common"
						name="user_0"
						value={row['@_userName'] ?? ''}
						placeholder="Enter Value ..."
					/>
				{:else if def.name === 'Certificate / Store'}
					<select
						class="input-common"
						name="cert_0"
						value={row['@_certificateName'] ?? $certificates[0]['@_name']}
					>
						{#each $certificates as certificate}
							<option value={certificate['@_name']}>
								{certificate['@_name']}
							</option>
						{/each}
					</select>
				{:else if def.name === 'Update'}
					<button
						class="shadow-md p-1 px-2 ring-outline-token bg-secondary-400-500-token"
						on:click={updateMapping}
					>
						<Icon icon="material-symbols-light:update" class="h-7 w-7" />
					</button>
				{:else if def.name === 'Delete'}
					{#if row !== 'new'}
						<button
							class="shadow-md p-1 px-2 ring-outline-token bg-error-400-500-token"
							on:click={() => deleteMapping(row['@_link'])}
						>
							<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7" />
						</button>
					{/if}
				{/if}
			</TableAutoCard>
		{/each}
	{/if}
</Card>
