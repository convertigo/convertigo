<script>
	import { RadioGroup, RadioItem, SlideToggle, getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import Icon from '@iconify/svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;

	let viewRolesStore = writable([]);
	let configRolesStore = writable([]);
	let otherRolesStore = writable([]);

	let viewRolesChecked = false;
	let configRolesChecked = false;

	let importAction = '';
	let importPriority = 'priority-import';

	onMount(() => {
		rolesList();
	});

	function toggleViewRoles(shouldBeChecked) {
		viewRolesChecked = shouldBeChecked;
		//@ts-ignore
		viewRolesStore.update((roles) => roles.map((role) => ({ ...role, selected: shouldBeChecked })));
	}

	function toggleConfigRoles(shouldBeChecked) {
		configRolesChecked = shouldBeChecked;
		//@ts-ignore
		configRolesStore.update((roles) =>
			//@ts-ignore
			roles.map((role) => ({ ...role, selected: shouldBeChecked }))
		);
	}

	async function rolesList() {
		const res = await call('roles.List');
		console.log('roles List', res);

		const roleArray = res?.admin?.roles?.role;
		if (roleArray) {
			const viewRoles = roleArray
				.filter((role) => role['@_name'].endsWith('_VIEW'))
				.map((role) => ({ ...role, selected: false }));
			const configRoles = roleArray
				.filter((role) => role['@_name'].endsWith('_CONFIG'))
				.map((role) => ({ ...role, selected: false }));
			const otherRoles = roleArray
				.filter((role) => !role['@_name'].endsWith('_VIEW') && !role['@_name'].endsWith('_CONFIG'))
				.map((role) => ({ ...role, selected: false }));

			viewRolesStore.set(viewRoles);
			configRolesStore.set(configRoles);
			otherRolesStore.set(otherRoles);
		}
	}

	function toggleRoleSelection(roleName, roleType) {
		const store = roleType === 'view' ? viewRolesStore : configRolesStore;
		//@ts-ignore
		store.update((roles) =>
			//@ts-ignore
			roles.map((role) =>
				//@ts-ignore
				role['@_name'] === roleName ? { ...role, selected: !role.selected } : role
			)
		);
	}

	async function rolesAdd(event) {
		event.preventDefault();
		const fd = new FormData(event.target);

		//@ts-ignore
		const res = await call('roles.Add', fd);
		console.log('role add res:', res);
		modalStore.close();
	}

	/**
	 * @param {Event} e
	 */

	async function importRoles(e) {
		//@ts-ignore
		const fd = new FormData(e.target.form);
		try {
			//@ts-ignore
			const response = await call('roles.Import', fd);
			console.log(response);
		} catch (err) {
			console.error(err);
		}
	}
</script>

{#if mode == 'add'}
	<Card title="Add user" customStyle="padding: 40px;">
		<form on:submit={rolesAdd}>
			<div class="flex items-center gap-10 mb-10">
				<label class="border-common">
					<p class="label-common text-input">Name :</p>
					<input class="input-common" type="text" name="username" placeholder="Enter name .." />
				</label>

				<label class="border-common">
					<p class="label-common">Password :</p>
					<input
						class="input-common"
						type="password"
						name="password"
						placeholder="Enter password .."
					/>
				</label>
			</div>

			<!--	<p class="text-[16px] font-medium mt-10 mb-5">View Roles:</p>
			<div class="grid grid-cols-6 gap-5">
				{#each $viewRolesStore as view}
		
					<div class="flex">
						<SlideToggle
							active="bg-tertiary-500"
							name="roles"
							bind:checked={view.selected}
							on:click={() => toggleRoleSelection(view['@_name'], 'view')}
							value={view['@_name']}
						/>
						<p class="p-1 ml-2 font-normal">{view['@_name']}</p>
					</div>-->
			<!--
					<label class="flex items-center">
						<input
							type="checkbox"
							bind:checked={view.selected}
							name="roles"
							value={view['@_name']}
						/>
						<p class="p-1 ml-2 font-normal">{view['@_name']}</p>
					</label>
				{/each}
			</div>-->

			<!--	<p class="text-[16px] font-medium mt-10 mb-5">Config Roles</p>
			<div class="grid grid-cols-6 gap-5">
				{#each $configRolesStore as config}

					<label class="flex items-center">
						<input
							type="checkbox"
							bind:checked={config.selected}
							name="roles"
							value={config['@_name']}
						/>
						<p class="p-1 ml-2 font-normal">{config['@_name']}</p>
					</label>
				{/each}
			</div>-->

			<!--	<p class="text-[16px] font-medium mt-10 mb-5">Other Roles:</p>
			<div class="grid grid-cols-6 gap-5">
				{#each $otherRolesStore as other}
				
					<label class="flex items-center">
						<input
							type="checkbox"
							bind:checked={other.selected}
							name="roles"
							value={other['@_name']}
						/>
						<p class="p-1 ml-2 font-normal">{other['@_name']}</p>
					</label>

				{/each}
			</div>-->

			<div class="grid grid-cols-3 gap-10">
				<Card title="View Roles :">
					<div class="grid grid-cols-2 gap-5">
						{#each $viewRolesStore as view}
							<SlideToggle
								size="sm"
								name="roles"
								active="bg-success-400 dark:bg-success-700"
								background="bg-error-400 dark:bg-error-700"
								value={view.selected ? view['@_name'] : ''}
								checked={view.selected}
							>
								<span class="cursor-pointer">{view['@_name']}</span>
							</SlideToggle>
						{/each}
					</div>
					<div class="flex gap-5 mt-10">
						<button type="button" class="" on:click={() => toggleViewRoles(true)}>
							<Icon icon="ph:plus-fill" class="w-7 h-7 mr-3" />
							Check view</button
						>
						<button type="button" class="" on:click={() => toggleViewRoles(false)}>
							<Icon icon="typcn:minus-outline" class="w-7 h-7 mr-3" />
							Uncheck View
						</button>
					</div>
				</Card>

				<Card title="Config Roles :">
					<div class="grid grid-cols-2 gap-5">
						{#each $configRolesStore as config}
							<SlideToggle
								size="sm"
								name="roles"
								active="bg-success-400 dark:bg-success-700"
								background="bg-error-400 dark:bg-error-700"
								value={config.selected ? config['@_name'] : ''}
								checked={config.selected}
							>
								<span class="cursor-pointer">{config['@_name']}</span>
							</SlideToggle>
						{/each}
					</div>

					<div class="flex gap-5 mt-10">
						<button type="button" class="" on:click={() => toggleConfigRoles(true)}
							><Icon icon="ph:plus-fill" class="w-7 h-7 mr-3" />Check Config</button
						>
						<button type="button" class="" on:click={() => toggleConfigRoles(false)}
							><Icon icon="typcn:minus-outline" class="w-7 h-7 mr-3" />Uncheck Config</button
						>
					</div>
				</Card>

				<Card title="Other Roles">
					<div class="flex flex-col gap-5">
						{#each $otherRolesStore as other}
							<SlideToggle
								size="sm"
								name="roles"
								active="bg-success-400 dark:bg-success-700"
								background="bg-error-400 dark:bg-error-700"
								value={other.selected ? other['@_name'] : ''}
								checked={other.selected}
							>
								<span class="cursor-pointer">{other['@_name']}</span>
							</SlideToggle>
						{/each}
					</div>
				</Card>
			</div>

			<div class="flex gap-10 mt-10">
				<button type="submit" class="btn variant-filled-primary text-white">Confirm</button>

				<button class="variant-filled-error" on:click={() => modalStore.close()}>Cancel</button>
			</div>
		</form>
	</Card>
{/if}

{#if mode == 'import'}
	<Card>
		<form class="p-5 rounded-xl glass flex flex-col">
			<h1 class="text-xl mb-5 text-center">Import users</h1>
			<RadioGroup>
				<RadioItem bind:group={importAction} name="action-import" value="clear-import"
					>Clear & import</RadioItem
				>
				<RadioItem bind:group={importAction} name="action-import" value="">Merge users</RadioItem>
			</RadioGroup>
			{#if importAction == ''}
				<p class="mt-10 text-[14px] mb-5 text-center">In case of name conflict :</p>
				<RadioGroup>
					<RadioItem bind:group={importPriority} name="priority" value="priority-server"
						>Priority Server</RadioItem
					>
					<RadioItem bind:group={importPriority} name="priority" value="priority-import"
						>Priority import</RadioItem
					>
				</RadioGroup>
			{/if}
			<p class="font-medium mt-10">Actual users list will be saved aside in a backup file.</p>
			<input
				type="file"
				name="userfile"
				id="symbolUploadFile"
				accept=".properties"
				class="hidden"
				on:change={importRoles}
			/>
			<label for="symbolUploadFile" class="btn variant-filled mt-5">Import</label>
			<button class="mt-5 btn bg-white text-black font-light" on:click={() => modalStore.close()}
				>Cancel</button
			>
		</form>
	</Card>
{/if}

{#if mode == 'export'}
	export user
{/if}

{#if mode == 'delete all'}
	Delete all
{/if}
