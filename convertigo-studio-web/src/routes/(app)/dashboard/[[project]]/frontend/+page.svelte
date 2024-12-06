<!-- <script>
	import { page } from '$app/stores';
	import { checkTestPlatform, testPlatformStore } from '$lib/common/stores/testPlatform';
	import { onMount } from 'svelte';
	import { Segment } from '@skeletonlabs/skeleton-svelte';
	import 'react-device-frameset/styles/marvel-devices.min.css';
	import Icon from '@iconify/svelte';
	import { DeviceMockup } from 'svelte-device-mockups';
	import { getFrontendUrl } from '$lib/utils/service';

	let project;
	let _parts = [];

	let deviceVal = '2';
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
						name: connector.name,
						requestables: Object.values(connector.transaction || {})
					});
				}
				_parts = _parts.filter((part) => part.requestables.length > 0);
			});
		});
		return () => unsubscribe();
	});

	function openModalQrCode() {
		// modalStore.trigger({
		// 	type: 'component',
		// 	component: 'modalQrCode',
		// 	meta: {
		// 		href: getFrontendUrl($page.params.project)
		// 	}
		// });
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

	<div class="flex items-center justify-center gap-5">
		<button class="green-button" on:click={() => openModalQrCode()}>Qr Code</button>
		<Segment bind:value={deviceVal} classes="dark:bg-surface-800 bg-surface-100">
			<Segment.Item classes="justify" value="2"
				><Icon icon="fluent:phone-20-regular" class="dark:text-white text-black" /></Segment.Item
			>
			<Segment.Item classes="justify" value="1"
				><Icon icon="fluent:tablet-48-regular" class="dark:text-white text-black" /></Segment.Item
			>
			<Segment.Item classes="justify" value="0"
				><Icon icon="fluent:laptop-20-regular" class="dark:text-white text-black" /></Segment.Item
			>
		</Segment>
		<button class="basic-button" on:click={() => toggleLandscape()}>{buttonTextLandsape}</button>
		{#if deviceVal == '2'}
			<select on:change={handlePhoneTypeChange} class="select-common text-[12px]">
				{#each phones as phone}
					<option value={phone.name}>{phone.name}</option>
				{/each}
			</select>
		{/if}
	</div>
	<div class="flex flex-col items-center justify-start"> -->
<!-- {#if deviceVal >= 0 && deviceVal < devices.length}
			<DeviceMockup
				device={deviceVal == 2 ? selectedPhone.name : devices[deviceVal].name}
				scale={deviceVal == 2 ? selectedPhone.scale : devices[deviceVal].scale}
				{landscape}
				color={deviceVal == 2 ? selectedPhone.color : devices[deviceVal].color}
				src={getFrontendUrl(project['@_name'])}
			/>
		{/if} -->
<!-- </div>
{:else}
	Loading ...
{/if} -->
<!-- 
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
</style> -->
<script>
	import { onMount } from 'svelte';
	import { emulatedDevices } from '$lib/dashboard/bezels/emulatedDevices';
	import { assets } from '$app/paths';

	// Find default device
	let selectedDevice = emulatedDevices.find((device) => device.title === 'iPhone 12 Pro') ?? {
		title: 'Default Device',
		screen: {
			vertical: { width: 390, height: 844 },
			horizontal: { width: 844, height: 390 }
		},
		bezelVertical: `${assets}/default-bezel-vertical.png`,
		bezelHorizontal: `${assets}/default-bezel-horizontal.png`
	};

	let projectUrl = 'https://www.convertigo.com';
	let orientation = 'vertical';
	let scale = 1;

	function selectDevice(event) {
		const deviceTitle = event.target.value;
		selectedDevice =
			emulatedDevices.find((device) => device.title === deviceTitle) ?? selectedDevice;
		calculateScale();
	}

	function toggleOrientation() {
		orientation = orientation === 'vertical' ? 'horizontal' : 'vertical';
		calculateScale();
	}

	function calculateScale() {
		if (typeof window !== 'undefined') {
			const viewportWidth = window.innerWidth * 0.9;
			const viewportHeight = window.innerHeight * 0.9;

			const deviceWidth = selectedDevice.screen[orientation].width;
			const deviceHeight = selectedDevice.screen[orientation].height;

			scale = Math.min(viewportWidth / deviceWidth, viewportHeight / deviceHeight, 1);
		}
	}

	onMount(() => {
		calculateScale();
		window.addEventListener('resize', calculateScale);
		return () => window.removeEventListener('resize', calculateScale);
	});
</script>

<div class="container">
	<div class="controls">
		<label for="device-select">Choose a device:</label>
		<select id="device-select" on:change={selectDevice}>
			{#each emulatedDevices as device}
				<option value={device.title} selected={selectedDevice.title === device.title}>
					{device.title}
				</option>
			{/each}
		</select>

		<button class="toggle-button" on:click={toggleOrientation}>
			Toggle Orientation ({orientation === 'vertical' ? 'Portrait' : 'Landscape'})
		</button>
	</div>

	<div
		class="bezel-container"
		style="transform: scale({scale}); transform-origin: center;"
	>
		{#if orientation === 'vertical' && selectedDevice.bezelVertical}
			<img
				src="{assets}/{selectedDevice.bezelVertical}"
				alt={`${selectedDevice.title} Bezel`}
				class="bezel-image"
			/>
		{:else if selectedDevice.bezelHorizontal}
			<img
				src="{assets}/{selectedDevice.bezelHorizontal}"
				alt={`${selectedDevice.title} Bezel`}
				class="bezel-image"
			/>
		{/if}
		<iframe
			src={projectUrl}
			title={`${selectedDevice.title} Preview`}
			class="device-iframe no-scrollbar"
			style="
				width: {selectedDevice.screen[orientation].width}px;
				height: {selectedDevice.screen[orientation].height}px;
				padding: 15px"
		></iframe>
	</div>
</div>

<style>
.container {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin: 20px auto;
	gap: 20px;
	max-width: 100%;
}

.controls {
	display: flex;
	align-items: center;
	gap: 10px;
	flex-wrap: wrap;
	justify-content: center;
	margin-bottom: 10px;
}

#device-select {
	padding: 6px 12px;
	font-size: 1rem;
	border: 1px solid #ccc;
	border-radius: 4px;
	background-color: #f9f9f9;
	cursor: pointer;
	transition: all 0.2s ease-in-out;
}

#device-select:hover {
	background-color: #f0f0f0;
	border-color: #bbb;
}

.toggle-button {
	padding: 8px 15px;
	font-size: 0.9rem;
	background-color: #0078d4;
	color: white;
	border: none;
	border-radius: 5px;
	cursor: pointer;
	transition: all 0.3s ease-in-out;
}

.toggle-button:hover {
	background-color: #005bb5;
}

.bezel-container {
	position: relative;
	display: flex;
	justify-content: center;
	align-items: center;
	overflow: hidden;
	width: 100%;
	height: 100%;
}

.bezel-image {
	position: absolute;
	width: 100%;
	height: 100%;
	object-fit: contain;
	z-index: 99;
	pointer-events: none;
	transition: transform 0.3s ease-in-out; /* Smooth rotation */
}

.device-iframe {
	position: relative;
	border: none;
	z-index: 1;
	border-radius: 50px;
	-ms-overflow-style: none; /* For Internet Explorer and Edge */
	scrollbar-width: none; /* For Firefox */
}

/* For Webkit browsers like Chrome, Safari, and Opera */
.device-iframe::-webkit-scrollbar {
	display: none;
}
</style>