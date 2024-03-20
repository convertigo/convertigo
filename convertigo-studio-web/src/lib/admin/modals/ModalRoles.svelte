<script>
	import { RadioGroup, RadioItem, SlideToggle, getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import Icon from '@iconify/svelte';
	import ResponsiveContainer from '../components/ResponsiveContainer.svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;

	let viewRolesStore = writable([]);
	let configRolesStore = writable([]);
	let otherRolesStore = writable([]);

	let viewRolesChecked = writable(false);
	let configRolesChecked = writable(false);

	let importAction = '';
	let importPriority = 'priority-import';

	onMount(() => {
		rolesList();
	});

	function toggleViewRoles(shouldBeChecked) {
		viewRolesChecked.set(shouldBeChecked);
		//@ts-ignore
		viewRolesStore.update((roles) => roles.map((role) => ({ ...role, selected: shouldBeChecked })));
	}

	function toggleConfigRoles(shouldBeChecked) {
		configRolesChecked.set(shouldBeChecked);
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

	async function rolesAdd(event) {
		event.preventDefault();
		const fd = new FormData(event.target);

		//@ts-ignore
		const res = await call('roles.Add', fd);
		console.log('role add res:', res);
		rolesList();
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
	<Card title="Add User" style="padding: 40px;">
		<form on:submit={rolesAdd}>
			<div class="flex items-center gap-10 mb-10">
				<label class="border-common">
					<p class="label-common text-input">Name</p>
					<input class="input-common" type="text" name="username" placeholder="Enter name .." />
				</label>

				<label class="border-common">
					<p class="label-common">Password</p>
					<input
						class="input-common"
						type="password"
						name="password"
						placeholder="Enter password .."
					/>
				</label>
			</div>

			<ResponsiveContainer
				scrollable={true}
				smCols="sm:grid-cols-1"
				mdCols="md:grid-cols-3"
				lgCols="lg:grid-cols-3"
			>
				<div class="container-child">
					<h1 class="mb-5 font-bold text-xl">View Roles</h1>
					<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-2 gap-5">
						{#each $viewRolesStore as view}
							<div class="flex items-center">
								<SlideToggle
									size="sm"
									name="roles"
									active="bg-success-400 dark:bg-success-700"
									background="bg-error-400 dark:bg-error-700"
									value={view.selected ? view['@_name'] : ''}
									checked={view.selected}
								>
									<span class="cursor-pointer">{view['@_name'].replace('_VIEW', '')}</span>
								</SlideToggle>
							</div>
						{/each}
					</div>

					<RadioGroup class="mt-10 max-w-80">
						<RadioItem
							bind:group={$viewRolesChecked}
							on:click={() => toggleViewRoles(true)}
							name="viewRoles"
							value={true}
							active="variant-filled-secondary"
						>
							<Icon icon="mdi:plus" class="w-6 h-6" />
						</RadioItem>
						<RadioItem
							bind:group={$viewRolesChecked}
							on:click={() => toggleViewRoles(false)}
							name="viewRoles"
							value={false}
							active="variant-filled-surface"
						>
							<Icon icon="mdi:minus" class="w-6 h-6" />
						</RadioItem>
					</RadioGroup>
				</div>

				<div class="container-child">
					<h1 class="mb-5 font-bold text-xl">Config Roles</h1>
					<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-2 gap-5">
						{#each $configRolesStore as config}
							<div class="flex items-center">
								<SlideToggle
									size="sm"
									name="roles"
									active="bg-success-400 dark:bg-success-700"
									background="bg-error-400 dark:bg-error-700"
									value={config.selected ? config['@_name'] : ''}
									checked={config.selected}
								>
									<span class="cursor-pointer">{config['@_name'].replace('_CONFIG', '')}</span>
								</SlideToggle>
							</div>
						{/each}
					</div>

					<RadioGroup class="mt-10 flex max-w-80">
						<RadioItem
							bind:group={$configRolesChecked}
							on:click={() => toggleConfigRoles(true)}
							name="configRoles"
							value={true}
							active="variant-filled-secondary"
						>
							<Icon icon="mdi:plus" class="w-6 h-6" />
						</RadioItem>
						<RadioItem
							bind:group={$configRolesChecked}
							on:click={() => toggleConfigRoles(false)}
							name="configRoles"
							value={false}
							active="variant-filled-surface"
						>
							<Icon icon="mdi:minus" class="w-6 h-6" />
						</RadioItem>
					</RadioGroup>
				</div>

				<div class="container-child">
					<h1 class="mb-5 font-bold text-xl">Other Roles</h1>
					<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-2 gap-5">
						{#each $otherRolesStore as other}
							<div class="flex items-center">
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
							</div>
						{/each}
					</div>
				</div>
			</ResponsiveContainer>

			<div class="flex gap-5 mt-10">
				<button class="cancel-button w-60" on:click={() => modalStore.close()}>Cancel</button>
				<button type="submit" class="btn confirm-button w-60">Confirm</button>
			</div>
		</form>
	</Card>
{/if}

{#if mode == 'import'}
	<Card>
		<form class="p-5 rounded-xl glass flex flex-col">
			<h1 class="text-xl mb-5 text-center">Import users</h1>
			<RadioGroup active="bg-secondary-400-500-token">
				<RadioItem bind:group={importAction} name="action-import" value="clear-import"
					>Clear & import</RadioItem
				>
				<RadioItem bind:group={importAction} name="action-import" value="">Merge users</RadioItem>
			</RadioGroup>
			{#if importAction == ''}
				<p class="mt-10 text-[14px] mb-5 text-center">In case of name conflict</p>
				<RadioGroup active="bg-secondary-400-500-token">
					<RadioItem bind:group={importPriority} name="priority" value="priority-server"
						>Priority Server</RadioItem
					>
					<RadioItem bind:group={importPriority} name="priority" value="priority-import"
						>Priority import</RadioItem
					>
				</RadioGroup>
			{/if}
			<p class="font-medium mt-10">Actual users list will be saved aside in a backup file.</p>

			<div class="flex flex-wrap gap-5">
				<div class="flex-1">
					<button class="mt-5 w-full cancel-button" on:click={() => modalStore.close()}
						>Cancel</button
					>
				</div>
				<div class="flex-1">
					<input
						type="file"
						name="userfile"
						id="symbolUploadFile"
						accept=".properties"
						class="hidden"
						on:change={importRoles}
					/>
					<label for="symbolUploadFile" class="confirm-button btn mt-5 w-full">Import</label>
				</div>
			</div>
		</form>
	</Card>
{/if}

{#if mode == 'export'}
	export user
{/if}

{#if mode == 'delete all'}
	Delete all
{/if}

<style lang="postcss">
	.container-child {
		@apply flex flex-wrap flex-col;
	}
</style>
