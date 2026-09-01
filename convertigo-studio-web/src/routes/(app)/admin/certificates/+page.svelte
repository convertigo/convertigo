<script>
	import { getAdminPageDocHref } from '$lib/admin/AdminDocumentation.svelte';
	import Certificates from '$lib/admin/Certificates.svelte';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import FileUploadField from '$lib/admin/components/FileUploadField.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { onDestroy } from 'svelte';

	let {
		certificates,
		candidates,
		anonymous,
		carioca,
		install,
		remove,
		calling,
		loading,
		configure,
		del,
		mappingsConfigure,
		mappingsDel,
		importCertificates,
		exportCertificates,
		selectForExport,
		init
	} = $derived(Certificates);
	let { projects } = $derived(Projects);
	const certificatesDocHref = getAdminPageDocHref('/admin/certificates');

	onDestroy(() => {
		Projects.stop();
		Certificates.stop();
	});

	let modalCertInstall = $state();
	let modalCertRemove = $state();
	let modalImport = $state();
	let actionImport = $state('on');
	let exporting = $state(false);
	let exportRows = $derived([
		...[...certificates, ...candidates]
			.filter(({ name }) => name != null)
			.map((row) => ({ category: 'certificates', row })),
		...[...anonymous, ...carioca]
			.filter(({ link }) => link != null)
			.map((row) => ({ category: 'mappings', row }))
	]);
	let selectedExportCount = $derived(
		exportRows.filter(({ row: { export: selected } }) => selected).length
	);
	let allExported = $derived(exportRows.length > 0 && selectedExportCount == exportRows.length);

	function selectAllForExport(selected) {
		for (const { category, row } of exportRows) {
			selectForExport(category, row, selected);
		}
	}

	async function runExport() {
		if (await exportCertificates()) {
			exporting = false;
		}
	}
</script>

<ModalDynamic bind:this={modalCertInstall}>
	<Card title="Install a new certificate">
		<form
			onsubmit={async (event) => {
				await install(event);
				modalCertInstall.close();
			}}
		>
			<p>
				The certificates can be <b>individual certificates files</b> (*.pfx, *.p12 or *.cer) or
				<b>certificates store files</b> (*.store)
			</p>
			<fieldset disabled={calling}>
				<FileUploadField
					name="userfile"
					accept={{
						'application/x-pkcs12': ['.pfx', '.p12'],
						'application/pkcs12': ['.pfx', '.p12'],
						'application/pkix-cert': ['.cer'],
						'application/x-x509-ca-cert': ['.cer'],
						'application/octet-stream': ['.pfx', '.p12', '.cer', '.store']
					}}
					required
					allowDrop
					dropIcon="mdi:certificate"
					title="Drop or choose a certificate file"
					hint="then press Install"
					rejectedText="Unsupported file type. Please use .pfx, .p12, .cer, or .store."
				/>
				<ActionBar class="mt-4">
					<Button
						label="Install"
						icon="mdi:certificate"
						type="submit"
						class="button-primary w-fit!"
					/>
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						onclick={modalCertInstall.close}
					/>
				</ActionBar>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

<ModalDynamic bind:this={modalImport}>
	<Card title="Drop or choose a certificate ZIP archive and Import">
		<form
			onsubmit={async (event) => {
				if (await importCertificates(event)) {
					modalImport.close();
				}
			}}
		>
			<fieldset class="layout-y-stretch" disabled={calling}>
				<FileUploadField
					name="file"
					accept={{
						'application/zip': ['.zip'],
						'application/x-zip-compressed': ['.zip']
					}}
					required
					allowDrop
					dropIcon="mdi:import"
					title="Drop or choose a certificate ZIP archive"
					hint="then press Import"
				/>
				<div>
					Import policy
					<PropertyType
						type="segment"
						name="action-import"
						item={[
							{ text: 'Replace certificates', value: 'clear-import' },
							{ text: 'Merge certificates', value: 'on' }
						]}
						bind:value={actionImport}
						orientation="vertical"
					/>
				</div>
				{#if actionImport == 'on'}
					<div>
						In case of certificate name or mapping conflict, priority
						<PropertyType
							type="segment"
							name="priority"
							item={[
								{ text: 'Server', value: 'priority-server' },
								{ text: 'File', value: 'priority-import' }
							]}
							value="priority-import"
							orientation="vertical"
						/>
					</div>
					<div>Certificates that only exist on the server will be kept.</div>
				{:else}
					<div>All current certificate entries and certificate files will be replaced.</div>
				{/if}
				<div>
					Encrypted password values are imported unchanged. Source and target servers must already
					use compatible cryptographic configurations.
				</div>
				<div>The current certificate configuration will be saved as a dated ZIP backup.</div>
				<ActionBar>
					<Button label="Import" icon="mdi:import" type="submit" class="button-primary w-fit!" />
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						onclick={modalImport?.close}
					/>
				</ActionBar>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

<ModalDynamic bind:this={modalCertRemove}>
	<Card title="Remove a certificate">
		<form
			onsubmit={async (event) => {
				await remove(event);
				modalCertRemove.close();
			}}
		>
			<fieldset disabled={calling} class="layout-y">
				<PropertyType
					type="segment"
					orientation="vertical"
					name="certificateName"
					item={candidates.map(({ name }) => ({ value: name, text: name }))}
					value={candidates[0]?.name ?? ''}
				/>
				<ActionBar>
					<Button
						label="Remove"
						icon="mdi:certificate"
						type="submit"
						class="button-primary w-fit!"
					/>
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						onclick={modalCertRemove.close}
					/>
				</ActionBar>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

{#snippet cell({ row, def, category })}
	{@const {
		name,
		projectName,
		link,
		certificateName,
		virtualServerName,
		imputationGroup,
		userName,
		group,
		password,
		type,
		validPass,
		last
	} = row}
	{#if loading}
		<AutoPlaceholder {loading} />
	{:else if def.name == 'Certificate / Store' && def.setup}
		{#if exporting}
			{name}
		{:else if last}
			<PropertyType
				name="name_0"
				type="combo"
				item={candidates.map(({ name }) => ({ value: name, text: name }))}
				value={candidates[0]?.name}
			/>
		{:else}
			<input type="hidden" name="name_0" value={name} />
			{name}
		{/if}
	{:else if def.name == 'Project Name'}
		{#if exporting}
			{projectName || 'default'}
		{:else if last}
			<PropertyType
				name="convProject_0"
				type="combo"
				item={projects.map(({ name }) => ({ value: name, text: name }))}
				value={projects[0]?.name}
			/>
		{:else}
			<input type="hidden" name="convProject_0" value={projectName} />
			<input type="hidden" name="link" value={link} />
			{projectName}
		{/if}
	{:else if def.name == 'Virtual Server'}
		{#if exporting}
			{virtualServerName || '—'}
		{:else}
			<PropertyType
				name="virtualServer_0"
				type="text"
				value={virtualServerName}
				originalValue={virtualServerName}
			/>
		{/if}
	{:else if def.name == 'Authorization Group'}
		{#if exporting}
			{imputationGroup || '—'}
		{:else}
			<PropertyType
				name="group_0"
				type="text"
				value={imputationGroup}
				originalValue={imputationGroup}
			/>
		{/if}
	{:else if def.name == 'User'}
		{#if exporting}
			{userName || '—'}
		{:else}
			<PropertyType name="user_0" type="text" value={userName} originalValue={userName} />
		{/if}
	{:else if def.name == 'Certificate / Store'}
		{#if exporting}
			{certificateName}
		{:else}
			<PropertyType
				name="cert_0"
				type="combo"
				item={certificates.map(({ name }) => ({ value: name, text: name }))}
				value={certificateName ?? certificates[0]?.name}
			/>
		{/if}
	{:else if def.name == 'Type'}
		{#if exporting}
			{type ? (type == 'server' ? 'Server' : 'Client') : 'Not configured'}
		{:else}
			<PropertyType
				name="type_0"
				type="combo"
				item={[
					{ value: 'server', text: 'Server' },
					{ value: 'client', text: 'Client' }
				]}
				value={type ?? 'server'}
				originalValue={type}
			/>
		{/if}
	{:else if def.name == 'Password'}
		{#if exporting}
			{type ? 'Included (encrypted)' : '—'}
		{:else}
			<form class="w-full">
				{#if validPass == 'false'}
					<p class="font-medium text-error-700-300">Invalid password</p>
				{/if}
				<PropertyType
					name="pwd_0"
					type="password"
					value={password}
					originalValue={password}
					placeholder="Enter certificate password …"
				/>
			</form>
		{/if}
	{:else if def.name == 'Group'}
		{#if exporting}
			{group || '—'}
		{:else}
			<PropertyType
				name="group_0"
				type="text"
				value={group}
				originalValue={group}
				placeholder="Enter group value …"
			/>
		{/if}
	{:else if def.name === 'Actions'}
		{#if exporting}
			<PropertyType
				values={[false, true]}
				type="boolean"
				name="export"
				bind:value={() => row.export, (selected) => selectForExport(category, row, selected)}
			/>
		{:else}
			<ResponsiveButtons
				buttons={[
					{
						icon: 'mdi:delete-outline',
						title: def.setup ? 'Delete certificate entry' : 'Delete mapping',
						cls: 'button-ico-primary',
						hidden: last,
						onclick: def.setup ? del : mappingsDel
					},
					{
						icon: 'mdi:update',
						title: def.setup ? 'Update certificate entry' : 'Update mapping',
						cls: 'button-ico-primary',
						onclick: def.setup ? configure : mappingsConfigure
					}
				]}
				size="6"
				class="w-full min-w-16"
			/>
		{/if}
	{/if}
{/snippet}

<div class="layout-y-stretch">
	<Card title="Certificates" docHref={certificatesDocHref}>
		{#snippet cornerOption()}
			<ResponsiveButtons
				class="max-w-4xl"
				buttons={[
					{
						label: 'Install a new certificate',
						icon: 'mdi:certificate',
						cls: 'button-primary',
						hidden: exporting,
						onclick: modalCertInstall?.open
					},
					{
						label: 'Remove a certificate',
						icon: 'mdi:delete-outline',
						cls: 'button-secondary',
						hidden: exporting,
						disabled: candidates.length == 0,
						onclick: modalCertRemove?.open
					},
					{
						label: 'Import',
						icon: 'mdi:import',
						tooltip: 'Import a certificate ZIP archive',
						cls: 'button-secondary',
						hidden: exporting,
						onclick: modalImport?.open
					},
					{
						label: 'Select All',
						icon: 'mdi:check-all',
						tooltip: 'Select all certificates for export',
						cls: 'button-secondary',
						hidden: !exporting || allExported,
						onclick: () => selectAllForExport(true)
					},
					{
						label: 'Unselect All',
						icon: 'mdi:check-all',
						tooltip: 'Clear certificate selection for export',
						cls: 'button-secondary',
						hidden: !exporting || selectedExportCount == 0,
						onclick: () => selectAllForExport(false)
					},
					{
						label: 'Export',
						icon: 'mdi:export',
						tooltip: 'Choose certificates to export',
						cls: 'button-secondary',
						hidden: exporting,
						disabled: exportRows.length == 0,
						onclick: () => (exporting = true)
					},
					{
						label: `Export [${selectedExportCount}]`,
						icon: 'mdi:export',
						tooltip: 'Export selected certificates and their mappings',
						cls: 'button-primary',
						hidden: !exporting,
						disabled: selectedExportCount == 0,
						onclick: runExport
					},
					{
						label: 'Cancel',
						icon: 'mdi:close-circle-outline',
						tooltip: 'Exit export mode without exporting',
						cls: 'button-secondary',
						hidden: !exporting,
						onclick: () => (exporting = false)
					}
				]}
				disabled={!init || calling}
			/>
		{/snippet}
		<div class="w-full">
			Usually, individual certificates authenticate clients, certificates stores authenticate
			servers.
		</div>
		<TableAutoCard
			class="text-left"
			definition={[
				['Certificate / Store', 'min-w-52'],
				['Type', 'min-w-48'],
				['Password', 'min-w-60'],
				['Group', 'min-w-60'],
				['Actions', 'w-20']
			].map(([name, cls]) => ({
				name,
				class: cls,
				custom: true,
				setup: true
			}))}
			data={exporting
				? exportRows.filter(({ category }) => category == 'certificates').map(({ row }) => row)
				: candidates.length
					? [...certificates, { last: true, setup: true }]
					: certificates}
		>
			{#snippet children({ row, def })}
				{@render cell({ row, def, category: 'certificates' })}
			{/snippet}
		</TableAutoCard>
	</Card>
	<Card title="Mappings">
		<div class="w-full">
			Configure here the mappings between the authentication paths and the corresponding
			certificates. The mappings could refer to users either anonymous or authenticated by the
			Carioca/Vic portal.
		</div>
		<div class="w-full">
			In the case of the identification of anonymous users, you will have to choose the correct
			Convertigo project. The 'default' project allows the identification on all projects.
		</div>

		<TableAutoCard
			title="Mappings for anonymous users"
			class="text-left"
			definition={[
				['Project Name', 'min-w-52'],
				['Certificate / Store', 'min-w-52'],
				['Actions', 'w-20']
			].map(([name, cls]) => ({
				name,
				class: cls,
				custom: true
			}))}
			data={exporting
				? anonymous
				: certificates.length && projects.length
					? [...anonymous, { last: true }]
					: anonymous}
		>
			{#snippet children({ row, def })}
				{@render cell({ row, def, category: 'mappings' })}
			{/snippet}
		</TableAutoCard>
		<div class="w-full">
			<p>
				Otherwise, you will have to choose the virtual server, the authorization group and the
				related Carioca/Vic user.
			</p>
			<p>An empty 'virtual server' field selects all servers, all groups and all users.</p>
			<p>
				An empty 'authorization group' field selects all groups and all users of the specified
				virtual server.
			</p>
			<p>An empty 'user' field selects all users of the specified group.</p>
		</div>
		<TableAutoCard
			title="Mappings for carioca users"
			class="text-left"
			definition={[
				['Project Name', 'min-w-52'],
				['Virtual Server', 'min-w-60'],
				['Authorization Group', 'min-w-60'],
				['User', 'min-w-60'],
				['Certificate / Store', 'min-w-52'],
				['Actions', 'w-20']
			].map(([name, cls]) => ({ name, class: cls, custom: true }))}
			data={exporting
				? carioca
				: certificates.length && projects.length
					? [...carioca, { last: true }]
					: carioca}
		>
			{#snippet children({ row, def })}
				{@render cell({ row, def, category: 'mappings' })}
			{/snippet}
		</TableAutoCard>
	</Card>
</div>
