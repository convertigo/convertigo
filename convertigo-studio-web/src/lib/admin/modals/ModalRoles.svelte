<script>
	import { FileDropzone, RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import Icon from '@iconify/svelte';
	import ResponsiveContainer from '../components/ResponsiveContainer.svelte';
	import { usersList, rolesStore } from '../stores/rolesStore';
	import CheckState from '../components/CheckState.svelte';
	import ModalButtons from '../components/ModalButtons.svelte';

	const modalStore = getModalStore();
	const { mode, row } = $modalStore[0].meta ?? {};

	/** @type {{parent: any}} */
	let { parent } = $props();

	let importAction = $state('');
	let importPriority = $state('priority-import');

	onMount(() => {
		if (mode == 'add' && row?.role) {
			for (let part of $rolesStore) {
				for (let role of part.roles) {
					role.checked = row.role.includes(role.value);
				}
				part.toggle = part.roles.findIndex((role) => !role.checked) == -1;
			}
		}
	});

	function toggleRoles(checked, roles) {
		roles.forEach((role) => (role.checked = checked));
	}

	async function rolesAdd(event) {
		event.preventDefault();
		const fd = new FormData(event.target);

		//@ts-ignore
		const res = await call(`roles.${row ? 'Edit' : 'Add'}`, fd);
		if (res?.admin?.response?.['@_state'] == 'success') {
			usersList();
			modalStore.close();
		}
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
		} catch (err) {
			console.error(err);
		}
	}
</script>

{#if mode == 'add'}
	<Card title="{row ? 'Edit' : 'Add'} User" class="p-10">
		<form onsubmit={rolesAdd}>
			{#if row}
				<input type="hidden" name="oldUsername" value={row.name} />
			{/if}
			<div class="flex items-center gap-10 mb-10">
				<label class="border-common">
					<p class="label-common text-input">Name</p>
					<input
						class="input-common"
						type="text"
						name="username"
						placeholder="Enter name …"
						value={row?.name ?? ''}
					/>
				</label>

				<label class="border-common">
					<p class="label-common">Password</p>
					<input
						class="input-common"
						type="password"
						name="password"
						placeholder={row ? 'Leave blank for no change…' : 'Enter password …'}
						value=""
					/>
				</label>
			</div>

			<ResponsiveContainer
				scrollable={true}
				smCols="sm:grid-cols-1"
				mdCols="md:grid-cols-3"
				lgCols="lg:grid-cols-3"
			>
				{#each $rolesStore as { name, end, roles }, i}
					<div class="container-child">
						<div class="flex items-center gap-5">
							<h1 class="font-bold text-xl">{name}</h1>
							<RadioGroup class="" background="bg-surface-50 dark:bg-surface-700">
								{@const radioDef = [
									{ value: false, active: 'preset-filled-error', icon: 'mdi:minus' },
									{ value: true, active: 'preset-filled-secondary', icon: 'mdi:plus' }
								]}
								{#each radioDef as { value, active, icon }}
									<RadioItem
										bind:group={$rolesStore[i].toggle}
										on:click={() => toggleRoles(value, roles)}
										name="viewRoles"
										{value}
										{active}
									>
										<Icon {icon} class="w-4 h-4" />
									</RadioItem>
								{/each}
							</RadioGroup>
						</div>
						<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-2 gap-5 mt-10">
							{#each roles as { value, checked, description }}
								<div class="flex items-center" title={description}>
									<CheckState name="roles" {value} {checked}>
										{value.replace(end, '').replace(/_/g, ' ')}
									</CheckState>
								</div>
							{/each}
						</div>
					</div>
				{/each}
			</ResponsiveContainer>

			<ModalButtons />
		</form>
	</Card>
{/if}

{#if mode == 'import'}
	<Card title="Import Users">
		<form class=" flex flex-col">
			<RadioGroup active="dark:bg-primary-800 bg-primary-400" class="font-normal">
				<RadioItem bind:group={importAction} name="action-import" value="clear-import"
					><p class="text-[12px]">Clear & import</p></RadioItem
				>
				<RadioItem bind:group={importAction} name="action-import" value="">
					<p class="text-[12px]">Merge users</p>
				</RadioItem>
			</RadioGroup>
			{#if importAction == ''}
				<p class="mt-2 text-[11px] mb-2 font-normal">In case of name conflict :</p>
				<RadioGroup active="dark:bg-primary-800 bg-primary-400" class="font-normal">
					<RadioItem bind:group={importPriority} name="priority" value="priority-server"
						><p class="text-[12px]">Priority Server</p>
					</RadioItem>
					<RadioItem bind:group={importPriority} name="priority" value="priority-import"
						><p class="text-[12px]">Priority ServerPriority import</p>
					</RadioItem>
				</RadioGroup>
			{/if}
			<p class="font-normal text-[11px] mb-1 mt-5">
				Actual users list will be saved aside in a backup file.
			</p>

			<FileDropzone name="fileinput" id="fileinput" accept=".properties" on:change={importRoles}>
				<svelte:fragment slot="message"
					><div class="flex flex-col items-center">
						<Icon icon="icon-park:application-one" class="w-10 h-10" />Upload Users here or drag and
						drop
					</div></svelte:fragment
				>
				<svelte:fragment slot="meta">.properties files</svelte:fragment>
			</FileDropzone>
			<!-- <button class="mt-5 btn cancel-button w-full font-light" on:click={() => modalStore.close()}
				>Cancel</button
			> -->
			<!-- <div class="flex flex-wrap gap-5">
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
					<label for="symbolUploadFile" class="input-button w-60">Import</label>
				</div>
			</div> -->
			<ModalButtons showConfirmBtn={false} />
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
