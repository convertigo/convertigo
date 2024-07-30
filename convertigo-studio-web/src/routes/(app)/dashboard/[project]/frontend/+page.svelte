<script>
	import { page } from '$app/stores';
	import { checkTestPlatform, testPlatformStore } from '$lib/common/stores/testPlatform';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import 'react-device-frameset/styles/marvel-devices.min.css';
	import Icon from '@iconify/svelte';
	import { DeviceMockup } from 'svelte-device-mockups';
	import { updateUrls, appUrlStore, qrCodeUrlStore } from '$lib/common/stores/urlStore';

	const modalStore = getModalStore();
	let project;
	let _parts = [];
	let searchQuery = '';

	let deviceVal = 0;
	let landscape;
	let selectedPhoneType = 'iPhone X';

	const bgColors = [
		'bg-pale-violet border-[1px] border-pale-violet',
		'bg-pale-blue border-[1px] border-pale-blue',
		'bg-pale-green border-[1px] border-pale-green',
		'bg-pale-pink border-[1px] border-pale-pink'
	];
	const devices = [
		{ name: 'MacBook Pro', color: 'gold', scale: 5 },
		{ name: 'iPad', color: 'gold', scale: 5 },
		{ name: 'iPhone X', color: 'gold', scale: 5 },
		{ name: 'iPhone 5s', color: 'silver', scale: 5 },
		{ name: 'Nexus 5', color: 'yellow', scale: 5 },
		{ name: 'Samsung Galaxy S5', color: 'silver', scale: 5 },
		{ name: 'HTC one', color: 'yellow', scale: 5 },
		{ name: 'Lumia 920', color: 'yellow', scale: 5 }
	];

	const phones = [
		{ name: 'iPhone X', color: 'gold', scale: 5 },
		{ name: 'iPhone 5c', color: 'yellow', scale: 5 },
		{ name: 'iPhone 5s', color: 'silver', scale: 5 },
		{ name: 'Nexus 5', color: 'yellow', scale: 5 },
		{ name: 'Samsung Galaxy S5', color: 'silver', scale: 5 },
		{ name: 'HTC one', color: 'yellow', scale: 5 },
		{ name: 'Lumia 920', color: 'red', scale: 5 }
	];

	function toggleLandscape() {
		landscape = !landscape;
	}

	function handlePhoneTypeChange(event) {
		selectedPhoneType = event.target.value;
	}

	onMount(() => {
		const unsubscribe = page.subscribe(($page) => {
			const projectName = $page.params.project;
			checkTestPlatform(projectName).then(() => {
				project = $testPlatformStore[projectName];
				_parts = [{ name: 'Sequences', requestables: Object.values(project.sequence || {}) }];
				for (let connector of Object.values(project.connector || {})) {
					_parts.push({
						name: connector['@_name'],
						requestables: Object.values(connector.transaction || {})
					});
				}
				_parts = _parts.filter((part) => part.requestables.length > 0);
				updateUrls(projectName);
			});
		});
		return () => unsubscribe();
	});

	function openModalQrCode() {
		const projectName = $page.params.project;
		updateUrls(projectName);
		modalStore.trigger({
			type: 'component',
			component: 'modalQrCode',
			meta: {
				qrCodeUrl: $qrCodeUrlStore
			}
		});
	}

	$: selectedPhone = phones.find((phone) => phone.name === selectedPhoneType) || {
		name: 'iPhone X',
		color: 'gold',
		scale: 5
	};

	$: buttonTextLandsape = landscape ? 'Portrait' : 'Landscape';
</script>

{#if project}
	{@const [duration, y, opacity] = [200, -50, 1]}

	<div class="grid grid-cols-3">
		<div class="col-span-1 flex items-center justify-center gap-5">
			<button class="green-button" on:click={() => openModalQrCode()}>Qr Code</button>
			<RadioGroup active="dark:bg-surface-800 bg-surface-100">
				<RadioItem bind:group={deviceVal} name="justify" value={0}
					><Icon icon="fluent:laptop-20-regular" class="dark:text-white text-black" /></RadioItem
				>
				<RadioItem bind:group={deviceVal} name="justify" value={1}
					><Icon icon="fluent:tablet-48-regular" class="dark:text-white text-black" /></RadioItem
				>
				<RadioItem bind:group={deviceVal} name="justify" value={2}
					><Icon icon="fluent:phone-20-regular" class="dark:text-white text-black" /></RadioItem
				>
			</RadioGroup>
			<button class="basic-button" on:click={() => toggleLandscape()}>{buttonTextLandsape}</button>
			{#if deviceVal == 2}
				<select on:change={handlePhoneTypeChange} class="select-common text-[12px]">
					{#each phones as phone}
						<option value={phone.name}><span>{phone.name}</span></option>
					{/each}
				</select>
			{/if}
		</div>
		<div class="col-span-1 flex items-center justify-end gap-2"></div>
	</div>
	<div class="flex flex-col items-center justify-start mt-5">
		{#if deviceVal >= 0 && deviceVal < devices.length}
			<DeviceMockup
				device={deviceVal == 2 ? selectedPhone.name : devices[deviceVal].name}
				scale={deviceVal == 2 ? selectedPhone.scale : devices[deviceVal].scale}
				{landscape}
				color={deviceVal == 2 ? selectedPhone.color : devices[deviceVal].color}
				src={$appUrlStore}
			/>
		{/if}
	</div>
{:else}
	Loading ...
{/if}

<style lang="postcss">
	:global(.accordion-summary) {
		@apply overflow-hidden;
	}
	.sticky {
		position: -webkit-sticky; /* For Safari */
		position: sticky;
		top: 40px;
		height: calc(100vh - 40px); /* Adjust height to account for the top offset */
		overflow-y: auto; /* Allow scrolling if content overflows */
	}
	.qr-code {
		width: 200px; /* Adjust the size as needed */
		height: 200px; /* Adjust the size as needed */
	}

	.marked-p-class {
		color: aqua;
	}
</style>
