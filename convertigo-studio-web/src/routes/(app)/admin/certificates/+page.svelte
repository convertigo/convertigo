<script>
	import { FileUpload } from '@skeletonlabs/skeleton-svelte';
	import Certificates from '$lib/admin/Certificates.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
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
		init
	} = $derived(Certificates);
	let { projects } = $derived(Projects);

	onDestroy(() => {
		Projects.stop();
		Certificates.stop();
	});

	let modalCertInstall = $state();
	let modalCertRemove = $state();
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
				<FileUpload
					name="userfile"
					accept={{ 'application/x-pkcs12': ['.pfx', '.p12'], 'application/pkix-cert': ['.cer'] }}
					maxFiles={1}
					subtext="then press Install"
					classes="w-full preset-filled-surface-300-700"
					required
					allowDrop
				>
					{#snippet iconInterface()}<Ico icon="mdi:certificate" size="8" />{/snippet}
					{#snippet iconFile()}<Ico icon="mdi:briefcase-upload-outline" size="8" />{/snippet}
					{#snippet iconFileRemove()}<Ico icon="mdi:delete-outline" size="8" />{/snippet}
				</FileUpload>
				<div class="layout-x w-full justify-end">
					<Button
						label="Install"
						icon="mdi:certificate"
						type="submit"
						class="button-primary w-fit!"
					/>
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-error w-fit!"
						onclick={modalCertInstall.close}
					/>
				</div>
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
					item={Certificates.candidates.map(({ name }) => ({ value: name, text: name }))}
					value=""
				/>
				<div class="layout-x w-full justify-end">
					<Button
						label="Remove"
						icon="mdi:certificate"
						type="submit"
						class="button-primary w-fit!"
					/>
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-error w-fit!"
						onclick={modalCertRemove.close}
					/>
				</div>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

{#snippet cell({
	row: {
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
	},
	def
})}
	{#if loading}
		<AutoPlaceholder {loading} />
	{:else if def.name == 'Certificate / Store' && def.setup}
		{#if last}
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
		{#if last}
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
		<PropertyType
			name="virtualServer_0"
			type="text"
			value={virtualServerName}
			originalValue={virtualServerName}
		/>
	{:else if def.name == 'Authorization Group'}
		<PropertyType
			name="group_0"
			type="text"
			value={imputationGroup}
			originalValue={imputationGroup}
		/>
	{:else if def.name == 'User'}
		<PropertyType name="user_0" type="text" value={userName} originalValue={userName} />
	{:else if def.name == 'Certificate / Store'}
		<PropertyType
			name="cert_0"
			type="combo"
			item={certificates.map(({ name }) => ({ value: name, text: name }))}
			value={certificateName ?? certificates[0]?.name}
		/>
	{:else if def.name == 'Type'}
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
	{:else if def.name == 'Password'}
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
	{:else if def.name == 'Group'}
		<PropertyType
			name="group_0"
			type="text"
			value={group}
			originalValue={group}
			placeholder="Enter group value …"
		/>
	{:else if def.name === 'Actions'}
		<ResponsiveButtons
			buttons={[
				{
					icon: 'mdi:delete-outline',
					cls: 'button-ico-error',
					hidden: last,
					onclick: def.setup ? del : mappingsDel
				},
				{
					icon: 'mdi:update',
					cls: 'button-ico-success',
					onclick: def.setup ? configure : mappingsConfigure
				}
			]}
			size="6"
			class="w-full min-w-16"
		/>
	{/if}
{/snippet}

<div class="layout-y-stretch">
	<Card title="Certificates">
		{#snippet cornerOption()}
			<ResponsiveButtons
				class="max-w-lg"
				buttons={[
					{
						label: 'Install a new certificate',
						icon: 'mdi:certificate',
						cls: 'button-primary',
						onclick: modalCertInstall?.open
					},
					{
						label: 'Remove a certificate',
						icon: 'mdi:delete-outline',
						cls: 'button-error',
						disabled: !candidates.length,
						onclick: modalCertRemove?.open
					}
				]}
				disabled={!init}
			/>
		{/snippet}
		<div class="w-full">
			Usually, individual certificates authenticate clients, certificates stores authenticate
			servers.
		</div>
		<TableAutoCard
			class="text-left"
			definition={[
				['Actions', 'w-20'],
				['Certificate / Store', 'min-w-52'],
				['Type', 'min-w-48'],
				['Password', 'min-w-60'],
				['Group', 'min-w-60']
			].map(([name, cls]) => ({
				name,
				class: cls,
				custom: true,
				setup: true
			}))}
			data={candidates.length ? [...certificates, { last: true, setup: true }] : certificates}
		>
			{#snippet children({ row, def })}
				{@render cell({ row, def })}
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
				['Actions', 'w-20'],
				['Project Name', 'min-w-52'],
				['Certificate / Store', 'min-w-52']
			].map(([name, cls]) => ({
				name,
				class: cls,
				custom: true
			}))}
			data={certificates.length && projects.length ? [...anonymous, { last: true }] : anonymous}
		>
			{#snippet children({ row, def })}
				{@render cell({ row, def })}
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
				['Actions', 'w-20'],
				['Project Name', 'min-w-52'],
				['Virtual Server', 'min-w-60'],
				['Authorization Group', 'min-w-60'],
				['User', 'min-w-60'],
				['Certificate / Store', 'min-w-52']
			].map(([name, cls]) => ({ name, class: cls, custom: true }))}
			data={certificates.length && projects.length ? [...carioca, { last: true }] : carioca}
		>
			{#snippet children({ row, def })}
				{@render cell({ row, def })}
			{/snippet}
		</TableAutoCard>
	</Card>
</div>
