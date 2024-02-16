<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../admin-components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import Icon from '@iconify/svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;

	let viewRolesStore = writable([]);
	let configRolesStore = writable([]);

	let viewRolesChecked = false;
	let configRolesChecked = false;

	function toggleViewRoles(shouldBeChecked) {
		viewRolesChecked = shouldBeChecked;
	}

	function toggleConfigRoles(shouldBeChecked) {
		configRolesChecked = shouldBeChecked;
	}

	onMount(() => {
		rolesList();
	});

	async function rolesList() {
		const res = await call('roles.List');
		console.log('roles List', res);

		const roleArray = res?.admin?.roles?.role;
		if (roleArray) {
			const viewRoles = roleArray.filter((role) => role['@_name'].endsWith('_VIEW'));
			const configRoles = roleArray.filter((role) => role['@_name'].endsWith('_CONFIG'));

			viewRolesStore.set(viewRoles);
			configRolesStore.set(configRoles);
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

			<p class="text-[16px] font-medium mt-10 mb-5">View Roles:</p>
			<div class="grid grid-cols-5 gap-5">
				{#each $viewRolesStore as role}
					<label class="items-center flex">
						<input
							type="checkbox"
							bind:checked={viewRolesChecked}
							value={role['@_name']}
							name="roles"
						/>
						<p class="p-1 ml-2 font-normal">{role['@_name']}</p>
					</label>
				{/each}
			</div>

			<p class="text-[16px] font-medium mt-10 mb-5">Config Roles</p>
			<div class="grid grid-cols-5 gap-5">
				{#each $configRolesStore as role}
					<label class="items-center flex">
						<input
							type="checkbox"
							bind:checked={configRolesChecked}
							value={role['@_name']}
							name="roles"
						/>
						<p class="p-1 ml-2 font-normal">{role['@_name']}</p>
					</label>
				{/each}
			</div>

			<div class="flex gap-5 mt-10">
				<button
					type="button"
					class="btn bg-buttons text-white"
					on:click={() => toggleViewRoles(true)}
				>
					<Icon icon="ph:plus-fill" class="w-7 h-7 mr-3" />
					Check view</button
				>
				<button
					type="button"
					class="btn bg-buttons text-white"
					on:click={() => toggleViewRoles(false)}
				>
					<Icon icon="typcn:minus-outline" class="w-7 h-7 mr-3" />
					Uncheck View
				</button>

				<button
					type="button"
					class="btn bg-buttons text-white"
					on:click={() => toggleConfigRoles(true)}
					><Icon icon="ph:plus-fill" class="w-7 h-7 mr-3" />Check Config</button
				>
				<button
					type="button"
					class="btn bg-buttons text-white"
					on:click={() => toggleConfigRoles(false)}
					><Icon icon="typcn:minus-outline" class="w-7 h-7 mr-3" />Uncheck Config</button
				>
			</div>

			<div class="flex gap-10 mt-10">
				<button type="submit" class="btn bg-buttons w-40 text-white">Confirm</button>

				<button class="btn bg-buttons w-40 text-white" on:click={() => modalStore.close()}
					>Cancel</button
				>
			</div>
		</form>
	</Card>
{/if}

{#if mode == 'import'}
	import user
{/if}

{#if mode == 'export'}
	export user
{/if}

{#if mode == 'delete all'}
	Delete all
{/if}

<style lang="postcss">
	/**style for label*/
	.label-common {
		@apply text-[14px] cursor-pointer;
	}
	/**Style for Input*/
	.input-common {
		@apply placeholder:text-[16px] placeholder:dark:text-surface-400 placeholder:text-surface-200 dark:text-surface-100 text-surface-800 placeholder:font-light font-normal border-none dark:bg-surface-800 w-full;
		border-bottom: surface-200;
	}

	.input-text {
		@apply mt-1 pl-4 text-[16px] dark:text-surface-200 text-surface-600;
	}

	/**Style for checkbox*/
	.checkbox-common {
		@apply cursor-pointer;
	}

	.border-common {
		@apply border-b-[1px] dark:border-surface-600 border-surface-100;
	}
</style>
