<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { call, createFormDataFromParent } from '$lib/utils/service';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import { onMount } from 'svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import {
		certificatesList,
		candidates,
		certificates,
		anonymousBinding,
		cariocaBinding
	} from '$lib/admin/stores/certificatesStore';
	import { projectsStore, projectsCheck } from '$lib/admin/stores/projectsStore';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	const custom = true;

	let tabSet = $state(0);
	let candidatesState = $state([]);

	const notesTitle = {
		note: 'Note',
		certificate: 'A note for configuration',
		mappings: 'A note for Mappings'
	};

	onMount(async () => {
		await projectsCheck();
		await certificatesList();
		candidatesState = $candidates;
	});

	function openModalCertificates(mode) {
		// modalStore.trigger({
		// 	type: 'component',
		// 	component: 'modalCertificates',
		// 	meta: { mode }
		// });
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
	{#snippet cornerOption()}
		<ButtonsContainer>
			<button class="basic-button" onclick={() => openModalCertificates('Install')}>
				<Ico icon="fluent-mdl2:certificate" />
				<p>Install a new certificate</p>
			</button>
			<button class="delete-button" onclick={() => openModalCertificates('Remove')}>
				<Ico icon="mingcute:delete-line" />
				<p>Remove a certificate</p>
			</button>
		</ButtonsContainer>
	{/snippet}
	<div class="flex w-[20%]">
		<Accordion classes="dark:bg-sky-600 rounded w-[40%] dark:bg-opacity-30 mb-5">
			<Accordion.Item value="info">
				{#snippet control()}<Ico icon="fluent:note-48-filled" />{notesTitle.note}{/snippet}
				{#snippet panel()}<p class="font-bold">For each authentication certificate, you can :</p>
					<ul class="p-1 font-normal">
						<li>Install the certificate file</li>
						<li>Define its type (client or server), its password and eventually its group</li>
						<li>Define its mappings</li>
					</ul>{/snippet}
			</Accordion.Item>
		</Accordion>
	</div>

	<!-- <TabGroup>
		<Tab
			bind:group={tabSet}
			name="tab1"
			value={0}
			active="dark:bg-surface-500 bg-surface-50"
			class="font-bold"
		>
			Certificate configuration
		</Tab>
		<Tab
			bind:group={tabSet}
			name="tab2"
			value={1}
			active="dark:bg-surface-500 bg-surface-50"
			class="font-bold">Mappings configuration</Tab
		>

		{#snippet panel"> -->
	{#if tabSet === 0}
		<div class="flex w-[30%]">
			<Accordion classes="dark:bg-sky-600 rounded w-[40%] dark:bg-opacity-30 mt-5">
				<Accordion.Item value="info">
					{#snippet control()}<Ico icon="fluent:note-48-filled" />{notesTitle.certificate}{/snippet}
					{#snippet panel()}Configure here the certificates used by Convertigo. The certificates can
						be <strong
							>individual certificates files (*.pfx, *.p12 or *.cer) or <strong
								>certificates store files (*.store). Usually, individual certificates authenticate
								clients, certificates stores authenticate servers.</strong
							></strong
						>{/snippet}
				</Accordion.Item>
			</Accordion>
		</div>
		<TableAutoCard
			title="Installed Certificates"
			comment=""
			class="mt-5"
			definition={[
				{ name: 'Certificate / Store', custom },
				{ name: 'Type', custom },
				{ name: 'Password', custom },
				{ name: 'Group', custom },
				{ name: 'Update', custom },
				{ name: 'Delete', custom }
			]}
			data={$candidates.length ? [...$certificates, 'new'] : $certificates}
		>
			{#snippet children({ row, def })}
				{#if def.name === 'Certificate / Store'}
					{#if row == 'new'}
						<select class="input-common" name="name_0">
							{#each candidatesState as candidates}
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
					<button class="green-button" onclick={updateCertificate}>
						<Ico icon="dashicons:update" />
					</button>
				{:else if def.name === 'Delete'}
					{#if row != 'new'}
						<button
							class="delete-button"
							type="button"
							onclick={() => removeCertificates(row['@_name'])}
						>
							<Ico icon="mingcute:delete-line" />
						</button>
					{/if}
				{/if}
			{/snippet}
		</TableAutoCard>
	{:else if tabSet === 1}
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
			<div class="flex w-[30%]">
				<Accordion classes="dark:bg-sky-600 rounded w-[50vw] dark:bg-opacity-30 mt-5">
					<Accordion.Item value="info">
						{#snippet control()}<Ico
								icon="fluent:note-48-filled"
								class="w-7 h-7"
							/>{notesTitle.mappings}{/snippet}
						{#snippet panel()}
							Configure here the mappings between the authentication paths and the corresponding
							certificates. The mappings could refer to users either anonymous or authenticated by
							the Carioca/Vic portal. In the case of the identification of anonymous users, you will
							have to choose the correct Convertigo project. The 'default' project allows the
							identification on all projects. Otherwise, you will have to choose the virtual server,
							the authorization group and the related Carioca/Vic user. An empty 'virtual server'
							field selects all servers, all groups and all users. An empty 'authorization group'
							field selects all groups and all users of the specified virtual server. An empty
							'user' field selects all users of the specified group.
						{/snippet}
					</Accordion.Item>
				</Accordion>
			</div>
			<p class="font-bold text-surface-300 p-3"></p>
			{#each conf as { title, definition, store }}
				<TableAutoCard
					class="mt-5"
					{title}
					{definition}
					data={$certificates.length ? [...store, 'new'] : []}
				>
					{#snippet children({ row, def })}
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
							<button class="green-button" onclick={updateMapping}>
								<Ico icon="dashicons:update" />
							</button>
						{:else if def.name === 'Delete'}
							{#if row !== 'new'}
								<button class="delete-button" onclick={() => deleteMapping(row['@_link'])}>
									<Ico icon="mingcute:delete-line" />
								</button>
							{/if}
						{/if}
					{/snippet}
				</TableAutoCard>
			{/each}
		{/if}
	{:else if tabSet === 2}
		(tab panel 3 contents)
	{/if}
	<!-- {/snippet}
	</TabGroup> -->
</Card>
